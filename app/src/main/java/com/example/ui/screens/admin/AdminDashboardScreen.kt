package com.example.ui.screens.admin

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminEntity
import com.example.ui.components.ResponsiveAnnouncementBanner
import com.example.ui.viewmodel.AdminSection
import com.example.ui.viewmodel.AdminViewModel
import java.util.Locale

@Composable
fun AdminDashboardScreen(
    admin: AdminEntity,
    adminViewModel: AdminViewModel,
    modifier: Modifier = Modifier
) {
    val dashboardStats by adminViewModel.dashboardStats.collectAsState()
    val dashboardBanner by adminViewModel.dashboardBanner.collectAsState()
    val sessions by adminViewModel.sessions.collectAsState()
    val subjects by adminViewModel.subjects.collectAsState()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Admin Control Hub",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Logged in as ${admin.name} (${admin.email})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = admin.role,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Prominent Responsive Dashboard Banner
        dashboardBanner?.let { banner ->
            item {
                ResponsiveAnnouncementBanner(
                    banner = banner,
                    onDismiss = { /* local dismiss */ },
                    onActionClick = if (banner.actionUrl == "analytics") {
                        { adminViewModel.selectSection(AdminSection.ANALYTICS) }
                    } else if (banner.actionUrl.isNullOrBlank() || banner.actionUrl == "announcements") {
                        { adminViewModel.selectSection(AdminSection.ANNOUNCEMENTS) }
                    } else {
                        null // Allow ResponsiveAnnouncementBanner to trigger download/URL intent directly
                    }
                )
            }
        }

        // Dashboard Metric Cards Grid
        item {
            Text(
                text = "Key System Metrics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            val stats = dashboardStats
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Row 1: Users & Study Sessions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardMetricCard(
                        title = "Users",
                        primaryValue = "${stats?.totalUsers ?: 0}",
                        primaryLabel = "Total Registered",
                        icon = Icons.Filled.People,
                        iconColor = Color(0xFF2563EB),
                        subMetrics = listOf(
                            "Active" to "${stats?.activeUsers ?: 0}",
                            "Today" to "+${stats?.newUsersToday ?: 0}",
                            "This Week" to "+${stats?.newUsersThisWeek ?: 0}"
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    DashboardMetricCard(
                        title = "Study Activity",
                        primaryValue = "${stats?.totalSessions ?: 0}",
                        primaryLabel = "Total Sessions",
                        icon = Icons.Filled.Timer,
                        iconColor = Color(0xFF10B981),
                        subMetrics = listOf(
                            "Total Time" to formatDuration(stats?.totalStudyTimeSeconds ?: 0L),
                            "Today" to formatDuration(stats?.studyTimeTodaySeconds ?: 0L),
                            "This Week" to formatDuration(stats?.studyTimeThisWeekSeconds ?: 0L)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Social & Moderation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardMetricCard(
                        title = "Social Network",
                        primaryValue = "${stats?.totalFriendships ?: 0}",
                        primaryLabel = "Friendships",
                        icon = Icons.Filled.Diversity3,
                        iconColor = Color(0xFF8B5CF6),
                        subMetrics = listOf(
                            "Pending Req" to "${stats?.pendingFriendRequests ?: 0}",
                            "Connected" to "Active"
                        ),
                        modifier = Modifier.weight(1f)
                    )

                    DashboardMetricCard(
                        title = "Moderation",
                        primaryValue = "${stats?.openReports ?: 0}",
                        primaryLabel = "Open Reports",
                        icon = Icons.Filled.Report,
                        iconColor = Color(0xFFF59E0B),
                        subMetrics = listOf(
                            "Resolution" to "Pending Review",
                            "Admins Active" to "${stats?.activeAdmins ?: 1}"
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Quick Actions Row
        item {
            Text(
                text = "Quick Administrative Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionPill(
                    icon = Icons.Filled.Campaign,
                    label = "Announcement",
                    onClick = { adminViewModel.selectSection(AdminSection.ANNOUNCEMENTS) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionPill(
                    icon = Icons.Filled.Notifications,
                    label = "Notify",
                    onClick = { adminViewModel.selectSection(AdminSection.NOTIFICATIONS) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionPill(
                    icon = Icons.Filled.MilitaryTech,
                    label = "Challenge",
                    onClick = { adminViewModel.selectSection(AdminSection.CHALLENGES) },
                    modifier = Modifier.weight(1f)
                )
                QuickActionPill(
                    icon = Icons.Filled.Book,
                    label = "Subjects",
                    onClick = { adminViewModel.selectSection(AdminSection.SUBJECTS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Interactive Study Time Chart
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Daily Study Activity (Last 7 Days)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { adminViewModel.selectSection(AdminSection.ANALYTICS) }) {
                            Text("Detailed Analytics", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Daily Bar Chart Canvas
                    DailyStudyBarChart(sessions = sessions)
                }
            }
        }

        // Popular Subjects Distribution
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Subject Engagement Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val subjectTimes = sessions.groupBy { it.subjectName }
                        .mapValues { (_, sList) -> sList.sumOf { it.durationSeconds } }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(5)

                    if (subjectTimes.isEmpty()) {
                        Text(
                            text = "No study sessions recorded yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val maxTime = subjectTimes.maxOfOrNull { it.second }?.coerceAtLeast(1L) ?: 1L
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            subjectTimes.forEach { (subName, sec) ->
                                val fraction = (sec.toFloat() / maxTime.toFloat()).coerceIn(0.05f, 1f)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = subName,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.width(110.dp)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(12.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                    Text(
                                        text = formatDuration(sec),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.width(60.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardMetricCard(
    title: String,
    primaryValue: String,
    primaryLabel: String,
    icon: ImageVector,
    iconColor: Color,
    subMetrics: List<Pair<String, String>>,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = primaryValue,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = primaryLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            subMetrics.forEach { (lbl, v) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = lbl, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = v, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun QuickActionPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun DailyStudyBarChart(sessions: List<com.example.data.model.StudySessionEntity>) {
    // Generate daily hours for the last 7 days
    val daysLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val dayValues = remember(sessions) {
        // Calculate daily distribution
        val now = System.currentTimeMillis()
        val dayMillis = 86400000L
        (6 downTo 0).map { i ->
            val dayStart = now - (i * dayMillis) - (now % dayMillis)
            val dayEnd = dayStart + dayMillis
            val sec = sessions.filter { it.startTime in dayStart..dayEnd }.sumOf { it.durationSeconds }
            (sec / 3600f).coerceAtLeast(0.2f)
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
    ) {
        val width = size.width
        val height = size.height
        val barCount = dayValues.size
        val barWidth = width / (barCount * 1.8f)
        val spacing = width / barCount
        val maxVal = (dayValues.maxOrNull() ?: 1f).coerceAtLeast(1f)

        for (i in 0 until barCount) {
            val v = dayValues[i]
            val barHeight = (v / maxVal) * (height * 0.85f)
            val x = i * spacing + (spacing - barWidth) / 2
            val y = height - barHeight

            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        daysLabels.forEach { label ->
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp
            )
        }
    }
}

fun formatDuration(totalSeconds: Long): String {
    val hrs = totalSeconds / 3600
    val mins = (totalSeconds % 3600) / 60
    return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
}
