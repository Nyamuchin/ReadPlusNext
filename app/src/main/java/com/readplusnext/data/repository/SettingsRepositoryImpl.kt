package com.readplusnext.data.repository

import com.readplusnext.data.preferences.PreferencesManager
import com.readplusnext.domain.model.AppSettings
import com.readplusnext.domain.model.ReadingMode
import com.readplusnext.domain.model.ThemeMode
import com.readplusnext.domain.model.ThemeStyle
import com.readplusnext.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val preferences: PreferencesManager,
) : SettingsRepository {

    override val settings: Flow<AppSettings> = preferences.settings

    override suspend fun setThemeStyle(style: ThemeStyle) = preferences.setThemeStyle(style)

    override suspend fun setThemeMode(mode: ThemeMode) = preferences.setThemeMode(mode)

    override suspend fun setReadingMode(mode: ReadingMode) = preferences.setReadingMode(mode)

    override suspend fun setComicGridColumns(columns: Int) = preferences.setComicGridColumns(columns)

    override suspend fun setVideoGridColumns(columns: Int) = preferences.setVideoGridColumns(columns)
}
