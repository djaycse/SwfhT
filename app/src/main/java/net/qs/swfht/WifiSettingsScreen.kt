package net.qs.swfht

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import net.qs.swfht.data.WorkDataStore
import net.qs.swfht.worker.LocationWorker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiSettingsScreen(
    store: WorkDataStore,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentSsid by store.wifiSsid.collectAsState(initial = "TRANSPORT GUEST")
    val currentInterval by store.pollIntervalMinutes.collectAsState(initial = 30L)
    
    var ssidValue by remember(currentSsid) { mutableStateOf(currentSsid) }
    var intervalValue by remember(currentInterval) { mutableStateOf(currentInterval.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wi-Fi & Polling Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize()
        ) {
            Text(
                "Office auto-detection settings",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                "The app will periodically check if the specified office Wi-Fi network is available. If found, the app will cross-check your GPS location against any of the office locations you have configured, and automatically set whether you are in your Team hub or another office location.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = ssidValue,
                onValueChange = { ssidValue = it },
                label = { Text("Office Wi-Fi network name (SSID)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = intervalValue,
                onValueChange = { intervalValue = it },
                label = { Text("Wi-Fi polling Interval (minutes)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        store.saveWifiSsid(ssidValue)
                        val interval = intervalValue.toLongOrNull() ?: 30L
                        store.savePollInterval(if (interval < 15) 15 else interval) // WorkManager min is 15
                        
                        // Trigger immediate scan
                        val workRequest = OneTimeWorkRequestBuilder<LocationWorker>().build()
                        WorkManager.getInstance(context).enqueue(workRequest)
                        
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
