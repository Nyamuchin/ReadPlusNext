package com.readplusnext.ui.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.readplusnext.domain.model.AppSettings
import com.readplusnext.domain.model.Category
import com.readplusnext.domain.model.CategoryType
import com.readplusnext.domain.model.Comic
import com.readplusnext.domain.model.ImportSummary
import com.readplusnext.domain.model.Video
import com.readplusnext.domain.repository.CategoryRepository
import com.readplusnext.domain.repository.ComicRepository
import com.readplusnext.domain.repository.SettingsRepository
import com.readplusnext.domain.repository.VideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class HomeTab { COMIC, VIDEO }

data class HomeUiState(
    val comics: List<Comic> = emptyList(),
    val videos: List<Video> = emptyList(),
    val comicCategories: List<Category> = emptyList(),
    val videoCategories: List<Category> = emptyList(),
    val selectedComicCategory: Long? = null,
    val selectedVideoCategory: Long? = null,
    val selectedTab: HomeTab = HomeTab.COMIC,
    val settings: AppSettings = AppSettings(),
    val importing: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val comicRepository: ComicRepository,
    private val videoRepository: VideoRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val selectedComicCategory = MutableStateFlow<Long?>(null)
    private val selectedVideoCategory = MutableStateFlow<Long?>(null)
    private val selectedTab = MutableStateFlow(HomeTab.COMIC)
    private val importing = MutableStateFlow(false)
    private val message = MutableStateFlow<String?>(null)

    private data class Content(
        val comics: List<Comic>,
        val videos: List<Video>,
        val comicCategories: List<Category>,
        val videoCategories: List<Category>,
    )

    private val comicsFlow: Flow<List<Comic>> = selectedComicCategory.flatMapLatest { id ->
        if (id == null) comicRepository.observeComics() else comicRepository.observeComicsByCategory(id)
    }

    private val videosFlow: Flow<List<Video>> = selectedVideoCategory.flatMapLatest { id ->
        if (id == null) videoRepository.observeVideos() else videoRepository.observeVideosByCategory(id)
    }

    private val contentFlow: Flow<Content> = combine(
        comicsFlow,
        videosFlow,
        categoryRepository.observeCategories(CategoryType.COMIC),
        categoryRepository.observeCategories(CategoryType.VIDEO),
    ) { comics, videos, comicCategories, videoCategories ->
        Content(comics, videos, comicCategories, videoCategories)
    }

    val uiState: StateFlow<HomeUiState> = combine(
        contentFlow,
        settingsRepository.settings,
        selectedTab,
        importing,
        message,
    ) { content, settings, tab, isImporting, currentMessage ->
        HomeUiState(
            comics = content.comics,
            videos = content.videos,
            comicCategories = content.comicCategories,
            videoCategories = content.videoCategories,
            selectedComicCategory = selectedComicCategory.value,
            selectedVideoCategory = selectedVideoCategory.value,
            selectedTab = tab,
            settings = settings,
            importing = isImporting,
            message = currentMessage,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun selectTab(tab: HomeTab) {
        selectedTab.value = tab
    }

    fun selectComicCategory(id: Long?) {
        selectedComicCategory.value = id
    }

    fun selectVideoCategory(id: Long?) {
        selectedVideoCategory.value = id
    }

    fun setComicColumns(columns: Int) {
        viewModelScope.launch { settingsRepository.setComicGridColumns(columns) }
    }

    fun setVideoColumns(columns: Int) {
        viewModelScope.launch { settingsRepository.setVideoGridColumns(columns) }
    }

    fun createCategory(name: String, type: CategoryType) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching { categoryRepository.createCategory(trimmed, type) }
                .onFailure { message.value = "新建分类失败" }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            runCatching { categoryRepository.deleteCategory(category.id) }
                .onSuccess {
                    if (selectedComicCategory.value == category.id) selectedComicCategory.value = null
                    if (selectedVideoCategory.value == category.id) selectedVideoCategory.value = null
                    message.value = "已删除分类「${category.name}」"
                }
                .onFailure { message.value = "删除分类失败" }
        }
    }

    fun renameComic(id: Long, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching { comicRepository.renameComic(id, trimmed) }
                .onFailure { message.value = "重命名失败" }
        }
    }

    fun renameVideo(id: Long, title: String) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            runCatching { videoRepository.renameVideo(id, trimmed) }
                .onFailure { message.value = "重命名失败" }
        }
    }

    fun deleteComic(id: Long) {
        viewModelScope.launch {
            runCatching { comicRepository.deleteComic(id) }
                .onSuccess { message.value = "已删除漫画" }
                .onFailure { message.value = "删除失败" }
        }
    }

    fun deleteVideo(id: Long) {
        viewModelScope.launch {
            runCatching { videoRepository.deleteVideo(id) }
                .onSuccess { message.value = "已删除视频" }
                .onFailure { message.value = "删除失败" }
        }
    }

    fun setComicCategories(comicId: Long, ids: Set<Long>) {
        viewModelScope.launch {
            runCatching {
                val current = comicRepository.categoryIds(comicId).toSet()
                (ids - current).forEach { comicRepository.addToCategory(comicId, it) }
                (current - ids).forEach { comicRepository.removeFromCategory(comicId, it) }
            }.onFailure { message.value = "更新分类失败" }
        }
    }

    fun setVideoCategories(videoId: Long, ids: Set<Long>) {
        viewModelScope.launch {
            runCatching {
                val current = videoRepository.categoryIds(videoId).toSet()
                (ids - current).forEach { videoRepository.addToCategory(videoId, it) }
                (current - ids).forEach { videoRepository.removeFromCategory(videoId, it) }
            }.onFailure { message.value = "更新分类失败" }
        }
    }

    suspend fun comicCategoryIds(comicId: Long): Set<Long> =
        runCatching { comicRepository.categoryIds(comicId).toSet() }.getOrDefault(emptySet())

    suspend fun videoCategoryIds(videoId: Long): Set<Long> =
        runCatching { videoRepository.categoryIds(videoId).toSet() }.getOrDefault(emptySet())

    fun importComics(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            importing.value = true
            message.value = "正在导入 ${uris.size} 个漫画文件…"
            val summary = runCatching { comicRepository.importComics(uris) }
                .getOrElse { ImportSummary(imported = 0, failed = uris.size) }
            message.value = importMessage("漫画", summary)
            importing.value = false
        }
    }

    fun importComicFolder(treeUri: Uri) {
        viewModelScope.launch {
            importing.value = true
            message.value = "正在扫描文件夹…"
            val summary = runCatching { comicRepository.importComicFolder(treeUri) }
                .getOrElse { ImportSummary(imported = 0, failed = 1) }
            message.value = importMessage("漫画", summary)
            importing.value = false
        }
    }

    fun importVideos(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            importing.value = true
            message.value = "正在导入 ${uris.size} 个视频文件…"
            val summary = runCatching { videoRepository.importVideos(uris) }
                .getOrElse { ImportSummary(imported = 0, failed = uris.size) }
            message.value = importMessage("视频", summary)
            importing.value = false
        }
    }

    fun importVideoFolder(treeUri: Uri) {
        viewModelScope.launch {
            importing.value = true
            message.value = "正在扫描文件夹…"
            val summary = runCatching { videoRepository.importVideoFolder(treeUri) }
                .getOrElse { ImportSummary(imported = 0, failed = 1) }
            message.value = importMessage("视频", summary)
            importing.value = false
        }
    }

    fun clearMessage() {
        message.value = null
    }

    private fun importMessage(label: String, summary: ImportSummary): String = when {
        summary.imported == 0 && summary.failed == 0 -> "没有发现可导入的$label"
        summary.imported == 0 -> "导入失败：${summary.failed} 个$label 文件无法识别"
        summary.failed > 0 -> "已导入 ${summary.imported} 个$label，${summary.failed} 个失败"
        else -> "已导入 ${summary.imported} 个$label"
    }
}
