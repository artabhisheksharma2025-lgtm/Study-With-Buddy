package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
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

    val loggedInUserFlow: Flow<UserEntity?> = userDao.getLoggedInUserFlow()

    suspend fun getLoggedInUser(): UserEntity? = withContext(Dispatchers.IO) {
        userDao.getLoggedInUser()
    }

    suspend fun seedInitialDataIfNeeded() = withContext(Dispatchers.IO) {
        val users = userDao.getAllUsers()
        if (users.isEmpty()) {
            // Seed Default Demo Users so Friends, QR Code, Search, Requests & Comparisons work out of the box!
            val rahul = UserEntity(
                userId = "user_rahul",
                fullName = "Rahul Sharma",
                username = "rahuls",
                email = "rahul@example.com",
                passwordHash = "password123",
                studyId = "STU-7K92P4",
                privacyVisibility = "FRIENDS_ONLY",
                joinedDate = System.currentTimeMillis() - 86400000L * 30
            )

            val priya = UserEntity(
                userId = "user_priya",
                fullName = "Priya Patel",
                username = "priyap",
                email = "priya@example.com",
                passwordHash = "password123",
                studyId = "STU-PRY881",
                privacyVisibility = "PUBLIC",
                joinedDate = System.currentTimeMillis() - 86400000L * 45
            )

            val ankit = UserEntity(
                userId = "user_ankit",
                fullName = "Ankit Verma",
                username = "ankitv",
                email = "ankit@example.com",
                passwordHash = "password123",
                studyId = "STU-ANK552",
                privacyVisibility = "FRIENDS_ONLY",
                joinedDate = System.currentTimeMillis() - 86400000L * 20
            )

            // Default Logged In User for quick initial launch (Abhishek)
            val defaultUser = UserEntity(
                userId = "user_abhishek",
                fullName = "Abhishek Sharma",
                username = "abhisheks",
                email = "abhishek@example.com",
                passwordHash = "password123",
                studyId = "STU-ABH45821",
                privacyVisibility = "FRIENDS_ONLY",
                joinedDate = System.currentTimeMillis() - 86400000L * 15,
                isLoggedIn = true
            )

            userDao.insertUser(rahul)
            userDao.insertUser(priya)
            userDao.insertUser(ankit)
            userDao.insertUser(defaultUser)

            // Seed subjects for default users
            seedDefaultSubjectsForUser(defaultUser.userId)
            seedDefaultSubjectsForUser(rahul.userId)
            seedDefaultSubjectsForUser(priya.userId)
            seedDefaultSubjectsForUser(ankit.userId)

            // Seed Friendships & Requests
            friendDao.insertFriendship(
                FriendshipEntity(
                    friendshipId = "f_1",
                    userId1 = defaultUser.userId,
                    userId2 = rahul.userId,
                    createdAt = System.currentTimeMillis() - 86400000L * 10
                )
            )

            // Pending request from Priya to Abhishek
            friendDao.insertFriendRequest(
                FriendRequestEntity(
                    requestId = "req_1",
                    senderId = priya.userId,
                    receiverId = defaultUser.userId,
                    status = "PENDING",
                    createdAt = System.currentTimeMillis() - 3600000L * 2
                )
            )

            // Seed sample sessions for default user (Abhishek) & Rahul & Priya
            val todayDate = getTodayDateString()
            val yesterdayDate = getFormattedDateString(System.currentTimeMillis() - 86400000L)
            val day2Before = getFormattedDateString(System.currentTimeMillis() - 86400000L * 2)

            // Abhishek's sessions
            studySessionDao.insertSession(
                StudySessionEntity(
                    sessionId = "sess_abh_1",
                    userId = defaultUser.userId,
                    subjectId = "sub_math_${defaultUser.userId}",
                    subjectName = "Mathematics",
                    startTime = System.currentTimeMillis() - 7200000L,
                    endTime = System.currentTimeMillis() - 3600000L,
                    durationSeconds = 5100L, // 1h 25m
                    sessionDate = todayDate,
                    title = "Chapter 5 Revision",
                    notes = "Completed integration exercises."
                )
            )
            studySessionDao.insertSession(
                StudySessionEntity(
                    sessionId = "sess_abh_2",
                    userId = defaultUser.userId,
                    subjectId = "sub_phy_${defaultUser.userId}",
                    subjectName = "Physics",
                    startTime = System.currentTimeMillis() - 14400000L,
                    endTime = System.currentTimeMillis() - 10800000L,
                    durationSeconds = 7200L, // 2h 00m
                    sessionDate = todayDate,
                    title = "Thermodynamics Problems",
                    notes = "Solved 15 numericals."
                )
            )
            studySessionDao.insertSession(
                StudySessionEntity(
                    sessionId = "sess_abh_3",
                    userId = defaultUser.userId,
                    subjectId = "sub_cs_${defaultUser.userId}",
                    subjectName = "Computer Science",
                    startTime = System.currentTimeMillis() - 86400000L - 7200000L,
                    endTime = System.currentTimeMillis() - 86400000L - 3600000L,
                    durationSeconds = 7200L, // 2h
                    sessionDate = yesterdayDate,
                    title = "Kotlin Jetpack Compose",
                    notes = "Built UI components."
                )
            )
            studySessionDao.insertSession(
                StudySessionEntity(
                    sessionId = "sess_abh_4",
                    userId = defaultUser.userId,
                    subjectId = "sub_eng_${defaultUser.userId}",
                    subjectName = "English",
                    startTime = System.currentTimeMillis() - 86400000L * 2 - 7200000L,
                    endTime = System.currentTimeMillis() - 86400000L * 2 - 3600000L,
                    durationSeconds = 5400L, // 1h 30m
                    sessionDate = day2Before,
                    title = "Essay Writing",
                    notes = "Practiced descriptive writing."
                )
            )

            // Rahul's sessions
            studySessionDao.insertSession(
                StudySessionEntity(
                    sessionId = "sess_rah_1",
                    userId = rahul.userId,
                    subjectId = "sub_math_${rahul.userId}",
                    subjectName = "Mathematics",
                    startTime = System.currentTimeMillis() - 3600000L * 3,
                    endTime = System.currentTimeMillis() - 3600000L,
                    durationSeconds = 8100L, // 2h 15m
                    sessionDate = todayDate,
                    title = "Calculus Practise"
                )
            )

            // Goals for Abhishek
            goalDao.insertGoal(
                StudyGoalEntity(
                    goalId = "goal_1",
                    userId = defaultUser.userId,
                    title = "Study 3 hours every day",
                    targetDurationMinutes = 180,
                    startDate = System.currentTimeMillis() - 86400000L,
                    endDate = System.currentTimeMillis() + 86400000L * 7
                )
            )
            goalDao.insertGoal(
                StudyGoalEntity(
                    goalId = "goal_2",
                    userId = defaultUser.userId,
                    title = "Study Mathematics for 10 hours this week",
                    targetDurationMinutes = 600,
                    subjectName = "Mathematics",
                    startDate = System.currentTimeMillis() - 86400000L * 3,
                    endDate = System.currentTimeMillis() + 86400000L * 4
                )
            )

            // Notifications
            notificationDao.insertNotification(
                AppNotificationEntity(
                    notificationId = "notif_1",
                    userId = defaultUser.userId,
                    title = "New Friend Request",
                    message = "Priya Patel sent you a friend request!"
                )
            )
            notificationDao.insertNotification(
                AppNotificationEntity(
                    notificationId = "notif_2",
                    userId = defaultUser.userId,
                    title = "Study Streak 🔥",
                    message = "Awesome! You are on a 3-Day Study Streak. Keep going!"
                )
            )
        }
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

        // Create welcome notification
        notificationDao.insertNotification(
            AppNotificationEntity(
                notificationId = "notif_" + UUID.randomUUID().toString(),
                userId = userId,
                title = "Welcome to Study Tracker! 🎉",
                message = "Your unique Study ID is $studyId. Share it with friends to connect!"
            )
        )

        Result.success(newUser)
    }

    suspend fun loginUser(email: String, passwordHash: String): Result<UserEntity> =
        withContext(Dispatchers.IO) {
            val user = userDao.getUserByEmail(email.trim())
                ?: return@withContext Result.failure(Exception("Account not found with this email."))

            if (user.passwordHash != passwordHash) {
                return@withContext Result.failure(Exception("Incorrect password. Please try again."))
            }

            userDao.logoutAllUsers()
            val updatedUser = user.copy(isLoggedIn = true)
            userDao.updateUser(updatedUser)

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
        userDao.getUserByStudyId(studyId.trim().uppercase())
    }

    fun getPendingReceivedRequests(userId: String): Flow<List<FriendRequestEntity>> =
        friendDao.getPendingReceivedRequestsFlow(userId)

    suspend fun sendFriendRequest(senderId: String, targetStudyId: String): Result<String> =
        withContext(Dispatchers.IO) {
            val targetUser = userDao.getUserByStudyId(targetStudyId.trim().uppercase())
                ?: return@withContext Result.failure(Exception("Study ID not found."))

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

            Result.success("Friend request sent to ${targetUser.fullName}!")
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

        notificationDao.insertNotification(
            AppNotificationEntity(
                notificationId = "notif_" + UUID.randomUUID().toString(),
                userId = request.senderId,
                title = "Friend Request Accepted 🎉",
                message = "$receiverName accepted your friend request!"
            )
        )
    }

    suspend fun rejectFriendRequest(request: FriendRequestEntity) = withContext(Dispatchers.IO) {
        val updatedReq = request.copy(status = "REJECTED", updatedAt = System.currentTimeMillis())
        friendDao.updateFriendRequest(updatedReq)
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
}
