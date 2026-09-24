package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*

@Database(
    entities = [
        UserEntity::class,
        SubjectEntity::class,
        StudySessionEntity::class,
        ActiveTimerEntity::class,
        FriendRequestEntity::class,
        FriendshipEntity::class,
        StudyGoalEntity::class,
        AppNotificationEntity::class,
        AdminEntity::class,
        AdminAuditLogEntity::class,
        AnnouncementEntity::class,
        ReportEntity::class,
        StudyChallengeEntity::class,
        AppErrorLogEntity::class,
        AppSettingEntity::class,
        DismissedAnnouncementEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun subjectDao(): SubjectDao
    abstract fun studySessionDao(): StudySessionDao
    abstract fun activeTimerDao(): ActiveTimerDao
    abstract fun friendDao(): FriendDao
    abstract fun studyGoalDao(): StudyGoalDao
    abstract fun notificationDao(): AppNotificationDao
    abstract fun adminDao(): AdminDao
    abstract fun auditLogDao(): AdminAuditLogDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun reportDao(): ReportDao
    abstract fun challengeDao(): StudyChallengeDao
    abstract fun errorLogDao(): AppErrorLogDao
    abstract fun appSettingDao(): AppSettingDao
    abstract fun dismissedAnnouncementDao(): DismissedAnnouncementDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `dismissed_announcements` (
                        `userId` TEXT NOT NULL,
                        `announcementId` TEXT NOT NULL,
                        `dismissedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `announcementId`)
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_tracker_db"
                )
                    .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
