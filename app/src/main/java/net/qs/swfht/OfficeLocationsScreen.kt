package net.qs.swfht

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.qs.swfht.data.WorkDataStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficeLocationsScreen(
    store: WorkDataStore,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val officeLocations by store.officeLocations.collectAsState(initial = emptyList())
    
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Office Locations") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (officeLocations.size < 5) {
                FloatingActionButton(onClick = {
                    val newList = officeLocations + OfficeLocation("New Location", 0.0, 0.0, WorkLocation.BASE)
                    scope.launch { store.saveOfficeLocations(newList) }
                }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Location")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(officeLocations) { index, location ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = location.name,
                                onValueChange = { newVal ->
                                    val newList = officeLocations.toMutableList().apply {
                                        this[index] = location.copy(name = newVal)
                                    }
                                    scope.launch { store.saveOfficeLocations(newList) }
                                },
                                label = { Text("Location Name") },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = {
                                val newList = officeLocations.toMutableList().apply { removeAt(index) }
                                scope.launch { store.saveOfficeLocations(newList) }
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        OutlinedTextField(
                            value = location.lat.toString(),
                            onValueChange = { newVal ->
                                val lat = newVal.toDoubleOrNull() ?: 0.0
                                val newList = officeLocations.toMutableList().apply {
                                    this[index] = location.copy(lat = lat)
                                }
                                scope.launch { store.saveOfficeLocations(newList) }
                            },
                            label = { Text("Latitude") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = location.lng.toString(),
                            onValueChange = { newVal ->
                                val lng = newVal.toDoubleOrNull() ?: 0.0
                                val newList = officeLocations.toMutableList().apply {
                                    this[index] = location.copy(lng = lng)
                                }
                                scope.launch { store.saveOfficeLocations(newList) }
                            },
                            label = { Text("Longitude") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = location.type == WorkLocation.BASE,
                                onClick = {
                                    val newList = officeLocations.toMutableList().apply {
                                        this[index] = location.copy(type = WorkLocation.BASE)
                                    }
                                    scope.launch { store.saveOfficeLocations(newList) }
                                }
                            )
                            Text("Team Hub")
                            Spacer(Modifier.width(8.dp))
                            RadioButton(
                                selected = location.type == WorkLocation.OTHER,
                                onClick = {
                                    val newList = officeLocations.toMutableList().apply {
                                        this[index] = location.copy(type = WorkLocation.OTHER)
                                    }
                                    scope.launch { store.saveOfficeLocations(newList) }
                                }
                            )
                            Text("Other Office")
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
                                            val newList = officeLocations.toMutableList().apply {
                                                this[index] = location.copy(lat = it.latitude, lng = it.longitude)
                                            }
                                            store.saveOfficeLocations(newList)
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

            if (officeLocations.isEmpty()) {
                item {
                    Text("No office locations configured. Up to 5 can be added.")
                }
            }
        }
    }
}
