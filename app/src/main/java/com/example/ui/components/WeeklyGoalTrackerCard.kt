package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StudyGoalEntity
import com.example.data.repository.AppRepository
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.IndigoPrimary
import java.util.Calendar
import java.util.Locale

@Composable
fun WeeklyGoalTrackerCard(
    goal: StudyGoalEntity?,
    weeklyTimeSeconds: Long,
    totalSessionsCount: Int,
    onOpenSetGoalDialog: () -> Unit,
    onStartStudy: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val targetMinutes = goal?.targetDurationMinutes ?: (20 * 60) // default 20h if unset
    val targetSec = targetMinutes * 60L
    val targetHours = targetMinutes / 60f
    val currentHours = weeklyTimeSeconds / 3600f

    val progressFraction = if (targetSec > 0) {
        (weeklyTimeSeconds.toFloat() / targetSec.toFloat()).coerceAtLeast(0f)
    } else 0f

    val clampedProgress = progressFraction.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedProgress,
        animationSpec = tween(durationMillis = 800),
        label = "weekly_goal_progress"
    )

    val percentage = (progressFraction * 100).toInt()
    val remainingSec = (targetSec - weeklyTimeSeconds).coerceAtLeast(0L)

    // Calculate days remaining in the current week (Sunday = 1, Saturday = 7)
    val cal = Calendar.getInstance()
    val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
    // Days until Sunday:
    val daysRemainingInWeek = when (dayOfWeek) {
        Calendar.MONDAY -> 7
        Calendar.TUESDAY -> 6
        Calendar.WEDNESDAY -> 5
        Calendar.THURSDAY -> 4
        Calendar.FRIDAY -> 3
        Calendar.SATURDAY -> 2
        Calendar.SUNDAY -> 1
        else -> 3
    }

    val dailyHoursNeeded = if (remainingSec > 0 && daysRemainingInWeek > 0) {
        (remainingSec / 3600f) / daysRemainingInWeek
    } else 0f

    // Theme & Status Badge
    val (statusLabel, statusColor, statusBg) = when {
        percentage >= 100 -> Triple("🎉 Goal Crushed!", EmeraldAccent, Color(0xFFDCFCE7))
        percentage >= 75 -> Triple("🔥 Almost There!", Color(0xFF0D9488), Color(0xFFCCFBF1))
        percentage >= 40 -> Triple("⚡ On Track", IndigoPrimary, Color(0xFFE0E7FF))
        else -> Triple("💪 Keep Going", FlameOrange, Color(0xFFFFEDD5))
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_goal_tracker_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Title, Badge & Edit Target Button
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
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(IndigoPrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TrackChanges,
                            contentDescription = null,
                            tint = IndigoPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Weekly Study Goal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = goal?.title ?: "Target: ${targetHours.toInt()} Hours / Week",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = statusBg,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }

                    IconButton(
                        onClick = onOpenSetGoalDialog,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_weekly_goal_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit Weekly Goal",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Numbers: Current Studied Hours vs Target & Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = String.format(Locale.getDefault(), "%.1f", currentHours),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = " / ${String.format(Locale.getDefault(), "%.1f", targetHours)} hrs",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                        )
                    }
                    Text(
                        text = if (percentage >= 100) "Goal completed! 🎉" else "${AppRepository.formatDurationShort(remainingSec)} remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (percentage >= 100) EmeraldAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "$percentage%",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Custom Multi-Tier Progress Bar with Milestone Markers
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                // Animated Progress Fill with Gradient
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedProgress)
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            Brush.horizontalGradient(
                                if (percentage >= 100) {
                                    listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF34D399))
                                } else {
                                    listOf(IndigoPrimary, Color(0xFF6366F1), EmeraldAccent)
                                }
                            )
                        )
                )

                // Milestone Divider Ticks (25%, 50%, 75%)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.4f)))
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.4f)))
                    Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.White.copy(alpha = 0.4f)))
                }
            }

            // Milestone Labels (25%, 50%, 75%, 100%)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, start = 2.dp, end = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0%", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                Text("25%", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                Text("50%", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                Text("75%", fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                Text("100%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (percentage >= 100) EmeraldAccent else MaterialTheme.colorScheme.outline)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Contextual 3-Pill Breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GoalMetricPill(
                    icon = Icons.Filled.Schedule,
                    title = "Daily Needed",
                    value = if (percentage >= 100) "Done!" else "${String.format(Locale.getDefault(), "%.1f", dailyHoursNeeded)}h/d",
                    modifier = Modifier.weight(1f)
                )
                GoalMetricPill(
                    icon = Icons.Filled.CalendarMonth,
                    title = "Days Left",
                    value = "$daysRemainingInWeek Days",
                    modifier = Modifier.weight(1f)
                )
                GoalMetricPill(
                    icon = Icons.Filled.CheckCircleOutline,
                    title = "Sessions",
                    value = "$totalSessionsCount Done",
                    modifier = Modifier.weight(1f)
                )
            }

            // Quick Actions: Adjust Target or Start Study
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenSetGoalDialog,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Change Goal", fontSize = 12.sp)
                }

                if (onStartStudy != null) {
                    Button(
                        onClick = onStartStudy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = IndigoPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Study Now", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalMetricPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                Text(title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
