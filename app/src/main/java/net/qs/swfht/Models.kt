package net.qs.swfht

enum class WorkLocation {
    HOME,
    BASE,
    OTHER,
    LEAVE
}

data class DayState(
    val planned: WorkLocation = WorkLocation.HOME,
    val actual: WorkLocation = WorkLocation.HOME,
    val locationName: String? = null
)

data class MonthStats(
    val actualNonWfhPercent: Int,
    val actualBaseCount: Int,
    val plannedNonWfhPercent: Int,
    val plannedBaseCount: Int,
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
