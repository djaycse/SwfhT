package net.qs.swfht

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.qs.swfht.data.WorkDataStore

// UI-specific model to handle text input smoothly
private data class OfficeLocationUI(
    val name: String,
    val latStr: String,
    val lngStr: String,
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
                OfficeLocationUI(it.name, it.lat.toString(), it.lng.toString(), it.type) 
            }
            hasLoadedInitial = true
        }
    }

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
                },
                actions = {
                    IconButton(onClick = {
                        val finalLocations = editableLocations.map {
                            OfficeLocation(
                                name = it.name,
                                lat = it.latStr.toDoubleOrNull() ?: 0.0,
                                lng = it.lngStr.toDoubleOrNull() ?: 0.0,
                                type = it.type
                            )
                        }
                        scope.launch {
                            store.saveOfficeLocations(finalLocations)
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
                    editableLocations = editableLocations + OfficeLocationUI("New Location", "0.0", "0.0", WorkLocation.BASE)
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
                                    value = location.latStr,
                                    onValueChange = { newVal ->
                                        editableLocations = editableLocations.toMutableList().apply {
                                            this[index] = location.copy(latStr = newVal)
                                        }
                                    },
                                    label = { Text("Latitude") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = location.lngStr,
                                    onValueChange = { newVal ->
                                        editableLocations = editableLocations.toMutableList().apply {
                                            this[index] = location.copy(lngStr = newVal)
                                        }
                                    },
                                    label = { Text("Longitude") },
                                    modifier = Modifier.fillMaxWidth(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = location.type == WorkLocation.BASE,
                                        onClick = {
                                            editableLocations = editableLocations.toMutableList().apply {
                                                this[index] = location.copy(type = WorkLocation.BASE)
                                            }
                                        }
                                    )
                                    Text("Team Hub")
                                    Spacer(Modifier.width(8.dp))
                                    RadioButton(
                                        selected = location.type == WorkLocation.OTHER,
                                        onClick = {
                                            editableLocations = editableLocations.toMutableList().apply {
                                                this[index] = location.copy(type = WorkLocation.OTHER)
                                            }
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
                                                    editableLocations = editableLocations.toMutableList().apply {
                                                        this[index] = location.copy(
                                                            latStr = it.latitude.toString(),
                                                            lngStr = it.longitude.toString()
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
