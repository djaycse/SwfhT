package net.qs.inoffice

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.qs.inoffice.data.WorkDataStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsLogScreen(
    store: WorkDataStore,
    onBack: () -> Unit
) {
    val logText by store.gpsLog.collectAsState(initial = "")
    val clipboardManager = LocalClipboardManager.current

    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    // Automatically scroll to bottom when text changes
    LaunchedEffect(logText) {
        if (logText.isNotEmpty()) {
            vScroll.animateScrollTo(vScroll.maxValue)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Log") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (logText.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(logText))
                        }
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy all")
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
                "Log of when the app detects you have entered (+) or left (-) a configured office location.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.medium
            ) {
                SelectionContainer {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(vScroll)
                            .horizontalScroll(hScroll)
                            .padding(8.dp)
                    ) {
                        Text(
                            text = logText.ifEmpty { "No log entries yet." },
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
