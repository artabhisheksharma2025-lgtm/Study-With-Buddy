package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyGoalEntity
import com.example.data.repository.AppRepository
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.IndigoPrimary
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyGoalTrackerCard(
    dailyGoals: List<StudyGoalEntity> = emptyList(),
    weeklyGoals: List<StudyGoalEntity> = emptyList(),
    dailyGoal: StudyGoalEntity? = null,
    weeklyGoal: StudyGoalEntity? = null,
    todayTimeSeconds: Long = 0L,
    weeklyTimeSeconds: Long = 0L,
    todaySubjectTimes: Map<String, Long> = emptyMap(),
    weeklySubjectTimes: Map<String, Long> = emptyMap(),
    todaySessionsCount: Int = 0,
    totalSessionsCount: Int = 0,
    currentStreakDays: Int = 0,
    initialPeriod: GoalPeriod = GoalPeriod.DAILY,
    onOpenSetGoalDialog: (GoalPeriod) -> Unit,
    onEditGoal: ((StudyGoalEntity) -> Unit)? = null,
    onDeleteGoal: ((StudyGoalEntity) -> Unit)? = null,
    onStartStudy: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedPeriod by remember { mutableStateOf(initialPeriod) }

    // Combine list or fallback to single goal
    val effectiveDailyGoals = if (dailyGoals.isNotEmpty()) dailyGoals else listOfNotNull(dailyGoal)
    val effectiveWeeklyGoals = if (weeklyGoals.isNotEmpty()) weeklyGoals else listOfNotNull(weeklyGoal)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("study_goal_tracker_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Period Toggle: [ ☀️ Daily Goals (count) ]  [ 📅 Weekly Goals (count) ]
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
            ) {
                SegmentedButton(
                    selected = selectedPeriod == GoalPeriod.DAILY,
                    onClick = { selectedPeriod = GoalPeriod.DAILY },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    modifier = Modifier.testTag("goal_tracker_tab_daily")
                ) {
                    val countStr = if (effectiveDailyGoals.isNotEmpty()) " (${effectiveDailyGoals.size})" else ""
                    Text("☀️ Daily Goals$countStr", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                SegmentedButton(
                    selected = selectedPeriod == GoalPeriod.WEEKLY,
                    onClick = { selectedPeriod = GoalPeriod.WEEKLY },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    modifier = Modifier.testTag("goal_tracker_tab_weekly")
                ) {
                    val countStr = if (effectiveWeeklyGoals.isNotEmpty()) " (${effectiveWeeklyGoals.size})" else ""
                    Text("📅 Weekly Goals$countStr", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            AnimatedContent(
                targetState = selectedPeriod,
                label = "goal_period_content"
            ) { period ->
                if (period == GoalPeriod.DAILY) {
                    MultipleDailyGoalsContent(
                        goals = effectiveDailyGoals,
                        todayTimeSeconds = todayTimeSeconds,
                        todaySubjectTimes = todaySubjectTimes,
                        todaySessionsCount = todaySessionsCount,
                        currentStreakDays = currentStreakDays,
                        onAddNewGoal = { onOpenSetGoalDialog(GoalPeriod.DAILY) },
                        onEditGoal = onEditGoal ?: { onOpenSetGoalDialog(GoalPeriod.DAILY) },
                        onDeleteGoal = onDeleteGoal,
                        onStartStudy = onStartStudy
                    )
                } else {
                    MultipleWeeklyGoalsContent(
                        goals = effectiveWeeklyGoals,
                        weeklyTimeSeconds = weeklyTimeSeconds,
                        weeklySubjectTimes = weeklySubjectTimes,
                        totalSessionsCount = totalSessionsCount,
                        onAddNewGoal = { onOpenSetGoalDialog(GoalPeriod.WEEKLY) },
                        onEditGoal = onEditGoal ?: { onOpenSetGoalDialog(GoalPeriod.WEEKLY) },
                        onDeleteGoal = onDeleteGoal,
                        onStartStudy = onStartStudy
                    )
                }
            }
        }
    }
}

@Composable
private fun MultipleDailyGoalsContent(
    goals: List<StudyGoalEntity>,
    todayTimeSeconds: Long,
    todaySubjectTimes: Map<String, Long>,
    todaySessionsCount: Int,
    currentStreakDays: Int,
    onAddNewGoal: () -> Unit,
    onEditGoal: (StudyGoalEntity) -> Unit,
    onDeleteGoal: ((StudyGoalEntity) -> Unit)?,
    onStartStudy: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header with "+ Add Daily Goal" action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.WbSunny,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Today's Study Targets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${AppRepository.formatDurationShort(todayTimeSeconds)} studied today • $todaySessionsCount sessions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FilledTonalButton(
                onClick = onAddNewGoal,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("add_daily_goal_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Goal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (goals.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.Flag, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("No daily goals created yet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Set daily targets (e.g. 1h Math, 2h Focus) to stay consistent.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onAddNewGoal, shape = RoundedCornerShape(10.dp)) {
                        Text("+ Create First Daily Goal")
                    }
                }
            }
        } else {
            // Render each daily goal with its own progress bar
            goals.forEach { goal ->
                val currentSec = if (goal.subjectName.isNotBlank()) {
                    todaySubjectTimes[goal.subjectName] ?: 0L
                } else {
                    todayTimeSeconds
                }
                SingleGoalProgressBarItem(
                    goal = goal,
                    currentSeconds = currentSec,
                    isDaily = true,
                    onEdit = { onEditGoal(goal) },
                    onDelete = if (onDeleteGoal != null) { { onDeleteGoal(goal) } } else null
                )
            }
        }

        if (onStartStudy != null) {
            Button(
                onClick = onStartStudy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Studying Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MultipleWeeklyGoalsContent(
    goals: List<StudyGoalEntity>,
    weeklyTimeSeconds: Long,
    weeklySubjectTimes: Map<String, Long>,
    totalSessionsCount: Int,
    onAddNewGoal: () -> Unit,
    onEditGoal: (StudyGoalEntity) -> Unit,
    onDeleteGoal: ((StudyGoalEntity) -> Unit)?,
    onStartStudy: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Header with "+ Add Weekly Goal" action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(IndigoPrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = IndigoPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column {
                    Text(
                        text = "Weekly Study Targets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${AppRepository.formatDurationShort(weeklyTimeSeconds)} studied this week",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            FilledTonalButton(
                onClick = onAddNewGoal,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("add_weekly_goal_button")
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Goal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (goals.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Filled.TrackChanges, contentDescription = null, tint = IndigoPrimary, modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("No weekly goals created yet", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        "Set weekly targets (e.g. 20h Overall, 8h Chemistry) to hit your milestones.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onAddNewGoal, shape = RoundedCornerShape(10.dp)) {
                        Text("+ Create First Weekly Goal")
                    }
                }
            }
        } else {
            // Render each weekly goal with its own progress bar
            goals.forEach { goal ->
                val currentSec = if (goal.subjectName.isNotBlank()) {
                    weeklySubjectTimes[goal.subjectName] ?: 0L
                } else {
                    weeklyTimeSeconds
                }
                SingleGoalProgressBarItem(
                    goal = goal,
                    currentSeconds = currentSec,
                    isDaily = false,
                    onEdit = { onEditGoal(goal) },
                    onDelete = if (onDeleteGoal != null) { { onDeleteGoal(goal) } } else null
                )
            }
        }

        if (onStartStudy != null) {
            Button(
                onClick = onStartStudy,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Studying Now", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SingleGoalProgressBarItem(
    goal: StudyGoalEntity,
    currentSeconds: Long,
    isDaily: Boolean,
    onEdit: () -> Unit,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val targetMinutes = goal.targetDurationMinutes.coerceAtLeast(10)
    val targetSec = targetMinutes * 60L
    val targetHours = targetMinutes / 60f
    val currentHours = currentSeconds / 3600f

    val progressFraction = if (targetSec > 0) {
        (currentSeconds.toFloat() / targetSec.toFloat()).coerceAtLeast(0f)
    } else 0f
    val clampedProgress = progressFraction.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 700),
        label = "goal_item_progress_${goal.goalId}"
    )

    val percentage = (progressFraction * 100).toInt()
    val remainingSec = (targetSec - currentSeconds).coerceAtLeast(0L)

    val (statusLabel, statusColor, statusBg) = when {
        percentage >= 100 -> Triple("🎉 Completed!", EmeraldAccent, Color(0xFFDCFCE7))
        percentage >= 75 -> Triple("🔥 Almost There!", Color(0xFF0D9488), Color(0xFFCCFBF1))
        percentage >= 40 -> Triple("⚡ On Track", IndigoPrimary, Color(0xFFE0E7FF))
        else -> Triple("💪 In Progress", FlameOrange, Color(0xFFFFEDD5))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
            .fillMaxWidth()
            .testTag("goal_item_${goal.goalId}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Title & Subject Badge & Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (goal.subjectName.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = goal.subjectName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = if (percentage >= 100) "Goal crushed! 🎉"
                        else if (isDaily) "${AppRepository.formatDurationShort(remainingSec)} remaining today"
                        else "${AppRepository.formatDurationShort(remainingSec)} left this week",
                        fontSize = 11.sp,
                        color = if (percentage >= 100) EmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(30.dp)
                            .testTag("edit_goal_${goal.goalId}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Goal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    if (onDelete != null) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier
                                .size(30.dp)
                                .testTag("delete_goal_${goal.goalId}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "Delete Goal",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Studied vs Target & Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "${AppRepository.formatDurationShort(currentSeconds)} / ${if (targetHours % 1f == 0f) "${targetHours.toInt()}h" else String.format(Locale.getDefault(), "%.1fh", targetHours)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$percentage%",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Multi-tier Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .testTag("progress_bar_${goal.goalId}")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            Brush.horizontalGradient(
                                if (percentage >= 100) {
                                    listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF34D399))
                                } else {
                                    listOf(IndigoPrimary, Color(0xFF6366F1), EmeraldAccent)
                                }
                            )
                        )
                )

                // Divider ticks (25%, 50%, 75%)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.35f)))
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.35f)))
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.35f)))
                }
            }

            // Milestone Labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0%", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                Text("25%", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                Text("50%", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                Text("75%", fontSize = 8.sp, color = MaterialTheme.colorScheme.outline)
                Text("100%", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = if (percentage >= 100) EmeraldAccent else MaterialTheme.colorScheme.outline)
            }
        }
    }
}

// Backward-compatible individual cards
@Composable
fun DailyGoalTrackerCard(
    goal: StudyGoalEntity?,
    todayTimeSeconds: Long,
    todaySessionsCount: Int = 0,
    currentStreakDays: Int = 0,
    onOpenSetGoalDialog: () -> Unit,
    onStartStudy: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    StudyGoalTrackerCard(
        dailyGoal = goal,
        todayTimeSeconds = todayTimeSeconds,
        todaySessionsCount = todaySessionsCount,
        currentStreakDays = currentStreakDays,
        initialPeriod = GoalPeriod.DAILY,
        onOpenSetGoalDialog = { onOpenSetGoalDialog() },
        onStartStudy = onStartStudy,
        modifier = modifier
    )
}

@Composable
fun WeeklyGoalTrackerCard(
    goal: StudyGoalEntity?,
    weeklyTimeSeconds: Long,
    totalSessionsCount: Int,
    onOpenSetGoalDialog: () -> Unit,
    onStartStudy: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    StudyGoalTrackerCard(
        weeklyGoal = goal,
        weeklyTimeSeconds = weeklyTimeSeconds,
        totalSessionsCount = totalSessionsCount,
        initialPeriod = GoalPeriod.WEEKLY,
        onOpenSetGoalDialog = { onOpenSetGoalDialog() },
        onStartStudy = onStartStudy,
        modifier = modifier
    )
}
