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
import net.qs.swfht.data.WorkDataStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalSettingsScreen(
    store: WorkDataStore,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentOfficePercent by store.goalOfficePercent.collectAsState(initial = 50)
    val currentTeamHubDays by store.goalTeamHubDays.collectAsState(initial = 5)
    
    var officePercentValue by remember(currentOfficePercent) { mutableStateOf(currentOfficePercent.toString()) }
    var teamHubDaysValue by remember(currentTeamHubDays) { mutableStateOf(currentTeamHubDays.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Attendance Goals") },
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
                "Configure your requirements",
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                "Set the targets for your office attendance and team hub days. These will be used to calculate your progress on the main dashboard.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = officePercentValue,
                onValueChange = { officePercentValue = it },
                label = { Text("Office Attendance Goal (%)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = teamHubDaysValue,
                onValueChange = { teamHubDaysValue = it },
                label = { Text("Team Hub Goal (days per month)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    scope.launch {
                        val percent = officePercentValue.toIntOrNull() ?: 50
                        val days = teamHubDaysValue.toIntOrNull() ?: 5
                        
                        store.saveGoalOfficePercent(percent.coerceIn(0, 100))
                        store.saveGoalTeamHubDays(days.coerceAtLeast(0))
                        
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
