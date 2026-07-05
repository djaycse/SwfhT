package net.qs.swfht

import android.widget.ImageView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
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
        mutableStateMapOf<String, WorkLocation>().apply {
            savedMap.forEach { (date, value) ->
                put(date, WorkLocation.valueOf(value))
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

                        Text("SwfhT v1.0")
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

            CalendarGrid(
                month = currentMonth,
                dayStates = dayStates,
                scope = scope,
                store = store
            )

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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .background(Color(0xFFFFA500))
                                .border(1.dp, Color.Gray)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Tap day to mark attendance at team hub")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .background(Color(0xFF4CAF50))
                                .border(1.dp, Color.Gray)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Tap again to mark attendance at another office")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(16.dp)
                                .border(1.dp, Color.Gray)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Tap again to mark as Work-From-Home")
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
                    Text("Version 1.0:")
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
