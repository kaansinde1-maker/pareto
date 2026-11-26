package com.pareto.timetracker.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        CategoryEntity::class,
        SessionEntity::class,
        ActiveTimerEntity::class,
        GoalEntity::class,
        EventEntity::class,
        DailyGoalStatusEntity::class,
        PoolMetaEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun sessionDao(): SessionDao
    abstract fun activeTimerDao(): ActiveTimerDao
    abstract fun goalDao(): GoalDao
    abstract fun eventDao(): EventDao
    abstract fun dailyGoalStatusDao(): DailyGoalStatusDao
    abstract fun poolMetaDao(): PoolMetaDao
    abstract fun poolReadDao(): PoolReadDao
}
