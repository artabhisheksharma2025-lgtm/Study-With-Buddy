package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        AppNotificationEntity::class
    ],
    version = 1,
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "study_tracker_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
