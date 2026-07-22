package net.qs.swfht.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.flow.first
import net.qs.swfht.DayState
import net.qs.swfht.data.WorkDataStore
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.*

class LocationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val store = WorkDataStore(applicationContext)
        val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Check for necessary permissions
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure()
        }

        // Try to see if we are currently connected to the target wifi or if it's in scan results
        val wifiSsid = store.wifiSsid.first()
        val connectionInfo = wifiManager.connectionInfo
        val currentSsid = connectionInfo.ssid.removeSurrounding("\"")
        
        val isAtWorkWifi = if (currentSsid == wifiSsid) {
            true
        } else {
            val scanResults = try { wifiManager.scanResults } catch (e: Exception) { emptyList() }
            scanResults.any { it.SSID.removeSurrounding("\"") == wifiSsid }
        }

        if (!isAtWorkWifi) {
            return Result.success()
        }

        // Found wifi, now check GPS
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(applicationContext)
        val locationTask = fusedLocationClient.lastLocation
        val currentLocation: Location? = try {
            Tasks.await(locationTask)
        } catch (e: Exception) {
            null
        }

        if (currentLocation == null) return Result.success() // Can't get location, maybe next time

        val officeLocations = store.officeLocations.first()
        val match = officeLocations.find { office ->
            calculateDistance(currentLocation.latitude, currentLocation.longitude, office.lat, office.lng) <= 200
        }

        if (match != null) {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val currentMap = store.workMap.first()
            val currentState = currentMap[today] ?: DayState()
            
            if (currentState.actual != match.type) {
                store.save(today, currentState.copy(actual = match.type))
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
