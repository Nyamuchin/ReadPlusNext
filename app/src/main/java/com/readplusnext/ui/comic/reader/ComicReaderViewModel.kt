package com.readplusnext.ui.comic.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readplusnext.domain.model.Comic
import com.readplusnext.domain.model.ComicPage
import com.readplusnext.domain.model.ReadingMode
import com.readplusnext.domain.repository.ComicRepository
import com.readplusnext.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReaderUiState(
    val comic: Comic? = null,
    val pages: List<ComicPage> = emptyList(),
    val readingMode: ReadingMode = ReadingMode.LEFT_TO_RIGHT,
    val startPage: Int = 0,
    val loading: Boolean = true,
)

@HiltViewModel
class ComicReaderViewModel @Inject constructor(
    private val comicRepository: ComicRepository,
    private val settingsRepository: SettingsRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val comicId: Long = checkNotNull(savedStateHandle["comicId"])
    private val requestedPage: Int = savedStateHandle.get<Int>("page") ?: -1

    private val comic = MutableStateFlow<Comic?>(null)
    private val pages = MutableStateFlow<List<ComicPage>>(emptyList())
    private val startPage = MutableStateFlow(0)
    private val loading = MutableStateFlow(true)
    private var lastSavedPage = -1

    val uiState: StateFlow<ReaderUiState> = combine(
        comic,
        pages,
        settingsRepository.settings,
        startPage,
        loading,
    ) { currentComic, pageList, settings, initialPage, isLoading ->
        ReaderUiState(
            comic = currentComic,
            pages = pageList,
            readingMode = settings.readingMode,
            startPage = initialPage,
            loading = isLoading,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReaderUiState())

    init {
        viewModelScope.launch {
            val loaded = comicRepository.getComic(comicId)
            if (loaded != null) {
                val pageList = runCatching { comicRepository.pages(loaded) }.getOrDefault(emptyList())
                val lastIndex = (pageList.size - 1).coerceAtLeast(0)
                val initial = if (requestedPage >= 0) {
                    requestedPage.coerceIn(0, lastIndex)
                } else {
                    loaded.lastReadPage.coerceIn(0, lastIndex)
                }
                comic.value = loaded
                pages.value = pageList
                startPage.value = initial
                lastSavedPage = initial
                saveProgress(initial)
            }
            loading.value = false
        }
    }

    fun onPageChanged(page: Int) {
        if (page == lastSavedPage) return
        lastSavedPage = page
        saveProgress(page)
    }

    fun cycleReadingMode() {
        viewModelScope.launch {
            val current = uiState.value.readingMode
            val next = when (current) {
                ReadingMode.LEFT_TO_RIGHT -> ReadingMode.RIGHT_TO_LEFT
                ReadingMode.RIGHT_TO_LEFT -> ReadingMode.VERTICAL_SCROLL
                ReadingMode.VERTICAL_SCROLL -> ReadingMode.LEFT_TO_RIGHT
            }
            runCatching { settingsRepository.setReadingMode(next) }
        }
    }

    private fun saveProgress(page: Int) {
        viewModelScope.launch {
            runCatching { comicRepository.saveProgress(comicId, page) }
        }
    }
}
