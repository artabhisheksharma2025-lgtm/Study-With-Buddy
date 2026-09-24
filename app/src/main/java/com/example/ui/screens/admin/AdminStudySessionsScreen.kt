package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminEntity
import com.example.data.model.StudySessionEntity
import com.example.data.util.SecurityUtils
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStudySessionsScreen(
    admin: AdminEntity,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by adminViewModel.sessions.collectAsState()
    val users by adminViewModel.users.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedSessionForEdit by remember { mutableStateOf<StudySessionEntity?>(null) }
    var sessionToDelete by remember { mutableStateOf<StudySessionEntity?>(null) }
    var deleteReason by remember { mutableStateOf("") }

    val userMap = remember(users) { users.associateBy { it.userId } }

    val filteredSessions = remember(sessions, searchQuery) {
        sessions.filter { s ->
            val uName = userMap[s.userId]?.fullName ?: ""
            searchQuery.isBlank() ||
                    s.subjectName.contains(searchQuery, ignoreCase = true) ||
                    s.title.contains(searchQuery, ignoreCase = true) ||
                    uName.contains(searchQuery, ignoreCase = true) ||
                    s.sessionId.contains(searchQuery, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by subject, title, or student name...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_session_search_field")
        )

        // Summary row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total Sessions: ${filteredSessions.size}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            val discrepanciesCount = sessions.count { adminViewModel.validateSession(it).hasDiscrepancy }
            if (discrepanciesCount > 0) {
                Surface(
                    color = Color(0xFFFEF3C7),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "⚠️ $discrepanciesCount Discrepancies Flagged",
                        color = Color(0xFFB45309),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Sessions list
        if (filteredSessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No study sessions found.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSessions, key = { it.sessionId }) { session ->
                    val user = userMap[session.userId]
                    val validation = adminViewModel.validateSession(session)

                    SessionAdminCard(
                        session = session,
                        userName = user?.fullName ?: "Student (${session.userId.take(6)})",
                        hasDiscrepancy = validation.hasDiscrepancy,
                        discrepancySeconds = validation.discrepancySeconds,
                        canManage = SecurityUtils.canManageSessions(admin.role),
                        onEdit = { selectedSessionForEdit = session },
                        onDelete = { sessionToDelete = session }
                    )
                }
            }
        }
    }

    // Session Correction Dialog
    selectedSessionForEdit?.let { session ->
        val validation = adminViewModel.validateSession(session)
        SessionCorrectionDialog(
            session = session,
            validation = validation,
            onDismiss = { selectedSessionForEdit = null },
            onSave = { newDur, newTitle, newNotes, reason ->
                adminViewModel.correctSession(session.sessionId, newDur, newTitle, newNotes, reason)
                selectedSessionForEdit = null
            }
        )
    }

    // Session Delete Dialog
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = {
                sessionToDelete = null
                deleteReason = ""
            },
            title = { Text("Delete Study Session?", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Are you sure you want to remove this session for '${session.subjectName}' (${formatDuration(session.durationSeconds)})?")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = deleteReason,
                        onValueChange = { deleteReason = it },
                        label = { Text("Reason for deletion (Audit record)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val r = if (deleteReason.isBlank()) "Admin session removal" else deleteReason
                        adminViewModel.deleteSession(session.sessionId, r)
                        sessionToDelete = null
                        deleteReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete Session")
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SessionAdminCard(
    session: StudySessionEntity,
    userName: String,
    hasDiscrepancy: Boolean,
    discrepancySeconds: Long,
    canManage: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("admin_session_card_${session.sessionId}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = session.subjectName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$userName • ${session.sessionDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = formatDuration(session.durationSeconds),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            if (hasDiscrepancy) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(14.dp))
                        Text(
                            text = "Timestamp discrepancy of ${discrepancySeconds}s between clock span and stored duration",
                            color = Color(0xFFDC2626),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Type: ${session.sessionType}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onEdit) {
                        Text("Inspect / Edit", fontSize = 12.sp)
                    }
                    if (canManage) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCorrectionDialog(
    session: StudySessionEntity,
    validation: com.example.data.repository.AdminRepository.SessionValidationResult,
    onDismiss: () -> Unit,
    onSave: (newDurationSeconds: Long, newTitle: String, newNotes: String, reason: String) -> Unit
) {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault())
    var durationMinutes by remember { mutableStateOf("${session.durationSeconds / 60}") }
    var title by remember { mutableStateOf(session.title) }
    var notes by remember { mutableStateOf(session.notes) }
    var reason by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Session Telemetry & Audit", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Start: ${sdf.format(Date(session.startTime))}", fontSize = 11.sp)
                        Text("End: ${sdf.format(Date(session.endTime))}", fontSize = 11.sp)
                        Text("Paused: ${session.pausedDurationSeconds}s", fontSize = 11.sp)
                        Text("Calculated Active Span: ${validation.calculatedActiveDurationSeconds}s (${validation.calculatedActiveDurationSeconds / 60}m)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Stored Duration: ${session.durationSeconds}s (${session.durationSeconds / 60}m)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Timezone: ${session.timezone}", fontSize = 11.sp)
                    }
                }

                if (err != null) {
                    Text(err ?: "", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                }

                OutlinedTextField(
                    value = durationMinutes,
                    onValueChange = { durationMinutes = it },
                    label = { Text("Corrected Duration (Minutes)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Session Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for correction (Audit log)") },
                    placeholder = { Text("e.g. Timer sync anomaly resolved") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val mins = durationMinutes.toLongOrNull()
                if (mins == null || mins < 0) {
                    err = "Please enter a valid positive number of minutes."
                    return@Button
                }
                val r = if (reason.isBlank()) "Admin telemetry adjustment" else reason
                onSave(mins * 60L, title, notes, r)
            }) {
                Text("Save Correction")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
