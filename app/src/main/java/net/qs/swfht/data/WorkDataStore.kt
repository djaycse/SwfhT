package net.qs.swfht.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore instance
val Context.dataStore by preferencesDataStore(name = "swfht_store")

class WorkDataStore(private val context: Context) {

    companion object {
        private val KEY_DATA = stringPreferencesKey("work_map")
    }

    // Read full map as a single string
    val workMap: Flow<Map<String, String>> =
        context.dataStore.data.map { prefs ->
            val raw = prefs[KEY_DATA] ?: ""
            if (raw.isEmpty()) return@map emptyMap()

            raw.split("|")
                .filter { it.contains(":") }
                .associate {
                    val parts = it.split(":")
                    parts[0] to parts[1]
                }
        }

    // Save one entry
    suspend fun save(date: String, value: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_DATA] ?: ""

            val map = current.split("|")
                .filter { it.contains(":") && !it.startsWith("$date:") }
                .toMutableList()

            map.add("$date:$value")

            prefs[KEY_DATA] = map.joinToString("|")
        }
    }

    // Delete one entry (reset to HOME)
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