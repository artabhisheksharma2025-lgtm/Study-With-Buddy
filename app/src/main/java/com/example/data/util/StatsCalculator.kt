package com.example.data.util

import com.example.data.model.StudySessionEntity
import java.text.SimpleDateFormat
import java.util.*

data class UserStudyStats(
    val totalTimeSeconds: Long = 0L,
    val todayTimeSeconds: Long = 0L,
    val weeklyTimeSeconds: Long = 0L,
    val monthlyTimeSeconds: Long = 0L,
    val totalSessionsCount: Int = 0,
    val todaySessionsCount: Int = 0,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val averageSessionSeconds: Long = 0L,
    val mostStudiedSubject: String = "None",
    val subjectBreakdown: List<SubjectStat> = emptyList(),
    val dailyActivity: Map<String, Long> = emptyMap(), // "Mon" -> seconds
    val activeDates: Set<String> = emptySet(), // "YYYY-MM-DD"
    val todaySubjectTimes: Map<String, Long> = emptyMap(),
    val weeklySubjectTimes: Map<String, Long> = emptyMap()
)

data class SubjectStat(
    val subjectName: String,
    val totalTimeSeconds: Long,
    val sessionCount: Int,
    val percentage: Float,
    val lastStudiedDate: String
)

object StatsCalculator {

    fun calculateStats(sessions: List<StudySessionEntity>): UserStudyStats {
        if (sessions.isEmpty()) {
            return UserStudyStats()
        }

        val now = Calendar.getInstance()
        val todayStr = formatDate(now.time)

        // Yesterday
        val calYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = formatDate(calYesterday.time)

        // Week start (7 days ago)
        val calWeekAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
        val weekAgoMillis = calWeekAgo.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        // Month start
        val calMonthAgo = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
        val monthAgoMillis = calMonthAgo.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }.timeInMillis

        var totalTime = 0L
        var todayTime = 0L
        var weeklyTime = 0L
        var monthlyTime = 0L
        var todayCount = 0

        val subjectTimeMap = mutableMapOf<String, Long>()
        val subjectCountMap = mutableMapOf<String, Int>()
        val subjectLastDateMap = mutableMapOf<String, String>()

        val activeDatesSet = mutableSetOf<String>()
        val todaySubjectMap = mutableMapOf<String, Long>()
        val weeklySubjectMap = mutableMapOf<String, Long>()

        // For weekly graph (Last 7 days: Mon, Tue...)
        val daysOfWeekList = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        val dailyMap = daysOfWeekList.associateWith { 0L }.toMutableMap()

        val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())

        for (session in sessions) {
            val duration = session.durationSeconds
            totalTime += duration
            activeDatesSet.add(session.sessionDate)
            val subName = session.subjectName.ifBlank { "Other" }

            // Today
            if (session.sessionDate == todayStr) {
                todayTime += duration
                todayCount++
                todaySubjectMap[subName] = (todaySubjectMap[subName] ?: 0L) + duration
            }

            // Weekly
            if (session.endTime >= weekAgoMillis) {
                weeklyTime += duration
                weeklySubjectMap[subName] = (weeklySubjectMap[subName] ?: 0L) + duration
                val dayName = dayOfWeekFormat.format(Date(session.endTime))
                if (dailyMap.containsKey(dayName)) {
                    dailyMap[dayName] = (dailyMap[dayName] ?: 0L) + duration
                }
            }

            // Monthly
            if (session.endTime >= monthAgoMillis) {
                monthlyTime += duration
            }

            // Subject grouping
            subjectTimeMap[subName] = (subjectTimeMap[subName] ?: 0L) + duration
            subjectCountMap[subName] = (subjectCountMap[subName] ?: 0) + 1

            val existingLastDate = subjectLastDateMap[subName]
            if (existingLastDate == null || session.sessionDate > existingLastDate) {
                subjectLastDateMap[subName] = session.sessionDate
            }
        }

        // Streak calculation
        val (currentStreak, longestStreak) = calculateStreaks(activeDatesSet, todayStr, yesterdayStr)

        // Subject Breakdown list
        val subjectStatsList = subjectTimeMap.map { (name, duration) ->
            val count = subjectCountMap[name] ?: 0
            val pct = if (totalTime > 0) (duration.toFloat() / totalTime) * 100f else 0f
            val lastDate = subjectLastDateMap[name] ?: "N/A"
            SubjectStat(
                subjectName = name,
                totalTimeSeconds = duration,
                sessionCount = count,
                percentage = pct,
                lastStudiedDate = lastDate
            )
        }.sortedByDescending { it.totalTimeSeconds }

        val mostStudied = subjectStatsList.firstOrNull()?.subjectName ?: "None"
        val avgSession = if (sessions.isNotEmpty()) totalTime / sessions.size else 0L

        return UserStudyStats(
            totalTimeSeconds = totalTime,
            todayTimeSeconds = todayTime,
            weeklyTimeSeconds = weeklyTime,
            monthlyTimeSeconds = monthlyTime,
            totalSessionsCount = sessions.size,
            todaySessionsCount = todayCount,
            currentStreakDays = currentStreak,
            longestStreakDays = longestStreak,
            averageSessionSeconds = avgSession,
            mostStudiedSubject = mostStudied,
            subjectBreakdown = subjectStatsList,
            dailyActivity = dailyMap,
            activeDates = activeDatesSet,
            todaySubjectTimes = todaySubjectMap,
            weeklySubjectTimes = weeklySubjectMap
        )
    }

    private fun calculateStreaks(
        activeDates: Set<String>,
        todayStr: String,
        yesterdayStr: String
    ): Pair<Int, Int> {
        if (activeDates.isEmpty()) return Pair(0, 0)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sortedDates = activeDates.mapNotNull {
            try {
                sdf.parse(it)
            } catch (e: Exception) {
                null
            }
        }.sortedDescending()

        if (sortedDates.isEmpty()) return Pair(0, 0)

        // Current Streak
        var currentStreak = 0
        var checkCal = Calendar.getInstance()

        // Check if today is present
        val hasToday = activeDates.contains(todayStr)
        val hasYesterday = activeDates.contains(yesterdayStr)

        if (!hasToday && !hasYesterday) {
            currentStreak = 0
        } else {
            if (hasToday) {
                checkCal.time = Date()
            } else {
                checkCal.add(Calendar.DAY_OF_YEAR, -1)
            }

            while (true) {
                val dateStr = formatDate(checkCal.time)
                if (activeDates.contains(dateStr)) {
                    currentStreak++
                    checkCal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }

        // Longest Streak
        var longestStreak = 0
        var tempStreak = 0
        val sortedAscending = sortedDates.sorted()

        var previousCal: Calendar? = null
        for (date in sortedAscending) {
            val curCal = Calendar.getInstance().apply { time = date }
            if (previousCal == null) {
                tempStreak = 1
            } else {
                val diffDays = (curCal.timeInMillis - previousCal.timeInMillis) / (1000 * 60 * 60 * 24)
                if (diffDays == 1L) {
                    tempStreak++
                } else if (diffDays > 1L) {
                    tempStreak = 1
                }
            }
            if (tempStreak > longestStreak) {
                longestStreak = tempStreak
            }
            previousCal = curCal
        }

        return Pair(currentStreak, longestStreak)
    }

    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(date)
    }
}
