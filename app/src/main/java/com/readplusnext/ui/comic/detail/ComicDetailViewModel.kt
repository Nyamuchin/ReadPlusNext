package com.readplusnext.ui.comic.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readplusnext.domain.model.Comic
import com.readplusnext.domain.model.ComicPage
import com.readplusnext.domain.repository.ComicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ComicDetailUiState(
    val comic: Comic? = null,
    val pages: List<ComicPage> = emptyList(),
    val busy: Boolean = false,
    val message: String? = null,
    val deleted: Boolean = false,
    val loading: Boolean = true,
)

@HiltViewModel
class ComicDetailViewModel @Inject constructor(
    private val comicRepository: ComicRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val comicId: Long = checkNotNull(savedStateHandle["comicId"])

    private val pages = MutableStateFlow<List<ComicPage>>(emptyList())
    private val busy = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)
    private val deleted = MutableStateFlow(false)
    private val loading = MutableStateFlow(true)

    val uiState: StateFlow<ComicDetailUiState> = combine(
        comicRepository.observeComic(comicId),
        pages,
        busy,
        message,
        loading,
    ) { comic, pageList, isBusy, currentMessage, isLoading ->
        ComicDetailUiState(
            comic = comic,
            pages = pageList,
            busy = isBusy,
            message = currentMessage,
            deleted = deleted.value,
            loading = isLoading,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ComicDetailUiState())

    init {
        viewModelScope.launch {
            val comic = comicRepository.getComic(comicId)
            if (comic != null) {
                pages.value = runCatching { comicRepository.pages(comic) }.getOrDefault(emptyList())
            }
            loading.value = false
        }
    }

    fun rename(title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching { comicRepository.renameComic(comicId, trimmed) }
                .onFailure { message.value = "重命名失败" }
        }
    }

    fun replaceCoverFromPage(pageKey: String) {
        viewModelScope.launch {
            busy.value = true
            val result = runCatching { comicRepository.generateCoverFromPage(comicId, pageKey) }
            message.value = if (result.getOrNull() != null) "封面已更新" else "封面更新失败"
            busy.value = false
        }
    }

    fun replaceCoverFromUri(uri: Uri) {
        viewModelScope.launch {
            busy.value = true
            val result = runCatching { comicRepository.generateCoverFromUri(comicId, uri) }
            message.value = if (result.getOrNull() != null) "封面已更新" else "封面更新失败"
            busy.value = false
        }
    }

    fun delete() {
        viewModelScope.launch {
            runCatching { comicRepository.deleteComic(comicId) }
                .onSuccess { deleted.value = true }
                .onFailure { message.value = "删除失败" }
        }
    }

    fun clearMessage() {
        message.value = null
    }
}
