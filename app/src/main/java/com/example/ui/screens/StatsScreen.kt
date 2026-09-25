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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyGoalEntity
import com.example.data.repository.AppRepository
import com.example.data.util.UserStudyStats
import com.example.ui.components.BarChartComposable
import com.example.ui.components.DailyGoalTrackerCard
import com.example.ui.components.GoalPeriod
import com.example.ui.components.StatCard
import com.example.ui.components.StreakCalendarCard
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    stats: UserStudyStats,
    dailyGoal: StudyGoalEntity? = null,
    weeklyGoal: StudyGoalEntity? = null,
    onOpenSetGoalDialog: ((GoalPeriod) -> Unit)? = null,
    onOpenSetWeeklyGoalDialog: (() -> Unit)? = null,
    onStartStudy: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(StatsTab.WEEKLY) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("📊 Study Statistics", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        modifier = modifier.testTag("screen_stats")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
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

            // Streak Calendar Card
            item {
                StreakCalendarCard(
                    currentStreak = stats.currentStreakDays,
                    longestStreak = stats.longestStreakDays,
                    activeDates = stats.activeDates,
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
