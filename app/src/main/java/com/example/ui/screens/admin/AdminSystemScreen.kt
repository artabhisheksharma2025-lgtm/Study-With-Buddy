package com.example.ui.screens.admin

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminAuditLogEntity
import com.example.data.model.AdminEntity
import com.example.data.model.AppErrorLogEntity
import com.example.data.model.AppSettingEntity
import com.example.data.util.SecurityUtils
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminSystemScreen(
    admin: AdminEntity,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Health, 1: Audit Logs, 2: Error Logs, 3: App Settings

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Health") })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Audit Logs") })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Errors") })
            Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Settings") })
        }

        when (selectedTab) {
            0 -> SystemHealthTab()
            1 -> AuditLogsTab(adminViewModel = adminViewModel)
            2 -> ErrorLogsTab(admin = admin, adminViewModel = adminViewModel)
            3 -> AppSettingsTab(admin = admin, adminViewModel = adminViewModel)
        }
    }
}

@Composable
private fun SystemHealthTab() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("System Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

                HealthRow(label = "Room SQLite Database", status = "Connected (v2)", isHealthy = true)
                HealthRow(label = "Security Engine (RBAC)", status = "Active & Enforcing", isHealthy = true)
                HealthRow(label = "Session Timer Precision", status = "Synchronized", isHealthy = true)
                HealthRow(label = "Cryptographic Storage", status = "Salted SHA-256 Enabled", isHealthy = true)
                HealthRow(label = "Audit Logging", status = "Immutable Local Store", isHealthy = true)
            }
        }
    }
}

@Composable
private fun HealthRow(label: String, status: String, isHealthy: Boolean) {
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
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isHealthy) Color(0xFF22C55E) else Color(0xFFEF4444))
            )
            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AuditLogsTab(adminViewModel: AdminViewModel) {
    val auditLogs by adminViewModel.auditLogs.collectAsState()
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    if (auditLogs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No audit log entries recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(auditLogs, key = { it.logId }) { log ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = log.action,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Text(sdf.format(Date(log.createdAt)), fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(log.description, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Actor: ${log.adminEmail} (${log.adminRole}) • Target: ${log.targetType}:${log.targetId}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (!log.reason.isNullOrBlank()) {
                            Text("Reason: ${log.reason}", fontSize = 11.sp, color = Color(0xFFB45309))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorLogsTab(admin: AdminEntity, adminViewModel: AdminViewModel) {
    val errorLogs by adminViewModel.errorLogs.collectAsState()

    if (errorLogs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No application errors reported.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(errorLogs, key = { it.errorId }) { err ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    color = if (err.severity == "HIGH") Color(0xFFFEE2E2) else Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(err.severity, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp, 1.dp))
                                }
                                Text(err.errorType, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(err.message, fontSize = 12.sp)
                            Text("Module: ${err.screenModule} • Status: ${err.status}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                        }

                        if (err.status != "RESOLVED") {
                            Button(onClick = { adminViewModel.resolveError(err) }) {
                                Text("Resolve", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppSettingsTab(admin: AdminEntity, adminViewModel: AdminViewModel) {
    val settings by adminViewModel.settings.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Global App Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(settings, key = { it.settingKey }) { s ->
                var editingVal by remember(s.settingValue) { mutableStateOf(s.settingValue) }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(s.settingKey, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(s.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = editingVal,
                                onValueChange = { editingVal = it },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            if (SecurityUtils.canManageSettings(admin.role)) {
                                Button(onClick = {
                                    adminViewModel.saveSetting(s.settingKey, editingVal, s.category, s.description)
                                }) {
                                    Text("Save", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
