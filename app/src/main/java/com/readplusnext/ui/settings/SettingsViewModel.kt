package com.readplusnext.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readplusnext.domain.model.AppSettings
import com.readplusnext.domain.model.BackupProgress
import com.readplusnext.domain.model.ReadingMode
import com.readplusnext.domain.model.ThemeMode
import com.readplusnext.domain.model.ThemeStyle
import com.readplusnext.domain.repository.BackupRepository
import com.readplusnext.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val backup: BackupProgress = BackupProgress(),
    val message: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
) : ViewModel() {

    private val message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        backupRepository.progress,
        message,
    ) { settings, progress, currentMessage ->
        SettingsUiState(settings = settings, backup = progress, message = currentMessage)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setThemeStyle(style: ThemeStyle) {
        viewModelScope.launch { runCatching { settingsRepository.setThemeStyle(style) } }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { runCatching { settingsRepository.setThemeMode(mode) } }
    }

    fun setReadingMode(mode: ReadingMode) {
        viewModelScope.launch { runCatching { settingsRepository.setReadingMode(mode) } }
    }

    fun setComicColumns(columns: Int) {
        viewModelScope.launch { runCatching { settingsRepository.setComicGridColumns(columns) } }
    }

    fun setVideoColumns(columns: Int) {
        viewModelScope.launch { runCatching { settingsRepository.setVideoGridColumns(columns) } }
    }

    fun exportBackup(toUri: Uri) {
        viewModelScope.launch {
            backupRepository.export(toUri)
                .onSuccess { message.value = "备份已导出" }
                .onFailure { message.value = "备份失败：${it.message}" }
        }
    }

    fun importBackup(fromUri: Uri) {
        viewModelScope.launch {
            backupRepository.import(fromUri)
                .onSuccess { message.value = "备份已恢复" }
                .onFailure { message.value = "恢复失败：${it.message}" }
        }
    }

    fun clearMessage() {
        message.value = null
    }
}
