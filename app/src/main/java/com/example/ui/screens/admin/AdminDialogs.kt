package com.example.ui.screens.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.viewmodel.AdminSection
import com.example.ui.viewmodel.AdminViewModel

@Composable
fun AdminGlobalSearchDialog(
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit,
    onSelectSection: (AdminSection) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val searchResults by adminViewModel.searchResults.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        adminViewModel.performGlobalSearch(it)
                    },
                    placeholder = { Text("Search users, sessions, reports, subjects...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = {
                                query = ""
                                adminViewModel.performGlobalSearch("")
                            }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (query.isBlank()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Type to search across all platform entities.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    val res = searchResults
                    if (res == null || (res.users.isEmpty() && res.sessions.isEmpty() && res.reports.isEmpty() && res.subjects.isEmpty())) {
                        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("No matching results found.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (res.users.isNotEmpty()) {
                                item {
                                    Text("Users (${res.users.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                items(res.users.take(5)) { u ->
                                    Surface(
                                        onClick = { onSelectSection(AdminSection.USERS) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Column {
                                                Text(u.fullName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("${u.email} • ${u.studyId}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                    }
                                }
                            }

                            if (res.sessions.isNotEmpty()) {
                                item {
                                    Text("Study Sessions (${res.sessions.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                items(res.sessions.take(5)) { s ->
                                    Surface(
                                        onClick = { onSelectSection(AdminSection.SESSIONS) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Column {
                                                Text(s.subjectName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text("${formatDuration(s.durationSeconds)} • ${s.sessionDate}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                    }
                                }
                            }

                            if (res.reports.isNotEmpty()) {
                                item {
                                    Text("Reports (${res.reports.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                items(res.reports.take(5)) { rep ->
                                    Surface(
                                        onClick = { onSelectSection(AdminSection.REPORTS) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(Icons.Filled.Report, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Column {
                                                Text("${rep.category} (Report #${rep.reportId})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(rep.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun AdminExportDialog(
    adminViewModel: AdminViewModel,
    onDismiss: () -> Unit
) {
    var exportType by remember { mutableStateOf("USERS") } // USERS, SESSIONS, SUBJECTS
    var exportedCsv by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Database Records", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Select data category to generate an export file. Sensitive passwords and salts are never included.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("USERS" to "Users", "SESSIONS" to "Sessions", "SUBJECTS" to "Subjects").forEach { (type, label) ->
                        FilterChip(
                            selected = exportType == type,
                            onClick = {
                                exportType = type
                                exportedCsv = null
                            },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                if (exportedCsv == null) {
                    Button(
                        onClick = {
                            adminViewModel.exportData(exportType) { csv ->
                                exportedCsv = csv
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate $exportType CSV")
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                    ) {
                        Text(
                            text = exportedCsv ?: "",
                            fontSize = 10.sp,
                            modifier = Modifier
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Export $exportType CSV", exportedCsv)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "$exportType CSV copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy CSV to Clipboard")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
