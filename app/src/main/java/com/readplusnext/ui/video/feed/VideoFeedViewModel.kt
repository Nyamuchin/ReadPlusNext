package com.readplusnext.ui.video.feed

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readplusnext.domain.model.Category
import com.readplusnext.domain.model.CategoryType
import com.readplusnext.domain.model.Video
import com.readplusnext.domain.repository.CategoryRepository
import com.readplusnext.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class VideoFeedUiState(
    val videos: List<Video> = emptyList(),
    val startIndex: Int = 0,
    val startPositions: Map<Long, Long> = emptyMap(),
    val categoryNames: Map<Long, List<String>> = emptyMap(),
    val loading: Boolean = true,
)

@HiltViewModel
class VideoFeedViewModel @Inject constructor(
    private val videoRepository: VideoRepository,
    private val categoryRepository: CategoryRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val videoId: Long = checkNotNull(savedStateHandle["videoId"])

    private val _uiState = MutableStateFlow(VideoFeedUiState())
    val uiState: StateFlow<VideoFeedUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val videos = runCatching { videoRepository.observeVideos().first() }.getOrDefault(emptyList())
            val categories: List<Category> = runCatching {
                categoryRepository.getCategories(CategoryType.VIDEO)
            }.getOrDefault(emptyList())
            val categoryById = categories.associateBy { it.id }
            val positions = videos.associate { video ->
                video.id to runCatching {
                    videoRepository.observePlaybackPosition(video.id).first()
                }.getOrDefault(0L)
            }
            val names = videos.associate { video ->
                val ids = runCatching { videoRepository.categoryIds(video.id) }.getOrDefault(emptyList())
                video.id to ids.mapNotNull { categoryById[it]?.name }
            }
            _uiState.value = VideoFeedUiState(
                videos = videos,
                startIndex = videos.indexOfFirst { it.id == videoId }.coerceAtLeast(0),
                startPositions = positions,
                categoryNames = names,
                loading = false,
            )
        }
    }

    fun savePlaybackPosition(videoId: Long, positionMs: Long) {
        viewModelScope.launch {
            runCatching { videoRepository.savePlaybackPosition(videoId, positionMs) }
        }
    }
}
