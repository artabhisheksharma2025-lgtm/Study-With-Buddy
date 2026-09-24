package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val userId: String,
    val fullName: String,
    val username: String,
    val email: String,
    val passwordHash: String,
    val studyId: String,
    val profilePhoto: String = "",
    val privacyVisibility: String = "FRIENDS_ONLY", // "PRIVATE", "FRIENDS_ONLY", "PUBLIC"
    val joinedDate: Long = System.currentTimeMillis(),
    val isLoggedIn: Boolean = false,
    val notificationGoalReminders: Boolean = true,
    val notificationStreakReminders: Boolean = true,
    val notificationFriendRequests: Boolean = true,
    val notificationFriendActivity: Boolean = true,
    val accountStatus: String = "ACTIVE", // ACTIVE, SUSPENDED, DISABLED
    val lastActiveTime: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val suspensionReason: String = ""
)

@Entity(tableName = "subjects")
data class SubjectEntity(
    @PrimaryKey val subjectId: String,
    val userId: String,
    val name: String,
    val colorHex: String = "#3F51B5",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE", // ACTIVE, INACTIVE
    val description: String = "",
    val icon: String = "menu_book",
    val isDefault: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey val sessionId: String,
    val userId: String,
    val subjectId: String,
    val subjectName: String,
    val startTime: Long,
    val endTime: Long,
    val durationSeconds: Long,
    val sessionDate: String, // format YYYY-MM-DD
    val title: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val sessionType: String = "TIMER", // TIMER, MANUAL
    val isDeleted: Boolean = false,
    val pausedDurationSeconds: Long = 0L,
    val timezone: String = "UTC",
    val updatedTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "active_timer")
data class ActiveTimerEntity(
    @PrimaryKey val userId: String,
    val subjectId: String,
    val subjectName: String,
    val title: String = "",
    val elapsedSeconds: Long = 0L,
    val isRunning: Boolean = false,
    val startTimeMillis: Long = 0L,
    val lastUpdatedMillis: Long = System.currentTimeMillis()
)

@Entity(tableName = "friend_requests")
data class FriendRequestEntity(
    @PrimaryKey val requestId: String,
    val senderId: String,
    val receiverId: String,
    val status: String, // "PENDING", "ACCEPTED", "REJECTED"
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "friendships")
data class FriendshipEntity(
    @PrimaryKey val friendshipId: String,
    val userId1: String,
    val userId2: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "study_goals")
data class StudyGoalEntity(
    @PrimaryKey val goalId: String,
    val userId: String,
    val title: String,
    val targetDurationMinutes: Int = 0,
    val targetSessions: Int = 0,
    val startDate: Long,
    val endDate: Long,
    val subjectName: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_notifications")
data class AppNotificationEntity(
    @PrimaryKey val notificationId: String,
    val userId: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)
