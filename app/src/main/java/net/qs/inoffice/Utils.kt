package net.qs.inoffice

import androidx.compose.ui.graphics.Color
import java.time.YearMonth
import kotlin.math.roundToInt

fun colorFor(state: WorkLocation): Color {
    return when (state) {
        WorkLocation.HOME -> Color.Transparent
        WorkLocation.BASE -> Color(0xFFFFA500)
        WorkLocation.OTHER -> Color(0xFF4CAF50)
        WorkLocation.LEAVE -> Color.Transparent
    }
}

fun calculateStats(
    month: YearMonth,
    data: Map<String, DayState>
): MonthStats {

    val daysInMonth = month.lengthOfMonth()

    var actualBaseCount = 0
    var actualNonWfhWeekdays = 0
    var plannedBaseCount = 0
    var plannedNonWfhWeekdays = 0
    var totalWeekdays = 0

    for (day in 1..daysInMonth) {
        val date = month.atDay(day)
        val isWeekday = date.dayOfWeek.value in 1..5

        val key = "%d-%02d-%02d".format(month.year, month.monthValue, day)
        val dayState = data[key] ?: DayState()

        // Actual
        if (dayState.actual == WorkLocation.BASE) {
            actualBaseCount++
        }

        // Planned
        if (dayState.planned == WorkLocation.BASE) {
            plannedBaseCount++
        }

        if (isWeekday && dayState.planned != WorkLocation.LEAVE) {
            totalWeekdays++
            if (dayState.actual != WorkLocation.HOME && dayState.actual != WorkLocation.LEAVE) {
                actualNonWfhWeekdays++
            }
            if (dayState.planned != WorkLocation.HOME) {
                plannedNonWfhWeekdays++
            }
        }
    }

    val actualPercent = if (totalWeekdays == 0) 0
    else ((actualNonWfhWeekdays * 100.0) / totalWeekdays).roundToInt()

    val plannedPercent = if (totalWeekdays == 0) 0
    else ((plannedNonWfhWeekdays * 100.0) / totalWeekdays).roundToInt()

    return MonthStats(
        actualNonWfhPercent = actualPercent,
        actualBaseCount = actualBaseCount,
        plannedNonWfhPercent = plannedPercent,
        plannedBaseCount = plannedBaseCount,
        totalDays = totalWeekdays
    )
}

fun calculateInsights(
    month: YearMonth,
    data: Map<String, DayState>
): MonthInsights {

    val daysInMonth = month.lengthOfMonth()

    var longest = 0
    var current = 0
    var temp = 0

    for (day in 1..daysInMonth) {

        val key = "%d-%02d-%02d".format(month.year, month.monthValue, day)
        val dayState = data[key] ?: DayState()
        val state = dayState.actual

        val isOffice = state != WorkLocation.HOME && state != WorkLocation.LEAVE

        if (isOffice) {
            temp++
            current = temp
        } else {
            temp = 0
        }

        if (temp > longest) longest = temp
    }

    val warning = when {
        current == 0 -> "No office days yet this month"
        current < 3 -> "Low momentum"
        else -> null
    }

    return MonthInsights(
        longestStreak = longest,
        currentStreak = current,
        warningText = warning
    )
}
