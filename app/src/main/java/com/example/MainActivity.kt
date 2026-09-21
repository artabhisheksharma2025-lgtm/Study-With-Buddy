package com.example

import android.os.Bundle
import android.widget.Toast
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
import com.example.data.repository.AppRepository
import com.example.ui.components.AddGoalDialog
import com.example.ui.components.AppBottomNavigation
import com.example.ui.components.AppTab
import com.example.ui.components.ManualSessionDialog
import com.example.ui.screens.*
import com.example.ui.theme.StudyTrackerTheme
import com.example.ui.viewmodel.AuthUiState
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.FriendsViewModel
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase
    private lateinit var repository: AppRepository

    private lateinit var authViewModel: AuthViewModel
    private lateinit var mainViewModel: MainViewModel
    private lateinit var friendsViewModel: FriendsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = AppDatabase.getInstance(applicationContext)
        repository = AppRepository(database)

        authViewModel = AuthViewModel(repository)
        mainViewModel = MainViewModel(repository)
        friendsViewModel = FriendsViewModel(repository)

        setContent {
            StudyTrackerTheme {
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
                        AuthScreen(authViewModel = authViewModel)
                    }
                    is AuthUiState.Authenticated -> {
                        MainAppContent(
                            user = state.user,
                            mainViewModel = mainViewModel,
                            friendsViewModel = friendsViewModel,
                            authViewModel = authViewModel
                        )
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
    authViewModel: AuthViewModel
) {
    val context = LocalContext.current
    var currentTab by remember { mutableStateOf(AppTab.HOME) }

    val stats by mainViewModel.userStats.collectAsState()
    val subjects by mainViewModel.subjects.collectAsState()
    val uiMessage by mainViewModel.uiEventMessage.collectAsState()

    var showManualSessionDialog by remember { mutableStateOf(false) }
    var showAddGoalDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiMessage) {
        uiMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            mainViewModel.clearUiMessage()
        }
    }

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
                        onOpenAddGoalDialog = { showAddGoalDialog = true }
                    )
                }
                AppTab.STUDY -> {
                    StudyScreen(mainViewModel = mainViewModel)
                }
                AppTab.STATS -> {
                    StatsScreen(stats = stats)
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
                        authViewModel = authViewModel
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
    }
}
