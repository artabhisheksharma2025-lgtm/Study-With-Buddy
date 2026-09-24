package com.example.ui.components

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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

@Composable
fun SetWeeklyGoalDialog(
    initialTargetHours: Float,
    initialTitle: String = "Weekly Study Target",
    onSaveGoal: (hours: Float, title: String) -> Unit,
    onDismiss: () -> Unit
) {
    var titleInput by remember { mutableStateOf(initialTitle.ifBlank { "Weekly Study Target" }) }
    var hoursInput by remember { mutableStateOf(String.format(Locale.getDefault(), "%.0f", initialTargetHours.coerceAtLeast(5f))) }
    var sliderValue by remember { mutableStateOf(initialTargetHours.coerceIn(5f, 60f)) }

    val currentHours = hoursInput.toFloatOrNull() ?: sliderValue
    val dailyPace = currentHours / 7f

    val presetGoals = listOf(10f, 15f, 20f, 25f, 30f, 40f)

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
                Text("🎯 Set Weekly Study Goal", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Goal Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
                            text = "${String.format(Locale.getDefault(), "%.1f", currentHours)} Hours",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Weekly Target (~${String.format(Locale.getDefault(), "%.1f", dailyPace)} hours/day)",
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
                        Text("${sliderValue.toInt()}h", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = sliderValue,
                        onValueChange = {
                            sliderValue = it
                            hoursInput = String.format(Locale.getDefault(), "%.0f", it)
                        },
                        valueRange = 5f..60f,
                        steps = 54, // 1h step between 5 and 60
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Quick Presets
                Text("Popular Weekly Targets:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presetGoals.take(3).forEach { target ->
                        SuggestionChip(
                            onClick = {
                                sliderValue = target
                                hoursInput = target.toInt().toString()
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
                    presetGoals.drop(3).forEach { target ->
                        SuggestionChip(
                            onClick = {
                                sliderValue = target
                                hoursInput = target.toInt().toString()
                            },
                            label = { Text("${target.toInt()}h") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Manual Input
                OutlinedTextField(
                    value = hoursInput,
                    onValueChange = {
                        hoursInput = it
                        it.toFloatOrNull()?.let { num ->
                            if (num in 1f..100f) {
                                sliderValue = num.coerceIn(5f, 60f)
                            }
                        }
                    },
                    label = { Text("Custom Target Hours") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalHours = hoursInput.toFloatOrNull() ?: sliderValue
                    if (finalHours > 0) {
                        onSaveGoal(finalHours, titleInput.trim())
                        onDismiss()
                    }
                },
                enabled = (hoursInput.toFloatOrNull() ?: 0f) > 0f
            ) {
                Text("Save Weekly Goal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
