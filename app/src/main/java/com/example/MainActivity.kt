package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.example.data.local.AppDatabase
import com.example.data.repository.AdminRepository
import com.example.data.repository.AppRepository
import com.example.ui.components.AddGoalDialog
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.AppTab
import com.example.ui.components.GoalPeriod
import com.example.ui.components.ManualSessionDialog
import com.example.ui.components.SetStudyGoalDialog
import com.example.ui.components.SetWeeklyGoalDialog
import com.example.ui.screens.*
import com.example.ui.screens.admin.AdminPanelScreen
import com.example.ui.theme.StudyTrackerTheme
import com.example.ui.viewmodel.*

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: AppRepository
    private lateinit var adminRepository: AdminRepository

    private lateinit var authViewModel: AuthViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var friendsViewModel: FriendsViewModel
    private lateinit var adminViewModel: AdminViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getInstance(applicationContext)
        repository = AppRepository(database)
        adminRepository = AdminRepository(database)

        authViewModel = AuthViewModel(repository, adminRepository)
        mainViewModel = MainViewModel(repository)
        friendsViewModel = FriendsViewModel(repository)
        adminViewModel = AdminViewModel(adminRepository)

        setContent {
            StudyTrackerTheme {
                var isInAdminPanel by remember { mutableStateOf(false) }

                if (isInAdminPanel) {
                    AdminPanelScreen(
                        adminViewModel = adminViewModel,
                        onExitToUserApp = { isInAdminPanel = false }
                    )
                } else {
                    val authState by authViewModel.authState.collectAsState()

                    when (val state = authState) {
                        is AuthUiState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is AuthUiState.SignedOut -> {
                            AuthScreen(
                                authViewModel = authViewModel,
                                onAdminVerified = { admin ->
                                    adminViewModel.setAuthenticatedAdmin(admin)
                                    isInAdminPanel = true
                                }
                            )
                        }
                        is AuthUiState.Authenticated -> {
                            MainAppContent(
                                user = state.user,
                                mainViewModel = mainViewModel,
                                friendsViewModel = friendsViewModel,
                                authViewModel = authViewModel,
                                adminViewModel = adminViewModel,
                                onOpenAdminPanel = { isInAdminPanel = true }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainAppContent(
    user: com.example.data.model.UserEntity,
    mainViewModel: MainViewModel,
    friendsViewModel: FriendsViewModel,
    authViewModel: AuthViewModel,
    adminViewModel: AdminViewModel,
    onOpenAdminPanel: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppTab.HOME) }

    val stats by mainViewModel.userStats.collectAsState()
    val subjects by mainViewModel.subjects.collectAsState()
    val dailyGoal by mainViewModel.dailyGoal.collectAsState()
    val weeklyGoal by mainViewModel.weeklyGoal.collectAsState()
    val uiMessage by mainViewModel.uiEventMessage.collectAsState()

    var showGoalsScreen by remember { mutableStateOf(false) }
    var showManualSessionDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }
    var showSetStudyGoalDialog by remember { mutableStateOf(false) }
    var selectedGoalPeriod by remember { mutableStateOf(GoalPeriod.DAILY) }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            mainViewModel.clearUiMessage()
        }
    }

    if (showGoalsScreen) {
        BackHandler {
            showGoalsScreen = false
        }
        GoalsScreen(
            mainViewModel = mainViewModel,
            stats = stats,
            onOpenCreateGoalDialog = { showAddGoalDialog = true }
        )
    } else {
        Scaffold(
            bottomBar = {
                AppBottomNavigation(
                    currentTab = currentTab,
                    onTabSelected = { currentTab = it }
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .testTag("main_app_scaffold")
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    AppTab.HOME -> {
                        HomeScreen(
                            user = user,
                            stats = stats,
                            mainViewModel = mainViewModel,
                            onNavigateTab = { currentTab = it },
                            onOpenManualSessionDialog = { showManualSessionDialog = true },
                            onOpenAddGoalDialog = { showAddGoalDialog = true },
                            onOpenGoalsScreen = { showGoalsScreen = true },
                            onScanFriendQR = {
                                currentTab = AppTab.FRIENDS
                            }
                        )
                    }
                    AppTab.STUDY -> {
                        StudyScreen(mainViewModel = mainViewModel)
                    }
                    AppTab.STATS -> {
                        StatsScreen(
                            stats = stats,
                            dailyGoal = dailyGoal,
                            weeklyGoal = weeklyGoal,
                            onOpenSetGoalDialog = { period ->
                                selectedGoalPeriod = period
                                showSetStudyGoalDialog = true
                            },
                            onStartStudy = { currentTab = AppTab.STUDY }
                        )
                    }
                    AppTab.FRIENDS -> {
                        FriendsScreen(
                            friendsViewModel = friendsViewModel,
                            mainViewModel = mainViewModel
                        )
                    }
                    AppTab.PROFILE -> {
                        ProfileScreen(
                            user = user,
                            stats = stats,
                            mainViewModel = mainViewModel,
                            authViewModel = authViewModel,
                            adminViewModel = adminViewModel,
                            onOpenAdminPanel = onOpenAdminPanel,
                            onOpenGoalsScreen = { showGoalsScreen = true }
                        )
                    }
                }
            }

            if (showManualSessionDialog) {
                ManualSessionDialog(
                    subjects = subjects,
                    onSaveSession = { subName, duration, title, notes ->
                        mainViewModel.addManualSession(subName, duration, title, notes)
                    },
                    onDismiss = { showManualSessionDialog = false }
                )
            }

            if (showAddGoalDialog) {
                AddGoalDialog(
                    subjects = subjects,
                    onCreateGoal = { title, hrs, sess, days, subName ->
                        mainViewModel.createGoal(title, hrs, sess, days, subName)
                    },
                    onDismiss = { showAddGoalDialog = false }
                )
            }

            if (showSetStudyGoalDialog) {
                val dailyHrs = (dailyGoal?.targetDurationMinutes ?: (2 * 60)) / 60f
                val weeklyHrs = (weeklyGoal?.targetDurationMinutes ?: (20 * 60)) / 60f

                SetStudyGoalDialog(
                    initialPeriod = selectedGoalPeriod,
                    initialDailyHours = dailyHrs,
                    initialWeeklyHours = weeklyHrs,
                    initialTitle = if (selectedGoalPeriod == GoalPeriod.DAILY) dailyGoal?.title ?: "Daily Study Target" else weeklyGoal?.title ?: "Weekly Study Target",
                    onSaveGoal = { period, hours, title ->
                        if (period == GoalPeriod.DAILY) {
                            mainViewModel.setDailyGoal(hours, title)
                        } else {
                            mainViewModel.setWeeklyGoal(hours, title)
                        }
                    },
                    onDismiss = { showSetStudyGoalDialog = false }
                )
            }
        }
    }
}
