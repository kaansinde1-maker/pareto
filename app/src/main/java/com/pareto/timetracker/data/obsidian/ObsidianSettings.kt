package com.pareto.timetracker.data.obsidian

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.documentfile.provider.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Seçilen Obsidian vault klasörü (kalıcı SAF izniyle). */
@Singleton
class ObsidianSettings @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) {
    val treeUri: Flow<Uri?> = dataStore.data.map { prefs -> prefs[KEY_TREE_URI]?.let(Uri::parse) }

    suspend fun currentTreeUri(): Uri? = treeUri.first()

    suspend fun setTreeUri(uri: Uri) {
        val previous = currentTreeUri()
        try {
            context.contentResolver.takePersistableUriPermission(uri, PERMISSION_FLAGS)
        } catch (e: SecurityException) {
            // Sağlayıcı kalıcı izin vermediyse yazma sırasında hata olarak görünür.
        }
        if (previous != null && previous != uri) {
            try {
                context.contentResolver.releasePersistableUriPermission(previous, PERMISSION_FLAGS)
            } catch (e: SecurityException) {
                // önemsiz
            }
        }
        dataStore.edit { it[KEY_TREE_URI] = uri.toString() }
    }

    fun folderName(uri: Uri): String? = DocumentFile.fromTreeUri(context, uri)?.name

    private companion object {
        val KEY_TREE_URI = stringPreferencesKey("obsidian_tree_uri")
        const val PERMISSION_FLAGS =
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    }
}
