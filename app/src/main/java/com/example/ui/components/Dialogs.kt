package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.SubjectEntity

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
