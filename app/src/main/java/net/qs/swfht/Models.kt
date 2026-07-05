package net.qs.swfht

enum class WorkLocation {
    HOME,
    BASE,
    OTHER
}

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