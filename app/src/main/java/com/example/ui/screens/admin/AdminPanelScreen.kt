package com.example.ui.screens.admin

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.ui.viewmodel.AdminAuthUiState
import com.example.ui.viewmodel.AdminSection
import com.example.ui.viewmodel.AdminViewModel

@Composable
fun AdminPanelScreen(
    adminViewModel: AdminViewModel,
    onExitToUserApp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by adminViewModel.adminAuthState.collectAsState()
    val currentSection by adminViewModel.currentSection.collectAsState()

    when (val state = authState) {
        is AdminAuthUiState.Authenticated -> {
            val admin = state.admin
            AdminScaffold(
                admin = admin,
                adminViewModel = adminViewModel,
                onExitToUserApp = onExitToUserApp
            ) {
                when (currentSection) {
                    AdminSection.DASHBOARD -> AdminDashboardScreen(admin = admin, adminViewModel = adminViewModel)
                    AdminSection.USERS -> AdminUserManagementScreen(admin = admin, adminViewModel = adminViewModel)
                    AdminSection.SESSIONS -> AdminStudySessionsScreen(admin = admin, adminViewModel = adminViewModel)
                    AdminSection.SUBJECTS -> AdminSubjectsScreen(admin = admin, adminViewModel = adminViewModel)
                    AdminSection.FRIENDSHIPS,
                    AdminSection.FRIEND_REQUESTS -> AdminSocialScreen(admin = admin, adminViewModel = adminViewModel)
                    AdminSection.REPORTS -> AdminReportsScreen(admin = admin, adminViewModel = adminViewModel)
                    AdminSection.ANNOUNCEMENTS -> AdminAnnouncementsScreen(admin = admin, adminViewModel = adminViewModel)
                    AdminSection.NOTIFICATIONS -> AdminNotificationsScreen(admin = admin, adminViewModel = adminViewModel)
                    AdminSection.CHALLENGES -> AdminChallengesScreen(admin = admin, adminViewModel = adminViewModel)
                    AdminSection.ANALYTICS -> AdminAnalyticsScreen(admin = admin, adminViewModel = adminViewModel)
                    AdminSection.SYSTEM_HEALTH,
                    AdminSection.ERROR_LOGS,
                    AdminSection.AUDIT_LOGS,
                    AdminSection.APP_SETTINGS -> AdminSystemScreen(admin = admin, adminViewModel = adminViewModel)
                    AdminSection.ADMIN_MANAGEMENT -> AdminManagementScreen(admin = admin, adminViewModel = adminViewModel)
                    AdminSection.PROFILE -> AdminProfileScreen(admin = admin, adminViewModel = adminViewModel, onLogout = { adminViewModel.logoutAdmin() })
                    AdminSection.EXPORT -> AdminDashboardScreen(admin = admin, adminViewModel = adminViewModel)
                }
            }
        }
        is AdminAuthUiState.ForcePasswordChange,
        is AdminAuthUiState.Unauthenticated -> {
            AdminAuthScreen(
                adminViewModel = adminViewModel,
                onExitToUserApp = onExitToUserApp,
                modifier = modifier
            )
        }
    }
}
