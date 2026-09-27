package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.StudySessionEntity
import com.example.data.model.SubjectEntity
import com.example.data.repository.AppRepository
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletTertiary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Subject breakdown on a specific calendar day.
 */
data class SubjectDayStudy(
    val subjectName: String,
    val durationSeconds: Long,
    val sessionCount: Int,
    val percentage: Float,
    val colorHex: String = "#3F51B5"
)

/**
 * Aggregated study data for a calendar day.
 */
data class DayStudyData(
    val dateString: String, // "yyyy-MM-dd"
    val dayOfMonth: Int,
    val dayOfWeek: Int, // Calendar.SUNDAY=1, MONDAY=2, etc.
    val totalDurationSeconds: Long,
    val sessionCount: Int,
    val subjectBreakdown: List<SubjectDayStudy>,
    val sessions: List<StudySessionEntity>,
    val isToday: Boolean,
    val isCurrentMonth: Boolean
)

object CalendarDateUtils {
    private val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val monthYearSdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
    private val fullDateSdf = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault())
    private val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())

    fun getTodayDateString(): String = dateSdf.format(Date())

    fun formatMonthYear(calendar: Calendar): String = monthYearSdf.format(calendar.time)

    fun formatFullDate(dateString: String): String {
        return try {
            val d = dateSdf.parse(dateString) ?: return dateString
            fullDateSdf.format(d)
        } catch (e: Exception) {
            dateString
        }
    }

    fun formatTime(timeMillis: Long): String {
        return try {
            timeSdf.format(Date(timeMillis))
        } catch (e: Exception) {
            ""
        }
    }

    fun formatDurationShort(durationSeconds: Long): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            durationSeconds > 0 -> "<1m"
            else -> "0m"
        }
    }

    fun formatDurationDetailed(durationSeconds: Long): String {
        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60
        return when {
            hours > 0 -> "${hours} hr ${minutes} min"
            minutes > 0 -> "${minutes} min"
            else -> "${seconds} sec"
        }
    }
}

/**
 * Main Interactive Study Calendar Component
 * Displays calendar with study duration for each day, month navigation,
 * selected date breakdown with subjects studied, and a button to open full date view.
 */
@Composable
fun StudyCalendarView(
    studySessions: List<StudySessionEntity>,
    subjects: List<SubjectEntity>,
    onOpenFullDateDetails: (dateString: String, dayData: DayStudyData) -> Unit,
    onAddSessionForDate: (dateString: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var calendarMonth by remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
        })
    }

    val todayDateString = remember { CalendarDateUtils.getTodayDateString() }
    var selectedDateString by remember { mutableStateOf(todayDateString) }

    // Map subjects to colors
    val subjectColorMap = remember(subjects) {
        subjects.associate { it.name.lowercase() to it.colorHex }
    }

    // Group sessions by sessionDate (yyyy-MM-dd)
    val sessionsByDate = remember(studySessions) {
        studySessions.groupBy { it.sessionDate }
    }

    // Calculate days grid for the current displayed month
    val displayedYear = calendarMonth.get(Calendar.YEAR)
    val displayedMonth = calendarMonth.get(Calendar.MONTH) // 0-based

    val daysGrid = remember(calendarMonth, sessionsByDate, subjectColorMap) {
        calculateMonthDays(
            year = displayedYear,
            month = displayedMonth,
            todayDateString = todayDateString,
            sessionsByDate = sessionsByDate,
            subjectColorMap = subjectColorMap
        )
    }

    // Monthly summary stats
    val (monthlyTotalSeconds, monthlyActiveDays, monthlyTopSubject) = remember(daysGrid) {
        val currentMonthDays = daysGrid.filter { it.isCurrentMonth && it.totalDurationSeconds > 0 }
        val totalSec = currentMonthDays.sumOf { it.totalDurationSeconds }
        val activeDays = currentMonthDays.size

        val subjectTimeMap = mutableMapOf<String, Long>()
        currentMonthDays.flatMap { it.subjectBreakdown }.forEach { sub ->
            subjectTimeMap[sub.subjectName] = (subjectTimeMap[sub.subjectName] ?: 0L) + sub.durationSeconds
        }
        val topSub = subjectTimeMap.maxByOrNull { it.value }?.key ?: "None"
        Triple(totalSec, activeDays, topSub)
    }

    // Selected day study data
    val selectedDayData = remember(selectedDateString, daysGrid, sessionsByDate, subjectColorMap) {
        daysGrid.find { it.dateString == selectedDateString } ?: run {
            // If date is outside current month grid, compute on the fly
            buildDayStudyData(
                dateString = selectedDateString,
                dayOfMonth = 1,
                dayOfWeek = Calendar.MONDAY,
                isToday = selectedDateString == todayDateString,
                isCurrentMonth = false,
                sessions = sessionsByDate[selectedDateString] ?: emptyList(),
                subjectColorMap = subjectColorMap
            )
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Month Navigation Card & Summary
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Month Selector Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            val newCal = (calendarMonth.clone() as Calendar).apply {
                                add(Calendar.MONTH, -1)
                            }
                            calendarMonth = newCal
                        },
                        modifier = Modifier.testTag("calendar_prev_month")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous Month")
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = CalendarDateUtils.formatMonthYear(calendarMonth),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$monthlyActiveDays study days • ${CalendarDateUtils.formatDurationShort(monthlyTotalSeconds)} total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Jump to today button
                        TextButton(
                            onClick = {
                                val now = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1) }
                                calendarMonth = now
                                selectedDateString = todayDateString
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Today", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        IconButton(
                            onClick = {
                                val newCal = (calendarMonth.clone() as Calendar).apply {
                                    add(Calendar.MONTH, 1)
                                }
                                calendarMonth = newCal
                            },
                            modifier = Modifier.testTag("calendar_next_month")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Monthly Quick Metrics Pill Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = IndigoPrimary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Total Hours", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = CalendarDateUtils.formatDurationShort(monthlyTotalSeconds),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = IndigoPrimary
                            )
                        }
                    }

                    Surface(
                        color = FlameOrange.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Active Days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "$monthlyActiveDays Days",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = FlameOrange
                            )
                        }
                    }

                    Surface(
                        color = EmeraldAccent.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Top Subject", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = monthlyTopSubject.take(9),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Weekday Header: Mon Tue Wed Thu Fri Sat Sun
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                    weekdays.forEach { dayName ->
                        Text(
                            text = dayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Calendar Grid (7 columns per row)
                val weeks = daysGrid.chunked(7)
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        week.forEach { day ->
                            CalendarDayCell(
                                day = day,
                                isSelected = day.dateString == selectedDateString,
                                onSelect = {
                                    selectedDateString = day.dateString
                                },
                                onDoubleTapOrOpen = {
                                    selectedDateString = day.dateString
                                    onOpenFullDateDetails(day.dateString, day)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Heatmap Intensity Legend
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Study time: ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    IntensityBadge(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), label = "0m")
                    Spacer(modifier = Modifier.width(6.dp))
                    IntensityBadge(color = EmeraldAccent.copy(alpha = 0.25f), label = "<1h")
                    Spacer(modifier = Modifier.width(6.dp))
                    IntensityBadge(color = EmeraldAccent.copy(alpha = 0.55f), label = "1-3h")
                    Spacer(modifier = Modifier.width(6.dp))
                    IntensityBadge(color = EmeraldAccent, label = "3h+", isTextWhite = true)
                }
            }
        }

        // Selected Date Summary & Subject Breakdown Panel
        SelectedDateBreakdownCard(
            dayData = selectedDayData,
            onOpenFullDetails = {
                onOpenFullDateDetails(selectedDayData.dateString, selectedDayData)
            },
            onAddSession = {
                onAddSessionForDate(selectedDayData.dateString)
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun IntensityBadge(color: Color, label: String, isTextWhite: Boolean = false) {
    Surface(
        color = color,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isTextWhite) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/**
 * Individual Day Cell in Calendar Grid
 */
@Composable
fun CalendarDayCell(
    day: DayStudyData,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDoubleTapOrOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasStudy = day.totalDurationSeconds > 0
    val durationText = if (hasStudy) CalendarDateUtils.formatDurationShort(day.totalDurationSeconds) else ""

    // Heatmap background color based on study duration
    val backgroundColor = when {
        !day.isCurrentMonth -> Color.Transparent
        day.totalDurationSeconds >= 3 * 3600L -> EmeraldAccent
        day.totalDurationSeconds >= 3600L -> EmeraldAccent.copy(alpha = 0.55f)
        day.totalDurationSeconds > 0 -> EmeraldAccent.copy(alpha = 0.22f)
        day.isToday -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }

    val isDarkBackground = day.isCurrentMonth && day.totalDurationSeconds >= 3 * 3600L
    val textColor = when {
        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
        isDarkBackground -> Color.White
        day.isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val borderModifier = if (isSelected) {
        Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
    } else if (day.isToday) {
        Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .heightIn(min = 54.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .then(borderModifier)
            .clickable { onSelect() }
            .testTag("calendar_day_${day.dateString}"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(2.dp)
        ) {
            // Day Number
            Text(
                text = "${day.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.isToday || isSelected || hasStudy) FontWeight.Bold else FontWeight.Normal,
                color = textColor,
                fontSize = 13.sp
            )

            // Study Duration / Sessions Indicator
            if (hasStudy && day.isCurrentMonth) {
                Text(
                    text = durationText,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkBackground) Color.White else EmeraldAccent,
                    maxLines = 1
                )

                // Session dots or flame emoji
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    if (day.totalDurationSeconds >= 2 * 3600L) {
                        Text("🔥", fontSize = 8.sp)
                    } else {
                        repeat(minOf(day.sessionCount, 3)) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 1.dp)
                                    .size(3.5.dp)
                                    .clip(CircleShape)
                                    .background(if (isDarkBackground) Color.White else EmeraldAccent)
                            )
                        }
                    }
                }
            } else if (day.isToday) {
                Text(
                    text = "Today",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

/**
 * Selected Date Summary & Subject Breakdown Card
 * Fulfills the user request: "date select before how many study and which subject and open full that date"
 */
@Composable
fun SelectedDateBreakdownCard(
    dayData: DayStudyData,
    onOpenFullDetails: () -> Unit,
    onAddSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    val todayString = CalendarDateUtils.getTodayDateString()
    val isToday = dayData.dateString == todayString
    val dateLabel = if (isToday) "Today (${CalendarDateUtils.formatFullDate(dayData.dateString)})"
    else CalendarDateUtils.formatFullDate(dayData.dateString)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = modifier.testTag("selected_date_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Date & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isToday) "📅 Today's Study" else "📅 Selected Date",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (dayData.totalDurationSeconds > 0) {
                    Surface(
                        color = EmeraldAccent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = CalendarDateUtils.formatDurationShort(dayData.totalDurationSeconds),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldAccent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(14.dp))

            // Body: Study Duration & Which Subjects Were Studied
            if (dayData.totalDurationSeconds > 0) {
                // Key metrics row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccessTime, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Total Studied: ${CalendarDateUtils.formatDurationDetailed(dayData.totalDurationSeconds)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${dayData.sessionCount} session(s)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // "Which Subject" Breakdown Section
                Text(
                    text = "📚 Subjects Studied on this Date:",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                dayData.subjectBreakdown.forEach { sub ->
                    val color = remember(sub.colorHex) {
                        try {
                            Color(android.graphics.Color.parseColor(sub.colorHex))
                        } catch (e: Exception) {
                            IndigoPrimary
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sub.subjectName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = CalendarDateUtils.formatDurationShort(sub.durationSeconds),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "(${String.format(Locale.getDefault(), "%.0f", sub.percentage)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LinearProgressIndicator(
                            progress = { sub.percentage / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape),
                            color = color,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            } else {
                // Empty state for this date
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No study recorded on this date.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Tap below to log a study session or open date details.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons: Open Full Date Details & Add Session
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onAddSession,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("button_add_session_date")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Log Study", fontSize = 13.sp)
                }

                Button(
                    onClick = onOpenFullDetails,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                    modifier = Modifier
                        .weight(1.3f)
                        .testTag("button_open_full_date")
                ) {
                    Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Full That Date 📋", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

/**
 * Full Date Details Dialog / Modal Sheet
 * Fulfills the user request: "and open full that date"
 * Displays complete timeline of all study sessions on that date, subject breakdown, notes, and actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullDateDetailsDialog(
    dayData: DayStudyData,
    onDismiss: () -> Unit,
    onDeleteSession: (sessionId: String) -> Unit,
    onAddSession: () -> Unit
) {
    var sessionToDelete by remember { mutableStateOf<StudySessionEntity?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Study Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = CalendarDateUtils.formatFullDate(dayData.dateString),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        FilledTonalButton(
                            onClick = onAddSession,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Study", fontSize = 12.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("dialog_full_date_details")
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Metric Summary Banner
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = IndigoPrimary.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Total Time Studied", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = CalendarDateUtils.formatDurationDetailed(dayData.totalDurationSeconds),
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = IndigoPrimary
                                    )
                                }

                                Surface(
                                    color = EmeraldAccent,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text(
                                        text = "${dayData.sessionCount} Sessions",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Which Subjects Studied on this Date
                item {
                    Text(
                        text = "📚 Subjects Breakdown (${dayData.subjectBreakdown.size} Subjects)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (dayData.subjectBreakdown.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No subjects studied yet on this date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                dayData.subjectBreakdown.forEach { sub ->
                                    val color = remember(sub.colorHex) {
                                        try {
                                            Color(android.graphics.Color.parseColor(sub.colorHex))
                                        } catch (e: Exception) {
                                            IndigoPrimary
                                        }
                                    }

                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = sub.subjectName,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = CalendarDateUtils.formatDurationShort(sub.durationSeconds),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "${sub.sessionCount} sess • ${String.format(Locale.getDefault(), "%.0f", sub.percentage)}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(6.dp))

                                        LinearProgressIndicator(
                                            progress = { sub.percentage / 100f },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(CircleShape),
                                            color = color,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Chronological Study Sessions List
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏱️ Study Sessions Timeline",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${dayData.sessions.size} logged",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (dayData.sessions.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.MenuBook,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "No study sessions on this date",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Log a past or current study session to complete your streak!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onAddSession,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add Session Now")
                                }
                            }
                        }
                    }
                } else {
                    items(dayData.sessions, key = { it.sessionId }) { session ->
                        StudySessionDetailCard(
                            session = session,
                            onDelete = { sessionToDelete = session }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        // Delete Confirmation Dialog
        sessionToDelete?.let { sess ->
            AlertDialog(
                onDismissRequest = { sessionToDelete = null },
                title = { Text("Delete Study Session?") },
                text = {
                    Text("Are you sure you want to delete this ${sess.subjectName} session (${CalendarDateUtils.formatDurationShort(sess.durationSeconds)})? This will adjust your total stats.")
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteSession(sess.sessionId)
                            sessionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { sessionToDelete = null }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Card representing an individual study session in the full date details view.
 */
@Composable
fun StudySessionDetailCard(
    session: StudySessionEntity,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        color = IndigoPrimary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = session.subjectName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = IndigoPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (session.sessionType == "TIMER") "⏱️ Timer" else "✍️ Manual",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.DeleteOutline,
                        contentDescription = "Delete Session",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (session.title.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timing & Duration Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${CalendarDateUtils.formatTime(session.startTime)} - ${CalendarDateUtils.formatTime(session.endTime)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = CalendarDateUtils.formatDurationDetailed(session.durationSeconds),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldAccent
                )
            }

            if (session.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = session.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Calculates calendar day items for a 7-column month grid (Monday through Sunday)
 */
fun calculateMonthDays(
    year: Int,
    month: Int,
    todayDateString: String,
    sessionsByDate: Map<String, List<StudySessionEntity>>,
    subjectColorMap: Map<String, String>
): List<DayStudyData> {
    val result = mutableListOf<DayStudyData>()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val maxDaysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

    // Calendar.DAY_OF_WEEK: Sunday=1, Monday=2, Tuesday=3, ... Saturday=7
    // In our Mon-Sun grid: Monday=0, Tuesday=1 ... Sunday=6
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    val leadingEmptyDays = when (firstDayOfWeek) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    // Previous month filler days
    val prevMonthCal = (cal.clone() as Calendar).apply {
        add(Calendar.MONTH, -1)
    }
    val prevMonthMaxDays = prevMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    for (i in leadingEmptyDays downTo 1) {
        val dayNum = prevMonthMaxDays - i + 1
        prevMonthCal.set(Calendar.DAY_OF_MONTH, dayNum)
        val dateStr = sdf.format(prevMonthCal.time)
        val sessions = sessionsByDate[dateStr] ?: emptyList()
        result.add(
            buildDayStudyData(
                dateString = dateStr,
                dayOfMonth = dayNum,
                dayOfWeek = prevMonthCal.get(Calendar.DAY_OF_WEEK),
                isToday = dateStr == todayDateString,
                isCurrentMonth = false,
                sessions = sessions,
                subjectColorMap = subjectColorMap
            )
        )
    }

    // Current month days
    for (day in 1..maxDaysInMonth) {
        cal.set(Calendar.DAY_OF_MONTH, day)
        val dateStr = sdf.format(cal.time)
        val sessions = sessionsByDate[dateStr] ?: emptyList()
        result.add(
            buildDayStudyData(
                dateString = dateStr,
                dayOfMonth = day,
                dayOfWeek = cal.get(Calendar.DAY_OF_WEEK),
                isToday = dateStr == todayDateString,
                isCurrentMonth = true,
                sessions = sessions,
                subjectColorMap = subjectColorMap
            )
        )
    }

    // Trailing filler days to complete rows of 7
    val trailingDays = (7 - (result.size % 7)) % 7
    val nextMonthCal = (cal.clone() as Calendar).apply {
        add(Calendar.MONTH, 1)
    }
    for (day in 1..trailingDays) {
        nextMonthCal.set(Calendar.DAY_OF_MONTH, day)
        val dateStr = sdf.format(nextMonthCal.time)
        val sessions = sessionsByDate[dateStr] ?: emptyList()
        result.add(
            buildDayStudyData(
                dateString = dateStr,
                dayOfMonth = day,
                dayOfWeek = nextMonthCal.get(Calendar.DAY_OF_WEEK),
                isToday = dateStr == todayDateString,
                isCurrentMonth = false,
                sessions = sessions,
                subjectColorMap = subjectColorMap
            )
        )
    }

    return result
}

fun buildDayStudyData(
    dateString: String,
    dayOfMonth: Int,
    dayOfWeek: Int,
    isToday: Boolean,
    isCurrentMonth: Boolean,
    sessions: List<StudySessionEntity>,
    subjectColorMap: Map<String, String>
): DayStudyData {
    val totalSec = sessions.sumOf { it.durationSeconds }
    val sessionCount = sessions.size

    // Group by subject
    val subjectBreakdown = if (totalSec > 0) {
        sessions.groupBy { it.subjectName }.map { (subName, sessList) ->
            val subTotalSec = sessList.sumOf { it.durationSeconds }
            val pct = (subTotalSec.toFloat() / totalSec.toFloat()) * 100f
            val color = subjectColorMap[subName.lowercase()] ?: "#3F51B5"
            SubjectDayStudy(
                subjectName = subName,
                durationSeconds = subTotalSec,
                sessionCount = sessList.size,
                percentage = pct,
                colorHex = color
            )
        }.sortedByDescending { it.durationSeconds }
    } else {
        emptyList()
    }

    return DayStudyData(
        dateString = dateString,
        dayOfMonth = dayOfMonth,
        dayOfWeek = dayOfWeek,
        totalDurationSeconds = totalSec,
        sessionCount = sessionCount,
        subjectBreakdown = subjectBreakdown,
        sessions = sessions.sortedBy { it.startTime },
        isToday = isToday,
        isCurrentMonth = isCurrentMonth
    )
}
