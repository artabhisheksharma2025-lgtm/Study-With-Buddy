package com.example.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyGoalEntity
import com.example.data.model.SubjectEntity
import java.util.Locale

@Composable
fun ManualSessionDialog(
    subjects: List<SubjectEntity>,
    onSaveSession: (subjectName: String, durationMinutes: Long, title: String, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedSubjectName by remember { mutableStateOf(subjects.firstOrNull()?.name ?: "Mathematics") }
    var durationMinutesInput by remember { mutableStateOf("45") }
    var titleInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    var showSubjectMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("➕ Add Study Session", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Select Subject Dropdown
                Text("Select Subject:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Box {
                    OutlinedButton(
                        onClick = { showSubjectMenu = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("manual_subject_selector")
                    ) {
                        Text(selectedSubjectName)
                    }

                    DropdownMenu(
                        expanded = showSubjectMenu,
                        onDismissRequest = { showSubjectMenu = false }
                    ) {
                        subjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name) },
                                onClick = {
                                    selectedSubjectName = sub.name
                                    showSubjectMenu = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = durationMinutesInput,
                    onValueChange = { durationMinutesInput = it },
                    label = { Text("Duration (Minutes)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_duration_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Session Title (Optional)") },
                    placeholder = { Text("e.g. Chapter 4 Practice Problems") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_title_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Formula review, chapter summary...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manual_notes_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = durationMinutesInput.toLongOrNull() ?: 0L
                    if (duration > 0) {
                        onSaveSession(selectedSubjectName, duration, titleInput, notesInput)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("manual_save_button")
            ) {
                Text("Save Session")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun AddGoalDialog(
    subjects: List<SubjectEntity>,
    onCreateGoal: (title: String, targetHours: Int, targetSessions: Int, daysCount: Int, subjectName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var titleInput by remember { mutableStateOf("Weekly Math Goal") }
    var targetHoursInput by remember { mutableStateOf("10") }
    var targetSessionsInput by remember { mutableStateOf("5") }
    var daysCountInput by remember { mutableStateOf("7") }
    var selectedSubjectName by remember { mutableStateOf("All Subjects") }

    var showSubjectMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎯 Create Study Goal", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Goal Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = targetHoursInput,
                        onValueChange = { targetHoursInput = it },
                        label = { Text("Target Hours") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = targetSessionsInput,
                        onValueChange = { targetSessionsInput = it },
                        label = { Text("Target Sessions") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = daysCountInput,
                    onValueChange = { daysCountInput = it },
                    label = { Text("Duration (Days)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Specific Subject (Optional):", style = MaterialTheme.typography.labelMedium)
                Box {
                    OutlinedButton(
                        onClick = { showSubjectMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedSubjectName)
                    }

                    DropdownMenu(
                        expanded = showSubjectMenu,
                        onDismissRequest = { showSubjectMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Subjects") },
                            onClick = {
                                selectedSubjectName = "All Subjects"
                                showSubjectMenu = false
                            }
                        )
                        subjects.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub.name) },
                                onClick = {
                                    selectedSubjectName = sub.name
                                    showSubjectMenu = false
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hrs = targetHoursInput.toIntOrNull() ?: 10
                    val sess = targetSessionsInput.toIntOrNull() ?: 5
                    val days = daysCountInput.toIntOrNull() ?: 7
                    onCreateGoal(titleInput, hrs, sess, days, if (selectedSubjectName == "All Subjects") "" else selectedSubjectName)
                    onDismiss()
                }
            ) {
                Text("Create Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

enum class GoalPeriod(val title: String) {
    DAILY("Daily Goal"),
    WEEKLY("Weekly Goal")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetStudyGoalDialog(
    initialPeriod: GoalPeriod = GoalPeriod.DAILY,
    initialDailyHours: Float = 2.0f,
    initialWeeklyHours: Float = 20.0f,
    initialTitle: String = "",
    initialSubject: String = "",
    subjects: List<SubjectEntity> = emptyList(),
    goalToEdit: StudyGoalEntity? = null,
    onSaveGoal: (period: GoalPeriod, hours: Float, title: String) -> Unit = { _, _, _ -> },
    onSaveGoalDetailed: ((period: GoalPeriod, hours: Float, title: String, subjectName: String, goalId: String?) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val isEditMode = goalToEdit != null
    val determinedInitialPeriod = if (goalToEdit != null) {
        if (goalToEdit.goalId.startsWith("daily_") || goalToEdit.title.contains("daily", ignoreCase = true) || (goalToEdit.endDate - goalToEdit.startDate) <= 129600000L) {
            GoalPeriod.DAILY
        } else {
            GoalPeriod.WEEKLY
        }
    } else {
        initialPeriod
    }

    var selectedPeriod by remember { mutableStateOf(determinedInitialPeriod) }
    var selectedSubject by remember { mutableStateOf(goalToEdit?.subjectName ?: initialSubject) }
    var showSubjectDropdown by remember { mutableStateOf(false) }

    val initialHrs = if (goalToEdit != null) goalToEdit.targetDurationMinutes / 60f else if (determinedInitialPeriod == GoalPeriod.DAILY) initialDailyHours else initialWeeklyHours

    var dailyTitleInput by remember {
        mutableStateOf(
            if (goalToEdit != null && determinedInitialPeriod == GoalPeriod.DAILY) goalToEdit.title
            else if (initialTitle.isNotBlank()) initialTitle
            else if (selectedSubject.isNotBlank()) "Daily $selectedSubject Goal"
            else "Daily Study Target"
        )
    }
    var weeklyTitleInput by remember {
        mutableStateOf(
            if (goalToEdit != null && determinedInitialPeriod == GoalPeriod.WEEKLY) goalToEdit.title
            else if (initialTitle.isNotBlank()) initialTitle
            else if (selectedSubject.isNotBlank()) "Weekly $selectedSubject Goal"
            else "Weekly Study Target"
        )
    }

    var dailySliderValue by remember { mutableStateOf(if (determinedInitialPeriod == GoalPeriod.DAILY) initialHrs.coerceIn(0.5f, 12f) else initialDailyHours.coerceIn(0.5f, 12f)) }
    var weeklySliderValue by remember { mutableStateOf(if (determinedInitialPeriod == GoalPeriod.WEEKLY) initialHrs.coerceIn(5f, 60f) else initialWeeklyHours.coerceIn(5f, 60f)) }

    var dailyHoursInput by remember {
        val hrs = if (determinedInitialPeriod == GoalPeriod.DAILY) initialHrs else initialDailyHours
        mutableStateOf(if (hrs % 1f == 0f) "${hrs.toInt()}" else String.format(Locale.getDefault(), "%.1f", hrs))
    }
    var weeklyHoursInput by remember {
        val hrs = if (determinedInitialPeriod == GoalPeriod.WEEKLY) initialHrs else initialWeeklyHours
        mutableStateOf(String.format(Locale.getDefault(), "%.0f", hrs.coerceAtLeast(5f)))
    }

    val currentDailyHours = dailyHoursInput.toFloatOrNull() ?: dailySliderValue
    val currentWeeklyHours = weeklyHoursInput.toFloatOrNull() ?: weeklySliderValue
    val dailyPaceForWeekly = currentWeeklyHours / 7f

    val dailyPresets = listOf(1.0f, 1.5f, 2.0f, 3.0f, 4.0f, 5.0f)
    val weeklyPresets = listOf(10f, 15f, 20f, 25f, 30f, 40f)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.TrackChanges,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isEditMode) {
                        if (selectedPeriod == GoalPeriod.DAILY) "Edit Daily Study Goal" else "Edit Weekly Study Goal"
                    } else {
                        if (selectedPeriod == GoalPeriod.DAILY) "🎯 Add Daily Study Goal" else "🎯 Add Weekly Study Goal"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Segmented Selector for Daily vs Weekly Goal
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SegmentedButton(
                        selected = selectedPeriod == GoalPeriod.DAILY,
                        onClick = {
                            selectedPeriod = GoalPeriod.DAILY
                            if (dailyTitleInput.isBlank() || dailyTitleInput.contains("Weekly")) {
                                dailyTitleInput = if (selectedSubject.isNotBlank()) "Daily $selectedSubject Goal" else "Daily Study Target"
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        modifier = Modifier.testTag("goal_period_daily")
                    ) {
                        Text("☀️ Daily Goal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    SegmentedButton(
                        selected = selectedPeriod == GoalPeriod.WEEKLY,
                        onClick = {
                            selectedPeriod = GoalPeriod.WEEKLY
                            if (weeklyTitleInput.isBlank() || weeklyTitleInput.contains("Daily")) {
                                weeklyTitleInput = if (selectedSubject.isNotBlank()) "Weekly $selectedSubject Goal" else "Weekly Study Target"
                            }
                        },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        modifier = Modifier.testTag("goal_period_weekly")
                    ) {
                        Text("📅 Weekly Goal", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Subject Selection Dropdown (Optional / Specific Subject)
                if (subjects.isNotEmpty()) {
                    Column {
                        Text("Target Subject:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box {
                            OutlinedButton(
                                onClick = { showSubjectDropdown = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("goal_subject_selector"),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(if (selectedSubject.isBlank()) "All Subjects (Overall)" else selectedSubject)
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                                }
                            }

                            DropdownMenu(
                                expanded = showSubjectDropdown,
                                onDismissRequest = { showSubjectDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("All Subjects (Overall)") },
                                    onClick = {
                                        selectedSubject = ""
                                        showSubjectDropdown = false
                                    }
                                )
                                subjects.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.name) },
                                        onClick = {
                                            selectedSubject = sub.name
                                            showSubjectDropdown = false
                                            if (selectedPeriod == GoalPeriod.DAILY && (dailyTitleInput.isBlank() || dailyTitleInput == "Daily Study Target" || dailyTitleInput.startsWith("Daily "))) {
                                                dailyTitleInput = "Daily ${sub.name} Goal"
                                            } else if (selectedPeriod == GoalPeriod.WEEKLY && (weeklyTitleInput.isBlank() || weeklyTitleInput == "Weekly Study Target" || weeklyTitleInput.startsWith("Weekly "))) {
                                                weeklyTitleInput = "Weekly ${sub.name} Goal"
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedPeriod == GoalPeriod.DAILY) {
                    // --- DAILY GOAL CONFIGURATION ---
                    OutlinedTextField(
                        value = dailyTitleInput,
                        onValueChange = { dailyTitleInput = it },
                        label = { Text("Goal Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_goal_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Large Hours Display
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (currentDailyHours % 1f == 0f) "${currentDailyHours.toInt()} Hours" else "${String.format(Locale.getDefault(), "%.1f", currentDailyHours)} Hours",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            val totalMinutes = (currentDailyHours * 60).toInt()
                            Text(
                                text = if (selectedSubject.isNotBlank()) "$selectedSubject • ~$totalMinutes minutes today" else "Today's Target (~$totalMinutes minutes)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Daily Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Adjust Target", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", dailySliderValue)}h",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = dailySliderValue,
                            onValueChange = {
                                dailySliderValue = it
                                dailyHoursInput = if (it % 1f == 0f) "${it.toInt()}" else String.format(Locale.getDefault(), "%.1f", it)
                            },
                            valueRange = 0.5f..12f,
                            steps = 22,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("daily_goal_slider")
                        )
                    }

                    // Daily Presets
                    Text("Popular Daily Targets:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dailyPresets.take(3).forEach { target ->
                            val label = if (target % 1f == 0f) "${target.toInt()}h" else "${target}h"
                            SuggestionChip(
                                onClick = {
                                    dailySliderValue = target
                                    dailyHoursInput = if (target % 1f == 0f) "${target.toInt()}" else "$target"
                                },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        dailyPresets.drop(3).forEach { target ->
                            val label = if (target % 1f == 0f) "${target.toInt()}h" else "${target}h"
                            SuggestionChip(
                                onClick = {
                                    dailySliderValue = target
                                    dailyHoursInput = if (target % 1f == 0f) "${target.toInt()}" else "$target"
                                },
                                label = { Text(label) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Custom Input
                    OutlinedTextField(
                        value = dailyHoursInput,
                        onValueChange = {
                            dailyHoursInput = it
                            it.toFloatOrNull()?.let { num ->
                                if (num in 0.5f..24f) {
                                    dailySliderValue = num.coerceIn(0.5f, 12f)
                                }
                            }
                        },
                        label = { Text("Custom Daily Target (Hours)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_goal_custom_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else {
                    // --- WEEKLY GOAL CONFIGURATION ---
                    OutlinedTextField(
                        value = weeklyTitleInput,
                        onValueChange = { weeklyTitleInput = it },
                        label = { Text("Goal Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weekly_goal_title_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Large Hours Display with Daily Pace
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${String.format(Locale.getDefault(), "%.1f", currentWeeklyHours)} Hours",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (selectedSubject.isNotBlank()) "$selectedSubject • ~${String.format(Locale.getDefault(), "%.1f", dailyPaceForWeekly)} hrs/day" else "Weekly Target (~${String.format(Locale.getDefault(), "%.1f", dailyPaceForWeekly)} hours/day)",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Interactive Slider
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fine-tune Target", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${weeklySliderValue.toInt()}h", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        Slider(
                            value = weeklySliderValue,
                            onValueChange = {
                                weeklySliderValue = it
                                weeklyHoursInput = String.format(Locale.getDefault(), "%.0f", it)
                            },
                            valueRange = 5f..60f,
                            steps = 54,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("weekly_goal_slider")
                        )
                    }

                    // Quick Presets
                    Text("Popular Weekly Targets:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        weeklyPresets.take(3).forEach { target ->
                            SuggestionChip(
                                onClick = {
                                    weeklySliderValue = target
                                    weeklyHoursInput = target.toInt().toString()
                                },
                                label = { Text("${target.toInt()}h") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        weeklyPresets.drop(3).forEach { target ->
                            SuggestionChip(
                                onClick = {
                                    weeklySliderValue = target
                                    weeklyHoursInput = target.toInt().toString()
                                },
                                label = { Text("${target.toInt()}h") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Manual Input
                    OutlinedTextField(
                        value = weeklyHoursInput,
                        onValueChange = {
                            weeklyHoursInput = it
                            it.toFloatOrNull()?.let { num ->
                                if (num in 1f..100f) {
                                    weeklySliderValue = num.coerceIn(5f, 60f)
                                }
                            }
                        },
                        label = { Text("Custom Weekly Target (Hours)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weekly_goal_custom_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedPeriod == GoalPeriod.DAILY) {
                        val finalHours = dailyHoursInput.toFloatOrNull() ?: dailySliderValue
                        if (finalHours > 0) {
                            if (onSaveGoalDetailed != null) {
                                onSaveGoalDetailed(GoalPeriod.DAILY, finalHours, dailyTitleInput.trim(), selectedSubject, goalToEdit?.goalId)
                            } else {
                                onSaveGoal(GoalPeriod.DAILY, finalHours, dailyTitleInput.trim())
                            }
                            onDismiss()
                        }
                    } else {
                        val finalHours = weeklyHoursInput.toFloatOrNull() ?: weeklySliderValue
                        if (finalHours > 0) {
                            if (onSaveGoalDetailed != null) {
                                onSaveGoalDetailed(GoalPeriod.WEEKLY, finalHours, weeklyTitleInput.trim(), selectedSubject, goalToEdit?.goalId)
                            } else {
                                onSaveGoal(GoalPeriod.WEEKLY, finalHours, weeklyTitleInput.trim())
                            }
                            onDismiss()
                        }
                    }
                },
                enabled = if (selectedPeriod == GoalPeriod.DAILY) {
                    (dailyHoursInput.toFloatOrNull() ?: 0f) > 0f
                } else {
                    (weeklyHoursInput.toFloatOrNull() ?: 0f) > 0f
                },
                modifier = Modifier.testTag("save_study_goal_button")
            ) {
                Text(
                    if (isEditMode) "Update Goal"
                    else if (selectedPeriod == GoalPeriod.DAILY) "Create Daily Goal"
                    else "Create Weekly Goal"
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_study_goal_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SetWeeklyGoalDialog(
    initialTargetHours: Float,
    initialTitle: String = "Weekly Study Target",
    onSaveGoal: (hours: Float, title: String) -> Unit,
    onDismiss: () -> Unit
) {
    SetStudyGoalDialog(
        initialPeriod = GoalPeriod.WEEKLY,
        initialWeeklyHours = initialTargetHours,
        initialTitle = initialTitle,
        onSaveGoal = { _, hours, title ->
            onSaveGoal(hours, title)
        },
        onDismiss = onDismiss
    )
}
