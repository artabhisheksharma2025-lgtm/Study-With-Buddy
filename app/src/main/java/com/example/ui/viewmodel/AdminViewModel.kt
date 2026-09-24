package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AdminRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class AdminAuthUiState {
    object Unauthenticated : AdminAuthUiState()
    data class ForcePasswordChange(val admin: AdminEntity) : AdminAuthUiState()
    data class Authenticated(val admin: AdminEntity) : AdminAuthUiState()
}

enum class AdminSection(val title: String, val iconName: String) {
    DASHBOARD("Dashboard", "dashboard"),
    USERS("Users", "people"),
    SESSIONS("Study Sessions", "timer"),
    SUBJECTS("Subjects", "book"),
    FRIENDSHIPS("Friendships", "diversity_3"),
    FRIEND_REQUESTS("Friend Requests", "person_add"),
    REPORTS("Reports & Abuse", "report"),
    ANNOUNCEMENTS("Announcements & Banners", "campaign"),
    NOTIFICATIONS("Notifications", "notifications"),
    CHALLENGES("Study Challenges", "military_tech"),
    ANALYTICS("Analytics", "analytics"),
    SYSTEM_HEALTH("System Health", "health_and_safety"),
    ERROR_LOGS("Error Logs", "bug_report"),
    AUDIT_LOGS("Audit Logs", "history_edu"),
    ADMIN_MANAGEMENT("Admin Management", "admin_panel_settings"),
    APP_SETTINGS("App Settings", "settings"),
    PROFILE("Admin Profile", "account_circle"),
    EXPORT("Data Export", "download")
}

data class GlobalSearchResult(
    val users: List<UserEntity> = emptyList(),
    val sessions: List<StudySessionEntity> = emptyList(),
    val reports: List<ReportEntity> = emptyList(),
    val subjects: List<SubjectEntity> = emptyList()
)

class AdminViewModel(private val adminRepo: AdminRepository) : ViewModel() {

    private val _adminAuthState = MutableStateFlow<AdminAuthUiState>(AdminAuthUiState.Unauthenticated)
    val adminAuthState: StateFlow<AdminAuthUiState> = _adminAuthState.asStateFlow()

    private val _currentSection = MutableStateFlow(AdminSection.DASHBOARD)
    val currentSection: StateFlow<AdminSection> = _currentSection.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _loadingText = MutableStateFlow("Loading...")
    val loadingText: StateFlow<String> = _loadingText.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Global Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<GlobalSearchResult?>(null)
    val searchResults: StateFlow<GlobalSearchResult?> = _searchResults.asStateFlow()

    // Dashboard data
    private val _dashboardStats = MutableStateFlow<AdminRepository.DashboardStats?>(null)
    val dashboardStats: StateFlow<AdminRepository.DashboardStats?> = _dashboardStats.asStateFlow()

    val dashboardBanner: StateFlow<AnnouncementEntity?> = adminRepo.getDashboardBannerFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userBanner: StateFlow<AnnouncementEntity?> = adminRepo.getUserBannerFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Data streams
    val users: StateFlow<List<UserEntity>> = adminRepo.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessions: StateFlow<List<StudySessionEntity>> = adminRepo.getAllSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects: StateFlow<List<SubjectEntity>> = adminRepo.getAllSubjectsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendships: StateFlow<List<FriendshipEntity>> = adminRepo.getAllFriendshipsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val friendRequests: StateFlow<List<FriendRequestEntity>> = adminRepo.getAllFriendRequestsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reports: StateFlow<List<ReportEntity>> = adminRepo.getAllReportsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val announcements: StateFlow<List<AnnouncementEntity>> = adminRepo.getAllAnnouncementsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val challenges: StateFlow<List<StudyChallengeEntity>> = adminRepo.getAllChallengesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AdminAuditLogEntity>> = adminRepo.getAllAuditLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val errorLogs: StateFlow<List<AppErrorLogEntity>> = adminRepo.getAllErrorLogsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val admins: StateFlow<List<AdminEntity>> = adminRepo.getAllAdminsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<List<AppSettingEntity>> = adminRepo.getAllSettingsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            adminRepo.seedAdminDataIfNeeded()
            adminRepo.fetchAndSyncOnlineUsers()
            adminRepo.fetchAndSyncOnlineAnnouncements()
            refreshDashboard()
        }
    }

    fun selectSection(section: AdminSection) {
        _currentSection.value = section
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun refreshDashboard() {
        viewModelScope.launch {
            _dashboardStats.value = adminRepo.computeDashboardStats()
        }
    }

    // --- Admin Authentication ---

    fun setAuthenticatedAdmin(admin: AdminEntity) {
        if (admin.mustChangePassword) {
            _adminAuthState.value = AdminAuthUiState.ForcePasswordChange(admin)
        } else {
            _adminAuthState.value = AdminAuthUiState.Authenticated(admin)
            refreshDashboard()
        }
    }

    suspend fun verifyAndAuthenticateAdmin(email: String, passwordAttempt: String): Result<AdminEntity> {
        val result = adminRepo.authenticateAdmin(email, passwordAttempt)
        result.onSuccess { admin ->
            if (admin.mustChangePassword) {
                _adminAuthState.value = AdminAuthUiState.ForcePasswordChange(admin)
            } else {
                _adminAuthState.value = AdminAuthUiState.Authenticated(admin)
                refreshDashboard()
            }
        }.onFailure { ex ->
            _errorMessage.value = ex.message ?: "Authentication failed."
        }
        return result
    }

    fun loginAdmin(email: String, passwordAttempt: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Authenticating Administrator..."
            clearMessages()

            val result = adminRepo.authenticateAdmin(email, passwordAttempt)
            _isLoading.value = false

            result.onSuccess { admin ->
                if (admin.mustChangePassword) {
                    _adminAuthState.value = AdminAuthUiState.ForcePasswordChange(admin)
                } else {
                    _adminAuthState.value = AdminAuthUiState.Authenticated(admin)
                    refreshDashboard()
                }
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Authentication failed."
            }
        }
    }

    fun changePassword(oldPassword: String, newPassword: String) {
        val currentAdmin = when (val state = _adminAuthState.value) {
            is AdminAuthUiState.Authenticated -> state.admin
            is AdminAuthUiState.ForcePasswordChange -> state.admin
            else -> return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Securing and updating credentials..."
            clearMessages()

            val result = adminRepo.changeAdminPassword(currentAdmin.adminUid, oldPassword, newPassword)
            _isLoading.value = false

            result.onSuccess { updatedAdmin ->
                _adminAuthState.value = AdminAuthUiState.Authenticated(updatedAdmin)
                _successMessage.value = "Password updated successfully. Admin access active."
                refreshDashboard()
            }.onFailure { ex ->
                _errorMessage.value = ex.message ?: "Failed to update password."
            }
        }
    }

    fun logoutAdmin() {
        _adminAuthState.value = AdminAuthUiState.Unauthenticated
        _currentSection.value = AdminSection.DASHBOARD
        clearMessages()
    }

    private fun getAuthenticatedAdmin(): AdminEntity? {
        val state = _adminAuthState.value
        return if (state is AdminAuthUiState.Authenticated) state.admin else null
    }

    // --- Global Search ---

    fun performGlobalSearch(query: String) {
        _searchQuery.value = query
        val q = query.trim().lowercase()
        if (q.isBlank()) {
            _searchResults.value = null
            return
        }

        val allUsers = users.value
        val allSessions = sessions.value
        val allReports = reports.value
        val allSubjects = subjects.value

        val matchedUsers = allUsers.filter {
            it.fullName.lowercase().contains(q) ||
                    it.email.lowercase().contains(q) ||
                    it.studyId.lowercase().contains(q) ||
                    it.userId.lowercase().contains(q)
        }

        val matchedSessions = allSessions.filter {
            it.subjectName.lowercase().contains(q) ||
                    it.title.lowercase().contains(q) ||
                    it.userId.lowercase().contains(q) ||
                    it.sessionId.lowercase().contains(q)
        }

        val matchedReports = allReports.filter {
            it.reporterName.lowercase().contains(q) ||
                    it.reportedUserName.lowercase().contains(q) ||
                    it.category.lowercase().contains(q) ||
                    it.description.lowercase().contains(q)
        }

        val matchedSubjects = allSubjects.filter {
            it.name.lowercase().contains(q) ||
                    it.description.lowercase().contains(q)
        }

        _searchResults.value = GlobalSearchResult(
            users = matchedUsers,
            sessions = matchedSessions,
            reports = matchedReports,
            subjects = matchedSubjects
        )
    }

    // --- User Actions ---

    fun updateUserStatus(userId: String, newStatus: String, reason: String) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Updating user status..."
            clearMessages()

            val result = adminRepo.updateUserStatus(admin, userId, newStatus, reason)
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = "User status updated to $newStatus."
                refreshDashboard()
            }.onFailure { ex ->
                _errorMessage.value = ex.message
            }
        }
    }

    fun deleteUser(userId: String, softDelete: Boolean, reason: String) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Processing account deletion..."
            clearMessages()

            val result = adminRepo.deleteUserAccount(admin, userId, softDelete, reason)
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = if (softDelete) "Account soft-deleted." else "Account permanently deleted."
                refreshDashboard()
            }.onFailure { ex ->
                _errorMessage.value = ex.message
            }
        }
    }

    fun editUserDetails(
        userId: String,
        fullName: String,
        email: String,
        password: String,
        status: String,
        studyId: String
    ) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Updating user details..."
            clearMessages()

            val result = adminRepo.updateUserDetails(
                actorAdmin = admin,
                targetUserId = userId,
                newFullName = fullName,
                newEmail = email,
                newPassword = password,
                newStatus = status,
                newStudyId = studyId
            )
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = "User $fullName updated successfully."
                refreshDashboard()
            }.onFailure { ex ->
                _errorMessage.value = ex.message
            }
        }
    }

    fun refreshOnlineData() {
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Synchronizing online cloud data..."
            adminRepo.fetchAndSyncOnlineUsers()
            adminRepo.fetchAndSyncOnlineAnnouncements()
            _isLoading.value = false
            refreshDashboard()
        }
    }

    // --- Session Actions ---

    fun validateSession(session: StudySessionEntity) = adminRepo.validateSessionTimestamps(session)

    fun correctSession(
        sessionId: String,
        newDurationSeconds: Long,
        newTitle: String,
        newNotes: String,
        reason: String
    ) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Saving session correction..."
            clearMessages()

            val result = adminRepo.correctSession(admin, sessionId, newDurationSeconds, newTitle, newNotes, reason)
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = "Study session corrected and audit log recorded."
                refreshDashboard()
            }.onFailure { ex ->
                _errorMessage.value = ex.message
            }
        }
    }

    fun deleteSession(sessionId: String, reason: String) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Deleting session..."
            clearMessages()

            val result = adminRepo.deleteSession(admin, sessionId, reason)
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = "Study session removed."
                refreshDashboard()
            }.onFailure { ex ->
                _errorMessage.value = ex.message
            }
        }
    }

    // --- Subject Actions ---

    fun saveSubject(
        subjectId: String?,
        name: String,
        colorHex: String,
        description: String,
        status: String,
        isDefault: Boolean
    ) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Saving subject..."
            clearMessages()

            val result = adminRepo.saveSubject(admin, subjectId, name, colorHex, description, status, isDefault)
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = "Subject saved successfully."
            }.onFailure { ex ->
                _errorMessage.value = ex.message
            }
        }
    }

    fun toggleSubjectStatus(subject: SubjectEntity) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            val result = adminRepo.toggleSubjectStatus(admin, subject)
            result.onFailure { _errorMessage.value = it.message }
        }
    }

    // --- Report Actions ---

    fun updateReport(reportId: String, newStatus: String, note: String) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Updating report..."
            clearMessages()

            val result = adminRepo.updateReport(admin, reportId, newStatus, note)
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = "Report marked as $newStatus."
                refreshDashboard()
            }.onFailure { ex ->
                _errorMessage.value = ex.message
            }
        }
    }

    // --- Announcement Actions ---

    fun saveAnnouncement(announcement: AnnouncementEntity) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Publishing announcement..."
            clearMessages()

            val result = adminRepo.saveAnnouncement(admin, announcement)
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = "Announcement banner saved."
                refreshDashboard()
            }.onFailure { ex ->
                _errorMessage.value = ex.message
            }
        }
    }

    fun deleteAnnouncement(announcement: AnnouncementEntity) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            adminRepo.deleteAnnouncement(admin, announcement)
            _successMessage.value = "Announcement removed."
        }
    }

    // --- Notification Actions ---

    fun broadcastNotification(title: String, message: String, targetUserId: String? = null) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Broadcasting notification..."
            clearMessages()

            val result = adminRepo.sendAdminNotification(admin, title, message, targetUserId)
            _isLoading.value = false

            result.onSuccess { count ->
                _successMessage.value = "Dispatched notification to $count user(s)."
            }.onFailure { ex ->
                _errorMessage.value = ex.message
            }
        }
    }

    // --- Challenge Actions ---

    fun saveChallenge(challenge: StudyChallengeEntity) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            val result = adminRepo.saveChallenge(admin, challenge)
            result.onSuccess {
                _successMessage.value = "Challenge updated."
            }.onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    fun deleteChallenge(challenge: StudyChallengeEntity) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            adminRepo.deleteChallenge(admin, challenge)
            _successMessage.value = "Challenge removed."
        }
    }

    // --- Error Logs ---

    fun resolveError(error: AppErrorLogEntity) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            adminRepo.resolveErrorLog(admin, error)
            _successMessage.value = "Error log marked as RESOLVED."
            refreshDashboard()
        }
    }

    // --- App Settings ---

    fun saveSetting(key: String, value: String, category: String, description: String) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            val result = adminRepo.saveSetting(admin, key, value, category, description)
            result.onSuccess {
                _successMessage.value = "Setting saved."
            }.onFailure {
                _errorMessage.value = it.message
            }
        }
    }

    // --- Admin Management (Super Admin only) ---

    fun createAdmin(email: String, name: String, password: String, role: String) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Provisioning administrator..."
            clearMessages()

            val result = adminRepo.createAdminAccount(admin, email, name, password, role)
            _isLoading.value = false

            result.onSuccess {
                _successMessage.value = "Administrator account created for ${it.email}."
            }.onFailure { ex ->
                _errorMessage.value = ex.message
            }
        }
    }

    fun updateAdminRole(targetAdminUid: String, newRole: String) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            val result = adminRepo.updateAdminRole(admin, targetAdminUid, newRole)
            result.onSuccess { _successMessage.value = "Admin role updated." }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun setAdminStatus(targetAdminUid: String, newStatus: String) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            val result = adminRepo.setAdminStatus(admin, targetAdminUid, newStatus)
            result.onSuccess { _successMessage.value = "Admin status updated to $newStatus." }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    fun removeAdmin(targetAdminUid: String) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            val result = adminRepo.removeAdminAccount(admin, targetAdminUid)
            result.onSuccess { _successMessage.value = "Administrator removed." }
                .onFailure { _errorMessage.value = it.message }
        }
    }

    // --- Export ---

    fun exportData(exportType: String, onResult: (String) -> Unit) {
        val admin = getAuthenticatedAdmin() ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _loadingText.value = "Exporting $exportType data..."
            clearMessages()

            val result = adminRepo.exportData(admin, exportType)
            _isLoading.value = false

            result.onSuccess { csvData ->
                _successMessage.value = "$exportType data exported successfully."
                onResult(csvData)
            }.onFailure { ex ->
                _errorMessage.value = ex.message
            }
        }
    }
}
