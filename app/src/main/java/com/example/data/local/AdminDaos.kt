package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AdminDao {
    @Query("SELECT * FROM admins WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getAdminByEmail(email: String): AdminEntity?

    @Query("SELECT * FROM admins WHERE adminUid = :adminUid LIMIT 1")
    suspend fun getAdminById(adminUid: String): AdminEntity?

    @Query("SELECT * FROM admins ORDER BY createdAt ASC")
    fun getAllAdminsFlow(): Flow<List<AdminEntity>>

    @Query("SELECT * FROM admins ORDER BY createdAt ASC")
    suspend fun getAllAdmins(): List<AdminEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAdmin(admin: AdminEntity)

    @Update
    suspend fun updateAdmin(admin: AdminEntity)

    @Delete
    suspend fun deleteAdmin(admin: AdminEntity)

    @Query("SELECT COUNT(*) FROM admins")
    suspend fun countAdmins(): Int
}

@Dao
interface AdminAuditLogDao {
    @Query("SELECT * FROM admin_audit_logs ORDER BY createdAt DESC")
    fun getAllAuditLogsFlow(): Flow<List<AdminAuditLogEntity>>

    @Query("SELECT * FROM admin_audit_logs ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentAuditLogs(limit: Int = 100): List<AdminAuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AdminAuditLogEntity)

    @Query("SELECT * FROM admin_audit_logs WHERE action = :action ORDER BY createdAt DESC")
    suspend fun getAuditLogsByAction(action: String): List<AdminAuditLogEntity>
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements ORDER BY createdDate DESC")
    fun getAllAnnouncementsFlow(): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements WHERE status = 'PUBLISHED' AND (displayLocation = :location OR displayLocation = 'BOTH') ORDER BY createdDate DESC")
    fun getPublishedAnnouncementsFlow(location: String): Flow<List<AnnouncementEntity>>

    @Query("SELECT * FROM announcements WHERE announcementId = :id LIMIT 1")
    suspend fun getAnnouncementById(id: String): AnnouncementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)

    @Update
    suspend fun updateAnnouncement(announcement: AnnouncementEntity)

    @Delete
    suspend fun deleteAnnouncement(announcement: AnnouncementEntity)
}

@Dao
interface ReportDao {
    @Query("SELECT * FROM user_reports ORDER BY createdDate DESC")
    fun getAllReportsFlow(): Flow<List<ReportEntity>>

    @Query("SELECT * FROM user_reports WHERE status = :status ORDER BY createdDate DESC")
    fun getReportsByStatusFlow(status: String): Flow<List<ReportEntity>>

    @Query("SELECT * FROM user_reports WHERE reportId = :id LIMIT 1")
    suspend fun getReportById(id: String): ReportEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: ReportEntity)

    @Update
    suspend fun updateReport(report: ReportEntity)

    @Delete
    suspend fun deleteReport(report: ReportEntity)

    @Query("SELECT COUNT(*) FROM user_reports WHERE status = 'OPEN' OR status = 'UNDER_REVIEW'")
    fun getOpenReportsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM user_reports WHERE status = 'OPEN' OR status = 'UNDER_REVIEW'")
    suspend fun countOpenReports(): Int
}

@Dao
interface StudyChallengeDao {
    @Query("SELECT * FROM study_challenges ORDER BY createdDate DESC")
    fun getAllChallengesFlow(): Flow<List<StudyChallengeEntity>>

    @Query("SELECT * FROM study_challenges WHERE status = 'ACTIVE' ORDER BY startDate ASC")
    fun getActiveChallengesFlow(): Flow<List<StudyChallengeEntity>>

    @Query("SELECT * FROM study_challenges WHERE challengeId = :id LIMIT 1")
    suspend fun getChallengeById(id: String): StudyChallengeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChallenge(challenge: StudyChallengeEntity)

    @Update
    suspend fun updateChallenge(challenge: StudyChallengeEntity)

    @Delete
    suspend fun deleteChallenge(challenge: StudyChallengeEntity)
}

@Dao
interface AppErrorLogDao {
    @Query("SELECT * FROM app_error_logs ORDER BY timestamp DESC")
    fun getAllErrorLogsFlow(): Flow<List<AppErrorLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErrorLog(error: AppErrorLogEntity)

    @Update
    suspend fun updateErrorLog(error: AppErrorLogEntity)

    @Query("SELECT COUNT(*) FROM app_error_logs WHERE status = 'NEW'")
    fun getUnresolvedErrorsCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM app_error_logs WHERE status = 'NEW'")
    suspend fun countUnresolvedErrors(): Int
}

@Dao
interface AppSettingDao {
    @Query("SELECT * FROM app_settings ORDER BY category, settingKey ASC")
    fun getAllSettingsFlow(): Flow<List<AppSettingEntity>>

    @Query("SELECT * FROM app_settings WHERE settingKey = :key LIMIT 1")
    suspend fun getSetting(key: String): AppSettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: AppSettingEntity)

    @Update
    suspend fun updateSetting(setting: AppSettingEntity)
}
