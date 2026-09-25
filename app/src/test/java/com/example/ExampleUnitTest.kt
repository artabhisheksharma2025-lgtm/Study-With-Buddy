package com.example

import com.example.data.model.StudyGoalEntity
import com.example.data.model.StudySessionEntity
import com.example.data.util.StatsCalculator
import org.junit.Assert.*
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExampleUnitTest {
    @Test
    fun testDailyGoalProgressCalculation() {
        val targetHours = 2.0f
        val targetMinutes = (targetHours * 60).toInt()
        val targetSeconds = targetMinutes * 60L

        val studiedTodaySeconds = 5400L // 1.5 hours
        val progressFraction = (studiedTodaySeconds.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)
        val percentage = (progressFraction * 100).toInt()

        assertEquals(75, percentage)
        val remainingSeconds = (targetSeconds - studiedTodaySeconds).coerceAtLeast(0L)
        assertEquals(1800L, remainingSeconds) // 30 minutes remaining
    }

    @Test
    fun testWeeklyGoalProgressCalculation() {
        val targetHours = 20.0f
        val targetMinutes = (targetHours * 60).toInt()
        val targetSeconds = targetMinutes * 60L

        val studiedWeeklySeconds = 72000L // 20 hours
        val progressFraction = (studiedWeeklySeconds.toFloat() / targetSeconds.toFloat()).coerceIn(0f, 1f)
        val percentage = (progressFraction * 100).toInt()

        assertEquals(100, percentage)
    }

    @Test
    fun testStatsCalculatorWithTodaySession() {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val session = StudySessionEntity(
            sessionId = "sess_1",
            userId = "user_1",
            subjectId = "sub_1",
            subjectName = "Mathematics",
            startTime = System.currentTimeMillis() - 3600000L,
            endTime = System.currentTimeMillis(),
            durationSeconds = 3600L,
            sessionDate = todayStr
        )

        val stats = StatsCalculator.calculateStats(listOf(session))
        assertEquals(3600L, stats.todayTimeSeconds)
        assertEquals(1, stats.todaySessionsCount)
        assertEquals(3600L, stats.weeklyTimeSeconds)
    }
}
