package net.qs.inoffice.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.qs.inoffice.DayState
import net.qs.inoffice.WorkLocation
import net.qs.inoffice.data.WorkDataStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.*

class LocationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = WorkDataStore(applicationContext)

        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectivityManager = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        // Check for necessary permissions
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // Background location is missing. Detection will likely fail.
                // We proceed just in case, but this is a common failure point.
            }
        }

        // Try to see if we are currently connected to the target wifi
        val currentSsid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val activeNetwork = connectivityManager.activeNetwork
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            val transportInfo = capabilities?.transportInfo
            if (transportInfo is WifiInfo) {
                val ssid = transportInfo.ssid?.removeSurrounding("\"")
                if (ssid == "<unknown ssid>") null else ssid
            } else {
                null
            }
        } else {
            @Suppress("DEPRECATION")
            wifiManager.connectionInfo?.ssid?.removeSurrounding("\"")
        }

        val wifiSsid = store.wifiSsid.first()
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val currentMap = store.workMap.first()
        val currentState = currentMap[today] ?: DayState()
        
        // Check if currently connected or if it's in scan results
        val isAtWorkWifi = if (currentSsid == wifiSsid) {
            true
        } else {
            val scanResults = try { wifiManager.scanResults } catch (e: Exception) { emptyList() }
            scanResults.any { result ->
                val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    result.wifiSsid?.toString()?.removeSurrounding("\"")
                } else {
                    result.SSID?.removeSurrounding("\"")
                }
                ssid == wifiSsid
            }
        }

        if (!isAtWorkWifi) {
            // Not at work wifi, clear current location label if it exists
            if (!currentState.locationName.isNullOrEmpty()) {
                store.save(today, currentState.copy(locationName = null))
            }
            return Result.success()
        }

        // Found wifi, now check GPS
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        val cts = CancellationTokenSource()
        val locationTask = fusedLocationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
        
        val currentLocation: Location? = try {
            Tasks.await(locationTask)
        } catch (e: Exception) {
            null
        }

        if (currentLocation == null) return Result.success() // Can't get location, maybe next time

        val officeLocations = store.officeLocations.first()
        val match = officeLocations.find { office ->
            calculateDistance(currentLocation.latitude, currentLocation.longitude, office.lat, office.lng) <= 50
        }

        if (match != null) {
            // Only toast if location changed or was empty
            if (currentState.locationName != match.name) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "Detected you are in location ${match.name}", Toast.LENGTH_SHORT).show()
                }
            }
            
            // Only update 'actual' if it's currently HOME or LEAVE.
            // This ensures once an office state is set, it remains as is for the day.
            val newActual = if (currentState.actual == WorkLocation.HOME || currentState.actual == WorkLocation.LEAVE) {
                match.type
            } else {
                currentState.actual
            }

            if (newActual != currentState.actual || currentState.locationName != match.name) {
                store.save(today, currentState.copy(actual = newActual, locationName = match.name))
            }
        } else {
            // At work wifi but no office match, clear location name
            if (!currentState.locationName.isNullOrEmpty()) {
                store.save(today, currentState.copy(locationName = null))
            }
        }

        return Result.success()
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth radius in meters
        val phi1 = lat1 * PI / 180
        val phi2 = lat2 * PI / 180
        val dPhi = (lat2 - lat1) * PI / 180
        val dLambda = (lon2 - lon1) * PI / 180

        val a = sin(dPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(dLambda / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }
}
