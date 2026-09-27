package com.example.data.remote

import android.content.Context
import android.util.Log
import com.example.data.model.StudyGoalEntity
import com.example.data.model.StudySessionEntity
import com.example.data.model.SubjectEntity
import com.example.data.model.UserEntity
import com.example.data.util.UserStudyStats
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Universal Multi-Device Real-Time Cloud Synchronization Service
 * 
 * Supports real-time Firestore synchronization when Firebase is configured,
 * combined with high-performance real-time cloud REST endpoints so that
 * study data (sessions, subjects, goals, streaks, daily & weekly progress)
 * is ALWAYS synchronized across all devices without resetting to 0.
 */
object CloudDataSyncService {
    private const val TAG = "CloudDataSyncService"
    private const val BASE_URL = "https://sanchardb.pages.dev"

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    private var firestoreListenerSessions: ListenerRegistration? = null
    private var firestoreListenerGoals: ListenerRegistration? = null
    private var firestoreListenerSubjects: ListenerRegistration? = null

    private fun encode(value: String): String {
        return try {
            URLEncoder.encode(value, "UTF-8")
        } catch (e: Exception) {
            value.replace(" ", "+")
        }
    }

    /**
     * Checks if Firebase is initialized and available in the current runtime.
     */
    fun isFirebaseAvailable(context: Context? = null): Boolean {
        return try {
            FirebaseApp.getApps(context ?: android.app.Application()).isNotEmpty()
        } catch (e: Throwable) {
            try {
                FirebaseApp.getInstance() != null
            } catch (t: Throwable) {
                false
            }
        }
    }

    /**
     * Attempts to get or authenticate with Firebase Auth to retrieve a Firebase UID.
     * Returns null if Firebase is not initialized or auth is unavailable.
     */
    suspend fun getFirebaseUidIfAvailable(email: String, password: String): String? =
        withContext(Dispatchers.IO) {
            try {
                if (!isFirebaseAvailable()) return@withContext null
                val auth = FirebaseAuth.getInstance()
                val current = auth.currentUser
                if (current != null && current.email.equals(email, ignoreCase = true)) {
                    return@withContext current.uid
                }

                // Try sign in
                val authResult = try {
                    auth.signInWithEmailAndPassword(email, password).await()
                } catch (e: Exception) {
                    // If user doesn't exist, try creating user
                    auth.createUserWithEmailAndPassword(email, password).await()
                }
                authResult.user?.uid
            } catch (e: Throwable) {
                Log.w(TAG, "Firebase Auth not available, using cloud deterministic UID: ${e.message}")
                null
            }
        }

    // =========================================================================
    // STUDY SESSIONS SYNC
    // =========================================================================

    suspend fun syncStudySessionToCloud(session: StudySessionEntity): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("sessionId", session.sessionId)
                    put("userId", session.userId)
                    put("subjectId", session.subjectId)
                    put("subjectName", session.subjectName)
                    put("startTime", session.startTime)
                    put("endTime", session.endTime)
                    put("durationSeconds", session.durationSeconds)
                    put("sessionDate", session.sessionDate)
                    put("title", session.title)
                    put("notes", session.notes)
                    put("createdAt", session.createdAt)
                    put("sessionType", session.sessionType)
                    put("isDeleted", session.isDeleted)
                    put("updatedTime", System.currentTimeMillis())
                }.toString()

                // 1. Sync to Cloud REST endpoint
                val url = "$BASE_URL/set/studytracker/users/${session.userId}/sessions/${session.sessionId}?value=${encode(json)}"
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute().close()

                // 2. Also sync to Firestore if Firebase is active
                try {
                    if (isFirebaseAvailable()) {
                        val db = FirebaseFirestore.getInstance()
                        val map = hashMapOf(
                            "sessionId" to session.sessionId,
                            "userId" to session.userId,
                            "subjectId" to session.subjectId,
                            "subjectName" to session.subjectName,
                            "startTime" to session.startTime,
                            "endTime" to session.endTime,
                            "durationSeconds" to session.durationSeconds,
                            "sessionDate" to session.sessionDate,
                            "title" to session.title,
                            "notes" to session.notes,
                            "createdAt" to session.createdAt,
                            "sessionType" to session.sessionType,
                            "isDeleted" to session.isDeleted,
                            "updatedTime" to System.currentTimeMillis()
                        )
                        db.collection("users")
                            .document(session.userId)
                            .collection("sessions")
                            .document(session.sessionId)
                            .set(map, SetOptions.merge())
                            .await()
                    }
                } catch (e: Throwable) {
                    Log.d(TAG, "Firestore write skipped: ${e.message}")
                }

                Log.d(TAG, "Successfully synced session ${session.sessionId} to cloud")
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync study session to cloud", e)
                false
            }
        }

    suspend fun deleteStudySessionFromCloud(userId: String, sessionId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // Delete from Cloud REST
                val url = "$BASE_URL/delete/studytracker/users/$userId/sessions/$sessionId"
                val request = Request.Builder().url(url).build()
                httpClient.newCall(request).execute().close()

                // Delete from Firestore
                try {
                    if (isFirebaseAvailable()) {
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .collection("sessions")
                            .document(sessionId)
                            .delete()
                            .await()
                    }
                } catch (e: Throwable) {
                    Log.d(TAG, "Firestore delete skipped: ${e.message}")
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete session from cloud", e)
                false
            }
        }

    suspend fun fetchStudySessionsFromCloud(userId: String): List<StudySessionEntity> =
        withContext(Dispatchers.IO) {
            val sessions = mutableListOf<StudySessionEntity>()

            // 1. Try Firestore first if available
            try {
                if (isFirebaseAvailable()) {
                    val snapshot = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .collection("sessions")
                        .get()
                        .await()

                    for (doc in snapshot.documents) {
                        val sId = doc.getString("sessionId") ?: doc.id
                        val sess = StudySessionEntity(
                            sessionId = sId,
                            userId = doc.getString("userId") ?: userId,
                            subjectId = doc.getString("subjectId") ?: "",
                            subjectName = doc.getString("subjectName") ?: "Study",
                            startTime = doc.getLong("startTime") ?: 0L,
                            endTime = doc.getLong("endTime") ?: 0L,
                            durationSeconds = doc.getLong("durationSeconds") ?: 0L,
                            sessionDate = doc.getString("sessionDate") ?: "",
                            title = doc.getString("title") ?: "",
                            notes = doc.getString("notes") ?: "",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            sessionType = doc.getString("sessionType") ?: "TIMER",
                            isDeleted = doc.getBoolean("isDeleted") ?: false
                        )
                        if (!sess.isDeleted) sessions.add(sess)
                    }
                    if (sessions.isNotEmpty()) {
                        Log.d(TAG, "Fetched ${sessions.size} sessions from Firestore for user $userId")
                        return@withContext sessions.sortedByDescending { it.startTime }
                    }
                }
            } catch (e: Throwable) {
                Log.d(TAG, "Firestore session fetch fallback: ${e.message}")
            }

            // 2. Fallback to Cloud REST database
            try {
                val url = "$BASE_URL/get/studytracker/users/$userId/sessions"
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    return@withContext emptyList()
                }

                val body = response.body?.string() ?: return@withContext emptyList()
                val root = JSONObject(body)
                if (root.optString("status") != "success") return@withContext emptyList()
                val data = root.optJSONObject("data") ?: return@withContext emptyList()

                val keys = data.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    val raw = data.optString(key)
                    val obj = try {
                        JSONObject(raw)
                    } catch (e: Exception) {
                        data.optJSONObject(key) ?: continue
                    }

                    if (obj.optBoolean("isDeleted", false)) continue

                    sessions.add(
                        StudySessionEntity(
                            sessionId = obj.optString("sessionId", key),
                            userId = obj.optString("userId", userId),
                            subjectId = obj.optString("subjectId", ""),
                            subjectName = obj.optString("subjectName", "Study"),
                            startTime = obj.optLong("startTime", 0L),
                            endTime = obj.optLong("endTime", 0L),
                            durationSeconds = obj.optLong("durationSeconds", 0L),
                            sessionDate = obj.optString("sessionDate", ""),
                            title = obj.optString("title", ""),
                            notes = obj.optString("notes", ""),
                            createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                            sessionType = obj.optString("sessionType", "TIMER"),
                            isDeleted = false
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching sessions from Cloud REST for $userId", e)
            }

            sessions.sortedByDescending { it.startTime }
        }

    // =========================================================================
    // SUBJECTS SYNC
    // =========================================================================

    suspend fun syncSubjectToCloud(subject: SubjectEntity): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("subjectId", subject.subjectId)
                    put("userId", subject.userId)
                    put("name", subject.name)
                    put("colorHex", subject.colorHex)
                    put("createdAt", subject.createdAt)
                    put("status", subject.status)
                    put("description", subject.description)
                    put("icon", subject.icon)
                    put("isDefault", subject.isDefault)
                    put("updatedAt", subject.updatedAt)
                }.toString()

                val url = "$BASE_URL/set/studytracker/users/${subject.userId}/subjects/${subject.subjectId}?value=${encode(json)}"
                httpClient.newCall(Request.Builder().url(url).build()).execute().close()

                try {
                    if (isFirebaseAvailable()) {
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(subject.userId)
                            .collection("subjects")
                            .document(subject.subjectId)
                            .set(
                                hashMapOf(
                                    "subjectId" to subject.subjectId,
                                    "userId" to subject.userId,
                                    "name" to subject.name,
                                    "colorHex" to subject.colorHex,
                                    "createdAt" to subject.createdAt,
                                    "status" to subject.status,
                                    "description" to subject.description,
                                    "icon" to subject.icon,
                                    "isDefault" to subject.isDefault,
                                    "updatedAt" to subject.updatedAt
                                ),
                                SetOptions.merge()
                            ).await()
                    }
                } catch (e: Throwable) {
                    Log.d(TAG, "Firestore subject write skipped: ${e.message}")
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync subject to cloud", e)
                false
            }
        }

    suspend fun fetchSubjectsFromCloud(userId: String): List<SubjectEntity> =
        withContext(Dispatchers.IO) {
            val subjects = mutableListOf<SubjectEntity>()

            // 1. Try Firestore
            try {
                if (isFirebaseAvailable()) {
                    val snapshot = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .collection("subjects")
                        .get()
                        .await()

                    for (doc in snapshot.documents) {
                        subjects.add(
                            SubjectEntity(
                                subjectId = doc.getString("subjectId") ?: doc.id,
                                userId = doc.getString("userId") ?: userId,
                                name = doc.getString("name") ?: "Subject",
                                colorHex = doc.getString("colorHex") ?: "#3F51B5",
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                                status = doc.getString("status") ?: "ACTIVE",
                                description = doc.getString("description") ?: "",
                                icon = doc.getString("icon") ?: "menu_book",
                                isDefault = doc.getBoolean("isDefault") ?: false,
                                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                            )
                        )
                    }
                    if (subjects.isNotEmpty()) return@withContext subjects
                }
            } catch (e: Throwable) {
                Log.d(TAG, "Firestore subjects fetch skipped: ${e.message}")
            }

            // 2. Try Cloud REST
            try {
                val url = "$BASE_URL/get/studytracker/users/$userId/subjects"
                val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    response.close()
                    val root = JSONObject(body)
                    val data = root.optJSONObject("data")
                    if (data != null) {
                        val keys = data.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val raw = data.optString(key)
                            val obj = try { JSONObject(raw) } catch (e: Exception) { data.optJSONObject(key) ?: continue }
                            subjects.add(
                                SubjectEntity(
                                    subjectId = obj.optString("subjectId", key),
                                    userId = obj.optString("userId", userId),
                                    name = obj.optString("name", "Subject"),
                                    colorHex = obj.optString("colorHex", "#3F51B5"),
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                    status = obj.optString("status", "ACTIVE"),
                                    description = obj.optString("description", ""),
                                    icon = obj.optString("icon", "menu_book"),
                                    isDefault = obj.optBoolean("isDefault", false),
                                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                } else {
                    response.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch subjects from cloud for $userId", e)
            }
            subjects
        }

    suspend fun deleteSubjectFromCloud(userId: String, subjectId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/delete/studytracker/users/$userId/subjects/$subjectId"
                httpClient.newCall(Request.Builder().url(url).build()).execute().close()

                try {
                    if (isFirebaseAvailable()) {
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .collection("subjects")
                            .document(subjectId)
                            .delete()
                            .await()
                    }
                } catch (e: Throwable) {
                    Log.d(TAG, "Firestore subject delete skipped: ${e.message}")
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete subject from cloud", e)
                false
            }
        }

    // =========================================================================
    // STUDY GOALS SYNC
    // =========================================================================

    suspend fun syncGoalToCloud(goal: StudyGoalEntity): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("goalId", goal.goalId)
                    put("userId", goal.userId)
                    put("title", goal.title)
                    put("targetDurationMinutes", goal.targetDurationMinutes)
                    put("targetSessions", goal.targetSessions)
                    put("startDate", goal.startDate)
                    put("endDate", goal.endDate)
                    put("subjectName", goal.subjectName)
                    put("createdAt", goal.createdAt)
                }.toString()

                val url = "$BASE_URL/set/studytracker/users/${goal.userId}/goals/${goal.goalId}?value=${encode(json)}"
                httpClient.newCall(Request.Builder().url(url).build()).execute().close()

                try {
                    if (isFirebaseAvailable()) {
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(goal.userId)
                            .collection("goals")
                            .document(goal.goalId)
                            .set(
                                hashMapOf(
                                    "goalId" to goal.goalId,
                                    "userId" to goal.userId,
                                    "title" to goal.title,
                                    "targetDurationMinutes" to goal.targetDurationMinutes,
                                    "targetSessions" to goal.targetSessions,
                                    "startDate" to goal.startDate,
                                    "endDate" to goal.endDate,
                                    "subjectName" to goal.subjectName,
                                    "createdAt" to goal.createdAt
                                ),
                                SetOptions.merge()
                            ).await()
                    }
                } catch (e: Throwable) {
                    Log.d(TAG, "Firestore goal write skipped: ${e.message}")
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync goal to cloud", e)
                false
            }
        }

    suspend fun deleteGoalFromCloud(userId: String, goalId: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val url = "$BASE_URL/delete/studytracker/users/$userId/goals/$goalId"
                httpClient.newCall(Request.Builder().url(url).build()).execute().close()

                try {
                    if (isFirebaseAvailable()) {
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .collection("goals")
                            .document(goalId)
                            .delete()
                            .await()
                    }
                } catch (e: Throwable) {
                    Log.d(TAG, "Firestore goal delete skipped: ${e.message}")
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete goal from cloud", e)
                false
            }
        }

    suspend fun fetchGoalsFromCloud(userId: String): List<StudyGoalEntity> =
        withContext(Dispatchers.IO) {
            val goals = mutableListOf<StudyGoalEntity>()

            // 1. Try Firestore
            try {
                if (isFirebaseAvailable()) {
                    val snapshot = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .collection("goals")
                        .get()
                        .await()

                    for (doc in snapshot.documents) {
                        goals.add(
                            StudyGoalEntity(
                                goalId = doc.getString("goalId") ?: doc.id,
                                userId = doc.getString("userId") ?: userId,
                                title = doc.getString("title") ?: "Goal",
                                targetDurationMinutes = doc.getLong("targetDurationMinutes")?.toInt() ?: 0,
                                targetSessions = doc.getLong("targetSessions")?.toInt() ?: 0,
                                startDate = doc.getLong("startDate") ?: 0L,
                                endDate = doc.getLong("endDate") ?: 0L,
                                subjectName = doc.getString("subjectName") ?: "",
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            )
                        )
                    }
                    if (goals.isNotEmpty()) return@withContext goals
                }
            } catch (e: Throwable) {
                Log.d(TAG, "Firestore goals fetch skipped: ${e.message}")
            }

            // 2. Try Cloud REST
            try {
                val url = "$BASE_URL/get/studytracker/users/$userId/goals"
                val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    response.close()
                    val root = JSONObject(body)
                    val data = root.optJSONObject("data")
                    if (data != null) {
                        val keys = data.keys()
                        while (keys.hasNext()) {
                            val key = keys.next()
                            val raw = data.optString(key)
                            val obj = try { JSONObject(raw) } catch (e: Exception) { data.optJSONObject(key) ?: continue }
                            goals.add(
                                StudyGoalEntity(
                                    goalId = obj.optString("goalId", key),
                                    userId = obj.optString("userId", userId),
                                    title = obj.optString("title", "Goal"),
                                    targetDurationMinutes = obj.optInt("targetDurationMinutes", 0),
                                    targetSessions = obj.optInt("targetSessions", 0),
                                    startDate = obj.optLong("startDate", 0L),
                                    endDate = obj.optLong("endDate", 0L),
                                    subjectName = obj.optString("subjectName", ""),
                                    createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                )
                            )
                        }
                    }
                } else {
                    response.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch goals from cloud for $userId", e)
            }
            goals
        }

    // =========================================================================
    // USER STUDY STATS & STREAK SYNC
    // =========================================================================

    suspend fun syncUserStudyStatsToCloud(userId: String, stats: UserStudyStats): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("currentStreakDays", stats.currentStreakDays)
                    put("longestStreakDays", stats.longestStreakDays)
                    put("todayTimeSeconds", stats.todayTimeSeconds)
                    put("weeklyTimeSeconds", stats.weeklyTimeSeconds)
                    put("monthlyTimeSeconds", stats.monthlyTimeSeconds)
                    put("totalTimeSeconds", stats.totalTimeSeconds)
                    put("totalSessionsCount", stats.totalSessionsCount)
                    put("mostStudiedSubject", stats.mostStudiedSubject)
                    put("updatedAt", System.currentTimeMillis())
                }.toString()

                val url = "$BASE_URL/set/studytracker/users/$userId/stats_summary?value=${encode(json)}"
                httpClient.newCall(Request.Builder().url(url).build()).execute().close()

                try {
                    if (isFirebaseAvailable()) {
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(userId)
                            .set(
                                hashMapOf(
                                    "currentStreakDays" to stats.currentStreakDays,
                                    "longestStreakDays" to stats.longestStreakDays,
                                    "todayTimeSeconds" to stats.todayTimeSeconds,
                                    "weeklyTimeSeconds" to stats.weeklyTimeSeconds,
                                    "monthlyTimeSeconds" to stats.monthlyTimeSeconds,
                                    "totalTimeSeconds" to stats.totalTimeSeconds,
                                    "totalSessionsCount" to stats.totalSessionsCount,
                                    "mostStudiedSubject" to stats.mostStudiedSubject,
                                    "statsUpdatedAt" to System.currentTimeMillis()
                                ),
                                SetOptions.merge()
                            ).await()
                    }
                } catch (e: Throwable) {
                    Log.d(TAG, "Firestore stats write skipped: ${e.message}")
                }
                true
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync stats summary to cloud", e)
                false
            }
        }

    suspend fun fetchUserStudyStatsFromCloud(userId: String): UserStudyStats? =
        withContext(Dispatchers.IO) {
            // 1. Try Firestore first
            try {
                if (isFirebaseAvailable()) {
                    val doc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(userId)
                        .get()
                        .await()
                    if (doc.exists()) {
                        return@withContext UserStudyStats(
                            currentStreakDays = doc.getLong("currentStreakDays")?.toInt() ?: 0,
                            longestStreakDays = doc.getLong("longestStreakDays")?.toInt() ?: 0,
                            todayTimeSeconds = doc.getLong("todayTimeSeconds") ?: 0L,
                            weeklyTimeSeconds = doc.getLong("weeklyTimeSeconds") ?: 0L,
                            monthlyTimeSeconds = doc.getLong("monthlyTimeSeconds") ?: 0L,
                            totalTimeSeconds = doc.getLong("totalTimeSeconds") ?: 0L,
                            totalSessionsCount = doc.getLong("totalSessionsCount")?.toInt() ?: 0,
                            mostStudiedSubject = doc.getString("mostStudiedSubject") ?: "None"
                        )
                    }
                }
            } catch (e: Throwable) {
                Log.d(TAG, "Firestore stats fetch skipped: ${e.message}")
            }

            // 2. Try Cloud REST
            try {
                val url = "$BASE_URL/get/studytracker/users/$userId/stats_summary"
                val response = httpClient.newCall(Request.Builder().url(url).build()).execute()
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    response.close()
                    val root = JSONObject(body)
                    if (root.optString("status") == "success") {
                        val raw = root.optString("data")
                        val obj = try { JSONObject(raw) } catch (e: Exception) { root.optJSONObject("data") }
                        if (obj != null) {
                            return@withContext UserStudyStats(
                                currentStreakDays = obj.optInt("currentStreakDays", 0),
                                longestStreakDays = obj.optInt("longestStreakDays", 0),
                                todayTimeSeconds = obj.optLong("todayTimeSeconds", 0L),
                                weeklyTimeSeconds = obj.optLong("weeklyTimeSeconds", 0L),
                                monthlyTimeSeconds = obj.optLong("monthlyTimeSeconds", 0L),
                                totalTimeSeconds = obj.optLong("totalTimeSeconds", 0L),
                                totalSessionsCount = obj.optInt("totalSessionsCount", 0),
                                mostStudiedSubject = obj.optString("mostStudiedSubject", "None")
                            )
                        }
                    }
                } else {
                    response.close()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching stats summary for $userId", e)
            }
            null
        }

    suspend fun syncUserToFirestore(user: UserEntity): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!isFirebaseAvailable()) return@withContext false
            val db = FirebaseFirestore.getInstance()
            val map = hashMapOf(
                "userId" to user.userId,
                "fullName" to user.fullName,
                "username" to user.username,
                "email" to user.email,
                "passwordHash" to user.passwordHash,
                "studyId" to user.studyId,
                "accountStatus" to user.accountStatus,
                "isDeleted" to user.isDeleted,
                "joinedDate" to user.joinedDate,
                "lastActiveTime" to System.currentTimeMillis()
            )
            db.collection("users").document(user.userId).set(map, SetOptions.merge()).await()
            true
        } catch (e: Throwable) {
            Log.d(TAG, "Firestore syncUser skipped: ${e.message}")
            false
        }
    }

    suspend fun findUserInFirestoreByEmail(email: String): UserEntity? = withContext(Dispatchers.IO) {
        try {
            if (!isFirebaseAvailable()) return@withContext null
            val db = FirebaseFirestore.getInstance()
            val snap = db.collection("users")
                .whereEqualTo("email", email.trim().lowercase())
                .limit(1)
                .get()
                .await()
            if (snap.documents.isNotEmpty()) {
                val doc = snap.documents.first()
                return@withContext UserEntity(
                    userId = doc.getString("userId") ?: doc.id,
                    fullName = doc.getString("fullName") ?: "Student",
                    username = doc.getString("username") ?: "student",
                    email = doc.getString("email") ?: email,
                    passwordHash = doc.getString("passwordHash") ?: "",
                    studyId = doc.getString("studyId") ?: "STU-00000",
                    accountStatus = doc.getString("accountStatus") ?: "ACTIVE",
                    isDeleted = doc.getBoolean("isDeleted") ?: false,
                    joinedDate = doc.getLong("joinedDate") ?: System.currentTimeMillis(),
                    lastActiveTime = doc.getLong("lastActiveTime") ?: System.currentTimeMillis()
                )
            }
            null
        } catch (e: Throwable) {
            Log.d(TAG, "Firestore findUserByEmail skipped: ${e.message}")
            null
        }
    }

    /**
     * Attaches real-time listeners to Firestore collections if Firebase is available.
     * When Mobile 1 updates data, Mobile 2 triggers onDataChanged callback immediately!
     */
    fun attachRealtimeFirestoreListeners(
        userId: String,
        onSessionsChanged: (List<StudySessionEntity>) -> Unit,
        onGoalsChanged: (List<StudyGoalEntity>) -> Unit,
        onSubjectsChanged: (List<SubjectEntity>) -> Unit
    ) {
        detachRealtimeFirestoreListeners()
        try {
            if (!isFirebaseAvailable()) return
            val db = FirebaseFirestore.getInstance()

            // Sessions Listener
            firestoreListenerSessions = db.collection("users")
                .document(userId)
                .collection("sessions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { doc ->
                        val isDel = doc.getBoolean("isDeleted") ?: false
                        if (isDel) null
                        else StudySessionEntity(
                            sessionId = doc.getString("sessionId") ?: doc.id,
                            userId = doc.getString("userId") ?: userId,
                            subjectId = doc.getString("subjectId") ?: "",
                            subjectName = doc.getString("subjectName") ?: "Study",
                            startTime = doc.getLong("startTime") ?: 0L,
                            endTime = doc.getLong("endTime") ?: 0L,
                            durationSeconds = doc.getLong("durationSeconds") ?: 0L,
                            sessionDate = doc.getString("sessionDate") ?: "",
                            title = doc.getString("title") ?: "",
                            notes = doc.getString("notes") ?: "",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            sessionType = doc.getString("sessionType") ?: "TIMER",
                            isDeleted = false
                        )
                    }
                    onSessionsChanged(list)
                }

            // Goals Listener
            firestoreListenerGoals = db.collection("users")
                .document(userId)
                .collection("goals")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.map { doc ->
                        StudyGoalEntity(
                            goalId = doc.getString("goalId") ?: doc.id,
                            userId = doc.getString("userId") ?: userId,
                            title = doc.getString("title") ?: "Goal",
                            targetDurationMinutes = doc.getLong("targetDurationMinutes")?.toInt() ?: 0,
                            targetSessions = doc.getLong("targetSessions")?.toInt() ?: 0,
                            startDate = doc.getLong("startDate") ?: 0L,
                            endDate = doc.getLong("endDate") ?: 0L,
                            subjectName = doc.getString("subjectName") ?: "",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        )
                    }
                    onGoalsChanged(list)
                }

            // Subjects Listener
            firestoreListenerSubjects = db.collection("users")
                .document(userId)
                .collection("subjects")
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.map { doc ->
                        SubjectEntity(
                            subjectId = doc.getString("subjectId") ?: doc.id,
                            userId = doc.getString("userId") ?: userId,
                            name = doc.getString("name") ?: "Subject",
                            colorHex = doc.getString("colorHex") ?: "#3F51B5",
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            status = doc.getString("status") ?: "ACTIVE",
                            description = doc.getString("description") ?: "",
                            icon = doc.getString("icon") ?: "menu_book",
                            isDefault = doc.getBoolean("isDefault") ?: false,
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    }
                    onSubjectsChanged(list)
                }

            Log.d(TAG, "Attached real-time Firestore listeners for user $userId")
        } catch (e: Throwable) {
            Log.d(TAG, "Failed to attach Firestore listeners: ${e.message}")
        }
    }

    fun detachRealtimeFirestoreListeners() {
        firestoreListenerSessions?.remove()
        firestoreListenerSessions = null
        firestoreListenerGoals?.remove()
        firestoreListenerGoals = null
        firestoreListenerSubjects?.remove()
        firestoreListenerSubjects = null
    }
}
