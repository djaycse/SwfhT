package net.qs.swfht.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import net.qs.swfht.DayState
import net.qs.swfht.WorkLocation

// DataStore instance
val Context.dataStore by preferencesDataStore(name = "swfht_store")

class WorkDataStore(private val context: Context) {

    companion object {
        private val KEY_DATA = stringPreferencesKey("work_map")
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
                    val date = parts[0]
                    val values = parts[1].split(",")
                    
                    val plannedStr = values[0]
                    val actualStr = if (values.size > 1) values[1] else values[0] // Default actual to planned for legacy

                    val planned = try { WorkLocation.valueOf(plannedStr) } catch(e: Exception) { WorkLocation.HOME }
                    val actual = try { WorkLocation.valueOf(actualStr) } catch(e: Exception) { WorkLocation.HOME }
                    
                    date to DayState(planned, actual)
                }
        }

    // Save one entry
    suspend fun save(date: String, state: DayState) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_DATA] ?: ""

            val map = current.split("|")
                .filter { it.contains(":") && !it.startsWith("$date:") }
                .toMutableList()

            map.add("$date:${state.planned.name},${state.actual.name}")

            prefs[KEY_DATA] = map.joinToString("|")
        }
    }

    // Delete one entry
    suspend fun delete(date: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_DATA] ?: ""

            val filtered = current.split("|")
                .filter { !it.startsWith("$date:") }
                .joinToString("|")

            prefs[KEY_DATA] = filtered
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
