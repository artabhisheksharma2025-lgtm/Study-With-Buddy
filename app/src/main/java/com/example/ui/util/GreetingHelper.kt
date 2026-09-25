package com.example.ui.util

import java.util.Calendar
import java.util.Locale

enum class TimeOfDayPeriod(
    val title: String,
    val timeRangeDescription: String,
    val emoji: String,
    val greetings: List<String>,
    val heroTitle: String,
    val heroSubtitle: String
) {
    MORNING(
        title = "Morning",
        timeRangeDescription = "5:00 AM – 11:59 AM",
        emoji = "☀️",
        greetings = listOf("Good morning!", "Morning!", "Rise and shine!"),
        heroTitle = "Rise and shine! Ready for today's goals?",
        heroSubtitle = "Kickstart your day with a focused study block."
    ),
    AFTERNOON(
        title = "Afternoon",
        timeRangeDescription = "12:00 PM – 4:59 PM",
        emoji = "☀️",
        greetings = listOf("Good afternoon!", "Afternoon!"),
        heroTitle = "Good afternoon! Keep the momentum going.",
        heroSubtitle = "Steady afternoon sessions turn goals into achievements."
    ),
    EVENING(
        title = "Evening",
        timeRangeDescription = "5:00 PM – 7:59 PM",
        emoji = "🌆",
        greetings = listOf("Good evening!", "Evening!"),
        heroTitle = "Good evening! Finish today's goals strong.",
        heroSubtitle = "Review your progress and complete your daily target."
    ),
    NIGHT(
        title = "Night",
        timeRangeDescription = "8:00 PM onwards / Before Bed",
        emoji = "🌙",
        greetings = listOf("Good night!", "Have a good night!", "Sweet dreams!"),
        heroTitle = "Have a good night! Sweet dreams.",
        heroSubtitle = "Great job today! Rest well and recharge for tomorrow."
    );

    companion object {
        fun fromCalendar(calendar: Calendar = Calendar.getInstance()): TimeOfDayPeriod {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> MORNING // 5:00 AM – 11:59 AM
                in 12..16 -> AFTERNOON // 12:00 PM – 4:59 PM
                in 17..19 -> EVENING // 5:00 PM – 7:59 PM
                else -> NIGHT // 8:00 PM onwards / Before Bed (20:00 to 04:59)
            }
        }
    }
}

object GreetingHelper {

    /**
     * Determines current period:
     * - Morning: 5:00 AM – 11:59 AM
     * - Afternoon: 12:00 PM – 4:59 PM
     * - Evening: 5:00 PM – 7:59 PM
     * - Night: 8:00 PM onwards / Before Bed
     */
    fun getCurrentPeriod(calendar: Calendar = Calendar.getInstance()): TimeOfDayPeriod {
        return TimeOfDayPeriod.fromCalendar(calendar)
    }

    /**
     * Gets list of greetings for the current or specified period:
     * - Morning: "Good morning!", "Morning!", "Rise and shine!"
     * - Afternoon: "Good afternoon!", "Afternoon!"
     * - Evening: "Good evening!", "Evening!"
     * - Night: "Good night!", "Have a good night!", "Sweet dreams!"
     */
    fun getGreetings(period: TimeOfDayPeriod = getCurrentPeriod()): List<String> {
        return period.greetings
    }

    /**
     * Formats greeting with user's first name, e.g.:
     * - "Good morning, Alex! ☀️"
     * - "Morning, Alex! ☀️"
     * - "Rise and shine, Alex! ☀️"
     * - "Good afternoon, Alex! ☀️"
     * - "Afternoon, Alex! ☀️"
     * - "Good evening, Alex! 🌆"
     * - "Evening, Alex! 🌆"
     * - "Good night, Alex! 🌙"
     * - "Have a good night, Alex! 🌙"
     * - "Sweet dreams, Alex! 🌙"
     */
    fun formatGreeting(
        fullName: String,
        variantIndex: Int = 0,
        calendar: Calendar = Calendar.getInstance()
    ): String {
        val period = getCurrentPeriod(calendar)
        val greetings = period.greetings
        val safeIndex = (variantIndex.coerceAtLeast(0)) % greetings.size
        val chosenGreeting = greetings[safeIndex]

        val firstName = fullName.trim().split(" ").firstOrNull()?.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
        } ?: fullName.trim()

        val cleanPhrase = chosenGreeting.removeSuffix("!").trim()

        return if (firstName.isNotBlank()) {
            "$cleanPhrase, $firstName! ${period.emoji}"
        } else {
            "$cleanPhrase! ${period.emoji}"
        }
    }
}
