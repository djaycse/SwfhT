package net.qs.swfht

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CalendarGrid(
    month: YearMonth,
    dayStates: MutableMap<String, DayState>,
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

        Spacer(Modifier.height(4.dp))

        val totalCells = startOffset + daysInMonth
        val rowsNeeded = kotlin.math.ceil(totalCells / 7.0).toInt()

        for (row in 0 until rowsNeeded) {
            Row(Modifier.fillMaxWidth()) {

                for (col in 0..6) {

                    val index = row * 7 + col
                    val dayNumber = index - startOffset + 1

                    val valid = dayNumber in 1..daysInMonth

                    val dateKey = if (valid)
                        "%d-%02d-%02d".format(month.year, month.monthValue, dayNumber)
                    else ""

                    val dayState = dayStates[dateKey] ?: DayState()

                    val isToday =
                        valid &&
                                dayNumber == today.dayOfMonth &&
                                month.year == today.year &&
                                month.monthValue == today.monthValue

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
                                    // Today highlight: full size themed border
                                    .border(
                                        width = if (isToday) 2.dp else 0.dp,
                                        color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent
                                    )
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            val nextPlanned = when (dayState.planned) {
                                                WorkLocation.HOME -> WorkLocation.BASE
                                                WorkLocation.BASE -> WorkLocation.OTHER
                                                WorkLocation.OTHER -> WorkLocation.HOME
                                            }
                                            val newState = dayState.copy(planned = nextPlanned)
                                            updateState(dateKey, newState, dayStates, scope, store)
                                        },
                                        onLongClick = {
                                            val nextActual = when (dayState.actual) {
                                                WorkLocation.HOME -> WorkLocation.BASE
                                                WorkLocation.BASE -> WorkLocation.OTHER
                                                WorkLocation.OTHER -> WorkLocation.HOME
                                            }
                                            val newState = dayState.copy(actual = nextActual)
                                            updateState(dateKey, newState, dayStates, scope, store)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Planned state: Filled circle (Inner)
                                if (dayState.planned != WorkLocation.HOME) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(if (isToday) 10.dp else 8.dp) // Smaller inner circle
                                            .background(colorFor(dayState.planned), CircleShape)
                                    )
                                }

                                // Actual state: Circle border (Outer)
                                if (dayState.actual != WorkLocation.HOME) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(if (isToday) 4.dp else 2.dp) // Outer circle
                                            .border(3.dp, colorFor(dayState.actual), CircleShape)
                                    )
                                }

                                Text(
                                    text = dayNumber.toString(),
                                    color = if (dayState.planned == WorkLocation.HOME) MaterialTheme.colorScheme.onSurface else Color.Black
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun updateState(
    dateKey: String,
    newState: DayState,
    dayStates: MutableMap<String, DayState>,
    scope: CoroutineScope,
    store: WorkDataStore
) {
    if (newState.planned == WorkLocation.HOME && newState.actual == WorkLocation.HOME) {
        dayStates.remove(dateKey)
    } else {
        dayStates[dateKey] = newState
    }

    scope.launch {
        if (newState.planned == WorkLocation.HOME && newState.actual == WorkLocation.HOME) {
            store.delete(dateKey)
        } else {
            store.save(dateKey, newState)
        }
    }
}
