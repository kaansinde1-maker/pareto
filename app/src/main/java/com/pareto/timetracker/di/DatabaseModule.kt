package com.pareto.timetracker.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import com.pareto.timetracker.data.db.ActiveTimerDao
import com.pareto.timetracker.data.db.AppDatabase
import com.pareto.timetracker.data.db.CategoryDao
import com.pareto.timetracker.data.db.DailyGoalStatusDao
import com.pareto.timetracker.data.db.EventDao
import com.pareto.timetracker.data.db.GoalDao
import com.pareto.timetracker.data.db.PoolMetaDao
import com.pareto.timetracker.data.db.PoolReadDao
import com.pareto.timetracker.data.db.SessionDao
import com.pareto.timetracker.data.prefs.settingsDataStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /** Veri kaybını önlemek için yıkıcı (destructive) geçiş yok; şema değişince açık migration yazılmalı. */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "pareto.db").build()

    @Provides fun provideCategoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideSessionDao(db: AppDatabase): SessionDao = db.sessionDao()
    @Provides fun provideActiveTimerDao(db: AppDatabase): ActiveTimerDao = db.activeTimerDao()
    @Provides fun provideGoalDao(db: AppDatabase): GoalDao = db.goalDao()
    @Provides fun provideEventDao(db: AppDatabase): EventDao = db.eventDao()
    @Provides fun provideDailyGoalStatusDao(db: AppDatabase): DailyGoalStatusDao = db.dailyGoalStatusDao()
    @Provides fun providePoolMetaDao(db: AppDatabase): PoolMetaDao = db.poolMetaDao()
    @Provides fun providePoolReadDao(db: AppDatabase): PoolReadDao = db.poolReadDao()

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        context.settingsDataStore
}
