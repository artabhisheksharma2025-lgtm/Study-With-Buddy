package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DismissedAnnouncementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DismissedAnnouncementDao {

    @Query("SELECT announcementId FROM dismissed_announcements WHERE userId = :userId")
    fun getDismissedAnnouncementIdsFlow(userId: String): Flow<List<String>>

    @Query("SELECT announcementId FROM dismissed_announcements WHERE userId = :userId")
    suspend fun getDismissedAnnouncementIds(userId: String): List<String>

    @Query("SELECT announcementId FROM dismissed_announcements")
    fun getAllDismissedAnnouncementIdsFlow(): Flow<List<String>>

    @Query("SELECT announcementId FROM dismissed_announcements")
    suspend fun getAllDismissedAnnouncementIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDismissedAnnouncement(dismissed: DismissedAnnouncementEntity)

    @Query("DELETE FROM dismissed_announcements WHERE announcementId = :announcementId")
    suspend fun deleteByAnnouncementId(announcementId: String)
}
