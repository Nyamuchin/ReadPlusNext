package com.readplusnext.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.readplusnext.domain.model.AppSettings
import com.readplusnext.domain.model.ReadingMode
import com.readplusnext.domain.model.ThemeMode
import com.readplusnext.domain.model.ThemeStyle
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private const val SETTINGS_STORE_NAME = "readplusnext_settings"

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = SETTINGS_STORE_NAME,
)

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.applicationContext.settingsDataStore

    val settings: Flow<AppSettings> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            AppSettings(
                themeStyle = prefs[Keys.THEME_STYLE].toThemeStyle(),
                themeMode = prefs[Keys.THEME_MODE].toThemeMode(),
                readingMode = prefs[Keys.READING_MODE].toReadingMode(),
                comicGridColumns = AppSettings.clampColumns(
                    prefs[Keys.COMIC_GRID_COLUMNS] ?: AppSettings.DEFAULT_GRID_COLUMNS,
                ),
                videoGridColumns = AppSettings.clampColumns(
                    prefs[Keys.VIDEO_GRID_COLUMNS] ?: AppSettings.DEFAULT_GRID_COLUMNS,
                ),
            )
        }

    suspend fun setThemeStyle(style: ThemeStyle) {
        dataStore.edit { it[Keys.THEME_STYLE] = style.name }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setReadingMode(mode: ReadingMode) {
        dataStore.edit { it[Keys.READING_MODE] = mode.name }
    }

    suspend fun setComicGridColumns(columns: Int) {
        dataStore.edit { it[Keys.COMIC_GRID_COLUMNS] = AppSettings.clampColumns(columns) }
    }

    suspend fun setVideoGridColumns(columns: Int) {
        dataStore.edit { it[Keys.VIDEO_GRID_COLUMNS] = AppSettings.clampColumns(columns) }
    }

    fun observePlaybackPosition(videoId: Long): Flow<Long> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs -> prefs[Keys.playbackPosition(videoId)] ?: 0L }

    suspend fun savePlaybackPosition(videoId: Long, positionMs: Long) {
        dataStore.edit { it[Keys.playbackPosition(videoId)] = positionMs.coerceAtLeast(0L) }
    }

    suspend fun clearPlaybackPosition(videoId: Long) {
        dataStore.edit { it.remove(Keys.playbackPosition(videoId)) }
    }

    private object Keys {
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val READING_MODE = stringPreferencesKey("reading_mode")
        val COMIC_GRID_COLUMNS = intPreferencesKey("comic_grid_columns")
        val VIDEO_GRID_COLUMNS = intPreferencesKey("video_grid_columns")
        fun playbackPosition(videoId: Long) = longPreferencesKey("video_position_$videoId")
    }
}

private fun String?.toThemeStyle(): ThemeStyle =
    ThemeStyle.values().firstOrNull { it.name == this } ?: ThemeStyle.MATERIAL

private fun String?.toThemeMode(): ThemeMode =
    ThemeMode.values().firstOrNull { it.name == this } ?: ThemeMode.SYSTEM

private fun String?.toReadingMode(): ReadingMode =
    ReadingMode.values().firstOrNull { it.name == this } ?: ReadingMode.LEFT_TO_RIGHT
