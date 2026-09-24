package com.example.data.model

import androidx.room.Entity

@Entity(
    tableName = "dismissed_announcements",
    primaryKeys = ["userId", "announcementId"]
)
data class DismissedAnnouncementEntity(
    val userId: String,
    val announcementId: String,
    val dismissedAt: Long = System.currentTimeMillis()
)
