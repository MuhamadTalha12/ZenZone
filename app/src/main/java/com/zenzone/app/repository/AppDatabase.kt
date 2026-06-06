package com.zenzone.app.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.zenzone.app.model.BlockedAppEntity
import com.zenzone.app.model.ChallengeEntity
import com.zenzone.app.model.FocusGoal
import com.zenzone.app.model.FocusSession

@Database(entities = [FocusGoal::class, FocusSession::class, ChallengeEntity::class, BlockedAppEntity::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun focusGoalDao(): FocusGoalDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun blockedAppDao(): BlockedAppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zenzone_database"
                )
                .fallbackToDestructiveMigration(true)
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
