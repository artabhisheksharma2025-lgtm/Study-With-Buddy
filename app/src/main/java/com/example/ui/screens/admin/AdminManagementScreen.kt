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
import com.example.data.util.SecurityUtils
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminManagementScreen(
    admin: AdminEntity,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val admins by adminViewModel.admins.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (SecurityUtils.canManageAdmins(admin.role)) {
                FloatingActionButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = "Provision Admin")
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
                text = "Administrator Directory (${admins.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(admins, key = { it.adminUid }) { a ->
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
                                Column {
                                    Text(a.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                    Text(a.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Surface(
                                    color = if (a.role == SecurityUtils.Roles.SUPER_ADMIN) Color(0xFFDBEAFE) else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = a.role,
                                        color = if (a.role == SecurityUtils.Roles.SUPER_ADMIN) Color(0xFF1D4ED8) else Color(0xFF475569),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Status: ${a.status}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)

                                if (a.email != "vishalsharma74944@gmail.com" && SecurityUtils.canManageAdmins(admin.role)) {
                                    Row {
                                        IconButton(onClick = {
                                            val nextStatus = if (a.status == "ACTIVE") "DISABLED" else "ACTIVE"
                                            adminViewModel.setAdminStatus(a.adminUid, nextStatus)
                                        }) {
                                            Icon(
                                                imageVector = if (a.status == "ACTIVE") Icons.Filled.Block else Icons.Filled.CheckCircle,
                                                contentDescription = "Toggle status",
                                                tint = if (a.status == "ACTIVE") Color(0xFFEAB308) else Color(0xFF22C55E),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        IconButton(onClick = { adminViewModel.removeAdmin(a.adminUid) }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
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

    if (showCreateDialog) {
        CreateAdminDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { email, name, password, role ->
                adminViewModel.createAdmin(email, name, password, role)
                showCreateDialog = false
            }
        )
    }
}

@Composable
fun CreateAdminDialog(
    onDismiss: () -> Unit,
    onCreate: (email: String, name: String, password: String, role: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(SecurityUtils.Roles.ADMIN) }
    val roles = listOf(SecurityUtils.Roles.ADMIN, SecurityUtils.Roles.MODERATOR, SecurityUtils.Roles.SUPPORT_ADMIN)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Provision Administrator", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Admin Email") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Temporary Password") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Role Selection", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    roles.forEach { r ->
                        FilterChip(
                            selected = role == r,
                            onClick = { role = r },
                            label = { Text(r.replace("_", " "), fontSize = 10.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(email, name, password, role) },
                enabled = email.isNotBlank() && name.isNotBlank() && password.length >= 8
            ) {
                Text("Provision Admin")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
