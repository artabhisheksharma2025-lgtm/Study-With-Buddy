package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    fun getLoggedInUserFlow(): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE isLoggedIn = 1 LIMIT 1")
    suspend fun getLoggedInUser(): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE studyId = :studyId LIMIT 1")
    suspend fun getUserByStudyId(studyId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserById(userId: String): UserEntity?

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun getUserByIdFlow(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE userId IN (:userIds)")
    suspend fun getUsersByIds(userIds: List<String>): List<UserEntity>

    @Query("SELECT * FROM users WHERE userId IN (:userIds)")
    fun getUsersByIdsFlow(userIds: List<String>): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Query("SELECT * FROM users ORDER BY joinedDate DESC")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Delete
    suspend fun deleteUser(user: UserEntity)

    @Query("DELETE FROM users WHERE userId = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("UPDATE users SET isLoggedIn = 0")
    suspend fun logoutAllUsers()

    @Query("UPDATE users SET isLoggedIn = 1 WHERE userId = :userId")
    suspend fun setLoggedInUser(userId: String)
}

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE userId = :userId ORDER BY name ASC")
    fun getSubjectsForUserFlow(userId: String): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE userId = :userId ORDER BY name ASC")
    suspend fun getSubjectsForUser(userId: String): List<SubjectEntity>

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    fun getAllSubjectsFlow(): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects ORDER BY name ASC")
    suspend fun getAllSubjects(): List<SubjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubjects(subjects: List<SubjectEntity>)

    @Update
    suspend fun updateSubject(subject: SubjectEntity)

    @Delete
    suspend fun deleteSubject(subject: SubjectEntity)
}

@Dao
interface StudySessionDao {
    @Query("SELECT * FROM study_sessions WHERE userId = :userId ORDER BY startTime DESC")
    fun getSessionsForUserFlow(userId: String): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE userId = :userId ORDER BY startTime DESC")
    suspend fun getSessionsForUser(userId: String): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    fun getAllSessionsFlow(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions ORDER BY startTime DESC")
    suspend fun getAllSessions(): List<StudySessionEntity>

    @Query("SELECT * FROM study_sessions WHERE sessionId = :sessionId LIMIT 1")
    suspend fun getSessionByIdDirect(sessionId: String): StudySessionEntity?

    @Query("SELECT * FROM study_sessions WHERE userId = :userId AND sessionId = :sessionId LIMIT 1")
    suspend fun getSessionById(userId: String, sessionId: String): StudySessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity)

    @Update
    suspend fun updateSession(session: StudySessionEntity)

    @Delete
    suspend fun deleteSession(session: StudySessionEntity)

    @Query("DELETE FROM study_sessions WHERE sessionId = :sessionId AND userId = :userId")
    suspend fun deleteSessionById(sessionId: String, userId: String)

    @Query("DELETE FROM study_sessions WHERE userId = :userId")
    suspend fun deleteSessionsForUser(userId: String)
}

@Dao
interface ActiveTimerDao {
    @Query("SELECT * FROM active_timer WHERE userId = :userId LIMIT 1")
    fun getActiveTimerFlow(userId: String): Flow<ActiveTimerEntity?>

    @Query("SELECT * FROM active_timer WHERE userId = :userId LIMIT 1")
    suspend fun getActiveTimer(userId: String): ActiveTimerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveActiveTimer(activeTimer: ActiveTimerEntity)

    @Query("DELETE FROM active_timer WHERE userId = :userId")
    suspend fun clearActiveTimer(userId: String)
}

@Dao
interface FriendDao {
    // Friend Requests
    @Query("SELECT * FROM friend_requests WHERE receiverId = :userId AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingReceivedRequestsFlow(userId: String): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE senderId = :userId AND status = 'PENDING' ORDER BY createdAt DESC")
    fun getPendingSentRequestsFlow(userId: String): Flow<List<FriendRequestEntity>>

    @Query("SELECT * FROM friend_requests WHERE (senderId = :userId1 AND receiverId = :userId2) OR (senderId = :userId2 AND receiverId = :userId1) LIMIT 1")
    suspend fun getFriendRequestBetween(userId1: String, userId2: String): FriendRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriendRequest(request: FriendRequestEntity)

    @Update
    suspend fun updateFriendRequest(request: FriendRequestEntity)

    @Query("SELECT COUNT(*) FROM friend_requests WHERE status = 'PENDING'")
    suspend fun countPendingRequests(): Int

    @Query("SELECT * FROM friend_requests ORDER BY createdAt DESC")
    fun getAllFriendRequestsFlow(): Flow<List<FriendRequestEntity>>

    @Delete
    suspend fun deleteFriendRequest(request: FriendRequestEntity)

    @Query("DELETE FROM friend_requests WHERE requestId = :requestId")
    suspend fun deleteFriendRequestById(requestId: String)

    // Friendships
    @Query("SELECT COUNT(*) FROM friendships")
    suspend fun countFriendships(): Int

    @Query("SELECT * FROM friendships WHERE userId1 = :userId OR userId2 = :userId")
    fun getFriendshipsFlow(userId: String): Flow<List<FriendshipEntity>>

    @Query("SELECT * FROM friendships ORDER BY createdAt DESC")
    fun getAllFriendshipsFlow(): Flow<List<FriendshipEntity>>

    @Query("SELECT * FROM friendships WHERE userId1 = :userId OR userId2 = :userId")
    suspend fun getFriendships(userId: String): List<FriendshipEntity>

    @Query("SELECT * FROM friendships WHERE (userId1 = :userId1 AND userId2 = :userId2) OR (userId1 = :userId2 AND userId2 = :userId1) LIMIT 1")
    suspend fun getFriendshipBetween(userId1: String, userId2: String): FriendshipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriendship(friendship: FriendshipEntity)

    @Delete
    suspend fun deleteFriendship(friendship: FriendshipEntity)

    @Query("DELETE FROM friendships WHERE (userId1 = :userId1 AND userId2 = :userId2) OR (userId1 = :userId2 AND userId2 = :userId1)")
    suspend fun removeFriendship(userId1: String, userId2: String)
}

@Dao
interface StudyGoalDao {
    @Query("SELECT * FROM study_goals WHERE userId = :userId ORDER BY createdAt DESC")
    fun getGoalsForUserFlow(userId: String): Flow<List<StudyGoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: StudyGoalEntity)

    @Update
    suspend fun updateGoal(goal: StudyGoalEntity)

    @Delete
    suspend fun deleteGoal(goal: StudyGoalEntity)
}

@Dao
interface AppNotificationDao {
    @Query("SELECT * FROM app_notifications WHERE userId = :userId ORDER BY timestamp DESC")
    fun getNotificationsForUserFlow(userId: String): Flow<List<AppNotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: AppNotificationEntity)

    @Query("UPDATE app_notifications SET isRead = 1 WHERE userId = :userId")
    suspend fun markAllAsRead(userId: String)
}
