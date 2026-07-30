package net.qs.sofat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.qs.sofat.data.WorkDataStore
import net.qs.sofat.worker.LocationWorker

// UI-specific model to handle text input smoothly
private data class OfficeLocationUI(
    val name: String,
    val coords: String,
    val type: WorkLocation
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeLocationsScreen(
    store: WorkDataStore,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedLocations by store.officeLocations.collectAsState(initial = null)
    
    var editableLocations by remember { mutableStateOf<List<OfficeLocationUI>>(emptyList()) }
    var hasLoadedInitial by remember { mutableStateOf(false) }

    LaunchedEffect(savedLocations) {
        if (!hasLoadedInitial && savedLocations != null) {
            editableLocations = savedLocations!!.map { 
                OfficeLocationUI(it.name, "${it.lat}, ${it.lng}", it.type) 
            }
            hasLoadedInitial = true
        }
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Office Locations") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val finalLocations = editableLocations.map {
                            val parts = it.coords.split(",")
                            val lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: 0.0
                            val lng = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 0.0
                            OfficeLocation(
                                name = it.name,
                                lat = lat,
                                lng = lng,
                                type = it.type
                            )
                        }
                        scope.launch {
                            store.saveOfficeLocations(finalLocations)
                            
                            // Trigger immediate scan
                            val workRequest = OneTimeWorkRequestBuilder<LocationWorker>().build()
                            WorkManager.getInstance(context).enqueue(workRequest)
                            
                            onBack()
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                    }
                }
            )
        },
        floatingActionButton = {
            if (editableLocations.size < 5) {
                FloatingActionButton(onClick = {
                    editableLocations = editableLocations + OfficeLocationUI("New Location", "0.0, 0.0", WorkLocation.BASE)
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Location")
                }
            }
        }
    ) { padding ->
        if (!hasLoadedInitial) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                itemsIndexed(editableLocations) { index, location ->
                    key(index) {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = location.name,
                                        onValueChange = { newVal ->
                                            editableLocations = editableLocations.toMutableList().apply {
                                                this[index] = location.copy(name = newVal)
                                            }
                                        },
                                        label = { Text("Location Name") },
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = {
                                        editableLocations = editableLocations.toMutableList().apply { removeAt(index) }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                }

                                Spacer(Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = location.coords,
                                    onValueChange = { newVal ->
                                        editableLocations = editableLocations.toMutableList().apply {
                                            this[index] = location.copy(coords = newVal)
                                        }
                                    },
                                    label = { Text("Coordinates (Lat, Long)") },
                                    placeholder = { Text("e.g. -33.86, 151.20") },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = location.type == WorkLocation.BASE,
                                        onClick = {
                                            editableLocations = editableLocations.toMutableList().apply {
                                                this[index] = location.copy(type = WorkLocation.BASE)
                                            }
                                        }
                                    )
                                    Text("Team hub")
                                    Spacer(Modifier.width(8.dp))
                                    RadioButton(
                                        selected = location.type == WorkLocation.OTHER,
                                        onClick = {
                                            editableLocations = editableLocations.toMutableList().apply {
                                                this[index] = location.copy(type = WorkLocation.OTHER)
                                            }
                                        }
                                    )
                                    Text("Other office")
                                }

                                Button(
                                    onClick = {
                                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                            scope.launch {
                                                val locTask = fusedLocationClient.lastLocation
                                                val loc = withContext(Dispatchers.IO) {
                                                    try { Tasks.await(locTask) } catch (e: Exception) { null }
                                                }
                                                loc?.let {
                                                    editableLocations = editableLocations.toMutableList().apply {
                                                        this[index] = location.copy(
                                                            coords = "${it.latitude}, ${it.longitude}"
                                                        )
                                                    }
                                                }
                                            }
                                        } else {
                                            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.MyLocation, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Set to current location")
                                }
                            }
                        }
                    }
                }

                if (editableLocations.isEmpty()) {
                    item {
                        Text("No office locations configured. Up to 5 can be added.")
                    }
                }
            }
        }
    }
}
