package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.remote.CloudDataSyncService
import com.example.data.remote.OnlineSyncService
import com.example.data.remote.OnlineUser
import com.example.data.util.SecurityUtils
import com.example.data.util.StatsCalculator
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    private val chatDao = db.chatDao()
    private val groupDao = db.studyGroupDao()

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

            // 2. Keep active user profile in sync with cloud updates (e.g. password/status changed by Admin)
            val currentUser = userDao.getLoggedInUser()
            if (currentUser != null) {
                val cloudUser = OnlineSyncService.findUserAuthByEmail(currentUser.email)
                    ?: OnlineSyncService.fetchUserAuthById(currentUser.userId)

                if (cloudUser != null) {
                    if (cloudUser.isDeleted || cloudUser.accountStatus == "SUSPENDED" || cloudUser.accountStatus == "DISABLED") {
                        // User has been suspended, disabled, or deleted by Admin -> log them out immediately
                        val updated = currentUser.copy(
                            isLoggedIn = false,
                            accountStatus = cloudUser.accountStatus,
                            isDeleted = cloudUser.isDeleted,
                            suspensionReason = cloudUser.suspensionReason
                        )
                        userDao.updateUser(updated)
                    } else if (cloudUser.passwordHash != currentUser.passwordHash ||
                        cloudUser.fullName != currentUser.fullName ||
                        cloudUser.studyId != currentUser.studyId ||
                        cloudUser.accountStatus != currentUser.accountStatus
                    ) {
                        // Admin updated user password, full name, study ID, or status -> update local record
                        userDao.updateUser(
                            currentUser.copy(
                                fullName = cloudUser.fullName,
                                passwordHash = cloudUser.passwordHash,
                                studyId = cloudUser.studyId,
                                accountStatus = cloudUser.accountStatus,
                                isDeleted = cloudUser.isDeleted
                            )
                        )
                    }
                }

                // Keep stats & active status synced online without overriding cloud auth password
                OnlineSyncService.syncUserOnline(currentUser, getStudyStatsForUser(currentUser.userId))

                // Sync user's study groups from cloud
                try {
                    syncUserStudyGroups(currentUser.studyId)
                } catch (e: Exception) {
                    Log.w("AppRepository", "Error syncing user study groups in background", e)
                }
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
        val user = userDao.getLoggedInUser()
        if (user != null) {
            syncAllUserDataFromCloud(user.userId)
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

    // --- Cloud Synchronization & Real-time Listeners ---

    suspend fun syncAllUserDataFromCloud(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("AppRepository", "Starting full cloud data sync for user: $userId")

            // 1. Fetch and restore Study Sessions from Cloud / Firestore
            val cloudSessions = CloudDataSyncService.fetchStudySessionsFromCloud(userId)
            if (cloudSessions.isNotEmpty()) {
                studySessionDao.insertSessions(cloudSessions)
                Log.d("AppRepository", "Restored ${cloudSessions.size} study sessions from cloud")
            }

            // 2. Fetch and restore Subjects from Cloud / Firestore
            val cloudSubjects = CloudDataSyncService.fetchSubjectsFromCloud(userId)
            if (cloudSubjects.isNotEmpty()) {
                subjectDao.insertSubjects(cloudSubjects)
                Log.d("AppRepository", "Restored ${cloudSubjects.size} subjects from cloud")
            } else {
                val localSubs = subjectDao.getSubjectsForUser(userId)
                if (localSubs.isEmpty()) {
                    seedDefaultSubjectsForUser(userId)
                    val newSubs = subjectDao.getSubjectsForUser(userId)
                    for (sub in newSubs) {
                        CloudDataSyncService.syncSubjectToCloud(sub)
                    }
                }
            }

            // 3. Fetch and restore Goals from Cloud / Firestore
            val cloudGoals = CloudDataSyncService.fetchGoalsFromCloud(userId)
            if (cloudGoals.isNotEmpty()) {
                goalDao.insertGoals(cloudGoals)
                Log.d("AppRepository", "Restored ${cloudGoals.size} study goals from cloud")
            }

            // 4. Calculate fresh stats from restored sessions and sync summary
            val allSessions = studySessionDao.getSessionsForUser(userId)
            val stats = StatsCalculator.calculateStats(allSessions)
            CloudDataSyncService.syncUserStudyStatsToCloud(userId, stats)

            val user = userDao.getUserById(userId)
            if (user != null) {
                OnlineSyncService.syncUserOnline(user, stats)
                CloudDataSyncService.syncUserToFirestore(user)
            }

            // 5. Attach real-time Firestore listeners for immediate cross-device updates
            attachRealtimeCloudSync(userId)

            true
        } catch (e: Exception) {
            Log.e("AppRepository", "Error syncing all user data from cloud for $userId", e)
            false
        }
    }

    fun attachRealtimeCloudSync(userId: String) {
        CloudDataSyncService.attachRealtimeFirestoreListeners(
            userId = userId,
            onSessionsChanged = { sessions ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (sessions.isNotEmpty()) {
                            studySessionDao.insertSessions(sessions)
                        }
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Realtime sessions update failed", e)
                    }
                }
            },
            onGoalsChanged = { goals ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (goals.isNotEmpty()) {
                            goalDao.insertGoals(goals)
                        }
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Realtime goals update failed", e)
                    }
                }
            },
            onSubjectsChanged = { subjects ->
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        if (subjects.isNotEmpty()) {
                            subjectDao.insertSubjects(subjects)
                        }
                    } catch (e: Exception) {
                        Log.e("AppRepository", "Realtime subjects update failed", e)
                    }
                }
            }
        )
    }

    // --- Authentication Operations ---
    suspend fun registerUser(
        fullName: String,
        username: String,
        email: String,
        passwordHash: String
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim()
        val cleanPassword = passwordHash.trim()

        // 1. Check local database first
        val existingLocal = userDao.getUserByEmail(cleanEmail)
        if (existingLocal != null && !existingLocal.isDeleted) {
            return@withContext Result.failure(Exception("An account with this email already exists. Please log in."))
        }

        // 2. Check cloud database across any device
        try {
            val existingOnline = CloudDataSyncService.findUserInFirestoreByEmail(cleanEmail)
                ?: OnlineSyncService.findUserAuthByEmail(cleanEmail)
            if (existingOnline != null && !existingOnline.isDeleted) {
                // Account exists on another phone/device -> save to local DB and guide user to log in
                userDao.insertUser(existingOnline)
                return@withContext Result.failure(Exception("An account with this email already exists online. Please log in with your password."))
            }
        } catch (e: Exception) {
            Log.w("AppRepository", "Cloud check skipped during registration due to network", e)
        }

        // Use Firebase Authentication UID as the user's unique ID
        var firebaseUid: String? = null
        try {
            firebaseUid = CloudDataSyncService.getFirebaseUidIfAvailable(cleanEmail, cleanPassword)
        } catch (e: Exception) {
            Log.w("AppRepository", "Firebase auth check during registration", e)
        }

        val userId = firebaseUid ?: ("uid_fb_" + SecurityUtils.sha256(cleanEmail).take(20))
        val studyId = generateUniqueStudyId()

        // Logout any currently logged-in user first
        userDao.logoutAllUsers()

        val newUser = UserEntity(
            userId = userId,
            fullName = fullName.trim(),
            username = username.trim(),
            email = cleanEmail,
            passwordHash = cleanPassword,
            studyId = studyId,
            isLoggedIn = true,
            joinedDate = System.currentTimeMillis(),
            lastActiveTime = System.currentTimeMillis()
        )

        userDao.insertUser(newUser)
        seedDefaultSubjectsForUser(userId)

        // Sync new user credentials, profile and seeded subjects to Firestore & Cloud
        try {
            CloudDataSyncService.syncUserToFirestore(newUser)
            OnlineSyncService.syncUserAuthOnline(newUser)
            OnlineSyncService.syncUserOnline(newUser, getStudyStatsForUser(userId))

            val seededSubs = subjectDao.getSubjectsForUser(userId)
            for (sub in seededSubs) {
                CloudDataSyncService.syncSubjectToCloud(sub)
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to sync new user to cloud", e)
        }

        attachRealtimeCloudSync(userId)

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
            val cleanEmail = email.trim()
            val cleanPassword = passwordHash.trim()

            // 1. Check cloud FIRST for the most up-to-date credentials (Firestore first, then REST)
            var cloudUser: UserEntity? = null
            try {
                cloudUser = CloudDataSyncService.findUserInFirestoreByEmail(cleanEmail)
                    ?: OnlineSyncService.findUserAuthByEmail(cleanEmail)
            } catch (e: Exception) {
                Log.w("AppRepository", "Cloud lookup failed during login", e)
            }

            // Also check if FirebaseAuth has user credentials
            try {
                val fbUid = CloudDataSyncService.getFirebaseUidIfAvailable(cleanEmail, cleanPassword)
                if (fbUid != null && cloudUser == null) {
                    cloudUser = OnlineSyncService.fetchUserAuthById(fbUid)
                }
            } catch (e: Exception) {
                Log.w("AppRepository", "Firebase auth check during login", e)
            }

            if (cloudUser != null) {
                if (cloudUser.isDeleted) {
                    return@withContext Result.failure(Exception("This account has been deleted by an administrator."))
                }

                if (cloudUser.accountStatus == "SUSPENDED") {
                    val reason = if (cloudUser.suspensionReason.isNotBlank()) " Reason: ${cloudUser.suspensionReason}" else ""
                    return@withContext Result.failure(Exception("This account has been suspended by an administrator.$reason"))
                }

                if (cloudUser.accountStatus == "DISABLED") {
                    return@withContext Result.failure(Exception("This account has been disabled by an administrator."))
                }

                // Verify password against cloud (handles Admin password resets and cross-device signups)
                if (cloudUser.passwordHash != cleanPassword) {
                    return@withContext Result.failure(Exception("Incorrect password. Please try again."))
                }

                // Password verified! Log user in
                userDao.logoutAllUsers()

                val localExisting = userDao.getUserById(cloudUser.userId) ?: userDao.getUserByEmail(cleanEmail)
                val userToSave = cloudUser.copy(
                    isLoggedIn = true,
                    lastActiveTime = System.currentTimeMillis()
                )

                if (localExisting != null) {
                    userDao.updateUser(userToSave)
                } else {
                    userDao.insertUser(userToSave)
                }

                // Restore all cloud study sessions, subjects, goals, streak, and stats!
                syncAllUserDataFromCloud(userToSave.userId)

                // Keep cloud presence and online active
                OnlineSyncService.syncUserOnline(userToSave, getStudyStatsForUser(userToSave.userId))
                CloudDataSyncService.syncUserToFirestore(userToSave)

                return@withContext Result.success(userToSave)
            }

            // 2. Fallback to local Room database if offline (no internet) or created offline
            val localUser = userDao.getUserByEmail(cleanEmail)
            if (localUser == null) {
                return@withContext Result.failure(Exception("Account not found with this email. Please check your email or sign up."))
            }

            if (localUser.isDeleted) {
                return@withContext Result.failure(Exception("This account has been deleted."))
            }

            if (localUser.accountStatus == "SUSPENDED") {
                val reason = if (localUser.suspensionReason.isNotBlank()) " Reason: ${localUser.suspensionReason}" else ""
                return@withContext Result.failure(Exception("This account has been suspended by an administrator.$reason"))
            }

            if (localUser.accountStatus == "DISABLED") {
                return@withContext Result.failure(Exception("This account has been disabled by an administrator."))
            }

            if (localUser.passwordHash != cleanPassword) {
                return@withContext Result.failure(Exception("Incorrect password. Please try again."))
            }

            userDao.logoutAllUsers()
            val updatedUser = localUser.copy(
                isLoggedIn = true,
                lastActiveTime = System.currentTimeMillis()
            )
            userDao.updateUser(updatedUser)

            // Try to sync credentials and presence to cloud if internet becomes available
            try {
                OnlineSyncService.syncUserAuthOnline(updatedUser)
                CloudDataSyncService.syncUserToFirestore(updatedUser)
                syncAllUserDataFromCloud(updatedUser.userId)
            } catch (e: Exception) {
                Log.w("AppRepository", "Failed to sync to cloud during offline login fallback", e)
            }

            Result.success(updatedUser)
        }

    suspend fun signInWithGoogleAccount(
        email: String,
        displayName: String,
        googleId: String? = null,
        idToken: String? = null
    ): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val cleanName = displayName.trim().ifBlank { cleanEmail.substringBefore("@").replaceFirstChar { it.uppercase() } }

        // 1. Try Firebase Auth with Google Credential if ID token is available
        var fbUid: String? = null
        if (!idToken.isNullOrBlank() && CloudDataSyncService.isFirebaseAvailable()) {
            try {
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                val authResult = com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(credential).await()
                fbUid = authResult.user?.uid
            } catch (e: Throwable) {
                Log.w("AppRepository", "Firebase Google credential sign in fallback: ${e.message}")
            }
        }

        // 2. Check if user already exists in cloud (Firestore or Cloud REST) or locally
        var cloudUser = CloudDataSyncService.findUserInFirestoreByEmail(cleanEmail)
            ?: OnlineSyncService.findUserAuthByEmail(cleanEmail)
            ?: userDao.getUserByEmail(cleanEmail)

        if (cloudUser != null) {
            if (cloudUser.isDeleted) {
                return@withContext Result.failure(Exception("This Google account has been deleted by an administrator."))
            }
            if (cloudUser.accountStatus == "SUSPENDED") {
                val reason = if (cloudUser.suspensionReason.isNotBlank()) " Reason: ${cloudUser.suspensionReason}" else ""
                return@withContext Result.failure(Exception("This account has been suspended by an administrator.$reason"))
            }
            if (cloudUser.accountStatus == "DISABLED") {
                return@withContext Result.failure(Exception("This account has been disabled by an administrator."))
            }
        }

        val targetUserId = fbUid ?: cloudUser?.userId ?: ("uid_google_" + SecurityUtils.sha256(cleanEmail).take(20))
        val targetStudyId = cloudUser?.studyId ?: generateUniqueStudyId()

        val userToSave = UserEntity(
            userId = targetUserId,
            fullName = if (cloudUser != null && cloudUser.fullName.isNotBlank()) cloudUser.fullName else cleanName,
            username = if (cloudUser != null && cloudUser.username.isNotBlank()) cloudUser.username else cleanEmail.substringBefore("@").lowercase().replace(".", "_"),
            email = cleanEmail,
            passwordHash = "google_oauth_verified",
            studyId = targetStudyId,
            isLoggedIn = true,
            accountStatus = cloudUser?.accountStatus ?: "ACTIVE",
            joinedDate = cloudUser?.joinedDate ?: System.currentTimeMillis(),
            lastActiveTime = System.currentTimeMillis()
        )

        userDao.logoutAllUsers()
        val localExisting = userDao.getUserById(userToSave.userId) ?: userDao.getUserByEmail(cleanEmail)
        if (localExisting != null) {
            userDao.updateUser(userToSave)
        } else {
            userDao.insertUser(userToSave)
        }

        // Sync auth & profile to Cloud REST and Firestore
        try {
            CloudDataSyncService.syncUserToFirestore(userToSave)
            OnlineSyncService.syncUserAuthOnline(userToSave)
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to sync Google user to cloud", e)
        }

        // Restore all cloud study sessions, subjects, goals, streak, and stats
        syncAllUserDataFromCloud(userToSave.userId)

        // Keep cloud presence and online active
        OnlineSyncService.syncUserOnline(userToSave, getStudyStatsForUser(userToSave.userId))

        Result.success(userToSave)
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
            try {
                CloudDataSyncService.syncSubjectToCloud(subject)
            } catch (e: Exception) {
                Log.e("AppRepository", "Failed to sync subject to cloud", e)
            }
            subject
        }

    suspend fun updateSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        subjectDao.insertSubject(subject)
        try {
            CloudDataSyncService.syncSubjectToCloud(subject)
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to sync updated subject to cloud", e)
        }
    }

    suspend fun deleteSubject(subject: SubjectEntity) = withContext(Dispatchers.IO) {
        subjectDao.deleteSubject(subject)
        try {
            CloudDataSyncService.deleteSubjectFromCloud(subject.userId, subject.subjectId)
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to delete subject from cloud", e)
        }
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
        notes: String,
        customDate: String? = null
    ): StudySessionEntity = withContext(Dispatchers.IO) {
        val sessionId = "sess_" + UUID.randomUUID().toString().take(8)
        val sessionDate = customDate ?: getFormattedDateString(endTime)

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

        // Sync session to Firestore & Cloud immediately
        try {
            CloudDataSyncService.syncStudySessionToCloud(session)
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to sync study session to cloud", e)
        }

        // Check if session triggered goal/streak notification
        notificationDao.insertNotification(
            AppNotificationEntity(
                notificationId = "notif_" + UUID.randomUUID().toString(),
                userId = userId,
                title = "Study Session Completed 🎉",
                message = "Logged ${formatDurationShort(durationSeconds)} in $subjectName."
            )
        )

        // Sync updated hours, streak & stats summary to cloud and online community
        try {
            val user = userDao.getUserById(userId)
            val allSessions = studySessionDao.getSessionsForUser(userId)
            val stats = StatsCalculator.calculateStats(allSessions)
            CloudDataSyncService.syncUserStudyStatsToCloud(userId, stats)
            if (user != null) {
                OnlineSyncService.syncUserOnline(user, stats)
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed background online sync on session complete", e)
        }

        session
    }

    suspend fun updateStudySession(session: StudySessionEntity) = withContext(Dispatchers.IO) {
        studySessionDao.updateSession(session)
        try {
            CloudDataSyncService.syncStudySessionToCloud(session)
            val allSessions = studySessionDao.getSessionsForUser(session.userId)
            val stats = StatsCalculator.calculateStats(allSessions)
            CloudDataSyncService.syncUserStudyStatsToCloud(session.userId, stats)
            val user = userDao.getUserById(session.userId)
            if (user != null) {
                OnlineSyncService.syncUserOnline(user, stats)
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to sync updated session to cloud", e)
        }
    }

    suspend fun deleteStudySession(sessionId: String, userId: String) = withContext(Dispatchers.IO) {
        studySessionDao.deleteSessionById(sessionId, userId)
        try {
            CloudDataSyncService.deleteStudySessionFromCloud(userId, sessionId)
            val allSessions = studySessionDao.getSessionsForUser(userId)
            val stats = StatsCalculator.calculateStats(allSessions)
            CloudDataSyncService.syncUserStudyStatsToCloud(userId, stats)
            val user = userDao.getUserById(userId)
            if (user != null) {
                OnlineSyncService.syncUserOnline(user, stats)
            }
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to sync deleted session from cloud", e)
        }
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

    fun getPendingSentRequests(userId: String): Flow<List<FriendRequestEntity>> =
        friendDao.getPendingSentRequestsFlow(userId)

    suspend fun cancelFriendRequest(request: FriendRequestEntity) = withContext(Dispatchers.IO) {
        friendDao.deleteFriendRequestById(request.requestId)
        val sender = userDao.getUserById(request.senderId)
        val receiver = userDao.getUserById(request.receiverId)
        if (sender != null && receiver != null) {
            try {
                OnlineSyncService.updateOnlineRequestStatus(receiver.studyId, sender.studyId, "CANCELLED")
            } catch (e: Exception) {
                Log.e("AppRepository", "Error cancelling online request", e)
            }
        }
    }

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
                // Record mutual friendship online so BOTH users see each other in My Friends
                OnlineSyncService.recordOnlineFriendship(sender, receiver)
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

            var newCount = 0

            // 1. Fetch pending incoming requests sent to me from other users
            val onlineRequests = OnlineSyncService.fetchIncomingOnlineRequests(user.studyId)
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
                                title = "New Online Friend Request 👥",
                                message = "${sender.fullName} sent you a study friend request!"
                            )
                        )
                        newCount++
                    }
                }
            }

            // 2. Fetch all online mutual friendships for current user!
            // When user B accepts user A's request, user A syncs and sees user B in My Friends immediately!
            val onlineFriendships = OnlineSyncService.fetchOnlineFriendships(user.studyId)
            for (of in onlineFriendships) {
                var friendUser = userDao.getUserByStudyId(of.friendStudyId)
                if (friendUser == null) {
                    friendUser = searchUserByStudyId(of.friendStudyId)
                }
                if (friendUser == null && of.friendStudyId.isNotBlank()) {
                    val fallback = UserEntity(
                        userId = of.friendUserId.ifBlank { "user_" + of.friendStudyId.lowercase().replace("-", "") },
                        fullName = of.friendName.ifBlank { "Study Buddy" },
                        username = of.friendUsername.ifBlank { of.friendStudyId.lowercase().replace("-", "") },
                        email = "${of.friendStudyId.lowercase().replace("-", "")}@student.community",
                        passwordHash = "cloud_user",
                        studyId = of.friendStudyId,
                        privacyVisibility = "PUBLIC",
                        isLoggedIn = false
                    )
                    userDao.insertUser(fallback)
                    friendUser = fallback
                }
                if (friendUser != null && friendUser.userId != user.userId) {
                    val existingFriendship = friendDao.getFriendshipBetween(user.userId, friendUser.userId)
                    if (existingFriendship == null) {
                        val fId = "f_sync_" + UUID.randomUUID().toString().take(8)
                        friendDao.insertFriendship(
                            FriendshipEntity(
                                friendshipId = fId,
                                userId1 = user.userId,
                                userId2 = friendUser.userId,
                                createdAt = of.timestamp
                            )
                        )

                        // Mark any pending request between them as ACCEPTED
                        val pendingReq = friendDao.getFriendRequestBetween(user.userId, friendUser.userId)
                        if (pendingReq != null && pendingReq.status != "ACCEPTED") {
                            friendDao.updateFriendRequest(pendingReq.copy(status = "ACCEPTED", updatedAt = System.currentTimeMillis()))
                        }

                        notificationDao.insertNotification(
                            AppNotificationEntity(
                                notificationId = "notif_friend_" + UUID.randomUUID().toString().take(8),
                                userId = user.userId,
                                title = "Friend Request Accepted 🎉",
                                message = "${friendUser.fullName} is now your study friend! Start chatting & sharing goals."
                            )
                        )
                        newCount++
                    }
                }
            }

            newCount
        } catch (e: Exception) {
            Log.e("AppRepository", "Error syncing online requests and friendships", e)
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

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun getAcceptedFriendsFlow(userId: String): Flow<List<UserEntity>> {
        return friendDao.getFriendshipsFlow(userId).flatMapLatest { friendships ->
            val friendIds = friendships.map {
                if (it.userId1 == userId) it.userId2 else it.userId1
            }
            if (friendIds.isEmpty()) flowOf(emptyList())
            else userDao.getUsersByIdsFlow(friendIds)
        }
    }

    suspend fun removeFriend(userId: String, friendId: String) = withContext(Dispatchers.IO) {
        val user1 = userDao.getUserById(userId)
        val user2 = userDao.getUserById(friendId)
        friendDao.removeFriendship(userId, friendId)
        if (user1 != null && user2 != null) {
            try {
                OnlineSyncService.removeOnlineFriendship(user1.studyId, user2.studyId)
            } catch (e: Exception) {
                Log.e("AppRepository", "Failed to remove online friendship", e)
            }
        }
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
        try {
            CloudDataSyncService.syncGoalToCloud(goal)
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to sync goal to cloud", e)
        }
    }

    suspend fun deleteGoal(goal: StudyGoalEntity) = withContext(Dispatchers.IO) {
        goalDao.deleteGoal(goal)
        try {
            CloudDataSyncService.deleteGoalFromCloud(goal.userId, goal.goalId)
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to delete goal from cloud", e)
        }
    }

    suspend fun setDailyGoal(
        userId: String,
        targetHours: Float,
        title: String = "Daily Study Target",
        goalId: String? = null,
        subjectName: String = ""
    ): StudyGoalEntity = withContext(Dispatchers.IO) {
        val targetMinutes = (targetHours * 60).toInt().coerceAtLeast(10)
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startOfDay = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        val endOfDay = cal.timeInMillis

        val id = goalId ?: "daily_goal_${userId}_${UUID.randomUUID().toString().take(6)}"
        val goal = StudyGoalEntity(
            goalId = id,
            userId = userId,
            title = title.ifBlank { "Daily Study Target" },
            targetDurationMinutes = targetMinutes,
            targetSessions = (targetHours / 1f).toInt().coerceAtLeast(1),
            startDate = startOfDay,
            endDate = endOfDay,
            subjectName = subjectName.trim(),
            createdAt = now
        )
        goalDao.insertGoal(goal)
        try {
            CloudDataSyncService.syncGoalToCloud(goal)
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to sync daily goal to cloud", e)
        }
        goal
    }

    suspend fun setWeeklyGoal(
        userId: String,
        targetHours: Float,
        title: String = "Weekly Study Target",
        goalId: String? = null,
        subjectName: String = ""
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

        val id = goalId ?: "weekly_goal_${userId}_${UUID.randomUUID().toString().take(6)}"
        val goal = StudyGoalEntity(
            goalId = id,
            userId = userId,
            title = title.ifBlank { "Weekly Study Target" },
            targetDurationMinutes = targetMinutes,
            targetSessions = (targetHours / 2f).toInt().coerceAtLeast(3),
            startDate = startOfWeek,
            endDate = endOfWeek,
            subjectName = subjectName.trim(),
            createdAt = now
        )
        goalDao.insertGoal(goal)
        try {
            CloudDataSyncService.syncGoalToCloud(goal)
        } catch (e: Exception) {
            Log.e("AppRepository", "Failed to sync weekly goal to cloud", e)
        }
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

    // --- Chat & Study Group Messaging ---

    fun getDirectChannelId(studyId1: String, studyId2: String): String {
        val s1 = studyId1.trim().uppercase().replace(" ", "")
        val s2 = studyId2.trim().uppercase().replace(" ", "")
        return if (s1 <= s2) "dm_${s1}_${s2}" else "dm_${s2}_${s1}"
    }

    fun getMessagesForChannelFlow(channelId: String): Flow<List<ChatMessageEntity>> =
        chatDao.getMessagesForChannelFlow(channelId)

    suspend fun getMessagesForChannel(channelId: String): List<ChatMessageEntity> = withContext(Dispatchers.IO) {
        chatDao.getMessagesForChannel(channelId)
    }

    suspend fun sendChatMessage(
        channelId: String,
        sender: UserEntity,
        text: String,
        isGroup: Boolean = false,
        groupId: String? = null
    ): ChatMessageEntity = withContext(Dispatchers.IO) {
        val message = ChatMessageEntity(
            messageId = "msg_" + UUID.randomUUID().toString().take(12),
            channelId = channelId,
            senderUserId = sender.userId,
            senderName = sender.fullName,
            senderStudyId = sender.studyId,
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            isGroup = isGroup,
            groupId = groupId,
            isRead = true
        )

        // Save locally in Room
        chatDao.insertMessage(message)

        // If in a study group, update group's last message
        if (isGroup && groupId != null) {
            val group = groupDao.getGroupById(groupId)
            if (group != null) {
                val updatedGroup = group.copy(
                    lastMessageText = "${sender.fullName.take(12)}: ${text.trim().take(40)}",
                    lastMessageTime = message.timestamp
                )
                groupDao.updateGroup(updatedGroup)
                OnlineSyncService.syncStudyGroupOnline(updatedGroup)
            }
        }

        // Send to cloud in background
        OnlineSyncService.sendChatMessageOnline(message)

        message
    }

    suspend fun syncChannelMessages(channelId: String, isGroup: Boolean = false): List<ChatMessageEntity> =
        withContext(Dispatchers.IO) {
            val onlineMsgs = OnlineSyncService.fetchChatMessagesOnline(channelId, isGroup)
            if (onlineMsgs.isNotEmpty()) {
                chatDao.insertMessages(onlineMsgs)
            }
            chatDao.getMessagesForChannel(channelId)
        }

    fun getStudyGroupsFlow(): Flow<List<StudyGroupEntity>> = groupDao.getAllGroupsFlow()

    suspend fun getStudyGroupById(groupId: String): StudyGroupEntity? = withContext(Dispatchers.IO) {
        groupDao.getGroupById(groupId)
    }

    suspend fun createStudyGroup(
        creator: UserEntity,
        name: String,
        description: String,
        selectedFriends: List<UserEntity>,
        colorHex: String = "#3F51B5",
        iconName: String = "groups"
    ): StudyGroupEntity = withContext(Dispatchers.IO) {
        val allMembers = (listOf(creator) + selectedFriends).distinctBy { it.userId }
        val groupId = "grp_" + UUID.randomUUID().toString().take(8)

        val newGroup = StudyGroupEntity(
            groupId = groupId,
            name = name.trim(),
            description = description.trim(),
            createdByUserId = creator.userId,
            createdByName = creator.fullName,
            createdByStudyId = creator.studyId,
            createdAt = System.currentTimeMillis(),
            memberUserIds = allMembers.joinToString(",") { it.userId },
            memberStudyIds = allMembers.joinToString(",") { it.studyId },
            memberNames = allMembers.joinToString(",") { it.fullName },
            iconName = iconName,
            colorHex = colorHex,
            lastMessageText = "Group created 🎉",
            lastMessageTime = System.currentTimeMillis()
        )

        groupDao.insertGroup(newGroup)
        OnlineSyncService.syncStudyGroupOnline(newGroup)

        // Send welcome announcement message in group chat
        val welcomeMsg = ChatMessageEntity(
            messageId = "msg_welcome_$groupId",
            channelId = groupId,
            senderUserId = creator.userId,
            senderName = creator.fullName,
            senderStudyId = creator.studyId,
            text = "Welcome to ${name.trim()}! Study goals and chat started 📚✨",
            timestamp = System.currentTimeMillis(),
            isGroup = true,
            groupId = groupId,
            isRead = true
        )
        chatDao.insertMessage(welcomeMsg)
        OnlineSyncService.sendChatMessageOnline(welcomeMsg)

        newGroup
    }

    suspend fun syncUserStudyGroups(userStudyId: String): List<StudyGroupEntity> = withContext(Dispatchers.IO) {
        val onlineGroups = OnlineSyncService.fetchGroupsForUserOnline(userStudyId)
        if (onlineGroups.isNotEmpty()) {
            groupDao.insertGroups(onlineGroups)
        }
        onlineGroups
    }

    suspend fun addFriendsToStudyGroup(
        groupId: String,
        addedBy: UserEntity,
        newFriends: List<UserEntity>
    ): StudyGroupEntity? = withContext(Dispatchers.IO) {
        val group = groupDao.getGroupById(groupId) ?: return@withContext null
        if (newFriends.isEmpty()) return@withContext group

        val existingUserIds = group.memberUserIds.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        val existingStudyIds = group.memberStudyIds.split(",").map { it.trim().uppercase() }.filter { it.isNotBlank() }.toMutableList()
        val existingNames = group.memberNames.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()

        val newlyAddedNames = mutableListOf<String>()
        for (friend in newFriends) {
            val fStudyId = friend.studyId.trim().uppercase()
            if (!existingStudyIds.contains(fStudyId)) {
                existingUserIds.add(friend.userId)
                existingStudyIds.add(fStudyId)
                existingNames.add(friend.fullName)
                newlyAddedNames.add(friend.fullName)
            }
        }

        if (newlyAddedNames.isEmpty()) return@withContext group

        val updatedGroup = group.copy(
            memberUserIds = existingUserIds.joinToString(","),
            memberStudyIds = existingStudyIds.joinToString(","),
            memberNames = existingNames.joinToString(","),
            lastMessageText = "${addedBy.fullName} added ${newlyAddedNames.joinToString(", ")}",
            lastMessageTime = System.currentTimeMillis()
        )

        groupDao.updateGroup(updatedGroup)
        OnlineSyncService.syncStudyGroupOnline(updatedGroup)

        // Post system announcement message in group chat
        val sysMsg = ChatMessageEntity(
            messageId = "msg_added_" + UUID.randomUUID().toString().take(10),
            channelId = groupId,
            senderUserId = addedBy.userId,
            senderName = "System",
            senderStudyId = addedBy.studyId,
            text = "${addedBy.fullName} added ${newlyAddedNames.joinToString(", ")} to the study group 👥✨",
            timestamp = System.currentTimeMillis(),
            isGroup = true,
            groupId = groupId,
            isRead = true
        )
        chatDao.insertMessage(sysMsg)
        OnlineSyncService.sendChatMessageOnline(sysMsg)

        updatedGroup
    }

    suspend fun deleteStudyGroup(groupId: String) = withContext(Dispatchers.IO) {
        groupDao.deleteGroupById(groupId)
        chatDao.deleteMessagesForChannel(groupId)
        try {
            OnlineSyncService.deleteStudyGroupOnline(groupId)
        } catch (e: Exception) {
            Log.w("AppRepository", "Error deleting study group online", e)
        }
    }
}
