package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "admins")
data class AdminEntity(
    @PrimaryKey val adminUid: String,
    val email: String,
    val name: String,
    val passwordHash: String,
    val passwordSalt: String,
    val role: String = "ADMIN", // SUPER_ADMIN, ADMIN, MODERATOR, SUPPORT_ADMIN
    val status: String = "ACTIVE", // ACTIVE, DISABLED, SUSPENDED
    val mustChangePassword: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long = 0L,
    val permissions: String = "ALL"
)

@Entity(tableName = "admin_audit_logs")
data class AdminAuditLogEntity(
    @PrimaryKey val logId: String,
    val adminId: String,
    val adminEmail: String,
    val adminRole: String,
    val action: String, // USER_DISABLED, SESSION_MODIFIED, etc.
    val targetType: String, // USER, SESSION, SUBJECT, REPORT, ANNOUNCEMENT, SYSTEM
    val targetId: String,
    val description: String,
    val previousValue: String? = null,
    val newValue: String? = null,
    val reason: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val ipReference: String = "127.0.0.1 (Secure Device Applet)"
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey val announcementId: String,
    val title: String,
    val message: String,
    val imageUrl: String? = null,
    val iconName: String? = "campaign",
    val priority: String = "NORMAL", // LOW, NORMAL, HIGH, URGENT
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + 86400000L * 7,
    val status: String = "PUBLISHED", // DRAFT, PUBLISHED, SCHEDULED, EXPIRED
    val targetAudience: String = "EVERYONE", // EVERYONE, NEW_USERS, ACTIVE_USERS, SELECTED
    val displayLocation: String = "BOTH", // ADMIN_DASHBOARD, USER_APP, BOTH
    val isDismissible: Boolean = true,
    val actionLabel: String? = null,
    val actionUrl: String? = null,
    val createdDate: Long = System.currentTimeMillis(),
    val createdBy: String = "Super Admin"
)

@Entity(tableName = "user_reports")
data class ReportEntity(
    @PrimaryKey val reportId: String,
    val reporterUserId: String,
    val reporterName: String = "Student",
    val reportedUserId: String,
    val reportedUserName: String = "User",
    val category: String = "INAPPROPRIATE_BEHAVIOR", // INAPPROPRIATE_BEHAVIOR, SPAM, FRIENDSHIP_ABUSE, PROFILE, OTHER
    val description: String,
    val evidenceReference: String? = null,
    val status: String = "OPEN", // OPEN, UNDER_REVIEW, RESOLVED, DISMISSED
    val internalAdminNote: String? = null,
    val createdDate: Long = System.currentTimeMillis(),
    val resolvedByAdminId: String? = null,
    val resolvedDate: Long? = null
)

@Entity(tableName = "study_challenges")
data class StudyChallengeEntity(
    @PrimaryKey val challengeId: String,
    val title: String,
    val description: String,
    val startDate: Long = System.currentTimeMillis(),
    val endDate: Long = System.currentTimeMillis() + 86400000L * 7,
    val targetHours: Float = 10.0f,
    val targetSessions: Int = 5,
    val subjectRequirement: String? = null,
    val participantEligibility: String = "ALL", // ALL, ACTIVE_USERS
    val status: String = "ACTIVE", // DRAFT, ACTIVE, COMPLETED, CANCELLED
    val createdDate: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_error_logs")
data class AppErrorLogEntity(
    @PrimaryKey val errorId: String,
    val errorType: String,
    val message: String,
    val userIdAffected: String? = null,
    val screenModule: String,
    val timestamp: Long = System.currentTimeMillis(),
    val severity: String = "MEDIUM", // LOW, MEDIUM, HIGH, CRITICAL
    val status: String = "NEW" // NEW, INVESTIGATING, RESOLVED
)

@Entity(tableName = "app_settings")
data class AppSettingEntity(
    @PrimaryKey val settingKey: String,
    val settingValue: String,
    val category: String = "GENERAL", // GENERAL, STUDY, NOTIFICATION, PRIVACY, MAINTENANCE
    val description: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val updatedBy: String = "System"
)
