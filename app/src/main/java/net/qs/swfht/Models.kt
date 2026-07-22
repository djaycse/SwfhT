package net.qs.swfht

enum class WorkLocation {
    HOME,
    BASE,
    OTHER
}

data class DayState(
    val planned: WorkLocation = WorkLocation.HOME,
    val actual: WorkLocation = WorkLocation.HOME
)

data class MonthStats(
    val nonWfhPercent: Int,
    val baseCount: Int,
    val totalDays: Int
)

data class MonthInsights(
    val longestStreak: Int,
    val currentStreak: Int,
    val warningText: String?
)

data class OfficeLocation(
    val name: String,
    val lat: Double,
    val lng: Double,
    val type: WorkLocation
)
