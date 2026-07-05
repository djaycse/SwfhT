package net.qs.swfht

import androidx.compose.ui.graphics.Color
import java.time.YearMonth

fun colorFor(state: WorkLocation): Color {
    return when (state) {
        WorkLocation.HOME -> Color.Transparent
        WorkLocation.BASE -> Color(0xFFFFA500)
        WorkLocation.OTHER -> Color(0xFF4CAF50)
    }
}

fun calculateStats(
    month: YearMonth,
    data: Map<String, WorkLocation>
): MonthStats {

    val daysInMonth = month.lengthOfMonth()

    var baseCount = 0
    var nonWfhWeekdays = 0
    var totalWeekdays = 0

    for (day in 1..daysInMonth) {
        val date = month.atDay(day)
        val isWeekday = date.dayOfWeek.value in 1..5

        val key = "${month.year}-${month.monthValue}-%02d".format(day)
        val state = data[key] ?: WorkLocation.HOME

        if (state == WorkLocation.BASE) {
            baseCount++
        }

        if (isWeekday) {
            totalWeekdays++
            if (state != WorkLocation.HOME) {
                nonWfhWeekdays++
            }
        }
    }

    val percent = if (totalWeekdays == 0) 0
    else (nonWfhWeekdays * 100) / totalWeekdays

    return MonthStats(
        nonWfhPercent = percent,
        baseCount = baseCount,
        totalDays = totalWeekdays
    )
}

fun calculateInsights(
    month: YearMonth,
    data: Map<String, WorkLocation>
): MonthInsights {

    val daysInMonth = month.lengthOfMonth()

    var longest = 0
    var current = 0
    var temp = 0

    for (day in 1..daysInMonth) {

        val key = "${month.year}-${month.monthValue}-%02d".format(day)
        val state = data[key] ?: WorkLocation.HOME

        val isOffice = state != WorkLocation.HOME

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