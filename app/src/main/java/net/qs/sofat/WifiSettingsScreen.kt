package net.qs.sofat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import net.qs.sofat.data.WorkDataStore
import net.qs.sofat.worker.LocationWorker
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

    var hasBackgroundLocation by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasBackgroundLocation = isGranted
    }

    val foregroundPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val fineGranted = results[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        if (fineGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
    }

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
                "The app will periodically check if the specified office Wi-Fi network is available. If found, the app will cross-check your GPS location and automatically set your attendance.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(16.dp))

            if (!hasBackgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Background Location Missing",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            "To detect your location while the app is closed, please set location permission to \"Allow all the time\" in system settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                    foregroundPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                } else {
                                    backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

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
