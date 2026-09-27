package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyGoalEntity
import com.example.data.model.StudySessionEntity
import com.example.data.model.SubjectEntity
import com.example.data.repository.AppRepository
import com.example.data.util.UserStudyStats
import com.example.ui.components.BarChartComposable
import com.example.ui.components.DailyGoalTrackerCard
import com.example.ui.components.DayStudyData
import com.example.ui.components.FullDateDetailsDialog
import com.example.ui.components.GoalPeriod
import com.example.ui.components.ManualSessionDialog
import com.example.ui.components.StatCard
import com.example.ui.components.StreakCalendarCard
import com.example.ui.components.StudyCalendarView
import com.example.ui.components.WeeklyGoalTrackerCard
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletTertiary

enum class StatsTab(val title: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    ALL_TIME("All Time")
}

enum class StatsViewMode(val title: String, val icon: ImageVector) {
    OVERVIEW("Overview", Icons.Filled.BarChart),
    CALENDAR("Study Calendar", Icons.Filled.CalendarMonth)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    stats: UserStudyStats,
    dailyGoal: StudyGoalEntity? = null,
    weeklyGoal: StudyGoalEntity? = null,
    studySessions: List<StudySessionEntity> = emptyList(),
    subjects: List<SubjectEntity> = emptyList(),
    initialViewMode: StatsViewMode = StatsViewMode.OVERVIEW,
    onDeleteSession: ((String) -> Unit)? = null,
    onAddManualSession: ((subjectName: String, durationMinutes: Long, title: String, notes: String, sessionDate: String?) -> Unit)? = null,
    onOpenSetGoalDialog: ((GoalPeriod) -> Unit)? = null,
    onOpenSetWeeklyGoalDialog: (() -> Unit)? = null,
    onStartStudy: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentMode by remember { mutableStateOf(initialViewMode) }
    var selectedTab by remember { mutableStateOf(StatsTab.WEEKLY) }

    // Dialog state for "Open Full That Date"
    var activeFullDateData by remember { mutableStateOf<DayStudyData?>(null) }
    // Dialog state for adding a study session on a selected date
    var dateToLogSession by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Study Stats & Calendar", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier.testTag("screen_stats")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Mode Selector: Overview vs Calendar
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                StatsViewMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = currentMode == mode,
                        onClick = { currentMode = mode },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = StatsViewMode.entries.size),
                        icon = {
                            Icon(mode.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    ) {
                        Text(mode.title, fontWeight = if (currentMode == mode) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (currentMode == StatsViewMode.CALENDAR) {
                // Interactive Calendar View
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        StudyCalendarView(
                            studySessions = studySessions,
                            subjects = subjects,
                            onOpenFullDateDetails = { dateStr, dayData ->
                                activeFullDateData = dayData
                            },
                            onAddSessionForDate = { dateStr ->
                                dateToLogSession = dateStr
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            } else {
                // Overview Mode
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Stats Period Tabs (Daily, Weekly, Monthly, All Time)
                    item {
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            StatsTab.entries.forEachIndexed { index, tab ->
                                SegmentedButton(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = StatsTab.entries.size)
                                ) {
                                    Text(tab.title, fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Summary Metrics based on selected period
                    item {
                        val (timeVal, sessionVal, periodLabel) = when (selectedTab) {
                            StatsTab.DAILY -> Triple(
                                AppRepository.formatDurationShort(stats.todayTimeSeconds),
                                "${stats.todaySessionsCount} Sessions",
                                "Today"
                            )
                            StatsTab.WEEKLY -> Triple(
                                AppRepository.formatDurationShort(stats.weeklyTimeSeconds),
                                "${stats.totalSessionsCount} Total Sessions",
                                "Last 7 Days"
                            )
                            StatsTab.MONTHLY -> Triple(
                                AppRepository.formatDurationShort(stats.monthlyTimeSeconds),
                                "${stats.totalSessionsCount} Sessions",
                                "This Month"
                            )
                            StatsTab.ALL_TIME -> Triple(
                                AppRepository.formatDurationShort(stats.totalTimeSeconds),
                                "${stats.totalSessionsCount} Total Sessions",
                                "All Time"
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Total Time",
                                value = timeVal,
                                subtitle = periodLabel,
                                icon = Icons.Filled.AccessTime,
                                iconTint = IndigoPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Streak",
                                value = "${stats.currentStreakDays} Days 🔥",
                                subtitle = "Longest: ${stats.longestStreakDays} Days",
                                icon = Icons.Filled.LocalFireDepartment,
                                iconTint = FlameOrange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Additional key figures
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Most Studied",
                                value = stats.mostStudiedSubject,
                                subtitle = "Top Subject",
                                icon = Icons.Filled.Star,
                                iconTint = VioletTertiary,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Average Session",
                                value = AppRepository.formatDurationShort(stats.averageSessionSeconds),
                                subtitle = "Per Session",
                                icon = Icons.Filled.Speed,
                                iconTint = EmeraldAccent,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Goal Progress Tracker (Daily or Weekly depending on active tab)
                    if (selectedTab == StatsTab.DAILY) {
                        item {
                            DailyGoalTrackerCard(
                                goal = dailyGoal,
                                todayTimeSeconds = stats.todayTimeSeconds,
                                todaySessionsCount = stats.todaySessionsCount,
                                currentStreakDays = stats.currentStreakDays,
                                onOpenSetGoalDialog = {
                                    if (onOpenSetGoalDialog != null) {
                                        onOpenSetGoalDialog(GoalPeriod.DAILY)
                                    } else {
                                        onOpenSetWeeklyGoalDialog?.invoke()
                                    }
                                },
                                onStartStudy = onStartStudy
                            )
                        }
                    } else if (selectedTab == StatsTab.WEEKLY) {
                        item {
                            WeeklyGoalTrackerCard(
                                goal = weeklyGoal,
                                weeklyTimeSeconds = stats.weeklyTimeSeconds,
                                totalSessionsCount = stats.totalSessionsCount,
                                onOpenSetGoalDialog = {
                                    if (onOpenSetGoalDialog != null) {
                                        onOpenSetGoalDialog(GoalPeriod.WEEKLY)
                                    } else {
                                        onOpenSetWeeklyGoalDialog?.invoke()
                                    }
                                },
                                onStartStudy = onStartStudy
                            )
                        }
                    }

                    // Weekly Bar Graph Chart
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "📈 Weekly Study Hours",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Mon - Sun",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                BarChartComposable(
                                    dailyData = stats.dailyActivity,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Streak Calendar Card with direct shortcut to full study calendar
                    item {
                        StreakCalendarCard(
                            currentStreak = stats.currentStreakDays,
                            longestStreak = stats.longestStreakDays,
                            activeDates = stats.activeDates,
                            onOpenFullCalendar = { currentMode = StatsViewMode.CALENDAR },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Subject Breakdown Progress List
                    item {
                        Text(
                            text = "📚 Subject Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (stats.subjectBreakdown.isEmpty()) {
                        item {
                            Text(
                                text = "No study data recorded yet.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(stats.subjectBreakdown) { subStat ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
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
                                                    .background(IndigoPrimary)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = subStat.subjectName,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = AppRepository.formatDurationShort(subStat.totalTimeSeconds),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress = { subStat.percentage / 100f },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = IndigoPrimary,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${subStat.sessionCount} sessions",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${String.format("%.1f", subStat.percentage)}% of total",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }

        // Full Date Details Dialog ("open full that date")
        activeFullDateData?.let { dayData ->
            FullDateDetailsDialog(
                dayData = dayData,
                onDismiss = { activeFullDateData = null },
                onDeleteSession = { sessionId ->
                    onDeleteSession?.invoke(sessionId)
                },
                onAddSession = {
                    dateToLogSession = dayData.dateString
                }
            )
        }

        // Manual Session Log Dialog for a specific date
        dateToLogSession?.let { dateStr ->
            ManualSessionDialog(
                subjects = subjects,
                initialDate = dateStr,
                onSaveSession = { subName, duration, title, notes, dt ->
                    onAddManualSession?.invoke(subName, duration, title, notes, dt)
                    dateToLogSession = null
                },
                onDismiss = { dateToLogSession = null }
            )
        }
    }
}

