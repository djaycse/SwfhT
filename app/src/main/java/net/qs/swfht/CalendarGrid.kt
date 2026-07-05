package net.qs.swfht

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.qs.swfht.data.WorkDataStore
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun CalendarGrid(
    month: YearMonth,
    dayStates: MutableMap<String, WorkLocation>,
    scope: CoroutineScope,
    store: WorkDataStore
) {

    val firstDay = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    val startOffset = firstDay.dayOfWeek.value - 1

    val today = LocalDate.now()

    Column {

        val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        Row(Modifier.fillMaxWidth()) {
            days.forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        for (row in 0..5) {
            Row(Modifier.fillMaxWidth()) {

                for (col in 0..6) {

                    val index = row * 7 + col
                    val dayNumber = index - startOffset + 1

                    val valid = dayNumber in 1..daysInMonth

                    val dateKey = if (valid)
                        "${month.year}-${month.monthValue}-%02d".format(dayNumber)
                    else ""

                    val state = dayStates[dateKey] ?: WorkLocation.HOME

                    val isToday =
                        valid &&
                                dayNumber == today.dayOfMonth &&
                                month.year == today.year &&
                                month.monthValue == today.monthValue

                    val animatedColor by animateColorAsState(
                        targetValue = colorFor(state),
                        label = "cellColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp)
                    ) {

                        if (valid) {

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = if (isToday) 2.dp else 0.dp,
                                        color = if (isToday) MaterialTheme.colorScheme.onSurface else Color.Transparent
                                    )
                                    .background(animatedColor)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {

                                        val next = when (state) {
                                            WorkLocation.HOME -> WorkLocation.BASE
                                            WorkLocation.BASE -> WorkLocation.OTHER
                                            WorkLocation.OTHER -> WorkLocation.HOME
                                        }

                                        if (next == WorkLocation.HOME) {
                                            dayStates.remove(dateKey)
                                        } else {
                                            dayStates[dateKey] = next
                                        }

                                        scope.launch {
                                            if (next == WorkLocation.HOME) {
                                                store.delete(dateKey)
                                            } else {
                                                store.save(dateKey, next.name)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    color = if (state == WorkLocation.HOME) MaterialTheme.colorScheme.onSurface else Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}