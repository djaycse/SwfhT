package net.qs.swfht.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.qs.swfht.DayState
import net.qs.swfht.OfficeLocation
import net.qs.swfht.WorkLocation

// DataStore instance
val Context.dataStore by preferencesDataStore(name = "swfht_store")

class WorkDataStore(private val context: Context) {

    companion object {
        private val KEY_DATA = stringPreferencesKey("work_map")
        private val KEY_OFFICE_LOCATIONS = stringPreferencesKey("office_locations")
        private val KEY_WIFI_SSID = stringPreferencesKey("wifi_ssid")
        private val KEY_POLL_INTERVAL = stringPreferencesKey("poll_interval")
    }

    // Read full map
    val workMap: Flow<Map<String, DayState>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[KEY_DATA] ?: ""
            if (raw.isEmpty()) return@map emptyMap()

            raw.split("|")
                .filter { it.contains(":") }
                .associate { entry ->
                    val parts = entry.split(":")
                    val rawDate = parts[0]
                    val date = try {
                        val dateParts = rawDate.split("-")
                        if (dateParts.size == 3) {
                            "%d-%02d-%02d".format(
                                dateParts[0].toInt(),
                                dateParts[1].toInt(),
                                dateParts[2].toInt()
                            )
                        } else rawDate
                    } catch (e: Exception) {
                        rawDate
                    }
                    val values = parts[1].split(",")
                    
                    val plannedStr = values[0]
                    val actualStr = if (values.size > 1) values[1] else values[0]
                    val locationName = if (values.size > 2) values[2].ifEmpty { null } else null

                    val planned = try { WorkLocation.valueOf(plannedStr) } catch(e: Exception) { WorkLocation.HOME }
                    val actual = try { WorkLocation.valueOf(actualStr) } catch(e: Exception) { WorkLocation.HOME }
                    
                    date to DayState(planned, actual, locationName)
                }
        }

    // Save one entry
    suspend fun save(date: String, state: DayState) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_DATA] ?: ""

            // Normalize all existing entries and filter out the one being saved
            val map = current.split("|")
                .filter { it.contains(":") }
                .mapNotNull { entry ->
                    val parts = entry.split(":")
                    val rawDate = parts[0]
                    val normalizedDate = try {
                        val dp = rawDate.split("-")
                        if (dp.size == 3) "%d-%02d-%02d".format(dp[0].toInt(), dp[1].toInt(), dp[2].toInt())
                        else rawDate
                    } catch (e: Exception) { rawDate }
                    
                    if (normalizedDate == date) null else "$normalizedDate:${parts[1]}"
                }
                .toMutableList()

            map.add("$date:${state.planned.name},${state.actual.name},${state.locationName ?: ""}")

            prefs[KEY_DATA] = map.joinToString("|")
        }
    }

    // Delete one entry
    suspend fun delete(date: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_DATA] ?: ""

            val filtered = current.split("|")
                .filter { it.contains(":") }
                .mapNotNull { entry ->
                    val parts = entry.split(":")
                    val rawDate = parts[0]
                    val normalizedDate = try {
                        val dp = rawDate.split("-")
                        if (dp.size == 3) "%d-%02d-%02d".format(dp[0].toInt(), dp[1].toInt(), dp[2].toInt())
                        else rawDate
                    } catch (e: Exception) { rawDate }
                    
                    if (normalizedDate == date) null else "$normalizedDate:${parts[1]}"
                }
                .joinToString("|")

            prefs[KEY_DATA] = filtered
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }

    val officeLocations: Flow<List<OfficeLocation>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[KEY_OFFICE_LOCATIONS] ?: ""
            if (raw.isEmpty()) return@map emptyList()

            raw.split("|")
                .filter { it.contains(",") }
                .map { entry ->
                    val parts = entry.split(",")
                    OfficeLocation(
                        name = if (parts.size > 3) parts[3] else "Office",
                        lat = parts[0].toDoubleOrNull() ?: 0.0,
                        lng = parts[1].toDoubleOrNull() ?: 0.0,
                        type = try {
                            WorkLocation.valueOf(parts[2])
                        } catch (e: Exception) {
                            WorkLocation.BASE
                        }
                    )
                }
        }

    suspend fun saveOfficeLocations(locations: List<OfficeLocation>) {
        context.dataStore.edit { prefs ->
            prefs[KEY_OFFICE_LOCATIONS] = locations.joinToString("|") {
                "${it.lat},${it.lng},${it.type.name},${it.name}"
            }
        }
    }

    val wifiSsid: Flow<String> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_WIFI_SSID] ?: "TRANSPORT"
        }

    suspend fun saveWifiSsid(ssid: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_WIFI_SSID] = ssid
        }
    }

    val pollIntervalMinutes: Flow<Long> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_POLL_INTERVAL]?.toLongOrNull() ?: 30L
        }

    suspend fun savePollInterval(minutes: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_POLL_INTERVAL] = minutes.toString()
        }
    }
}
