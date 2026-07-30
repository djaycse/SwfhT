package net.qs.sofat

import org.junit.Test
import org.junit.Assert.*
import java.time.YearMonth

class UtilsTest {

    @Test
    fun testAugust2026Weekdays() {
        // August 2026 has 21 weekdays (Mon-Fri)
        val month = YearMonth.of(2026, 8)
        var weekdays = 0
        for (day in 1..month.lengthOfMonth()) {
            val date = month.atDay(day)
            if (date.dayOfWeek.value in 1..5) {
                weekdays++
            }
        }
        assertEquals(21, weekdays)
    }

    @Test
    fun testCalculateStatsRounding() {
        val month = YearMonth.of(2026, 8) // 21 weekdays
        
        // 1 office day in 21 weekdays should be 5% (4.76% rounded)
        val data = mapOf(
            "2026-08-03" to DayState(actual = WorkLocation.BASE)
        )
        
        val stats = calculateStats(month, data)
        assertEquals("Actual percent should be 5% (1/21 rounded)", 5, stats.actualNonWfhPercent)
        assertEquals("Total weekdays should be 21", 21, stats.totalDays)
    }
    
    @Test
    fun testCalculateStatsWithLeave() {
        val month = YearMonth.of(2026, 8) // 21 weekdays
        
        // Mark one weekday as LEAVE (planned)
        val data = mapOf(
            "2026-08-03" to DayState(planned = WorkLocation.LEAVE),
            "2026-08-04" to DayState(actual = WorkLocation.BASE)
        )
        
        val stats = calculateStats(month, data)
        // Total weekdays = 21 - 1 = 20
        // Actual non-wfh = 1
        // 1/20 = 5%
        assertEquals(20, stats.totalDays)
        assertEquals(5, stats.actualNonWfhPercent)
    }
}
