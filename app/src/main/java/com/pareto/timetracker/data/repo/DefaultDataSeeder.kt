package com.pareto.timetracker.data.repo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.pareto.timetracker.R
import com.pareto.timetracker.data.db.CategoryDao
import com.pareto.timetracker.data.db.CategoryEntity
import com.pareto.timetracker.domain.Tag
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Varsayılan kategorileri sadece ilk açılışta, bir kez ekler. Sonradan silinseler/arşivlense de geri gelmez. */
@Singleton
class DefaultDataSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
    private val categoryDao: CategoryDao,
) {
    suspend fun seedIfNeeded() {
        if (dataStore.data.first()[SEEDED_KEY] == true) return

        val now = System.currentTimeMillis()
        val defaults = listOf(
            Triple(R.string.default_category_game, Tag.CONSUMPTION, 1),
            Triple(R.string.default_category_youtube, Tag.CONSUMPTION, 2),
            Triple(R.string.default_category_reading, Tag.INVESTMENT, 1),
            Triple(R.string.default_category_sport, Tag.INVESTMENT, 2),
            Triple(R.string.default_category_notes, Tag.INVESTMENT, 0),
        )
        categoryDao.insertAllIgnoringConflicts(
            defaults.mapIndexed { i, (nameRes, tag, colorIndex) ->
                val name = context.getString(nameRes)
                CategoryEntity(
                    name = name,
                    nameKey = CategoryRepository.nameKey(name),
                    tag = tag,
                    colorIndex = colorIndex,
                    createdAt = now + i, // listede varsayılan sırayı korur
                )
            },
        )
        dataStore.edit { it[SEEDED_KEY] = true }
    }

    private companion object {
        val SEEDED_KEY = booleanPreferencesKey("default_categories_seeded")
    }
}
