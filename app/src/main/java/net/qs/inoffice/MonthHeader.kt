package net.qs.inoffice

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import java.time.YearMonth

@Composable
fun MonthHeader(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(onClick = onPrev) { Text("<") }

        Text(
            text = "${month.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${month.year}",
            style = MaterialTheme.typography.titleLarge
        )

        Button(onClick = onNext) { Text(">") }
    }
}