package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.example.data.util.SecurityUtils
import com.example.ui.viewmodel.AdminViewModel

@Composable
fun AdminNotificationsScreen(
    admin: AdminEntity,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val users by adminViewModel.users.collectAsState()

    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var targetMode by remember { mutableStateOf("ALL") } // ALL or INDIVIDUAL
    var selectedUserId by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Send,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Broadcast Community Notification",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Deliver official study announcements, motivational reminders, or maintenance alerts to student notification centers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Audience selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = targetMode == "ALL",
                        onClick = { targetMode = "ALL" },
                        label = { Text("All Users (${users.size} students)") }
                    )
                    FilterChip(
                        selected = targetMode == "INDIVIDUAL",
                        onClick = { targetMode = "INDIVIDUAL" },
                        label = { Text("Specific User") }
                    )
                }

                if (targetMode == "INDIVIDUAL") {
                    OutlinedTextField(
                        value = selectedUserId,
                        onValueChange = { selectedUserId = it },
                        label = { Text("Target User ID or Study ID") },
                        placeholder = { Text("e.g. user_abhishek or STU-8821") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notification Title") },
                    placeholder = { Text("e.g. Weekend Study Sprint Starts Today!") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_notif_title_input")
                )

                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message Body") },
                    placeholder = { Text("Write your announcement or notice details here...") },
                    minLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_notif_body_input")
                )

                Button(
                    onClick = {
                        val targetUid = if (targetMode == "INDIVIDUAL" && selectedUserId.isNotBlank()) selectedUserId else null
                        adminViewModel.broadcastNotification(title, message, targetUid)
                        title = ""
                        message = ""
                    },
                    enabled = SecurityUtils.canManageNotifications(admin.role) && title.isNotBlank() && message.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("admin_dispatch_notif_button")
                ) {
                    Icon(Icons.Filled.Campaign, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (targetMode == "ALL") "Broadcast to All Students" else "Dispatch to User",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
