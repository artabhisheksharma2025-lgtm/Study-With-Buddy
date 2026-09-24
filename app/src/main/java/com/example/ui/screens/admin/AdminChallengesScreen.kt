package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminEntity
import com.example.data.model.StudyChallengeEntity
import com.example.data.util.SecurityUtils
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminChallengesScreen(
    admin: AdminEntity,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val challenges by adminViewModel.challenges.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var editingChallenge by remember { mutableStateOf<StudyChallengeEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            if (SecurityUtils.canManageChallenges(admin.role)) {
                FloatingActionButton(onClick = {
                    editingChallenge = null
                    showDialog = true
                }) {
                    Icon(Icons.Filled.Add, contentDescription = "Create Challenge")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Study Challenges & Community Sprints (${challenges.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            if (challenges.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No study challenges created yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(challenges, key = { it.challengeId }) { ch ->
                        val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(Icons.Filled.MilitaryTech, contentDescription = null, tint = Color(0xFFF59E0B))
                                        Text(ch.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    }
                                    Surface(
                                        color = if (ch.status == "ACTIVE") Color(0xFFDCFCE7) else Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(4.dp)
                                    ) {
                                        Text(
                                            text = ch.status,
                                            color = if (ch.status == "ACTIVE") Color(0xFF15803D) else Color(0xFF64748B),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(ch.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Goal: ${ch.targetHours}h • ${ch.targetSessions} sessions • ${sdf.format(Date(ch.startDate))} - ${sdf.format(Date(ch.endDate))}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )

                                    if (SecurityUtils.canManageChallenges(admin.role)) {
                                        Row {
                                            IconButton(onClick = {
                                                editingChallenge = ch
                                                showDialog = true
                                            }) {
                                                Icon(Icons.Filled.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                                            }
                                            IconButton(onClick = { adminViewModel.deleteChallenge(ch) }) {
                                                Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        ChallengeEditDialog(
            challenge = editingChallenge,
            onDismiss = { showDialog = false },
            onSave = { ch ->
                adminViewModel.saveChallenge(ch)
                showDialog = false
            }
        )
    }
}

@Composable
fun ChallengeEditDialog(
    challenge: StudyChallengeEntity?,
    onDismiss: () -> Unit,
    onSave: (StudyChallengeEntity) -> Unit
) {
    var title by remember { mutableStateOf(challenge?.title ?: "") }
    var description by remember { mutableStateOf(challenge?.description ?: "") }
    var hours by remember { mutableStateOf("${challenge?.targetHours ?: 10f}") }
    var sessionsCount by remember { mutableStateOf("${challenge?.targetSessions ?: 5}") }
    var status by remember { mutableStateOf(challenge?.status ?: "ACTIVE") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (challenge == null) "Create Study Challenge" else "Edit Challenge", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Challenge Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hours,
                        onValueChange = { hours = it },
                        label = { Text("Target Hours") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = sessionsCount,
                        onValueChange = { sessionsCount = it },
                        label = { Text("Target Sessions") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val id = challenge?.challengeId ?: "chal_${System.currentTimeMillis()}"
                    onSave(
                        StudyChallengeEntity(
                            challengeId = id,
                            title = title.trim(),
                            description = description.trim(),
                            startDate = challenge?.startDate ?: System.currentTimeMillis(),
                            endDate = challenge?.endDate ?: (System.currentTimeMillis() + 86400000L * 7),
                            targetHours = hours.toFloatOrNull() ?: 10.0f,
                            targetSessions = sessionsCount.toIntOrNull() ?: 5,
                            status = status
                        )
                    )
                },
                enabled = title.isNotBlank()
            ) {
                Text("Save Challenge")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
