package com.example.ui.screens.admin

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminEntity
import com.example.data.model.ReportEntity
import com.example.data.util.SecurityUtils
import com.example.ui.viewmodel.AdminViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminReportsScreen(
    admin: AdminEntity,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val reports by adminViewModel.reports.collectAsState()
    var selectedReportForReview by remember { mutableStateOf<ReportEntity?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Reports & Abuse Queue (${reports.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            val openCount = reports.count { it.status == "OPEN" || it.status == "UNDER_REVIEW" }
            if (openCount > 0) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "$openCount Action Required",
                        color = Color(0xFFDC2626),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (reports.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No user reports in queue.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(reports, key = { it.reportId }) { report ->
                    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedReportForReview = report }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        color = Color(0xFFFEE2E2),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = report.category,
                                            color = Color(0xFFDC2626),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Text(
                                        text = "Report #${report.reportId}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Surface(
                                    color = when (report.status) {
                                        "OPEN" -> Color(0xFFFEF3C7)
                                        "UNDER_REVIEW" -> Color(0xFFDBEAFE)
                                        "RESOLVED" -> Color(0xFFDCFCE7)
                                        else -> Color(0xFFF1F5F9)
                                    },
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = report.status,
                                        color = when (report.status) {
                                            "OPEN" -> Color(0xFFB45309)
                                            "UNDER_REVIEW" -> Color(0xFF1D4ED8)
                                            "RESOLVED" -> Color(0xFF15803D)
                                            else -> Color(0xFF64748B)
                                        },
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Reporter: ${report.reporterName} ➔ Reported: ${report.reportedUserName}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = report.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = sdf.format(Date(report.createdDate)),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                TextButton(onClick = { selectedReportForReview = report }) {
                                    Text("Moderate", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    selectedReportForReview?.let { report ->
        ReportModerationDialog(
            report = report,
            canModerate = SecurityUtils.canManageReports(admin.role),
            onDismiss = { selectedReportForReview = null },
            onUpdateStatus = { newStatus, note ->
                adminViewModel.updateReport(report.reportId, newStatus, note)
                selectedReportForReview = null
            },
            onSuspendUser = {
                adminViewModel.updateUserStatus(report.reportedUserId, "SUSPENDED", "Report #${report.reportId} violation")
                adminViewModel.updateReport(report.reportId, "RESOLVED", "Reported user suspended")
                selectedReportForReview = null
            }
        )
    }
}

@Composable
fun ReportModerationDialog(
    report: ReportEntity,
    canModerate: Boolean,
    onDismiss: () -> Unit,
    onUpdateStatus: (newStatus: String, note: String) -> Unit,
    onSuspendUser: () -> Unit
) {
    var note by remember { mutableStateOf(report.internalAdminNote ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Report Investigation", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Category: ${report.category}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text("Reporter: ${report.reporterName} (${report.reporterUserId})", fontSize = 11.sp)
                        Text("Reported User: ${report.reportedUserName} (${report.reportedUserId})", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text("Reason / Description:\n${report.description}", fontSize = 12.sp)
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Internal Admin Note") },
                    placeholder = { Text("Document investigation findings...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                if (canModerate) {
                    OutlinedButton(
                        onClick = onSuspendUser,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Suspend ${report.reportedUserName} & Resolve", fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { onUpdateStatus("DISMISSED", note) }) {
                    Text("Dismiss")
                }
                Button(onClick = { onUpdateStatus("RESOLVED", note) }) {
                    Text("Resolve Report")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
