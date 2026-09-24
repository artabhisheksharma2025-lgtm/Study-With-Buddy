package com.example.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.viewmodel.AdminViewModel

@Composable
fun AdminAnalyticsScreen(
    admin: AdminEntity,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by adminViewModel.sessions.collectAsState()
    val users by adminViewModel.users.collectAsState()
    var selectedRange by remember { mutableStateOf("7D") } // TODAY, 7D, 30D, ALL

    val filteredSessions = remember(sessions, selectedRange) {
        val now = System.currentTimeMillis()
        val cutoff = when (selectedRange) {
            "TODAY" -> now - 86400000L
            "7D" -> now - 86400000L * 7
            "30D" -> now - 86400000L * 30
            else -> 0L
        }
        sessions.filter { it.startTime >= cutoff }
    }

    val totalSeconds = filteredSessions.sumOf { it.durationSeconds }
    val avgSessionSec = if (filteredSessions.isNotEmpty()) totalSeconds / filteredSessions.size else 0L
    val activeUsersCount = filteredSessions.map { it.userId }.distinct().size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Platform Study Telemetry & Trends",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        // Time range filter
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "TODAY" to "Today",
                    "7D" to "Last 7 Days",
                    "30D" to "Last 30 Days",
                    "ALL" to "All Time"
                ).forEach { (key, lbl) ->
                    FilterChip(
                        selected = selectedRange == key,
                        onClick = { selectedRange = key },
                        label = { Text(lbl, fontSize = 12.sp) }
                    )
                }
            }
        }

        // Aggregate stat cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsSummaryCard(
                    label = "Total Study Time",
                    value = formatDuration(totalSeconds),
                    sub = "${filteredSessions.size} total sessions",
                    modifier = Modifier.weight(1f)
                )
                AnalyticsSummaryCard(
                    label = "Active Students",
                    value = "$activeUsersCount",
                    sub = "Out of ${users.size} registered",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AnalyticsSummaryCard(
                    label = "Avg Session Duration",
                    value = formatDuration(avgSessionSec),
                    sub = "Per study log",
                    modifier = Modifier.weight(1f)
                )
                AnalyticsSummaryCard(
                    label = "Longest Session",
                    value = formatDuration(filteredSessions.maxOfOrNull { it.durationSeconds } ?: 0L),
                    sub = "Peak endurance",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Daily Activity Visualization
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Activity Graph",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    DailyStudyBarChart(sessions = filteredSessions)
                }
            }
        }
    }
}

@Composable
fun AnalyticsSummaryCard(
    label: String,
    value: String,
    sub: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(sub, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
        }
    }
}
