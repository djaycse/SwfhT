package net.qs.inoffice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.qs.inoffice.data.WorkDataStore
import net.qs.inoffice.worker.LocationWorker

private data class OfficeLocationUI(
    val id: Int = -1,
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

    var locationsList by remember { mutableStateOf<List<OfficeLocationUI>>(emptyList()) }
    var editingLocation by remember { mutableStateOf<OfficeLocationUI?>(null) }
    var hasLoadedInitial by remember { mutableStateOf(false) }

    LaunchedEffect(savedLocations) {
        if (!hasLoadedInitial && savedLocations != null) {
            locationsList = savedLocations!!.mapIndexed { index, it ->
                OfficeLocationUI(index, it.name, "${it.lat}, ${it.lng}", it.type)
            }
            hasLoadedInitial = true
        }
    }

    BackHandler {
        if (editingLocation != null) {
            editingLocation = null
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text(if (editingLocation == null) "Offices" else if (editingLocation!!.id == -1) "Add Location" else "Edit Location") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (editingLocation != null) {
                            editingLocation = null
                        } else {
                            onBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (editingLocation == null && locationsList.size < 10) {
                FloatingActionButton(onClick = {
                    val hasTeamHub = locationsList.any { it.type == WorkLocation.BASE }
                    editingLocation = OfficeLocationUI(
                        name = "",
                        coords = "0.0, 0.0",
                        type = if (hasTeamHub) WorkLocation.OTHER else WorkLocation.BASE
                    )
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
            if (editingLocation == null) {
                LocationList(
                    modifier = Modifier.padding(padding),
                    locations = locationsList,
                    onEdit = { editingLocation = it }
                )
            } else {
                LocationEditor(
                    modifier = Modifier.padding(padding),
                    location = editingLocation!!,
                    onSave = { updated ->
                        val savedId = if (updated.id == -1) locationsList.size else updated.id
                        val newList = if (updated.id == -1) {
                            locationsList + updated.copy(id = savedId)
                        } else {
                            locationsList.map { if (it.id == updated.id) updated else it }
                        }

                        // Enforce single team hub logic
                        val finalLocations = if (updated.type == WorkLocation.BASE) {
                            newList.map { if (it.id != savedId) it.copy(type = WorkLocation.OTHER) else it }
                        } else {
                            newList
                        }

                        locationsList = finalLocations

                        // Persist
                        val domainLocations = finalLocations.map {
                            val parts = it.coords.split(",")
                            val lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: 0.0
                            val lng = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 0.0
                            OfficeLocation(it.name, lat, lng, it.type)
                        }
                        scope.launch {
                            store.saveOfficeLocations(domainLocations)
                            val workRequest = OneTimeWorkRequestBuilder<LocationWorker>().build()
                            WorkManager.getInstance(context).enqueue(workRequest)
                        }
                        editingLocation = null
                    },
                    onDelete = { id ->
                        val finalLocations = locationsList.filter { it.id != id }
                        locationsList = finalLocations
                        val domainLocations = finalLocations.map {
                            val parts = it.coords.split(",")
                            val lat = parts.getOrNull(0)?.trim()?.toDoubleOrNull() ?: 0.0
                            val lng = parts.getOrNull(1)?.trim()?.toDoubleOrNull() ?: 0.0
                            OfficeLocation(it.name, lat, lng, it.type)
                        }
                        scope.launch {
                            store.saveOfficeLocations(domainLocations)
                        }
                        editingLocation = null
                    }
                )
            }
        }
    }
}

@Composable
private fun LocationList(
    modifier: Modifier,
    locations: List<OfficeLocationUI>,
    onEdit: (OfficeLocationUI) -> Unit
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            ) {
                Text(
                    "Configure up to 10 office locations. Only one can be your Team Hub. The app will periodically check your GPS location against these (see Auto-detect settings). If you are detected within 50 meters of an office location, your actual attendance will be set automatically.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
            }
        }
        itemsIndexed(locations) { _, location ->
            val isTeamHub = location.type == WorkLocation.BASE
            val iconColor = colorFor(location.type).takeIf { it != Color.Transparent }
                ?: MaterialTheme.colorScheme.outline

            ListItem(
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            location.name.ifEmpty { "Unnamed Location" },
                            fontWeight = if (isTeamHub) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isTeamHub) {
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "(Team hub)",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorFor(WorkLocation.BASE)
                            )
                        }
                    }
                },
                supportingContent = { Text(location.coords) },
                leadingContent = {
                    Icon(
                        Icons.Default.Place,
                        contentDescription = null,
                        tint = iconColor
                    )
                },
                modifier = Modifier.clickable { onEdit(location) }
            )
            HorizontalDivider()
        }
        if (locations.isEmpty()) {
            item {
                Box(Modifier
                    .fillMaxSize()
                    .padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No office locations configured. Up to 10 can be added.")
                }
            }
        }
    }
}

@Composable
private fun LocationEditor(
    modifier: Modifier,
    location: OfficeLocationUI,
    onSave: (OfficeLocationUI) -> Unit,
    onDelete: (Int) -> Unit
) {
    var name by remember { mutableStateOf(location.name) }
    var coords by remember { mutableStateOf(location.coords) }
    var type by remember { mutableStateOf(location.type) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    Column(
        modifier = modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Location Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = coords,
            onValueChange = { coords = it },
            label = { Text("Coordinates (Lat, Long)") },
            placeholder = { Text("e.g. -33.86, 151.20") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    scope.launch {
                        val locTask = fusedLocationClient.lastLocation
                        val loc = withContext(Dispatchers.IO) {
                            try {
                                Tasks.await(locTask)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        loc?.let {
                            coords = "${it.latitude}, ${it.longitude}"
                        }
                    }
                } else {
                    permissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.MyLocation, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Set to current location")
        }

        Spacer(Modifier.height(24.dp))

        Text("Location Type", style = MaterialTheme.typography.titleSmall)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { type = WorkLocation.BASE }
        ) {
            RadioButton(
                selected = type == WorkLocation.BASE,
                onClick = { type = WorkLocation.BASE }
            )
            Text("Team hub")
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { type = WorkLocation.OTHER }
        ) {
            RadioButton(
                selected = type == WorkLocation.OTHER,
                onClick = { type = WorkLocation.OTHER }
            )
            Text("Other office")
        }

        Spacer(Modifier.weight(1f))

        if (location.id != -1) {
            Button(
                onClick = { onDelete(location.id) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Delete Location")
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = {
                onSave(location.copy(name = name, coords = coords, type = type))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save")
        }
    }
}
