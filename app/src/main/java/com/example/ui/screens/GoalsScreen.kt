package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyGoalEntity
import com.example.data.util.UserStudyStats
import com.example.ui.components.GoalPeriod
import com.example.ui.components.SetStudyGoalDialog
import com.example.ui.components.SingleGoalProgressBarItem
import com.example.ui.viewmodel.MainViewModel

enum class GoalsFilter(val label: String) {
    ALL("All Goals"),
    DAILY("☀️ Daily Goals"),
    WEEKLY("📅 Weekly Goals")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    mainViewModel: MainViewModel,
    stats: UserStudyStats,
    onOpenCreateGoalDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dailyGoals by mainViewModel.dailyGoals.collectAsState()
    val weeklyGoals by mainViewModel.weeklyGoals.collectAsState()
    val subjects by mainViewModel.subjects.collectAsState()

    var activeFilter by remember { mutableStateOf(GoalsFilter.ALL) }
    var showSetGoalDialog by remember { mutableStateOf(false) }
    var selectedGoalPeriod by remember { mutableStateOf(GoalPeriod.DAILY) }
    var goalToEdit by remember { mutableStateOf<StudyGoalEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🎯 Study Goals", fontWeight = FontWeight.Bold) },
                actions = {
                    FilledTonalButton(
                        onClick = {
                            goalToEdit = null
                            selectedGoalPeriod = GoalPeriod.DAILY
                            showSetGoalDialog = true
                        },
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("set_target_top_button"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New Goal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    goalToEdit = null
                    selectedGoalPeriod = GoalPeriod.DAILY
                    showSetGoalDialog = true
                },
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add Study Goal") },
                modifier = Modifier.testTag("add_goal_fab")
            )
        },
        modifier = modifier.testTag("screen_goals")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Filter Pills: All Goals, Daily, Weekly
            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    GoalsFilter.entries.forEachIndexed { index, filter ->
                        SegmentedButton(
                            selected = activeFilter == filter,
                            onClick = { activeFilter = filter },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = GoalsFilter.entries.size),
                            modifier = Modifier.testTag("goals_filter_${filter.name.lowercase()}")
                        ) {
                            val count = when (filter) {
                                GoalsFilter.ALL -> dailyGoals.size + weeklyGoals.size
                                GoalsFilter.DAILY -> dailyGoals.size
                                GoalsFilter.WEEKLY -> weeklyGoals.size
                            }
                            Text("${filter.label} ($count)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Quick Create Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            goalToEdit = null
                            selectedGoalPeriod = GoalPeriod.DAILY
                            showSetGoalDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_daily_goal_button")
                    ) {
                        Icon(Icons.Filled.WbSunny, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Daily Goal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            goalToEdit = null
                            selectedGoalPeriod = GoalPeriod.WEEKLY
                            showSetGoalDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("quick_add_weekly_goal_button")
                    ) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Weekly Goal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // DAILY STUDY GOALS SECTION
            if (activeFilter == GoalsFilter.ALL || activeFilter == GoalsFilter.DAILY) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("☀️ Daily Study Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "${dailyGoals.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                goalToEdit = null
                                selectedGoalPeriod = GoalPeriod.DAILY
                                showSetGoalDialog = true
                            }
                        ) {
                            Text("+ Add Daily")
                        }
                    }
                }

                if (dailyGoals.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.WbSunny, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No daily study goals set", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Add targets for subjects or overall hours to conquer each day.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        goalToEdit = null
                                        selectedGoalPeriod = GoalPeriod.DAILY
                                        showSetGoalDialog = true
                                    }
                                ) {
                                    Text("+ Set First Daily Goal")
                                }
                            }
                        }
                    }
                } else {
                    items(dailyGoals, key = { it.goalId }) { goal ->
                        val currentSec = if (goal.subjectName.isNotBlank()) {
                            stats.todaySubjectTimes[goal.subjectName] ?: 0L
                        } else {
                            stats.todayTimeSeconds
                        }
                        SingleGoalProgressBarItem(
                            goal = goal,
                            currentSeconds = currentSec,
                            isDaily = true,
                            onEdit = {
                                goalToEdit = goal
                                selectedGoalPeriod = GoalPeriod.DAILY
                                showSetGoalDialog = true
                            },
                            onDelete = { mainViewModel.deleteGoal(goal) }
                        )
                    }
                }
            }

            // WEEKLY STUDY GOALS SECTION
            if (activeFilter == GoalsFilter.ALL || activeFilter == GoalsFilter.WEEKLY) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("📅 Weekly Study Goals", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "${weeklyGoals.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        TextButton(
                            onClick = {
                                goalToEdit = null
                                selectedGoalPeriod = GoalPeriod.WEEKLY
                                showSetGoalDialog = true
                            }
                        ) {
                            Text("+ Add Weekly")
                        }
                    }
                }

                if (weeklyGoals.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(20.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Filled.DateRange, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No weekly study goals set", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Set weekly targets to build long-term study momentum.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        goalToEdit = null
                                        selectedGoalPeriod = GoalPeriod.WEEKLY
                                        showSetGoalDialog = true
                                    }
                                ) {
                                    Text("+ Set First Weekly Goal")
                                }
                            }
                        }
                    }
                } else {
                    items(weeklyGoals, key = { it.goalId }) { goal ->
                        val currentSec = if (goal.subjectName.isNotBlank()) {
                            stats.weeklySubjectTimes[goal.subjectName] ?: 0L
                        } else {
                            stats.weeklyTimeSeconds
                        }
                        SingleGoalProgressBarItem(
                            goal = goal,
                            currentSeconds = currentSec,
                            isDaily = false,
                            onEdit = {
                                goalToEdit = goal
                                selectedGoalPeriod = GoalPeriod.WEEKLY
                                showSetGoalDialog = true
                            },
                            onDelete = { mainViewModel.deleteGoal(goal) }
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(56.dp)) }
        }

        if (showSetGoalDialog) {
            val dailyHrs = (dailyGoals.firstOrNull()?.targetDurationMinutes ?: (2 * 60)) / 60f
            val weeklyHrs = (weeklyGoals.firstOrNull()?.targetDurationMinutes ?: (20 * 60)) / 60f

            SetStudyGoalDialog(
                initialPeriod = selectedGoalPeriod,
                initialDailyHours = dailyHrs,
                initialWeeklyHours = weeklyHrs,
                subjects = subjects,
                goalToEdit = goalToEdit,
                onSaveGoalDetailed = { period, hours, title, subjectName, goalId ->
                    if (period == GoalPeriod.DAILY) {
                        mainViewModel.addOrUpdateDailyGoal(
                            title = title,
                            targetHours = hours,
                            subjectName = subjectName,
                            goalId = goalId
                        )
                    } else {
                        mainViewModel.addOrUpdateWeeklyGoal(
                            title = title,
                            targetHours = hours,
                            subjectName = subjectName,
                            goalId = goalId
                        )
                    }
                },
                onDismiss = {
                    showSetGoalDialog = false
                    goalToEdit = null
                }
            )
        }
    }
}
