package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.StudyGoalEntity
import com.example.data.model.UserEntity
import com.example.data.repository.AppRepository
import com.example.data.util.UserStudyStats
import com.example.ui.components.AppTab
import com.example.ui.components.GoalPeriod
import com.example.ui.components.LiveCameraQRScannerDialog
import com.example.ui.components.QuickActionButton
import com.example.ui.components.SetStudyGoalDialog
import com.example.ui.components.SetWeeklyGoalDialog
import com.example.ui.components.StatCard
import com.example.ui.components.StreakCalendarCard
import com.example.ui.components.StudyGoalTrackerCard
import com.example.ui.components.WeeklyGoalTrackerCard
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.FlameOrange
import com.example.ui.theme.IndigoPrimary
import com.example.ui.theme.VioletTertiary
import com.example.ui.util.GreetingHelper
import com.example.ui.util.TimeOfDayPeriod
import com.example.ui.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: UserEntity,
    stats: UserStudyStats,
    mainViewModel: MainViewModel,
    onNavigateTab: (AppTab) -> Unit,
    onOpenManualSessionDialog: () -> Unit,
    onOpenAddGoalDialog: () -> Unit,
    onScanFriendQR: () -> Unit = {},
    onOpenGoalsScreen: (() -> Unit)? = null,
    onOpenCalendar: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val notifications by mainViewModel.notifications.collectAsState()
    val unreadNotifsCount = notifications.count { !it.isRead }
    val activeAnnouncements by mainViewModel.activeAnnouncements.collectAsState()

    val recentSessions by mainViewModel.studySessions.collectAsState()
    val goals by mainViewModel.studyGoals.collectAsState()
    val dailyGoals by mainViewModel.dailyGoals.collectAsState()
    val weeklyGoals by mainViewModel.weeklyGoals.collectAsState()
    val dailyGoal by mainViewModel.dailyGoal.collectAsState()
    val weeklyGoal by mainViewModel.weeklyGoal.collectAsState()

    var showSetStudyGoalDialog by remember { mutableStateOf(false) }
    var selectedGoalPeriod by remember { mutableStateOf(GoalPeriod.DAILY) }
    var goalToEdit by remember { mutableStateOf<StudyGoalEntity?>(null) }

    // Accurate Time-of-Day Greeting Logic:
    // Morning: 5:00 AM – 11:59 AM ("Good morning!", "Morning!", "Rise and shine!")
    // Afternoon: 12:00 PM – 4:59 PM ("Good afternoon!", "Afternoon!")
    // Evening: 5:00 PM – 7:59 PM ("Good evening!", "Evening!")
    // Night: 8:00 PM onwards / Before Bed ("Good night!", "Have a good night!", "Sweet dreams!")
    val currentPeriod = remember { GreetingHelper.getCurrentPeriod() }
    var greetingVariantIndex by remember { mutableIntStateOf(0) }
    val greetingText = remember(greetingVariantIndex, user.fullName) {
        GreetingHelper.formatGreeting(user.fullName, greetingVariantIndex)
    }
    var showGreetingInfoDialog by remember { mutableStateOf(false) }

    // Live Real-Time Clock: updates every second in real time
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val realTimeFormatted = remember(currentTimeMillis) {
        SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date(currentTimeMillis))
    }
    val realDateFormatted = remember(currentTimeMillis) {
        SimpleDateFormat("EEE, dd MMM", Locale.getDefault()).format(Date(currentTimeMillis))
    }

    var showNotificationsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_logo_1790222630460),
                            contentDescription = "Study With Buddy",
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(
                            modifier = Modifier
                                .clickable {
                                    // Tapping cycles through greetings for this period
                                    greetingVariantIndex = (greetingVariantIndex + 1) % currentPeriod.greetings.size
                                }
                                .testTag("home_greeting_header")
                        ) {
                            Text(
                                text = greetingText,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Study ID: ${user.studyId}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    imageVector = Icons.Outlined.Schedule,
                                    contentDescription = "Real Time Clock",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = realTimeFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(${currentPeriod.title})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = onScanFriendQR,
                        modifier = Modifier.testTag("home_scan_qr_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = "Scan Friend QR Code",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showNotificationsDialog = true },
                        modifier = Modifier.testTag("home_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifsCount > 0) {
                                    Badge { Text("$unreadNotifsCount") }
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.testTag("screen_home")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Live Administrative Announcements with Slide to Dismiss
            if (activeAnnouncements.isNotEmpty()) {
                items(activeAnnouncements, key = { it.announcementId }) { banner ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                                mainViewModel.dismissAnnouncement(banner.announcementId)
                                Toast.makeText(context, "Announcement dismissed", Toast.LENGTH_SHORT).show()
                                true
                            } else {
                                false
                            }
                        }
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val color by animateColorAsState(
                                when (dismissState.targetValue) {
                                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                                    else -> MaterialTheme.colorScheme.errorContainer
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(color)
                                    .padding(horizontal = 20.dp),
                                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                                    Alignment.CenterStart else Alignment.CenterEnd
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteSweep,
                                        contentDescription = "Dismiss Announcement",
                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Slide to Dismiss",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    ) {
                        com.example.ui.components.ResponsiveAnnouncementBanner(
                            banner = banner,
                            onDismiss = {
                                mainViewModel.dismissAnnouncement(banner.announcementId)
                            },
                            onActionClick = {
                                if (banner.actionUrl == "study" || banner.actionLabel?.contains("Timer", ignoreCase = true) == true) {
                                    onNavigateTab(AppTab.STUDY)
                                } else if (banner.actionUrl == "stats" || banner.actionLabel?.contains("Analytics", ignoreCase = true) == true) {
                                    onNavigateTab(AppTab.STATS)
                                }
                            }
                        )
                    }
                }
            }

            // Hero Illustration Banner
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_study_hero_1789969284065),
                            contentDescription = "Study Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.75f)
                                        )
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Ready to conquer your goals today?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Consistency is the key to mastery.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            // Stats Dashboard Overview (Grid)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Today's Study",
                            value = AppRepository.formatDurationShort(stats.todayTimeSeconds),
                            subtitle = "${stats.todaySessionsCount} Sessions",
                            icon = Icons.Filled.Timer,
                            iconTint = IndigoPrimary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_card_today")
                        )
                        StatCard(
                            title = "Current Streak",
                            value = "${stats.currentStreakDays} Days 🔥",
                            subtitle = "Best: ${stats.longestStreakDays} Days",
                            icon = Icons.Filled.LocalFireDepartment,
                            iconTint = FlameOrange,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_card_streak")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Weekly Study",
                            value = AppRepository.formatDurationShort(stats.weeklyTimeSeconds),
                            subtitle = "This Week",
                            icon = Icons.Filled.DateRange,
                            iconTint = VioletTertiary,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_card_weekly")
                        )

                        // Calculate today's daily goal progress
                        val activeDailyTargetMin = dailyGoal?.targetDurationMinutes ?: (2 * 60)
                        val dailyTargetSec = activeDailyTargetMin * 60L
                        val dailyGoalProgressPct = if (dailyTargetSec > 0) {
                            ((stats.todayTimeSeconds.toFloat() / dailyTargetSec.toFloat()) * 100).toInt().coerceAtMost(100)
                        } else 0
                        val targetHoursDisplay = if (activeDailyTargetMin >= 60) "${activeDailyTargetMin / 60}h" else "${activeDailyTargetMin}m"

                        StatCard(
                            title = "Daily Goal",
                            value = "$dailyGoalProgressPct%",
                            subtitle = "${targetHoursDisplay} Target",
                            icon = Icons.Filled.TrackChanges,
                            iconTint = EmeraldAccent,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedGoalPeriod = GoalPeriod.DAILY
                                    showSetStudyGoalDialog = true
                                }
                                .testTag("stat_card_goal")
                        )
                    }
                }
            }

            // Quick Actions Section
            item {
                Column {
                    Text(
                        text = "Quick Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            title = "▶ Start Studying",
                            subtitle = "Timer",
                            icon = Icons.Filled.PlayArrow,
                            gradientColors = listOf(IndigoPrimary, VioletTertiary),
                            testTag = "action_start_studying",
                            onClick = { onNavigateTab(AppTab.STUDY) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButton(
                            title = "➕ Add Session",
                            subtitle = "Manual",
                            icon = Icons.Filled.AddCircle,
                            gradientColors = listOf(VioletTertiary, FlameOrange),
                            testTag = "action_add_session",
                            onClick = onOpenManualSessionDialog,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        QuickActionButton(
                            title = "📊 Statistics",
                            subtitle = "Analytics",
                            icon = Icons.Filled.BarChart,
                            gradientColors = listOf(EmeraldAccent, IndigoPrimary),
                            testTag = "action_open_stats",
                            onClick = { onNavigateTab(AppTab.STATS) },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButton(
                            title = "👥 Friends",
                            subtitle = "Social",
                            icon = Icons.Filled.People,
                            gradientColors = listOf(FlameOrange, EmeraldAccent),
                            testTag = "action_open_friends",
                            onClick = { onNavigateTab(AppTab.FRIENDS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Streak Calendar Card with direct access to full calendar
            item {
                StreakCalendarCard(
                    currentStreak = stats.currentStreakDays,
                    longestStreak = stats.longestStreakDays,
                    activeDates = stats.activeDates,
                    onOpenFullCalendar = onOpenCalendar,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Section Header: Study Goals & Targets
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🎯 Study Goals & Targets",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (onOpenGoalsScreen != null) {
                        TextButton(
                            onClick = onOpenGoalsScreen,
                            modifier = Modifier.testTag("home_view_all_goals_button")
                        ) {
                            Text("View All (${dailyGoals.size + weeklyGoals.size})")
                            Icon(Icons.Filled.ChevronRight, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Daily / Weekly Study Goal Tracker with Dynamic Progress Bar
            item {
                StudyGoalTrackerCard(
                    dailyGoals = dailyGoals,
                    weeklyGoals = weeklyGoals,
                    dailyGoal = dailyGoal,
                    weeklyGoal = weeklyGoal,
                    todayTimeSeconds = stats.todayTimeSeconds,
                    weeklyTimeSeconds = stats.weeklyTimeSeconds,
                    todaySubjectTimes = stats.todaySubjectTimes,
                    weeklySubjectTimes = stats.weeklySubjectTimes,
                    todaySessionsCount = stats.todaySessionsCount,
                    totalSessionsCount = stats.totalSessionsCount,
                    currentStreakDays = stats.currentStreakDays,
                    onOpenSetGoalDialog = { period ->
                        goalToEdit = null
                        selectedGoalPeriod = period
                        showSetStudyGoalDialog = true
                    },
                    onEditGoal = { goal ->
                        goalToEdit = goal
                        selectedGoalPeriod = if (goal.goalId.startsWith("daily_") || goal.title.contains("daily", ignoreCase = true) || (goal.endDate - goal.startDate) <= 129600000L) GoalPeriod.DAILY else GoalPeriod.WEEKLY
                        showSetStudyGoalDialog = true
                    },
                    onDeleteGoal = { goal ->
                        mainViewModel.deleteGoal(goal)
                    },
                    onStartStudy = { onNavigateTab(AppTab.STUDY) }
                )
            }

            // Additional Subject Goals (if user created specific goals)
            val customGoals = goals.filter { it.goalId != weeklyGoal?.goalId }
            if (customGoals.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📚 Subject Specific Goals",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = onOpenAddGoalDialog) {
                                    Text("+ New Goal")
                                }
                            }

                            customGoals.forEach { goal ->
                                val targetSec = goal.targetDurationMinutes * 60L
                                val currentSec = stats.weeklyTimeSeconds
                                val pct = if (targetSec > 0) (currentSec.toFloat() / targetSec.toFloat()).coerceIn(0f, 1f) else 0f

                                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = goal.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${AppRepository.formatDurationShort(currentSec)} / ${goal.targetDurationMinutes / 60}h",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { pct },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = EmeraldAccent,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Recent Sessions Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Sessions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onNavigateTab(AppTab.STUDY) }) {
                        Text("View All")
                    }
                }
            }

            if (recentSessions.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(24.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Book,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your study journey starts here 📚",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Start your first study session.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { onNavigateTab(AppTab.STUDY) },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Start Studying")
                            }
                        }
                    }
                }
            } else {
                items(recentSessions.take(3)) { session ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(IndigoPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.School,
                                        contentDescription = null,
                                        tint = IndigoPrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = session.subjectName,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = session.title.ifBlank { "Study Session" },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Surface(
                                    color = EmeraldAccent.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = AppRepository.formatDurationShort(session.durationSeconds),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldAccent,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = session.sessionDate,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }

        // Notifications Modal Dialog with Slide-to-Dismiss (SwipeToDismissBox) and Real Time
        if (showNotificationsDialog) {
            AlertDialog(
                onDismissRequest = { showNotificationsDialog = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Notifications",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            if (unreadNotifsCount > 0) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                ) {
                                    Text(
                                        text = "$unreadNotifsCount new",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        IconButton(
                            onClick = { showNotificationsDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 480.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Live Real Time Status Bar
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "Live Time: $realTimeFormatted",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Text(
                                    text = realDateFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Slide to dismiss hint banner
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Notification dekhne ke baad slide kar ke hatayein (Swipe left or right to dismiss)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }

                        // Notifications List
                        if (notifications.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsNone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(50.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "No notifications yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Study alerts, session logs, and friend updates appear here in real time.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(notifications, key = { it.notificationId }) { notif ->
                                    val dismissState = rememberSwipeToDismissBoxState(
                                        confirmValueChange = { value ->
                                            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                                                mainViewModel.dismissNotification(notif.notificationId)
                                                Toast.makeText(context, "Notification dismissed", Toast.LENGTH_SHORT).show()
                                                true
                                            } else {
                                                false
                                            }
                                        }
                                    )

                                    SwipeToDismissBox(
                                        state = dismissState,
                                        backgroundContent = {
                                            val color by animateColorAsState(
                                                when (dismissState.targetValue) {
                                                    SwipeToDismissBoxValue.Settled -> Color.Transparent
                                                    else -> MaterialTheme.colorScheme.errorContainer
                                                }
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(color)
                                                    .padding(horizontal = 16.dp),
                                                contentAlignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd)
                                                    Alignment.CenterStart else Alignment.CenterEnd
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.DeleteSweep,
                                                        contentDescription = "Dismiss",
                                                        tint = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Slide to remove",
                                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Card(
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (!notif.isRead)
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f)
                                                else
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    if (!notif.isRead) {
                                                        mainViewModel.markNotificationAsRead(notif.notificationId)
                                                    }
                                                }
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        modifier = Modifier.weight(1f),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (!notif.isRead) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(8.dp)
                                                                    .clip(CircleShape)
                                                                    .background(MaterialTheme.colorScheme.primary)
                                                            )
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                        }
                                                        Text(
                                                            text = notif.title,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                    }

                                                    // Direct 1-tap dismiss button
                                                    IconButton(
                                                        onClick = {
                                                            mainViewModel.dismissNotification(notif.notificationId)
                                                            Toast.makeText(context, "Notification dismissed", Toast.LENGTH_SHORT).show()
                                                        },
                                                        modifier = Modifier.size(26.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Close,
                                                            contentDescription = "Dismiss notification",
                                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(4.dp))

                                                Text(
                                                    text = notif.message,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    lineHeight = 16.sp
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Real-time timestamp
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Outlined.Schedule,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                            text = formatNotificationRealTime(notif.timestamp),
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                    }

                                                    Text(
                                                        text = "Slide to remove ➔",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        fontSize = 10.5.sp
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (notifications.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    mainViewModel.clearAllNotifications()
                                    Toast.makeText(context, "All notifications cleared", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Text(
                                    "Clear All",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(1.dp))
                        }

                        Row {
                            if (unreadNotifsCount > 0) {
                                TextButton(
                                    onClick = {
                                        mainViewModel.markAllNotificationsRead()
                                    }
                                ) {
                                    Text("Mark Read", fontSize = 13.sp)
                                }
                            }

                            Button(
                                onClick = { showNotificationsDialog = false },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Close")
                            }
                        }
                    }
                }
            )
        }

        if (showSetStudyGoalDialog) {
            val dailyHrs = (dailyGoals.firstOrNull()?.targetDurationMinutes ?: (2 * 60)) / 60f
            val weeklyHrs = (weeklyGoals.firstOrNull()?.targetDurationMinutes ?: (20 * 60)) / 60f

            SetStudyGoalDialog(
                initialPeriod = selectedGoalPeriod,
                initialDailyHours = dailyHrs,
                initialWeeklyHours = weeklyHrs,
                initialTitle = if (selectedGoalPeriod == GoalPeriod.DAILY) dailyGoal?.title ?: "Daily Study Target" else weeklyGoal?.title ?: "Weekly Study Target",
                subjects = mainViewModel.subjects.collectAsState().value,
                goalToEdit = goalToEdit,
                onSaveGoalDetailed = { period, hours, title, subjectName, goalId ->
                    if (period == GoalPeriod.DAILY) {
                        mainViewModel.addOrUpdateDailyGoal(
                            title = title,
                            targetHours = hours,
                            subjectName = subjectName,
                            goalId = goalId
                        )
                    } else {
                        mainViewModel.addOrUpdateWeeklyGoal(
                            title = title,
                            targetHours = hours,
                            subjectName = subjectName,
                            goalId = goalId
                        )
                    }
                    goalToEdit = null
                },
                onSaveGoal = { period, hours, title ->
                    if (period == GoalPeriod.DAILY) {
                        mainViewModel.setDailyGoal(hours, title)
                    } else {
                        mainViewModel.setWeeklyGoal(hours, title)
                    }
                    goalToEdit = null
                },
                onDismiss = {
                    showSetStudyGoalDialog = false
                    goalToEdit = null
                }
            )
        }
    }
}

/**
 * Formats notification timestamps to real-time strings:
 * e.g., "Just now • 10:45 AM", "5 min ago • 10:40 AM", "Yesterday • 09:15 PM", "28 Sep • 11:00 AM"
 */
fun formatNotificationRealTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = (now - timestamp).coerceAtLeast(0)
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)

    val timeStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(timestamp))
    val dateStr = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))

    return when {
        minutes < 1 -> "Just now • $timeStr"
        minutes < 60 -> "$minutes min ago • $timeStr"
        hours < 24 -> "$hours hr ago • $timeStr"
        days < 2 -> "Yesterday • $timeStr"
        else -> "$dateStr • $timeStr"
    }
}

