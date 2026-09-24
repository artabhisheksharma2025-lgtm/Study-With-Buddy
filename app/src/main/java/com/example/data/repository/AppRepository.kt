package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.OnlineSyncService
import com.example.data.remote.OnlineUser
import com.example.data.util.StatsCalculator
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class AppRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val subjectDao = db.subjectDao()
    private val studySessionDao = db.studySessionDao()
    private val activeTimerDao = db.activeTimerDao()
    private val friendDao = db.friendDao()
    private val goalDao = db.studyGoalDao()
    private val notificationDao = db.notificationDao()
    private val dismissedAnnouncementDao = db.dismissedAnnouncementDao()
    private val announcementDao = db.announcementDao()

    val loggedInUserFlow: Flow<UserEntity?> = userDao.getLoggedInUserFlow().map { user ->
        if (user != null && (user.accountStatus == "SUSPENDED" || user.accountStatus == "DISABLED" || user.isDeleted)) {
            null
        } else {
            user
        }
    }

    suspend fun getLoggedInUser(): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getLoggedInUser()
    }

    suspend fun getStudyStatsForUser(userId: String): com.example.data.util.UserStudyStats = withContext(Dispatchers.IO) {
        val sessions = studySessionDao.getSessionsForUser(userId)
        StatsCalculator.calculateStats(sessions)
    }

    /**
     * Synchronizes live data from cloud (announcements, admin updates, etc.)
     * Automatically triggered on app launch or when user visits with internet.
     */
    suspend fun syncFromCloud() = withContext(Dispatchers.IO) {
        try {
            // 1. Fetch live announcements updated by Admin
            val cloudAnnouncements = OnlineSyncService.fetchAllOnlineAnnouncements()
            if (cloudAnnouncements.isNotEmpty()) {
                val currentLocalAnnouncements = announcementDao.getAllAnnouncements()
                val cloudIds = cloudAnnouncements.map { it.announcementId }.toSet()

                for (ann in cloudAnnouncements) {
                    announcementDao.insertAnnouncement(ann)
                }

                // Remove local user announcements that were deleted from cloud by admin
                for (local in currentLocalAnnouncements) {
                    if (local.displayLocation == "USER_APP" && !cloudIds.contains(local.announcementId)) {
                        announcementDao.deleteAnnouncement(local)
                    }
                }
            }

            // 2. Keep active user profile synced
            val currentUser = userDao.getLoggedInUser()
            if (currentUser != null && !currentUser.isDeleted) {
                OnlineSyncService.syncUserOnline(currentUser, getStudyStatsForUser(currentUser.userId))
                OnlineSyncService.syncUserAuthOnline(currentUser)
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error during cloud sync", e)
        }
    }

    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        // Clean up legacy dummy demo users so that ONLY real users who visit/login appear
        val dummyIds = listOf("user_rahul", "user_priya", "user_ankit", "user_abhishek")
        for (id in dummyIds) {
            userDao.deleteUserById(id)
            studySessionDao.deleteSessionsForUser(id)
        }
        // Immediately sync announcements & updates from cloud
        syncFromCloud()
    }


    suspend fun seedDefaultSubjectsForUser(userId: String) {
        val defaultSubjects = listOf(
            SubjectEntity("sub_math_$userId", userId, "Mathematics", "#3F51B5"),
            SubjectEntity("sub_phy_$userId", userId, "Physics", "#009688"),
            SubjectEntity("sub_chem_$userId", userId, "Chemistry", "#E91E63"),
            SubjectEntity("sub_bio_$userId", userId, "Biology", "#4CAF50"),
            SubjectEntity("sub_eng_$userId", userId, "English", "#FF9800"),
            SubjectEntity("sub_hist_$userId", userId, "History", "#795548"),
            SubjectEntity("sub_geo_$userId", userId, "Geography", "#00BCD4"),
            SubjectEntity("sub_cs_$userId", userId, "Computer Science", "#673AB7"),
            SubjectEntity("sub_other_$userId", userId, "Other", "#607D8B")
        )
        subjectDao.insertSubjects(defaultSubjects)
    }

    // --- Authentication Operations ---
    suspend fun registerUser(
        fullName: String,
        username: String,
        email: String,
        passwordHash: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val existingEmail = userDao.getUserByEmail(email)
        if (existingEmail != null) {
            return@withContext Result.failure(Exception("Email address already registered."))
        }

        val studyId = generateUniqueStudyId()
        val userId = "user_" + UUID.randomUUID().toString().take(8)

        // Logout existing users first
        userDao.logoutAllUsers()

        val newUser = UserEntity(
            userId = userId,
            fullName = fullName.trim(),
            username = username.trim(),
            email = email.trim(),
            passwordHash = passwordHash,
            studyId = studyId,
            isLoggedIn = true,
            joinedDate = System.currentTimeMillis()
        )

        userDao.insertUser(newUser)
        seedDefaultSubjectsForUser(userId)

        // Sync new user credentials and profile to cloud for Admin management
        OnlineSyncService.syncUserAuthOnline(newUser)
        OnlineSyncService.syncUserOnline(newUser, getStudyStatsForUser(userId))

        // Create welcome notification
        notificationDao.insertNotification(
            AppNotificationEntity(
                notificationId = "notif_" + UUID.randomUUID().toString(),
                userId = userId,
                title = "Welcome to Study With Buddy! 🎉",
                message = "Your unique Study ID is $studyId. Share it with friends to connect!"
            )
        )

        Result.success(newUser)
    }

    suspend fun loginUser(email: String, passwordHash: String): Result<UserEntity> =
        withContext(Dispatchers.IO) {
            var user = userDao.getUserByEmail(email.trim())
            if (user == null) {
                // If user registered on another device/session, check global cloud auth directory
                val onlineUsers = OnlineSyncService.fetchAllOnlineAuthUsers()
                val match = onlineUsers.find { it.email.equals(email.trim(), ignoreCase = true) }
                if (match != null) {
                    userDao.insertUser(match)
                    seedDefaultSubjectsForUser(match.userId)
                    user = match
                }
            }

            if (user == null) {
                return@withContext Result.failure(Exception("Account not found with this email."))
            }

            if (user.isDeleted) {
                return@withContext Result.failure(Exception("This account has been deleted."))
            }

            if (user.accountStatus == "SUSPENDED") {
                val reason = if (user.suspensionReason.isNotBlank()) " Reason: ${user.suspensionReason}" else ""
                return@withContext Result.failure(Exception("This account has been suspended by an administrator.$reason"))
            }

            if (user.accountStatus == "DISABLED") {
                return@withContext Result.failure(Exception("This account has been disabled by an administrator."))
            }

            if (user.passwordHash != passwordHash) {
                return@withContext Result.failure(Exception("Incorrect password. Please try again."))
            }

            userDao.logoutAllUsers()
            val updatedUser = user.copy(isLoggedIn = true)
            userDao.updateUser(updatedUser)

            // Keep cloud presence and auth active
            OnlineSyncService.syncUserAuthOnline(updatedUser)
            OnlineSyncService.syncUserOnline(updatedUser, getStudyStatsForUser(updatedUser.userId))

            Result.success(updatedUser)
        }

    suspend fun logoutUser() = withContext(Dispatchers.IO) {
        userDao.logoutAllUsers()
    }

    suspend fun updateUserProfile(user: UserEntity) = withContext(Dispatchers.IO) {
        userDao.updateUser(user)
    }

    // --- Subjects ---
    fun getSubjectsForUser(userId: String): Flow<List<SubjectEntity>> =
        subjectDao.getSubjectsForUserFlow(userId)

    suspend fun createCustomSubject(userId: String, name: String, colorHex: String = "#3F51B5"): SubjectEntity =
        withContext(Dispatchers.IO) {
            val subjectId = "sub_" + UUID.randomUUID().toString().take(8)
            val subject = SubjectEntity(
                subjectId = subjectId,
                userId = userId,
                name = name.trim(),
                colorHex = colorHex
            )
            subjectDao.insertSubject(subject)
            subject
        }

    suspend fun updateSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        subjectDao.insertSubject(subject)
    }

    suspend fun deleteSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        subjectDao.deleteSubject(subject)
    }

    // --- Study Sessions ---
    fun getStudySessionsForUser(userId: String): Flow<List<StudySessionEntity>> =
        studySessionDao.getSessionsForUserFlow(userId)

    suspend fun saveStudySession(
        userId: String,
        subjectId: String,
        subjectName: String,
        startTime: Long,
        endTime: Long,
        durationSeconds: Long,
        title: String,
        notes: String
    ): StudySessionEntity = withContext(Dispatchers.IO) {
        val sessionId = "sess_" + UUID.randomUUID().toString().take(8)
        val sessionDate = getFormattedDateString(endTime)

        val session = StudySessionEntity(
            sessionId = sessionId,
            userId = userId,
            subjectId = subjectId,
            subjectName = subjectName,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = durationSeconds,
            sessionDate = sessionDate,
            title = title,
            notes = notes
        )

        studySessionDao.insertSession(session)

        // Clear active timer for user
        activeTimerDao.clearActiveTimer(userId)

        // Check if session triggered goal/streak notification
        notificationDao.insertNotification(
            AppNotificationEntity(
                notificationId = "notif_" + UUID.randomUUID().toString(),
                userId = userId,
                title = "Study Session Completed 🎉",
                message = "Logged ${formatDurationShort(durationSeconds)} in $subjectName."
            )
        )

        // Sync updated hours and streak to online community in background
        try {
            val user = userDao.getUserById(userId)
            if (user != null) {
                val allSessions = studySessionDao.getSessionsForUser(userId)
                val stats = StatsCalculator.calculateStats(allSessions)
                OnlineSyncService.syncUserOnline(user, stats)
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed background online sync on session complete", e)
        }

        session
    }

    suspend fun updateStudySession(session: StudySessionEntity) = withContext(Dispatchers.IO) {
        studySessionDao.updateSession(session)
    }

    suspend fun deleteStudySession(sessionId: String, userId: String) = withContext(Dispatchers.IO) {
        studySessionDao.deleteSessionById(sessionId, userId)
    }

    // --- Active Timer ---
    fun getActiveTimer(userId: String): Flow<ActiveTimerEntity?> =
        activeTimerDao.getActiveTimerFlow(userId)

    suspend fun saveActiveTimer(timer: ActiveTimerEntity) = withContext(Dispatchers.IO) {
        activeTimerDao.saveActiveTimer(timer)
    }

    suspend fun clearActiveTimer(userId: String) = withContext(Dispatchers.IO) {
        activeTimerDao.clearActiveTimer(userId)
    }

    // --- Friends & Social ---
    suspend fun searchUserByStudyId(studyId: String): UserEntity? = withContext(Dispatchers.IO) {
        var clean = studyId.trim().uppercase().replace(" ", "-").removePrefix("#").removePrefix("@")
        // Try local lookup
        var local = userDao.getUserByStudyId(clean)
        if (local == null && !clean.startsWith("STU-")) {
            clean = "STU-$clean"
            local = userDao.getUserByStudyId(clean)
        }
        if (local != null) return@withContext local

        // Not found locally? Search live Cloud Database (Online Sync)
        try {
            val onlineUser = OnlineSyncService.searchUserOnline(clean)
            if (onlineUser != null) {
                val existing = userDao.getUserById(onlineUser.userId) ?: userDao.getUserByStudyId(onlineUser.studyId)
                val targetUser = existing ?: UserEntity(
                    userId = onlineUser.userId,
                    fullName = onlineUser.fullName,
                    username = onlineUser.username,
                    email = "${onlineUser.username}@student.community",
                    passwordHash = "cloud_user",
                    studyId = onlineUser.studyId,
                    privacyVisibility = onlineUser.privacyVisibility,
                    joinedDate = System.currentTimeMillis() - 86400000L * 7,
                    isLoggedIn = false
                )
                userDao.insertUser(targetUser)

                // If they have study stats online, seed sample session locally so streak and hours appear
                if (onlineUser.totalStudySeconds > 0) {
                    val sessions = studySessionDao.getSessionsForUser(targetUser.userId)
                    if (sessions.isEmpty()) {
                        studySessionDao.insertSession(
                            StudySessionEntity(
                                sessionId = "sess_online_${targetUser.userId}",
                                userId = targetUser.userId,
                                subjectId = "sub_online_${targetUser.userId}",
                                subjectName = "General Study",
                                startTime = System.currentTimeMillis() - onlineUser.totalStudySeconds * 1000L,
                                endTime = System.currentTimeMillis(),
                                durationSeconds = onlineUser.totalStudySeconds,
                                sessionDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                title = "Online Study Session"
                            )
                        )
                    }
                }
                return@withContext targetUser
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Error during online search", e)
        }

        null
    }

    fun getPendingReceivedRequests(userId: String): Flow<List<FriendRequestEntity>> =
        friendDao.getPendingReceivedRequestsFlow(userId)

    suspend fun sendFriendRequest(senderId: String, targetStudyId: String): Result<String> =
        withContext(Dispatchers.IO) {
            val cleanTargetId = targetStudyId.trim().uppercase().replace(" ", "-").removePrefix("#").removePrefix("@")
            var targetUser = userDao.getUserByStudyId(cleanTargetId)
            if (targetUser == null && !cleanTargetId.startsWith("STU-")) {
                targetUser = userDao.getUserByStudyId("STU-$cleanTargetId")
            }
            if (targetUser == null) {
                // Try searching online if not yet cached locally
                targetUser = searchUserByStudyId(cleanTargetId)
            }
            if (targetUser == null) {
                return@withContext Result.failure(Exception("Study ID '$cleanTargetId' not found locally or online."))
            }

            if (targetUser.userId == senderId) {
                return@withContext Result.failure(Exception("You cannot send a friend request to yourself."))
            }

            val existingFriendship = friendDao.getFriendshipBetween(senderId, targetUser.userId)
            if (existingFriendship != null) {
                return@withContext Result.failure(Exception("You are already friends with ${targetUser.fullName}."))
            }

            val existingRequest = friendDao.getFriendRequestBetween(senderId, targetUser.userId)
            if (existingRequest != null && existingRequest.status == "PENDING") {
                return@withContext Result.failure(Exception("A friend request is already pending between you and ${targetUser.fullName}."))
            }

            val reqId = "req_" + UUID.randomUUID().toString().take(8)
            val request = FriendRequestEntity(
                requestId = reqId,
                senderId = senderId,
                receiverId = targetUser.userId,
                status = "PENDING"
            )
            friendDao.insertFriendRequest(request)

            val sender = userDao.getUserById(senderId)
            val senderName = sender?.fullName ?: "A student"

            notificationDao.insertNotification(
                AppNotificationEntity(
                    notificationId = "notif_" + UUID.randomUUID().toString(),
                    userId = targetUser.userId,
                    title = "New Friend Request 👥",
                    message = "$senderName sent you a friend request!"
                )
            )

            // Propagate request online so other device receives it
            if (sender != null) {
                try {
                    OnlineSyncService.sendOnlineFriendRequest(sender, targetUser.studyId)
                } catch (e: Exception) {
                    Log.e("AppRepository", "Failed to propagate online friend request", e)
                }
            }

            Result.success("Friend request sent to ${targetUser.fullName} (Online)!")
        }

    suspend fun acceptFriendRequest(request: FriendRequestEntity) = withContext(Dispatchers.IO) {
        val updatedReq = request.copy(status = "ACCEPTED", updatedAt = System.currentTimeMillis())
        friendDao.updateFriendRequest(updatedReq)

        val friendshipId = "f_" + UUID.randomUUID().toString().take(8)
        friendDao.insertFriendship(
            FriendshipEntity(
                friendshipId = friendshipId,
                userId1 = request.senderId,
                userId2 = request.receiverId
            )
        )

        val receiver = userDao.getUserById(request.receiverId)
        val receiverName = receiver?.fullName ?: "Your friend"
        val sender = userDao.getUserById(request.senderId)

        notificationDao.insertNotification(
            AppNotificationEntity(
                notificationId = "notif_" + UUID.randomUUID().toString(),
                userId = request.senderId,
                title = "Friend Request Accepted 🎉",
                message = "$receiverName accepted your friend request!"
            )
        )

        // Update online status
        if (receiver != null && sender != null) {
            try {
                OnlineSyncService.updateOnlineRequestStatus(receiver.studyId, sender.studyId, "ACCEPTED")
            } catch (e: Exception) {
                Log.e("AppRepository", "Error updating online request status", e)
            }
        }
    }

    suspend fun rejectFriendRequest(request: FriendRequestEntity) = withContext(Dispatchers.IO) {
        val updatedReq = request.copy(status = "REJECTED", updatedAt = System.currentTimeMillis())
        friendDao.updateFriendRequest(updatedReq)

        val receiver = userDao.getUserById(request.receiverId)
        val sender = userDao.getUserById(request.senderId)
        if (receiver != null && sender != null) {
            try {
                OnlineSyncService.updateOnlineRequestStatus(receiver.studyId, sender.studyId, "REJECTED")
            } catch (e: Exception) {
                Log.e("AppRepository", "Error updating online request rejection", e)
            }
        }
    }

    suspend fun syncOnlineRequestsForCurrentUser(): Int = withContext(Dispatchers.IO) {
        val user = userDao.getLoggedInUser() ?: return@withContext 0
        try {
            // First sync current user online so others can find this user
            syncCurrentUserOnline()

            val onlineRequests = OnlineSyncService.fetchIncomingOnlineRequests(user.studyId)
            var newCount = 0
            for (req in onlineRequests) {
                var sender = userDao.getUserByStudyId(req.senderStudyId)
                if (sender == null) {
                    sender = searchUserByStudyId(req.senderStudyId)
                }
                if (sender != null && sender.userId != user.userId) {
                    val existing = friendDao.getFriendRequestBetween(sender.userId, user.userId)
                    val existingFriendship = friendDao.getFriendshipBetween(sender.userId, user.userId)
                    if (existing == null && existingFriendship == null) {
                        friendDao.insertFriendRequest(
                            FriendRequestEntity(
                                requestId = req.requestId,
                                senderId = sender.userId,
                                receiverId = user.userId,
                                status = "PENDING",
                                createdAt = req.timestamp
                            )
                        )
                        notificationDao.insertNotification(
                            AppNotificationEntity(
                                notificationId = "notif_online_" + req.requestId,
                                userId = user.userId,
                                title = "New Online Friend Request 🌐",
                                message = "${sender.fullName} (@${sender.username}) sent you a study friend request!"
                            )
                        )
                        newCount++
                    }
                }
            }
            newCount
        } catch (e: Exception) {
            Log.e("AppRepository", "Error syncing online requests", e)
            0
        }
    }

    suspend fun syncCurrentUserOnline(): Boolean = withContext(Dispatchers.IO) {
        val user = userDao.getLoggedInUser() ?: return@withContext false
        try {
            val sessions = studySessionDao.getSessionsForUser(user.userId)
            val stats = StatsCalculator.calculateStats(sessions)
            OnlineSyncService.syncUserOnline(user, stats)
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to sync user profile online", e)
            false
        }
    }

    suspend fun fetchOnlineCommunityUsers(): List<OnlineUser> = withContext(Dispatchers.IO) {
        try {
            OnlineSyncService.fetchAllOnlineUsers()
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to fetch online community users", e)
            emptyList()
        }
    }

    fun getAcceptedFriendsFlow(userId: String): Flow<List<UserEntity>> {
        return friendDao.getFriendshipsFlow(userId).map { friendships ->
            val friendIds = friendships.map {
                if (it.userId1 == userId) it.userId2 else it.userId1
            }
            if (friendIds.isEmpty()) emptyList()
            else userDao.getUsersByIds(friendIds)
        }
    }

    suspend fun removeFriend(userId: String, friendId: String) = withContext(Dispatchers.IO) {
        friendDao.removeFriendship(userId, friendId)
    }

    suspend fun getUserById(userId: String): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getUserById(userId)
    }

    // --- Study Goals ---
    fun getGoalsForUser(userId: String): Flow<List<StudyGoalEntity>> =
        goalDao.getGoalsForUserFlow(userId)

    suspend fun createGoal(
        userId: String,
        title: String,
        targetDurationMinutes: Int,
        targetSessions: Int,
        startDate: Long,
        endDate: Long,
        subjectName: String
    ) = withContext(Dispatchers.IO) {
        val goalId = "goal_" + UUID.randomUUID().toString().take(8)
        val goal = StudyGoalEntity(
            goalId = goalId,
            userId = userId,
            title = title.trim(),
            targetDurationMinutes = targetDurationMinutes,
            targetSessions = targetSessions,
            startDate = startDate,
            endDate = endDate,
            subjectName = subjectName.trim()
        )
        goalDao.insertGoal(goal)
    }

    suspend fun deleteGoal(goal: StudyGoalEntity) = withContext(Dispatchers.IO) {
        goalDao.deleteGoal(goal)
    }

    suspend fun setWeeklyGoal(
        userId: String,
        targetHours: Float,
        title: String = "Weekly Study Target"
    ): StudyGoalEntity = withContext(Dispatchers.IO) {
        val targetMinutes = (targetHours * 60).toInt()
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfWeek = cal.timeInMillis

        cal.add(Calendar.DAY_OF_WEEK, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endOfWeek = cal.timeInMillis

        val goalId = "weekly_goal_${userId}"
        val goal = StudyGoalEntity(
            goalId = goalId,
            userId = userId,
            title = title.ifBlank { "Weekly Study Target" },
            targetDurationMinutes = targetMinutes,
            targetSessions = (targetHours / 2f).toInt().coerceAtLeast(3),
            startDate = startOfWeek,
            endDate = endOfWeek,
            subjectName = "",
            createdAt = now
        )
        goalDao.insertGoal(goal)
        goal
    }

    // --- Notifications ---
    fun getNotificationsForUser(userId: String): Flow<List<AppNotificationEntity>> =
        notificationDao.getNotificationsForUserFlow(userId)

    suspend fun markAllNotificationsRead(userId: String) = withContext(Dispatchers.IO) {
        notificationDao.markAllAsRead(userId)
    }

    // --- Helpers & Utilities ---
    private fun generateUniqueStudyId(): String {
        val chars = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ"
        val code = (1..6).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "STU-$code"
    }

    companion object {
        fun getFormattedDateString(timeMillis: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            return sdf.format(Date(timeMillis))
        }

        fun getTodayDateString(): String {
            return getFormattedDateString(System.currentTimeMillis())
        }

        fun formatDurationShort(durationSeconds: Long): String {
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                else -> "${durationSeconds}s"
            }
        }

        fun formatTimerDisplay(seconds: Long): String {
            val h = seconds / 3600
            val m = (seconds % 3600) / 60
            val s = seconds % 60
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        }
    }

    fun getActiveAnnouncementsFlow(userId: String? = null): Flow<List<AnnouncementEntity>> {
        val publishedFlow = db.announcementDao().getPublishedAnnouncementsFlow("USER_APP")
        val dismissedFlow = if (!userId.isNullOrBlank()) {
            dismissedAnnouncementDao.getDismissedAnnouncementIdsFlow(userId)
        } else {
            dismissedAnnouncementDao.getAllDismissedAnnouncementIdsFlow()
        }
        return combine(publishedFlow, dismissedFlow) { announcements, dismissedIds ->
            val dismissedSet = dismissedIds.toSet()
            announcements.filter { it.announcementId !in dismissedSet }
        }
    }

    suspend fun dismissAnnouncementForUser(userId: String, announcementId: String) = withContext(Dispatchers.IO) {
        dismissedAnnouncementDao.insertDismissedAnnouncement(
            DismissedAnnouncementEntity(
                userId = userId,
                announcementId = announcementId,
                dismissedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getDismissedAnnouncementIds(userId: String): List<String> = withContext(Dispatchers.IO) {
        dismissedAnnouncementDao.getDismissedAnnouncementIds(userId)
    }
}
