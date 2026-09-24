package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.example.data.model.AdminEntity
import com.example.data.model.UserEntity
import com.example.data.util.SecurityUtils
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(
    admin: AdminEntity,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val users by adminViewModel.users.collectAsState()
    val sessions by adminViewModel.sessions.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, ACTIVE, SUSPENDED, DISABLED
    var selectedUserForDetail by remember { mutableStateOf<UserEntity?>(null) }
    var userToAction by remember { mutableStateOf<Pair<UserEntity, String>?>(null) } // user, actionType
    var actionReason by remember { mutableStateOf("") }

    val filteredUsers = remember(users, searchQuery, selectedFilter) {
        users.filter { u ->
            val matchQuery = searchQuery.isBlank() ||
                    u.fullName.contains(searchQuery, ignoreCase = true) ||
                    u.email.contains(searchQuery, ignoreCase = true) ||
                    u.studyId.contains(searchQuery, ignoreCase = true)

            val matchFilter = when (selectedFilter) {
                "ACTIVE" -> u.accountStatus == "ACTIVE" && !u.isDeleted
                "SUSPENDED" -> u.accountStatus == "SUSPENDED"
                "DISABLED" -> u.accountStatus == "DISABLED"
                "DELETED" -> u.isDeleted
                else -> true
            }

            matchQuery && matchFilter
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search & Filter Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, email, or Study ID...") },
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
                    .weight(1f)
                    .testTag("admin_user_search_field")
            )
        }

        // Filter chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val filters = listOf(
                "ALL" to "All Users (${users.size})",
                "ACTIVE" to "Active (${users.count { it.accountStatus == "ACTIVE" && !it.isDeleted }})",
                "SUSPENDED" to "Suspended (${users.count { it.accountStatus == "SUSPENDED" }})",
                "DISABLED" to "Disabled (${users.count { it.accountStatus == "DISABLED" }})",
                "DELETED" to "Deleted (${users.count { it.isDeleted }})"
            )
            items(filters) { (key, label) ->
                FilterChip(
                    selected = selectedFilter == key,
                    onClick = { selectedFilter = key },
                    label = { Text(label, fontSize = 12.sp) }
                )
            }
        }

        // Users List
        if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.PersonSearch,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No users found matching filter.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUsers, key = { it.userId }) { user ->
                    val userSessions = sessions.filter { it.userId == user.userId }
                    val totalSec = userSessions.sumOf { it.durationSeconds }

                    UserAdminCard(
                        user = user,
                        sessionsCount = userSessions.size,
                        totalStudySeconds = totalSec,
                        canManage = SecurityUtils.canManageUsers(admin.role),
                        canDelete = SecurityUtils.canDeleteUser(admin.role),
                        onViewDetails = { selectedUserForDetail = user },
                        onSuspend = { userToAction = user to "SUSPEND" },
                        onActivate = { adminViewModel.updateUserStatus(user.userId, "ACTIVE", "Restored by admin") },
                        onDisable = { userToAction = user to "DISABLE" },
                        onDelete = { userToAction = user to "DELETE" }
                    )
                }
            }
        }
    }

    // User Details Dialog
    selectedUserForDetail?.let { user ->
        val userSessions = sessions.filter { it.userId == user.userId }
        val totalSec = userSessions.sumOf { it.durationSeconds }
        UserDetailDialog(
            user = user,
            sessions = userSessions,
            totalStudySeconds = totalSec,
            onDismiss = { selectedUserForDetail = null }
        )
    }

    // Action Confirmation Dialog (Suspend / Disable / Delete)
    userToAction?.let { (user, actionType) ->
        AlertDialog(
            onDismissRequest = {
                userToAction = null
                actionReason = ""
            },
            icon = {
                Icon(
                    imageVector = when (actionType) {
                        "DELETE" -> Icons.Filled.DeleteForever
                        "SUSPEND" -> Icons.Filled.Block
                        else -> Icons.Filled.Lock
                    },
                    contentDescription = null,
                    tint = if (actionType == "DELETE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = when (actionType) {
                        "DELETE" -> "Delete User Account?"
                        "SUSPEND" -> "Suspend User Access?"
                        else -> "Disable User Account?"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = when (actionType) {
                            "DELETE" -> "WARNING: This will permanently or soft-delete ${user.fullName}'s account (${user.email}). All session records and friend links will be impacted."
                            "SUSPEND" -> "User ${user.fullName} will be prevented from logging in or tracking study time until reinstated."
                            else -> "Account ${user.fullName} will be disabled."
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = actionReason,
                        onValueChange = { actionReason = it },
                        label = { Text("Reason for audit log (Required)") },
                        placeholder = { Text("e.g. Terms violation, spamming, student request") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reason = if (actionReason.isBlank()) "Admin initiated $actionType" else actionReason
                        when (actionType) {
                            "DELETE" -> adminViewModel.deleteUser(user.userId, softDelete = true, reason = reason)
                            "SUSPEND" -> adminViewModel.updateUserStatus(user.userId, "SUSPENDED", reason)
                            "DISABLE" -> adminViewModel.updateUserStatus(user.userId, "DISABLED", reason)
                        }
                        userToAction = null
                        actionReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (actionType == "DELETE") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Confirm $actionType")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    userToAction = null
                    actionReason = ""
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UserAdminCard(
    user: UserEntity,
    sessionsCount: Int,
    totalStudySeconds: Long,
    canManage: Boolean,
    canDelete: Boolean,
    onViewDetails: () -> Unit,
    onSuspend: () -> Unit,
    onActivate: () -> Unit,
    onDisable: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewDetails() }
            .testTag("admin_user_card_${user.userId}")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = user.fullName.take(1).uppercase(),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Column {
                        Text(
                            text = user.fullName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${user.email} • ID: ${user.studyId}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                UserStatusBadge(status = user.accountStatus, isDeleted = user.isDeleted)
            }

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            // Stats summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Column {
                        Text("Study Time", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(formatDuration(totalStudySeconds), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Sessions", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$sessionsCount", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Column {
                        Text("Privacy", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(user.privacyVisibility, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(onClick = onViewDetails) {
                        Text("Details", fontSize = 12.sp)
                    }

                    if (canManage) {
                        if (user.accountStatus == "ACTIVE") {
                            IconButton(onClick = onSuspend, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.Block, contentDescription = "Suspend", tint = Color(0xFFEAB308), modifier = Modifier.size(18.dp))
                            }
                        } else {
                            IconButton(onClick = onActivate, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Activate", tint = Color(0xFF22C55E), modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    if (canDelete) {
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
fun UserStatusBadge(status: String, isDeleted: Boolean) {
    val (bg, fg, label) = when {
        isDeleted -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "DELETED")
        status == "ACTIVE" -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), "ACTIVE")
        status == "SUSPENDED" -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), "SUSPENDED")
        else -> Triple(Color(0xFFF1F5F9), Color(0xFF64748B), "DISABLED")
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

@Composable
fun UserDetailDialog(
    user: UserEntity,
    sessions: List<com.example.data.model.StudySessionEntity>,
    totalStudySeconds: Long,
    onDismiss: () -> Unit
) {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = user.fullName.take(1).uppercase(),
                        fontWeight = FontWeight.Bold
                    )
                }
                Column {
                    Text(user.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Study ID:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(user.studyId, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Account Status:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            UserStatusBadge(status = user.accountStatus, isDeleted = user.isDeleted)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Privacy:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(user.privacyVisibility, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Joined Date:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(sdf.format(Date(user.joinedDate)), fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Recorded Time:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatDuration(totalStudySeconds), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Sessions Count:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${sessions.size}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("Recent Study Sessions", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                if (sessions.isEmpty()) {
                    Text("No sessions recorded.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        sessions.take(4).forEach { s ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(s.subjectName, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("${formatDuration(s.durationSeconds)} • ${s.sessionDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
