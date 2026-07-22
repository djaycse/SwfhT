package net.qs.swfht

import android.widget.ImageView
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import net.qs.swfht.data.WorkDataStore
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SWFHTApp() {

    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var showHelp by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { WorkDataStore(context) }
    val scope = rememberCoroutineScope()

    val savedMap by store.workMap.collectAsState(initial = emptyMap())

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
                                    setImageResource(R.mipmap.ic_launcher_round)
                                }
                            },
                            modifier = Modifier.size(28.dp)
                        )

                        Spacer(Modifier.width(8.dp))

                        Text("SwfhT v${BuildConfig.VERSION_NAME}")
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
                                    Icon(Icons.Default.Help, contentDescription = null)
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
                .padding(16.dp)
                .fillMaxSize()
        ) {

            MonthHeader(
                month = currentMonth,
                onPrev = { currentMonth = currentMonth.minusMonths(1) },
                onNext = { currentMonth = currentMonth.plusMonths(1) }
            )

            Spacer(Modifier.height(12.dp))

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

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
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
                    Text("Team Hub", style = MaterialTheme.typography.bodySmall)

                    Spacer(Modifier.width(16.dp))

                    Box(
                        Modifier
                            .size(12.dp)
                            .background(Color(0xFF4CAF50))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Other Office", style = MaterialTheme.typography.bodySmall)

                    Spacer(Modifier.width(16.dp))

                    Box(
                        Modifier
                            .size(12.dp)
                            .border(1.5.dp, MaterialTheme.colorScheme.onSurface)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("WFH", style = MaterialTheme.typography.bodySmall)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Planned (Tap)", style = MaterialTheme.typography.bodySmall)

                    Spacer(Modifier.width(16.dp))

                    Box(
                        Modifier
                            .size(12.dp)
                            .border(1.5.dp, MaterialTheme.colorScheme.onSurfaceVariant, CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Actual (Long Press)", style = MaterialTheme.typography.bodySmall)
                }
            }

            Spacer(Modifier.height(16.dp))

            val stats = calculateStats(currentMonth, dayStates)

            val monthName = currentMonth.month.getDisplayName(
                TextStyle.FULL,
                Locale.getDefault()
            )

            Card {
                Column(Modifier.padding(12.dp)) {

                    Text(
                        text = "Stats for $monthName",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { (stats.nonWfhPercent / 50f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("In office: ${stats.nonWfhPercent}%")

                    if (stats.nonWfhPercent >= 50) {
                        Text(
                            "✔ Meets 50% requirement",
                            color = Color(0xFF2E7D32) // green
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    val teamHubLabel = if (stats.baseCount == 1) "day" else "days"
                    Text("Team hub: ${stats.baseCount} $teamHubLabel")

                    if (stats.baseCount >= 5) {
                        Text(
                            "✔ Meets 5 day requirement",
                            color = Color(0xFF2E7D32) // green
                        )
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
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Tap a date to toggle PLANNED state (Filled Circle).")
                    Text("Press and Hold a date to toggle ACTUAL state (Border Box).")
                    
                    HorizontalDivider()
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .background(Color(0xFFFFA500), CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Team Hub (Orange)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .background(Color(0xFF4CAF50), CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Other Office (Green)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("WFH (No colour)")
                    }
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
            title = { Text("SwfhT...") },
            text = {
                Column {
                    Text("The attendance tracker no one asked for.")
                    Spacer(Modifier.height(16.dp))
                    Text("Developed by the NBA dev team.")
                    Spacer(Modifier.height(16.dp))
                    Text("Change log...", style = MaterialTheme.typography.titleSmall)
                    Text("Version ${BuildConfig.VERSION_NAME}:")
                    Text("- First release.")
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
