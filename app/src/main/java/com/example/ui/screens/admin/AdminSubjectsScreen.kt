package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminEntity
import com.example.data.model.SubjectEntity
import com.example.data.util.SecurityUtils
import com.example.ui.viewmodel.AdminViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSubjectsScreen(
    admin: AdminEntity,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val subjects by adminViewModel.subjects.collectAsState()
    val sessions by adminViewModel.sessions.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }
    var editingSubject by remember { mutableStateOf<SubjectEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredSubjects = remember(subjects, searchQuery) {
        subjects.filter {
            searchQuery.isBlank() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        floatingActionButton = {
            if (SecurityUtils.canManageSubjects(admin.role)) {
                FloatingActionButton(
                    onClick = {
                        editingSubject = null
                        showEditDialog = true
                    },
                    modifier = Modifier.testTag("admin_add_subject_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Catalog Subject")
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
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search subjects...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Subject Catalog (${filteredSubjects.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredSubjects, key = { it.subjectId }) { subject ->
                    val subjectColor = runCatching { Color(android.graphics.Color.parseColor(subject.colorHex)) }
                        .getOrDefault(MaterialTheme.colorScheme.primary)
                    val historicalSessionsCount = sessions.count { it.subjectName.equals(subject.name, ignoreCase = true) }

                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(subjectColor)
                                )

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = subject.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        if (subject.isDefault) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "DEFAULT",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (subject.description.isNotBlank()) {
                                        Text(
                                            text = subject.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = "$historicalSessionsCount Historical Sessions Recorded",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Active / Inactive switch
                                if (SecurityUtils.canManageSubjects(admin.role)) {
                                    Switch(
                                        checked = subject.status == "ACTIVE",
                                        onCheckedChange = { adminViewModel.toggleSubjectStatus(subject) }
                                    )
                                    IconButton(onClick = {
                                        editingSubject = subject
                                        showEditDialog = true
                                    }) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit Subject")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        SubjectEditDialog(
            subject = editingSubject,
            onDismiss = { showEditDialog = false },
            onSave = { name, colorHex, desc, status, isDef ->
                adminViewModel.saveSubject(
                    subjectId = editingSubject?.subjectId,
                    name = name,
                    colorHex = colorHex,
                    description = desc,
                    status = status,
                    isDefault = isDef
                )
                showEditDialog = false
            }
        )
    }
}

@Composable
fun SubjectEditDialog(
    subject: SubjectEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String, description: String, status: String, isDefault: Boolean) -> Unit
) {
    var name by remember { mutableStateOf(subject?.name ?: "") }
    var description by remember { mutableStateOf(subject?.description ?: "") }
    var colorHex by remember { mutableStateOf(subject?.colorHex ?: "#2563EB") }
    var isDefault by remember { mutableStateOf(subject?.isDefault ?: false) }
    var status by remember { mutableStateOf(subject?.status ?: "ACTIVE") }

    val presetColors = listOf(
        "#2563EB", "#10B981", "#8B5CF6", "#F59E0B",
        "#EC4899", "#06B6D4", "#EF4444", "#6366F1"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (subject == null) "Add Subject to Catalog" else "Edit Subject", fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Subject Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Subject Color", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetColors.forEach { hex ->
                        val c = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Blue)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { colorHex = hex }
                        ) {
                            if (colorHex.equals(hex, ignoreCase = true)) {
                                Icon(
                                    Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.align(Alignment.Center).size(18.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Default for New Users:")
                    Switch(checked = isDefault, onCheckedChange = { isDefault = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, colorHex, description, status, isDefault) },
                enabled = name.isNotBlank()
            ) {
                Text("Save Subject")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
