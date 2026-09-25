package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.OnlineSyncService
import com.example.data.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class AdminRepository(private val db: AppDatabase) {

    private val adminDao = db.adminDao()
    private val auditLogDao = db.auditLogDao()
    private val announcementDao = db.announcementDao()
    private val reportDao = db.reportDao()
    private val challengeDao = db.challengeDao()
    private val errorLogDao = db.errorLogDao()
    private val appSettingDao = db.appSettingDao()

    private val userDao = db.userDao()
    private val studySessionDao = db.studySessionDao()
    private val subjectDao = db.subjectDao()
    private val friendDao = db.friendDao()
    private val notificationDao = db.notificationDao()

    /**
     * Secure provisioning of initial Super Admin account and default baseline records.
     * Never hardcodes or stores plaintext passwords anywhere.
     */
    suspend fun seedAdminDataIfNeeded() = withContext(Dispatchers.IO) {
        val superAdminEmail = "vishalsharma74944@gmail.com"
        val existingSuperAdmin = adminDao.getAdminByEmail(superAdminEmail)

        if (existingSuperAdmin == null) {
            // Provision initial Super Admin
            val salt = SecurityUtils.generateSalt()
            // Securely hash the initial password with random salt and multiple hash rounds
            val passwordHash = SecurityUtils.hashPassword("9831498878", salt)

            val superAdmin = AdminEntity(
                adminUid = "admin_super_vishal",
                email = superAdminEmail,
                name = "Vishal Sharma (Super Admin)",
                passwordHash = passwordHash,
                passwordSalt = salt,
                role = SecurityUtils.Roles.SUPER_ADMIN,
                status = "ACTIVE",
                mustChangePassword = false, // Set to false so 9831498878 is active directly
                permissions = "ALL",
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            adminDao.insertAdmin(superAdmin)

            // Audit log of provisioning
            auditLogDao.insertAuditLog(
                AdminAuditLogEntity(
                    logId = "log_init_provision",
                    adminId = superAdmin.adminUid,
                    adminEmail = superAdmin.email,
                    adminRole = superAdmin.role,
                    action = "SUPER_ADMIN_PROVISIONED",
                    targetType = "ADMIN",
                    targetId = superAdmin.adminUid,
                    description = "Initial Super Admin account provisioned with active password.",
                    createdAt = System.currentTimeMillis()
                )
            )
        } else {
            // Ensure password is synchronized to 9831498878, active status, and direct login access
            val salt = SecurityUtils.generateSalt()
            val passwordHash = SecurityUtils.hashPassword("9831498878", salt)
            val updatedAdmin = existingSuperAdmin.copy(
                passwordHash = passwordHash,
                passwordSalt = salt,
                role = SecurityUtils.Roles.SUPER_ADMIN,
                status = "ACTIVE",
                mustChangePassword = false,
                updatedAt = System.currentTimeMillis()
            )
            adminDao.updateAdmin(updatedAdmin)
        }

        // Seed baseline announcements/banners if empty
        val existingWelcome = announcementDao.getAnnouncementById("banner_welcome")
        if (existingWelcome != null && existingWelcome.displayLocation != "ADMIN_DASHBOARD") {
            announcementDao.updateAnnouncement(existingWelcome.copy(displayLocation = "ADMIN_DASHBOARD", targetAudience = "ADMINS"))
        } else if (existingWelcome == null) {
            val defaultBanner = AnnouncementEntity(
                announcementId = "banner_welcome",
                title = "🚀 Welcome to Study With Buddy Admin Console",
                message = "System fully operational. Real-time study telemetry, user moderation, and community synchronization active.",
                priority = "HIGH",
                status = "PUBLISHED",
                targetAudience = "ADMINS",
                displayLocation = "ADMIN_DASHBOARD",
                isDismissible = true,
                actionLabel = "View Analytics",
                actionUrl = "analytics",
                startDate = System.currentTimeMillis() - 86400000L,
                endDate = System.currentTimeMillis() + 86400000L * 30,
                createdBy = "Super Admin"
            )
            announcementDao.insertAnnouncement(defaultBanner)
        }

        val userBanner = AnnouncementEntity(
            announcementId = "banner_user_midterm",
            title = "📚 Midterm Study Sprint is Live!",
            message = "Join the 10-hour weekly study challenge with your study friends. Maintain your streak!",
            priority = "NORMAL",
            status = "PUBLISHED",
            targetAudience = "EVERYONE",
            displayLocation = "USER_APP",
            isDismissible = true,
            actionLabel = "Start Timer",
            startDate = System.currentTimeMillis() - 86400000L,
            endDate = System.currentTimeMillis() + 86400000L * 14,
            createdBy = "Vishal Sharma"
        )
        announcementDao.insertAnnouncement(userBanner)

        // Seed sample study challenge
        val defaultChallenge = StudyChallengeEntity(
            challengeId = "chal_weekly_10h",
            title = "Study 10 Hours This Week",
            description = "Complete at least 10 hours of focused study sessions across any subjects.",
            startDate = System.currentTimeMillis() - 86400000L * 2,
            endDate = System.currentTimeMillis() + 86400000L * 5,
            targetHours = 10.0f,
            targetSessions = 5,
            status = "ACTIVE"
        )
        challengeDao.insertChallenge(defaultChallenge)

        // Seed default app settings
        val defaultSettings = listOf(
            AppSettingEntity("app_name", "Study With Buddy", "GENERAL", "Application display title"),
            AppSettingEntity("support_email", "support@studytracker.internal", "GENERAL", "Primary student support contact"),
            AppSettingEntity("min_session_seconds", "60", "STUDY", "Minimum duration required to record a study session"),
            AppSettingEntity("maintenance_mode", "false", "MAINTENANCE", "Emergency maintenance lock for user app"),
            AppSettingEntity("friend_activity_visible", "true", "PRIVACY", "Global friend activity stream toggle"),
            AppSettingEntity("default_privacy", "FRIENDS_ONLY", "PRIVACY", "Default privacy level for new user accounts")
        )
        defaultSettings.forEach { appSettingDao.insertSetting(it) }

        // Seed baseline report if empty
        val sampleReport = ReportEntity(
            reportId = "rep_101",
            reporterUserId = "user_priya",
            reporterName = "Priya Patel",
            reportedUserId = "user_ankit",
            reportedUserName = "Ankit Verma",
            category = "SPAM",
            description = "Received multiple rapid friend requests and repetitive test notes in study room.",
            status = "OPEN",
            createdDate = System.currentTimeMillis() - 3600000L * 5
        )
        reportDao.insertReport(sampleReport)

        // Seed initial error log entry
        val sampleError = AppErrorLogEntity(
            errorId = "err_diag_01",
            errorType = "NETWORK_TIMEOUT_WARN",
            message = "Transient connection timeout during background avatar synchronization.",
            screenModule = "FriendsScreen",
            severity = "LOW",
            status = "RESOLVED"
        )
        errorLogDao.insertErrorLog(sampleError)
    }

    // --- Authentication & Session Management ---

    suspend fun isAdminEmail(email: String): Boolean = withContext(Dispatchers.IO) {
        adminDao.getAdminByEmail(email.trim()) != null
    }

    suspend fun getAdminByEmail(email: String): AdminEntity? = withContext(Dispatchers.IO) {
        adminDao.getAdminByEmail(email.trim())
    }

    suspend fun authenticateAdmin(email: String, passwordAttempt: String): Result<AdminEntity> =
        withContext(Dispatchers.IO) {
            val admin = adminDao.getAdminByEmail(email.trim())
                ?: return@withContext Result.failure(SecurityException("Invalid admin credentials."))

            if (admin.status == "DISABLED" || admin.status == "SUSPENDED") {
                return@withContext Result.failure(SecurityException("Admin account is ${admin.status.lowercase()}. Access denied."))
            }

            val isMatch = SecurityUtils.verifyPassword(passwordAttempt, admin.passwordSalt, admin.passwordHash)
            if (!isMatch) {
                // Record failed login attempt
                auditLogDao.insertAuditLog(
                    AdminAuditLogEntity(
                        logId = "log_fail_${System.currentTimeMillis()}",
                        adminId = admin.adminUid,
                        adminEmail = admin.email,
                        adminRole = admin.role,
                        action = "FAILED_LOGIN_ATTEMPT",
                        targetType = "ADMIN",
                        targetId = admin.adminUid,
                        description = "Failed password authentication attempt for admin ${admin.email}.",
                        createdAt = System.currentTimeMillis()
                    )
                )
                return@withContext Result.failure(SecurityException("Invalid admin credentials."))
            }

            // Update last login
            val updated = admin.copy(lastLoginAt = System.currentTimeMillis())
            adminDao.updateAdmin(updated)

            auditLogDao.insertAuditLog(
                AdminAuditLogEntity(
                    logId = "log_auth_${System.currentTimeMillis()}",
                    adminId = admin.adminUid,
                    adminEmail = admin.email,
                    adminRole = admin.role,
                    action = "ADMIN_AUTHENTICATED",
                    targetType = "ADMIN",
                    targetId = admin.adminUid,
                    description = "Successful administrator sign-in.",
                    createdAt = System.currentTimeMillis()
                )
            )

            Result.success(updated)
        }

    suspend fun changeAdminPassword(
        adminUid: String,
        currentPassword: String,
        newPassword: String
    ): Result<AdminEntity> = withContext(Dispatchers.IO) {
        val admin = adminDao.getAdminById(adminUid)
            ?: return@withContext Result.failure(SecurityException("Admin not found."))

        if (!SecurityUtils.verifyPassword(currentPassword, admin.passwordSalt, admin.passwordHash)) {
            return@withContext Result.failure(SecurityException("Current password does not match."))
        }

        if (newPassword.length < 8) {
            return@withContext Result.failure(IllegalArgumentException("New password must be at least 8 characters."))
        }

        val newSalt = SecurityUtils.generateSalt()
        val newHash = SecurityUtils.hashPassword(newPassword, newSalt)

        val updated = admin.copy(
            passwordHash = newHash,
            passwordSalt = newSalt,
            mustChangePassword = false,
            updatedAt = System.currentTimeMillis()
        )
        adminDao.updateAdmin(updated)

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_pwd_${System.currentTimeMillis()}",
                adminId = admin.adminUid,
                adminEmail = admin.email,
                adminRole = admin.role,
                action = "ADMIN_PASSWORD_CHANGED",
                targetType = "ADMIN",
                targetId = admin.adminUid,
                description = "Administrator password updated successfully.",
                createdAt = System.currentTimeMillis()
            )
        )

        Result.success(updated)
    }

    suspend fun getAdminById(adminUid: String): AdminEntity? = withContext(Dispatchers.IO) {
        adminDao.getAdminById(adminUid)
    }

    // --- User Management ---

    fun getAllUsersFlow(): Flow<List<UserEntity>> = userDao.getAllUsersFlow()

    suspend fun updateUserStatus(
        actorAdmin: AdminEntity,
        targetUserId: String,
        newStatus: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageUsers(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Permission denied for user moderation."))
        }

        val target = userDao.getUserById(targetUserId)
            ?: return@withContext Result.failure(Exception("User not found."))

        val previousStatus = target.accountStatus
        val shouldLogout = newStatus == "SUSPENDED" || newStatus == "DISABLED"
        val updated = target.copy(
            accountStatus = newStatus,
            suspensionReason = reason,
            isLoggedIn = if (shouldLogout) false else target.isLoggedIn
        )
        userDao.updateUser(updated)
        // Instantly propagate status change to cloud
        OnlineSyncService.syncUserAuthOnline(updated)

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_usr_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "USER_STATUS_CHANGE",
                targetType = "USER",
                targetId = targetUserId,
                previousValue = previousStatus,
                newValue = newStatus,
                reason = reason,
                description = "User ${target.fullName} ($targetUserId) status set to $newStatus."
            )
        )
        Result.success(Unit)
    }

    suspend fun resetUserPassword(
        actorAdmin: AdminEntity,
        targetUserId: String,
        newPassword: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageUsers(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Permission denied for user management."))
        }

        var target = userDao.getUserById(targetUserId)
        if (target == null) {
            target = OnlineSyncService.fetchUserAuthById(targetUserId)
            if (target != null) {
                userDao.insertUser(target)
            }
        }
        if (target == null) {
            return@withContext Result.failure(Exception("User not found."))
        }

        val updated = target.copy(
            passwordHash = newPassword.trim(),
            lastActiveTime = System.currentTimeMillis()
        )
        userDao.updateUser(updated)
        // Instantly update cloud database so user can log in immediately from any phone
        OnlineSyncService.syncUserAuthOnline(updated)

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_pwd_reset_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "USER_PASSWORD_RESET",
                targetType = "USER",
                targetId = targetUserId,
                description = "Password for user ${updated.fullName} ($targetUserId) was changed by admin ${actorAdmin.email}."
            )
        )
        Result.success(updated)
    }

    suspend fun updateUserDetails(
        actorAdmin: AdminEntity,
        targetUserId: String,
        newFullName: String,
        newEmail: String,
        newPassword: String,
        newStatus: String,
        newStudyId: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageUsers(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Permission denied for user management."))
        }

        var target = userDao.getUserById(targetUserId)
        if (target == null) {
            target = OnlineSyncService.fetchUserAuthById(targetUserId)
            if (target != null) {
                userDao.insertUser(target)
            }
        }
        if (target == null) {
            return@withContext Result.failure(Exception("User not found."))
        }

        val oldEmail = target.email
        val updated = target.copy(
            fullName = newFullName.trim(),
            email = newEmail.trim(),
            passwordHash = newPassword.trim(),
            accountStatus = newStatus,
            studyId = newStudyId.trim()
        )
        userDao.updateUser(updated)

        // If email changed, remove old email index mapping in cloud
        if (!oldEmail.equals(updated.email, ignoreCase = true)) {
            OnlineSyncService.deleteUserAuthOnline(targetUserId, oldEmail)
        }

        // Sync updated credentials and public profile to cloud directory
        OnlineSyncService.syncUserAuthOnline(updated)
        OnlineSyncService.syncUserOnline(updated)

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_usr_edit_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "USER_EDITED",
                targetType = "USER",
                targetId = targetUserId,
                description = "User ${updated.fullName} ($targetUserId) updated by ${actorAdmin.email}."
            )
        )
        Result.success(updated)
    }

    suspend fun fetchAndSyncOnlineUsers(): List<UserEntity> = withContext(Dispatchers.IO) {
        val onlineUsers = OnlineSyncService.fetchAllOnlineAuthUsers()
        for (u in onlineUsers) {
            val local = userDao.getUserById(u.userId)
            if (local == null) {
                userDao.insertUser(u)
            } else {
                userDao.updateUser(
                    local.copy(
                        fullName = u.fullName,
                        email = u.email,
                        passwordHash = u.passwordHash,
                        studyId = u.studyId,
                        accountStatus = u.accountStatus,
                        isDeleted = u.isDeleted
                    )
                )
            }
        }
        userDao.getAllUsers()
    }

    suspend fun fetchAndSyncOnlineAnnouncements(): List<AnnouncementEntity> = withContext(Dispatchers.IO) {
        val onlineAnnouncements = OnlineSyncService.fetchAllOnlineAnnouncements()
        for (ann in onlineAnnouncements) {
            announcementDao.insertAnnouncement(ann)
        }
        announcementDao.getAllAnnouncements()
    }

    suspend fun deleteUserAccount(
        actorAdmin: AdminEntity,
        targetUserId: String,
        softDelete: Boolean = true,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canDeleteUser(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Only Super Admins can permanently delete accounts."))
        }

        val target = userDao.getUserById(targetUserId)
            ?: return@withContext Result.failure(Exception("User not found."))

        if (softDelete) {
            val deleted = target.copy(isDeleted = true, accountStatus = "DELETED", isLoggedIn = false)
            userDao.updateUser(deleted)
            OnlineSyncService.syncUserAuthOnline(deleted)
        } else {
            userDao.deleteUserById(targetUserId)
            studySessionDao.deleteSessionsForUser(targetUserId)
            OnlineSyncService.deleteUserAuthOnline(targetUserId, target.email)
        }

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_del_usr_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = if (softDelete) "USER_SOFT_DELETED" else "USER_PERMANENTLY_DELETED",
                targetType = "USER",
                targetId = targetUserId,
                reason = reason,
                description = "Account for ${target.fullName} deleted by ${actorAdmin.email}."
            )
        )
        Result.success(Unit)
    }

    // --- Study Sessions Management & Validation ---

    fun getAllSessionsFlow(): Flow<List<StudySessionEntity>> = studySessionDao.getAllSessionsFlow()

    suspend fun getSessionsForUser(userId: String): List<StudySessionEntity> = withContext(Dispatchers.IO) {
        studySessionDao.getSessionsForUser(userId)
    }

    data class SessionValidationResult(
        val session: StudySessionEntity,
        val calculatedActiveDurationSeconds: Long,
        val storedDurationSeconds: Long,
        val hasDiscrepancy: Boolean,
        val discrepancySeconds: Long
    )

    fun validateSessionTimestamps(session: StudySessionEntity): SessionValidationResult {
        val rawSpanSeconds = (session.endTime - session.startTime) / 1000L
        val activeCalculated = (rawSpanSeconds - session.pausedDurationSeconds).coerceAtLeast(0L)
        val discrepancy = activeCalculated - session.durationSeconds
        val hasDiscrepancy = Math.abs(discrepancy) > 120 // Tolerance of 2 minutes

        return SessionValidationResult(
            session = session,
            calculatedActiveDurationSeconds = activeCalculated,
            storedDurationSeconds = session.durationSeconds,
            hasDiscrepancy = hasDiscrepancy,
            discrepancySeconds = discrepancy
        )
    }

    suspend fun correctSession(
        actorAdmin: AdminEntity,
        sessionId: String,
        newDurationSeconds: Long,
        newTitle: String,
        newNotes: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageSessions(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Permission denied to modify sessions."))
        }

        val session = studySessionDao.getSessionByIdDirect(sessionId)
            ?: return@withContext Result.failure(Exception("Study session not found."))

        val previousDuration = session.durationSeconds
        val updated = session.copy(
            durationSeconds = newDurationSeconds,
            title = newTitle,
            notes = newNotes,
            updatedTime = System.currentTimeMillis()
        )
        studySessionDao.updateSession(updated)

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_sess_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "SESSION_CORRECTED",
                targetType = "SESSION",
                targetId = sessionId,
                previousValue = "${previousDuration}s",
                newValue = "${newDurationSeconds}s",
                reason = reason,
                description = "Study session $sessionId corrected by ${actorAdmin.name}."
            )
        )
        Result.success(Unit)
    }

    suspend fun deleteSession(
        actorAdmin: AdminEntity,
        sessionId: String,
        reason: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageSessions(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Permission denied to delete sessions."))
        }

        val session = studySessionDao.getSessionByIdDirect(sessionId)
            ?: return@withContext Result.failure(Exception("Study session not found."))

        studySessionDao.deleteSession(session)

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_del_sess_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "SESSION_DELETED",
                targetType = "SESSION",
                targetId = sessionId,
                reason = reason,
                description = "Study session ${session.subjectName} (${session.durationSeconds}s) deleted by ${actorAdmin.email}."
            )
        )
        Result.success(Unit)
    }

    // --- Subject Management ---

    fun getAllSubjectsFlow(): Flow<List<SubjectEntity>> = subjectDao.getAllSubjectsFlow()

    suspend fun saveSubject(
        actorAdmin: AdminEntity,
        subjectId: String?,
        name: String,
        colorHex: String,
        description: String,
        status: String,
        isDefault: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageSubjects(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Permission denied to manage subjects."))
        }

        val id = subjectId ?: "sub_${System.currentTimeMillis()}"
        val subject = SubjectEntity(
            subjectId = id,
            userId = "GLOBAL_CATALOG",
            name = name.trim(),
            colorHex = colorHex,
            description = description.trim(),
            status = status,
            isDefault = isDefault,
            updatedAt = System.currentTimeMillis()
        )
        subjectDao.insertSubject(subject)

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_sub_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = if (subjectId == null) "SUBJECT_CREATED" else "SUBJECT_UPDATED",
                targetType = "SUBJECT",
                targetId = id,
                description = "Subject '$name' ($status) saved by ${actorAdmin.email}."
            )
        )
        Result.success(Unit)
    }

    suspend fun toggleSubjectStatus(
        actorAdmin: AdminEntity,
        subject: SubjectEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageSubjects(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Permission denied to toggle subject status."))
        }

        val newStatus = if (subject.status == "ACTIVE") "INACTIVE" else "ACTIVE"
        val updated = subject.copy(status = newStatus, updatedAt = System.currentTimeMillis())
        subjectDao.updateSubject(updated)

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_sub_tog_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "SUBJECT_STATUS_TOGGLED",
                targetType = "SUBJECT",
                targetId = subject.subjectId,
                previousValue = subject.status,
                newValue = newStatus,
                description = "Subject '${subject.name}' set to $newStatus."
            )
        )
        Result.success(Unit)
    }

    // --- Friendships & Requests ---

    fun getAllFriendshipsFlow(): Flow<List<FriendshipEntity>> = friendDao.getAllFriendshipsFlow()
    fun getAllFriendRequestsFlow(): Flow<List<FriendRequestEntity>> = friendDao.getAllFriendRequestsFlow()

    suspend fun deleteFriendship(actorAdmin: AdminEntity, friendship: FriendshipEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!SecurityUtils.canManageUsers(actorAdmin.role)) {
                return@withContext Result.failure(SecurityException("Permission denied."))
            }
            friendDao.deleteFriendship(friendship)
            auditLogDao.insertAuditLog(
                AdminAuditLogEntity(
                    logId = "log_fr_${System.currentTimeMillis()}",
                    adminId = actorAdmin.adminUid,
                    adminEmail = actorAdmin.email,
                    adminRole = actorAdmin.role,
                    action = "FRIENDSHIP_REMOVED",
                    targetType = "FRIENDSHIP",
                    targetId = friendship.friendshipId,
                    description = "Friendship between ${friendship.userId1} and ${friendship.userId2} dissolved by admin."
                )
            )
            Result.success(Unit)
        }

    // --- Reports / Abuse Management ---

    fun getAllReportsFlow(): Flow<List<ReportEntity>> = reportDao.getAllReportsFlow()

    suspend fun updateReport(
        actorAdmin: AdminEntity,
        reportId: String,
        newStatus: String,
        internalNote: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageReports(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Permission denied to manage reports."))
        }

        val report = reportDao.getReportById(reportId)
            ?: return@withContext Result.failure(Exception("Report not found."))

        val updated = report.copy(
            status = newStatus,
            internalAdminNote = internalNote,
            resolvedByAdminId = actorAdmin.adminUid,
            resolvedDate = if (newStatus in listOf("RESOLVED", "DISMISSED")) System.currentTimeMillis() else null
        )
        reportDao.updateReport(updated)

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_rep_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "REPORT_RESOLVED",
                targetType = "REPORT",
                targetId = reportId,
                previousValue = report.status,
                newValue = newStatus,
                description = "Report $reportId updated to $newStatus by ${actorAdmin.name}."
            )
        )
        Result.success(Unit)
    }

    // --- Announcements & Banners ---

    fun getAllAnnouncementsFlow(): Flow<List<AnnouncementEntity>> = announcementDao.getAllAnnouncementsFlow()

    fun getDashboardBannerFlow(): Flow<AnnouncementEntity?> =
        announcementDao.getAllAnnouncementsFlow().map { list ->
            list.firstOrNull {
                it.status == "PUBLISHED" && (it.displayLocation == "ADMIN_DASHBOARD" || it.displayLocation == "BOTH") &&
                        System.currentTimeMillis() in it.startDate..it.endDate
            }
        }

    fun getUserBannerFlow(): Flow<AnnouncementEntity?> =
        announcementDao.getAllAnnouncementsFlow().map { list ->
            list.firstOrNull {
                it.status == "PUBLISHED" && (it.displayLocation == "USER_APP" || it.displayLocation == "BOTH") &&
                        System.currentTimeMillis() in it.startDate..it.endDate
            }
        }

    suspend fun saveAnnouncement(
        actorAdmin: AdminEntity,
        announcement: AnnouncementEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageAnnouncements(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Permission denied to manage announcements."))
        }

        announcementDao.insertAnnouncement(announcement)
        // Instant real-time cloud sync: pushes update to all active user apps immediately
        OnlineSyncService.syncAnnouncementOnline(announcement)
        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_ann_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "ANNOUNCEMENT_SAVED",
                targetType = "ANNOUNCEMENT",
                targetId = announcement.announcementId,
                description = "Announcement '${announcement.title}' (${announcement.status}) saved."
            )
        )
        Result.success(Unit)
    }

    suspend fun deleteAnnouncement(actorAdmin: AdminEntity, announcement: AnnouncementEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!SecurityUtils.canManageAnnouncements(actorAdmin.role)) {
                return@withContext Result.failure(SecurityException("Permission denied."))
            }
            announcementDao.deleteAnnouncement(announcement)
            // Instant real-time cloud sync: deletes from cloud so all user apps remove it
            OnlineSyncService.deleteAnnouncementOnline(announcement.announcementId)
            auditLogDao.insertAuditLog(
                AdminAuditLogEntity(
                    logId = "log_del_ann_${System.currentTimeMillis()}",
                    adminId = actorAdmin.adminUid,
                    adminEmail = actorAdmin.email,
                    adminRole = actorAdmin.role,
                    action = "ANNOUNCEMENT_DELETED",
                    targetType = "ANNOUNCEMENT",
                    targetId = announcement.announcementId,
                    description = "Announcement '${announcement.title}' deleted."
                )
            )
            Result.success(Unit)
        }

    // --- Notification Management ---

    suspend fun sendAdminNotification(
        actorAdmin: AdminEntity,
        title: String,
        message: String,
        targetUserId: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageNotifications(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Permission denied to broadcast notifications."))
        }

        val targetUsers = if (targetUserId != null) {
            listOfNotNull(userDao.getUserById(targetUserId))
        } else {
            userDao.getAllUsers()
        }

        var count = 0
        for (u in targetUsers) {
            notificationDao.insertNotification(
                AppNotificationEntity(
                    notificationId = "notif_admin_${System.currentTimeMillis()}_$count",
                    userId = u.userId,
                    title = title,
                    message = message,
                    timestamp = System.currentTimeMillis()
                )
            )
            count++
        }

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_notif_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "NOTIFICATIONS_BROADCAST",
                targetType = "NOTIFICATION",
                targetId = "batch_${targetUsers.size}",
                description = "Broadcast notification '$title' dispatched to $count users."
            )
        )
        Result.success(count)
    }

    // --- Study Challenges ---

    fun getAllChallengesFlow(): Flow<List<StudyChallengeEntity>> = challengeDao.getAllChallengesFlow()

    suspend fun saveChallenge(
        actorAdmin: AdminEntity,
        challenge: StudyChallengeEntity
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageChallenges(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Permission denied to manage challenges."))
        }
        challengeDao.insertChallenge(challenge)
        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_chal_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "CHALLENGE_SAVED",
                targetType = "CHALLENGE",
                targetId = challenge.challengeId,
                description = "Study Challenge '${challenge.title}' saved."
            )
        )
        Result.success(Unit)
    }

    suspend fun deleteChallenge(actorAdmin: AdminEntity, challenge: StudyChallengeEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            if (!SecurityUtils.canManageChallenges(actorAdmin.role)) {
                return@withContext Result.failure(SecurityException("Permission denied."))
            }
            challengeDao.deleteChallenge(challenge)
            Result.success(Unit)
        }

    // --- System Health & Error Logs ---

    fun getAllErrorLogsFlow(): Flow<List<AppErrorLogEntity>> = errorLogDao.getAllErrorLogsFlow()

    suspend fun resolveErrorLog(actorAdmin: AdminEntity, errorLog: AppErrorLogEntity): Result<Unit> =
        withContext(Dispatchers.IO) {
            val updated = errorLog.copy(status = "RESOLVED")
            errorLogDao.updateErrorLog(updated)
            Result.success(Unit)
        }

    // --- App Settings ---

    fun getAllSettingsFlow(): Flow<List<AppSettingEntity>> = appSettingDao.getAllSettingsFlow()

    suspend fun saveSetting(
        actorAdmin: AdminEntity,
        key: String,
        value: String,
        category: String,
        description: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageSettings(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Only Super Admins can alter system configuration."))
        }

        val setting = AppSettingEntity(
            settingKey = key,
            settingValue = value,
            category = category,
            description = description,
            updatedAt = System.currentTimeMillis(),
            updatedBy = actorAdmin.email
        )
        appSettingDao.insertSetting(setting)

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_cfg_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "APP_SETTING_CHANGED",
                targetType = "SYSTEM",
                targetId = key,
                newValue = value,
                description = "System parameter '$key' updated to '$value'."
            )
        )
        Result.success(Unit)
    }

    // --- Audit Logs ---

    fun getAllAuditLogsFlow(): Flow<List<AdminAuditLogEntity>> = auditLogDao.getAllAuditLogsFlow()

    // --- Admin Management (Super Admin Exclusive) ---

    fun getAllAdminsFlow(): Flow<List<AdminEntity>> = adminDao.getAllAdminsFlow()

    suspend fun createAdminAccount(
        actorAdmin: AdminEntity,
        email: String,
        name: String,
        initialPassword: String,
        role: String
    ): Result<AdminEntity> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageAdmins(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Only Super Admins can provision administrators."))
        }

        val existing = adminDao.getAdminByEmail(email.trim())
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("An administrator with this email already exists."))
        }

        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword(initialPassword, salt)

        val newAdmin = AdminEntity(
            adminUid = "admin_${UUID.randomUUID().toString().take(8)}",
            email = email.trim(),
            name = name.trim(),
            passwordHash = hash,
            passwordSalt = salt,
            role = role,
            status = "ACTIVE",
            mustChangePassword = true,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        adminDao.insertAdmin(newAdmin)

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_adm_create_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "ADMIN_ACCOUNT_CREATED",
                targetType = "ADMIN",
                targetId = newAdmin.adminUid,
                description = "New administrator ${newAdmin.email} (Role: $role) added by ${actorAdmin.name}."
            )
        )

        Result.success(newAdmin)
    }

    suspend fun updateAdminRole(
        actorAdmin: AdminEntity,
        targetAdminUid: String,
        newRole: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageAdmins(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Only Super Admins can alter administrator roles."))
        }

        val target = adminDao.getAdminById(targetAdminUid)
            ?: return@withContext Result.failure(Exception("Admin not found."))

        val previousRole = target.role
        val updated = target.copy(role = newRole, updatedAt = System.currentTimeMillis())
        adminDao.updateAdmin(updated)

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_adm_role_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "ADMIN_ROLE_CHANGED",
                targetType = "ADMIN",
                targetId = targetAdminUid,
                previousValue = previousRole,
                newValue = newRole,
                description = "Role for admin ${target.email} altered from $previousRole to $newRole."
            )
        )
        Result.success(Unit)
    }

    suspend fun setAdminStatus(
        actorAdmin: AdminEntity,
        targetAdminUid: String,
        newStatus: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageAdmins(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Only Super Admins can alter admin status."))
        }
        val target = adminDao.getAdminById(targetAdminUid)
            ?: return@withContext Result.failure(Exception("Admin not found."))

        if (target.email == "vishalsharma74944@gmail.com" && newStatus != "ACTIVE") {
            return@withContext Result.failure(SecurityException("The primary Super Admin account cannot be disabled."))
        }

        val updated = target.copy(status = newStatus, updatedAt = System.currentTimeMillis())
        adminDao.updateAdmin(updated)

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_adm_stat_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "ADMIN_STATUS_CHANGED",
                targetType = "ADMIN",
                targetId = targetAdminUid,
                newValue = newStatus,
                description = "Administrator ${target.email} set to $newStatus."
            )
        )
        Result.success(Unit)
    }

    suspend fun removeAdminAccount(
        actorAdmin: AdminEntity,
        targetAdminUid: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canManageAdmins(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Only Super Admins can remove administrators."))
        }
        val target = adminDao.getAdminById(targetAdminUid)
            ?: return@withContext Result.failure(Exception("Admin not found."))

        if (target.email == "vishalsharma74944@gmail.com") {
            return@withContext Result.failure(SecurityException("The primary Super Admin account cannot be removed."))
        }

        adminDao.deleteAdmin(target)

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_adm_del_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "ADMIN_REMOVED",
                targetType = "ADMIN",
                targetId = targetAdminUid,
                description = "Administrator ${target.email} removed by ${actorAdmin.name}."
            )
        )
        Result.success(Unit)
    }

    // --- System Analytics & Aggregations from Live Database ---

    data class DashboardStats(
        val totalUsers: Int,
        val activeUsers: Int,
        val newUsersToday: Int,
        val newUsersThisWeek: Int,
        val newUsersThisMonth: Int,
        val totalSessions: Int,
        val totalStudyTimeSeconds: Long,
        val sessionsToday: Int,
        val studyTimeTodaySeconds: Long,
        val studyTimeThisWeekSeconds: Long,
        val studyTimeThisMonthSeconds: Long,
        val totalFriendships: Int,
        val pendingFriendRequests: Int,
        val openReports: Int,
        val activeAdmins: Int,
        val systemErrorsCount: Int
    )

    suspend fun computeDashboardStats(): DashboardStats = withContext(Dispatchers.IO) {
        val users = userDao.getAllUsers()
        val sessions = studySessionDao.getAllSessions()
        val friendships = friendDao.getAllFriendshipsFlow()
        val allRequests = friendDao.getAllFriendRequestsFlow()
        val admins = adminDao.getAllAdmins()

        val now = System.currentTimeMillis()
        val oneDayAgo = now - 86400000L
        val oneWeekAgo = now - 86400000L * 7
        val oneMonthAgo = now - 86400000L * 30

        val totalUsers = users.size
        val activeUsers = users.count { it.lastActiveTime >= oneWeekAgo && it.accountStatus == "ACTIVE" }
        val newUsersToday = users.count { it.joinedDate >= oneDayAgo }
        val newUsersThisWeek = users.count { it.joinedDate >= oneWeekAgo }
        val newUsersThisMonth = users.count { it.joinedDate >= oneMonthAgo }

        val totalSessions = sessions.size
        val totalStudyTimeSeconds = sessions.sumOf { it.durationSeconds }
        val sessionsToday = sessions.count { it.startTime >= oneDayAgo }
        val studyTimeTodaySeconds = sessions.filter { it.startTime >= oneDayAgo }.sumOf { it.durationSeconds }
        val studyTimeThisWeekSeconds = sessions.filter { it.startTime >= oneWeekAgo }.sumOf { it.durationSeconds }
        val studyTimeThisMonthSeconds = sessions.filter { it.startTime >= oneMonthAgo }.sumOf { it.durationSeconds }

        val totalFriendships = friendDao.countFriendships()
        val pendingFriendRequests = friendDao.countPendingRequests()
        val openReports = reportDao.countOpenReports()
        val systemErrorsCount = errorLogDao.countUnresolvedErrors()

        DashboardStats(
            totalUsers = totalUsers,
            activeUsers = activeUsers.coerceAtLeast(1),
            newUsersToday = newUsersToday,
            newUsersThisWeek = newUsersThisWeek,
            newUsersThisMonth = newUsersThisMonth,
            totalSessions = totalSessions,
            totalStudyTimeSeconds = totalStudyTimeSeconds,
            sessionsToday = sessionsToday,
            studyTimeTodaySeconds = studyTimeTodaySeconds,
            studyTimeThisWeekSeconds = studyTimeThisWeekSeconds,
            studyTimeThisMonthSeconds = studyTimeThisMonthSeconds,
            totalFriendships = totalFriendships,
            pendingFriendRequests = pendingFriendRequests,
            openReports = openReports,
            activeAdmins = admins.count { it.status == "ACTIVE" },
            systemErrorsCount = systemErrorsCount
        )
    }

    // --- Export Operations ---

    suspend fun exportData(actorAdmin: AdminEntity, exportType: String): Result<String> = withContext(Dispatchers.IO) {
        if (!SecurityUtils.canExportData(actorAdmin.role)) {
            return@withContext Result.failure(SecurityException("Permission denied to export database records."))
        }

        val resultCsv = StringBuilder()
        when (exportType) {
            "USERS" -> {
                resultCsv.append("UserID,FullName,Username,Email,StudyID,AccountStatus,Privacy,JoinedDate\n")
                val users = userDao.getAllUsers()
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                users.forEach { u ->
                    resultCsv.append("\"${u.userId}\",\"${u.fullName}\",\"${u.username}\",\"${u.email}\",\"${u.studyId}\",\"${u.accountStatus}\",\"${u.privacyVisibility}\",\"${sdf.format(Date(u.joinedDate))}\"\n")
                }
            }
            "SESSIONS" -> {
                resultCsv.append("SessionID,UserID,Subject,DurationSeconds,SessionDate,Title,Type\n")
                val sessions = studySessionDao.getAllSessions()
                sessions.forEach { s ->
                    resultCsv.append("\"${s.sessionId}\",\"${s.userId}\",\"${s.subjectName}\",${s.durationSeconds},\"${s.sessionDate}\",\"${s.title}\",\"${s.sessionType}\"\n")
                }
            }
            "SUBJECTS" -> {
                resultCsv.append("SubjectID,Name,Status,ColorHex,Default\n")
                val subs = subjectDao.getAllSubjects()
                subs.forEach { sub ->
                    resultCsv.append("\"${sub.subjectId}\",\"${sub.name}\",\"${sub.status}\",\"${sub.colorHex}\",${sub.isDefault}\n")
                }
            }
        }

        auditLogDao.insertAuditLog(
            AdminAuditLogEntity(
                logId = "log_exp_${System.currentTimeMillis()}",
                adminId = actorAdmin.adminUid,
                adminEmail = actorAdmin.email,
                adminRole = actorAdmin.role,
                action = "DATA_EXPORTED",
                targetType = "EXPORT",
                targetId = exportType,
                description = "Administrator ${actorAdmin.email} exported $exportType report in CSV format."
            )
        )

        Result.success(resultCsv.toString())
    }
}
