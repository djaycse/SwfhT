package net.qs.inoffice

import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import net.qs.inoffice.data.WorkDataStore
import net.qs.inoffice.worker.LocationWorker
import java.time.Duration
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InOfficeApp() {

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var showHelp by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showOfficeLocations by remember { mutableStateOf(false) }
    var showWifiSettings by remember { mutableStateOf(false) }
    var showGoalSettings by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { WorkDataStore(context) }
    val scope = rememberCoroutineScope()

    val savedMap by store.workMap.collectAsState(initial = emptyMap())
    val pollInterval by store.pollIntervalMinutes.collectAsState(initial = 30L)
    val goalPercent by store.goalOfficePercent.collectAsState(initial = 50)
    val goalDays by store.goalTeamHubDays.collectAsState(initial = 5)

    LaunchedEffect(pollInterval) {
        val workRequest =
            PeriodicWorkRequestBuilder<LocationWorker>(Duration.ofMinutes(pollInterval))
                .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "location_scan",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    LaunchedEffect(Unit) {
        // Trigger a one-time scan on app launch
        val workRequest = OneTimeWorkRequestBuilder<LocationWorker>().build()
        WorkManager.getInstance(context).enqueue(workRequest)
    }

    if (showOfficeLocations) {
        BackHandler { showOfficeLocations = false }
        OfficeLocationsScreen(
            store = store,
            onBack = { showOfficeLocations = false }
        )
        return
    }

    if (showWifiSettings) {
        BackHandler { showWifiSettings = false }
        WifiSettingsScreen(
            store = store,
            onBack = { showWifiSettings = false }
        )
        return
    }

    if (showGoalSettings) {
        BackHandler { showGoalSettings = false }
        GoalSettingsScreen(
            store = store,
            onBack = { showGoalSettings = false }
        )
        return
    }

    val dayStates = remember(savedMap) {
        mutableStateMapOf<String, DayState>().apply {
            savedMap.forEach { (date, value) ->
                put(date, value)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {

                    Row(verticalAlignment = Alignment.CenterVertically) {

                        AndroidView(
                            factory = { ctx ->
                                ImageView(ctx).apply {
                                    setImageResource(R.drawable.inoffice_logo)
                                    clipToOutline = true
                                }
                            },
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("InOffice v${BuildConfig.VERSION_NAME}")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu"
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("How to use") },
                                onClick = {
                                    menuExpanded = false
                                    showHelp = true
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.Help, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Office locations") },
                                onClick = {
                                    menuExpanded = false
                                    showOfficeLocations = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.LocationOn, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Wi-Fi Settings") },
                                onClick = {
                                    menuExpanded = false
                                    showWifiSettings = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Wifi, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Attendance Goals") },
                                onClick = {
                                    menuExpanded = false
                                    showGoalSettings = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Settings, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("About") },
                                onClick = {
                                    menuExpanded = false
                                    showAbout = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Info, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {

                Spacer(Modifier.height(16.dp))

                MonthHeader(
                    month = currentMonth,
                    onPrev = { currentMonth = currentMonth.minusMonths(1) },
                    onNext = { currentMonth = currentMonth.plusMonths(1) }
                )

                Spacer(Modifier.height(4.dp))

                var totalDrag by remember { mutableFloatStateOf(0f) }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(currentMonth) {
                            detectHorizontalDragGestures(
                                onDragStart = { totalDrag = 0f },
                                onDragEnd = {
                                    if (totalDrag > 100) {
                                        currentMonth = currentMonth.minusMonths(1)
                                    } else if (totalDrag < -100) {
                                        currentMonth = currentMonth.plusMonths(1)
                                    }
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    totalDrag += dragAmount
                                }
                            )
                        }
                ) {
                    CalendarGrid(
                        month = currentMonth,
                        dayStates = dayStates,
                        scope = scope,
                        store = store
                    )
                }

                Spacer(Modifier.height(16.dp))
            }

            Column(
                modifier = Modifier
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                val todayState = dayStates[todayStr] ?: DayState()

                if (!todayState.locationName.isNullOrEmpty()) {
                    Text(
                        text = "Currently at ${todayState.locationName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp)
                    )
                }

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .background(Color(0xFFFFA500))
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Team hub", style = MaterialTheme.typography.bodySmall)

                            Spacer(Modifier.width(16.dp))

                            Box(
                                Modifier
                                    .size(12.dp)
                                    .background(Color(0xFF4CAF50))
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Other office", style = MaterialTheme.typography.bodySmall)

                            Spacer(Modifier.width(16.dp))

                            val isDark = androidx.compose.foundation.isSystemInDarkTheme()
                            val dayOffColor =
                                if (isDark) net.qs.inoffice.ui.theme.DayOffDark else net.qs.inoffice.ui.theme.DayOffLight
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .background(dayOffColor)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Day off", style = MaterialTheme.typography.bodySmall)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(12.dp)
                                    .background(
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Plan", style = MaterialTheme.typography.bodySmall)

                            Spacer(Modifier.width(16.dp))

                            Box(
                                Modifier
                                    .size(12.dp)
                                    .border(
                                        2.dp,
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                        CircleShape
                                    )
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Actual", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                val stats = calculateStats(currentMonth, dayStates)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Left Card: In office
                    Card(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "In office ($goalPercent%)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Planned Row
                            StatRow(
                                label = "Plan",
                                value = stats.plannedNonWfhPercent,
                                goal = goalPercent,
                                isPercent = true
                            )

                            // Actual Row
                            StatRow(
                                label = "Actual",
                                value = stats.actualNonWfhPercent,
                                goal = goalPercent,
                                isPercent = true
                            )
                        }
                    }

                    // Right Card: Team hub
                    Card(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier.padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Team hub ($goalDays days)",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Planned Row
                            StatRow(
                                label = "Plan",
                                value = stats.plannedBaseCount,
                                goal = goalDays,
                                isPercent = false
                            )

                            // Actual Row
                            StatRow(
                                label = "Actual",
                                value = stats.actualBaseCount,
                                goal = goalDays,
                                isPercent = false
                            )
                        }
                    }
                }
            }
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("How to use") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "1. Set your office locations",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text("2. Set your office Wi-Fi settings and how often to scan for attendance")
                    Text("3. Give the app permission to run in background at all times")
                    Text("4. Monitor attendance statistics.")

                    Spacer(Modifier.height(8.dp))
                    Text("--- Optional ---")
                    Spacer(Modifier.height(8.dp))

                    Text(
                        "Tap a date to plan ahead (inner circle):",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text("- Date by default: Work from home")
                    Text("- Tap once: Work at Team hub")
                    Text("- Tap again: Work at another office")
                    Text("- Tap again: On leave")
                    Text("- Tap again to repeat the above")

                    Spacer(Modifier.height(8.dp))
                    Text("Tap and hold a date to manually set actual office attendance (outer circle).")
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelp = false }) {
                    Text("Got it")
                }
            }
        )
    }

    if (showAbout) {
        AlertDialog(
            onDismissRequest = { showAbout = false },
            title = { Text("InOffice...") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("A simple office attendance planned and tracker.")
                    Spacer(Modifier.height(16.dp))
                    Text("Developed by djaycse.")
                    Spacer(Modifier.height(16.dp))
                    Text("Copyright © 2026")
                }
            },
            confirmButton = {
                TextButton(onClick = { showAbout = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun StatRow(
    label: String,
    value: Int,
    goal: Int,
    isPercent: Boolean
) {
    val progress = (value.toFloat() / goal.toFloat()).coerceIn(0f, 1f)
    val isMet = progress >= 1f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = if (isMet) Color(0xFF2E7D32) else Color.Red,
            modifier = Modifier.size(14.dp)
        )

        Spacer(Modifier.width(4.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(45.dp)
        )

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .padding(horizontal = 4.dp),
            strokeCap = StrokeCap.Butt,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )

        Text(
            text = if (isPercent) "$value%" else "$value",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(30.dp)
        )
    }
}
