package com.readplusnext.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readplusnext.domain.model.Category
import com.readplusnext.domain.model.CategoryType
import com.readplusnext.domain.model.Comic
import com.readplusnext.domain.model.Video
import com.readplusnext.ui.util.comicProgressLabel
import com.readplusnext.ui.util.formatDuration
import com.readplusnext.ui.widget.CategoryPickerDialog
import com.readplusnext.ui.widget.ConfirmDialog
import com.readplusnext.ui.widget.GridColumnsControl
import com.readplusnext.ui.widget.InputDialog
import com.readplusnext.ui.widget.MediaGrid
import com.readplusnext.ui.widget.OptionListDialog

private val ComicMimeTypes = arrayOf(
    "application/zip",
    "application/x-cbz",
    "application/x-cbr",
    "application/vnd.comicbook-rar",
    "application/x-rar-compressed",
    "application/x-7z-compressed",
    "application/pdf",
    "application/epub+zip",
    "application/x-mobipocket-ebook",
    "application/vnd.amazon.ebook",
    "application/octet-stream",
)

private val VideoMimeTypes = arrayOf("video/*")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onOpenComic: (Long) -> Unit,
    onOpenVideo: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    comicCoverModifier: @Composable (Long) -> Modifier = { Modifier },
    videoCoverModifier: @Composable (Long) -> Modifier = { Modifier },
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showColumnsSheet by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<HomeDialog?>(null) }
    val context = LocalContext.current

    val comicPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            viewModel.importComics(uris)
        }
    }
    val comicFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> if (uri != null) viewModel.importComicFolder(uri) }
    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                runCatching {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            viewModel.importVideos(uris)
        }
    }
    val videoFolderPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri -> if (uri != null) viewModel.importVideoFolder(uri) }

    LaunchedEffect(state.message) {
        val message = state.message
        if (message != null) {
            snackbarHostState.showSnackbar(message)
            viewModel.clearMessage()
        }
    }

    val pagerState = rememberPagerState(initialPage = state.selectedTab.ordinal) { HomeTab.entries.size }

    LaunchedEffect(state.selectedTab) {
        if (pagerState.currentPage != state.selectedTab.ordinal) {
            pagerState.animateScrollToPage(state.selectedTab.ordinal)
        }
    }
    LaunchedEffect(pagerState.settledPage) {
        val tab = HomeTab.entries[pagerState.settledPage]
        if (tab != state.selectedTab) viewModel.selectTab(tab)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
        ) {
            HomeHeader(onOpenSettings = onOpenSettings)
            TabSwitcher(
                selected = state.selectedTab,
                onSelect = viewModel::selectTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )
            if (state.importing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1,
            ) { page ->
                when (HomeTab.entries[page]) {
                    HomeTab.COMIC -> ComicLibraryPage(
                        comics = state.comics,
                        categories = state.comicCategories,
                        selectedCategoryId = state.selectedComicCategory,
                        columns = state.settings.comicGridColumns,
                        onSelectCategory = viewModel::selectComicCategory,
                        onCreateCategory = { dialog = HomeDialog.NewCategory(CategoryType.COMIC) },
                        onDeleteCategory = { dialog = HomeDialog.DeleteCategory(it) },
                        onOpen = onOpenComic,
                        onLongPress = { dialog = HomeDialog.ComicMenu(it) },
                        onImport = { comicPicker.launch(ComicMimeTypes) },
                        coverModifier = comicCoverModifier,
                    )

                    HomeTab.VIDEO -> VideoLibraryPage(
                        videos = state.videos,
                        categories = state.videoCategories,
                        selectedCategoryId = state.selectedVideoCategory,
                        columns = state.settings.videoGridColumns,
                        onSelectCategory = viewModel::selectVideoCategory,
                        onCreateCategory = { dialog = HomeDialog.NewCategory(CategoryType.VIDEO) },
                        onDeleteCategory = { dialog = HomeDialog.DeleteCategory(it) },
                        onOpen = onOpenVideo,
                        onLongPress = { dialog = HomeDialog.VideoMenu(it) },
                        onImport = { videoPicker.launch(VideoMimeTypes) },
                        coverModifier = videoCoverModifier,
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 96.dp),
        )

        HomeBottomBar(
            onImport = {
                if (state.selectedTab == HomeTab.COMIC) {
                    comicPicker.launch(ComicMimeTypes)
                } else {
                    videoPicker.launch(VideoMimeTypes)
                }
            },
            onImportFolder = {
                if (state.selectedTab == HomeTab.COMIC) {
                    comicFolderPicker.launch(null)
                } else {
                    videoFolderPicker.launch(null)
                }
            },
            onLayout = { showColumnsSheet = true },
            onSettings = onOpenSettings,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
        )
    }

    HomeDialogs(
        dialog = dialog,
        state = state,
        viewModel = viewModel,
        onDismiss = { dialog = null },
        onReplace = { dialog = it },
    )

    if (showColumnsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showColumnsSheet = false },
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                Text("首页布局", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "漫画和视频可以分别设置每行显示的列数（2–5）。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(20.dp))
                GridColumnsControl(
                    comicColumns = state.settings.comicGridColumns,
                    videoColumns = state.settings.videoGridColumns,
                    onComicColumnsChange = viewModel::setComicColumns,
                    onVideoColumnsChange = viewModel::setVideoColumns,
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "ReadPlusNext",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "设置")
            }
        }
        Text(
            text = "本地漫画与视频库",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, top = 2.dp, bottom = 14.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TabSwitcher(
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    ButtonGroup(
        modifier = modifier,
        expandedRatio = 0.15f,
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        HomeTab.entries.forEachIndexed { index, tab ->
            val shapes = when {
                HomeTab.entries.size == 1 -> ToggleButtonDefaults.shapes()
                index == 0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                index == HomeTab.entries.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            ToggleButton(
                checked = selected == tab,
                onCheckedChange = { onSelect(tab) },
                shapes = shapes,
                contentPadding = PaddingValues(vertical = 12.dp),
                modifier = Modifier
                    .weight(1f)
                    .semantics { role = Role.Tab },
            ) {
                Text(
                    text = if (tab == HomeTab.COMIC) "漫画" else "视频",
                    style = MaterialTheme.typography.titleSmall,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun HomeBottomBar(
    onImport: () -> Unit,
    onImportFolder: () -> Unit,
    onLayout: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    HorizontalFloatingToolbar(
        expanded = true,
        floatingActionButton = {
            FloatingToolbarDefaults.StandardFloatingActionButton(onClick = onImport) {
                Icon(Icons.Outlined.Add, contentDescription = "导入")
            }
        },
        modifier = modifier,
    ) {
        IconButton(onClick = onImportFolder) {
            Icon(Icons.Outlined.CreateNewFolder, contentDescription = "从文件夹导入")
        }
        IconButton(onClick = onLayout) {
            Icon(Icons.Outlined.GridView, contentDescription = "首页布局")
        }
        IconButton(onClick = onSettings) {
            Icon(Icons.Outlined.Settings, contentDescription = "设置")
        }
    }
}

@Composable
private fun ComicLibraryPage(
    comics: List<Comic>,
    categories: List<Category>,
    selectedCategoryId: Long?,
    columns: Int,
    onSelectCategory: (Long?) -> Unit,
    onCreateCategory: () -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onOpen: (Long) -> Unit,
    onLongPress: (Comic) -> Unit,
    onImport: () -> Unit,
    coverModifier: @Composable (Long) -> Modifier = { Modifier },
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CategoryChipBar(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onSelect = onSelectCategory,
            onCreate = onCreateCategory,
            onDelete = onDeleteCategory,
        )
        MediaGrid(
            items = comics,
            columns = columns,
            itemKey = { it.id },
            model = { it.coverPath },
            title = { it.title },
            subtitle = { comicProgressLabel(it) },
            onClick = { onOpen(it.id) },
            onLongClick = onLongPress,
            coverModifier = { coverModifier(it.id) },
            emptyContent = {
                EmptyLibrary(
                    title = if (selectedCategoryId == null) "还没有漫画" else "这个分类还没有漫画",
                    hint = "点右下角的 + 导入 ZIP、PDF、EPUB、MOBI 等格式的漫画。",
                    icon = { Icon(Icons.Outlined.AutoStories, contentDescription = null, modifier = Modifier.size(56.dp)) },
                    onImport = onImport,
                )
            },
        )
    }
}

@Composable
private fun VideoLibraryPage(
    videos: List<Video>,
    categories: List<Category>,
    selectedCategoryId: Long?,
    columns: Int,
    onSelectCategory: (Long?) -> Unit,
    onCreateCategory: () -> Unit,
    onDeleteCategory: (Category) -> Unit,
    onOpen: (Long) -> Unit,
    onLongPress: (Video) -> Unit,
    onImport: () -> Unit,
    coverModifier: @Composable (Long) -> Modifier = { Modifier },
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CategoryChipBar(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onSelect = onSelectCategory,
            onCreate = onCreateCategory,
            onDelete = onDeleteCategory,
        )
        MediaGrid(
            items = videos,
            columns = columns,
            itemKey = { it.id },
            model = { it.coverPath },
            title = { it.title },
            subtitle = { formatDuration(it.duration) },
            onClick = { onOpen(it.id) },
            onLongClick = onLongPress,
            coverModifier = { coverModifier(it.id) },
            emptyContent = {
                EmptyLibrary(
                    title = if (selectedCategoryId == null) "还没有视频" else "这个分类还没有视频",
                    hint = "点右下角的 + 导入视频文件，或从文件夹批量导入。",
                    icon = { Icon(Icons.Outlined.Movie, contentDescription = null, modifier = Modifier.size(56.dp)) },
                    onImport = onImport,
                )
            },
        )
    }
}

@Composable
private fun CategoryChipBar(
    categories: List<Category>,
    selectedCategoryId: Long?,
    onSelect: (Long?) -> Unit,
    onCreate: () -> Unit,
    onDelete: (Category) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") {
            CategoryPill(
                label = "全部",
                selected = selectedCategoryId == null,
                onClick = { onSelect(null) },
            )
        }
        items(items = categories, key = { it.id }) { category ->
            CategoryPill(
                label = category.name,
                selected = selectedCategoryId == category.id,
                onClick = { onSelect(category.id) },
                onLongClick = { onDelete(category) },
            )
        }
        item(key = "create") {
            CategoryPill(
                label = "＋ 新建",
                selected = false,
                onClick = onCreate,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    val container = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(50), color = container, contentColor = contentColor) {
        Box(
            modifier = Modifier
                .combinedClickable(
                    role = Role.Button,
                    onLongClickLabel = if (onLongClick != null) "删除分类" else null,
                    onLongClick = onLongClick,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 9.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun EmptyLibrary(
    title: String,
    hint: String,
    icon: @Composable () -> Unit,
    onImport: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center,
        ) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                icon()
            }
        }
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(4.dp))
        Button(onClick = onImport, shape = MaterialTheme.shapes.large) {
            Text("导入")
        }
    }
}

private sealed interface HomeDialog {
    data class ComicMenu(val comic: Comic) : HomeDialog
    data class VideoMenu(val video: Video) : HomeDialog
    data class RenameComic(val comic: Comic) : HomeDialog
    data class RenameVideo(val video: Video) : HomeDialog
    data class ComicCategories(val comic: Comic) : HomeDialog
    data class VideoCategories(val video: Video) : HomeDialog
    data class DeleteComic(val comic: Comic) : HomeDialog
    data class DeleteVideo(val video: Video) : HomeDialog
    data class NewCategory(val type: CategoryType) : HomeDialog
    data class DeleteCategory(val category: Category) : HomeDialog
}

@Composable
private fun HomeDialogs(
    dialog: HomeDialog?,
    state: HomeUiState,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
    onReplace: (HomeDialog?) -> Unit,
) {
    when (dialog) {
        null -> Unit

        is HomeDialog.ComicMenu -> OptionListDialog(
            title = dialog.comic.title,
            options = listOf("重命名", "添加到分类", "删除漫画"),
            onSelect = { index ->
                onReplace(
                    when (index) {
                        0 -> HomeDialog.RenameComic(dialog.comic)
                        1 -> HomeDialog.ComicCategories(dialog.comic)
                        else -> HomeDialog.DeleteComic(dialog.comic)
                    },
                )
            },
            onDismiss = onDismiss,
        )

        is HomeDialog.VideoMenu -> OptionListDialog(
            title = dialog.video.title,
            options = listOf("重命名", "添加到分类", "从库中删除"),
            onSelect = { index ->
                onReplace(
                    when (index) {
                        0 -> HomeDialog.RenameVideo(dialog.video)
                        1 -> HomeDialog.VideoCategories(dialog.video)
                        else -> HomeDialog.DeleteVideo(dialog.video)
                    },
                )
            },
            onDismiss = onDismiss,
        )

        is HomeDialog.RenameComic -> InputDialog(
            title = "重命名漫画",
            initialValue = dialog.comic.title,
            onConfirm = { name ->
                viewModel.renameComic(dialog.comic.id, name)
                onReplace(null)
            },
            onDismiss = onDismiss,
        )

        is HomeDialog.RenameVideo -> InputDialog(
            title = "重命名视频",
            initialValue = dialog.video.title,
            onConfirm = { name ->
                viewModel.renameVideo(dialog.video.id, name)
                onReplace(null)
            },
            onDismiss = onDismiss,
        )

        is HomeDialog.ComicCategories -> ComicCategoryPicker(
            comic = dialog.comic,
            categories = state.comicCategories,
            viewModel = viewModel,
            onDismiss = onDismiss,
        )

        is HomeDialog.VideoCategories -> VideoCategoryPicker(
            video = dialog.video,
            categories = state.videoCategories,
            viewModel = viewModel,
            onDismiss = onDismiss,
        )

        is HomeDialog.DeleteComic -> ConfirmDialog(
            title = "删除漫画",
            message = "将删除「${dialog.comic.title}」及其已复制的漫画文件和封面，此操作无法撤销。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteComic(dialog.comic.id)
                onReplace(null)
            },
            onDismiss = onDismiss,
        )

        is HomeDialog.DeleteVideo -> ConfirmDialog(
            title = "从库中删除",
            message = "只移除「${dialog.video.title}」的库记录和封面，不会删除原视频文件。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteVideo(dialog.video.id)
                onReplace(null)
            },
            onDismiss = onDismiss,
        )

        is HomeDialog.NewCategory -> InputDialog(
            title = if (dialog.type == CategoryType.COMIC) "新建漫画分类" else "新建视频分类",
            label = "分类名称",
            onConfirm = { name ->
                viewModel.createCategory(name, dialog.type)
                onReplace(null)
            },
            onDismiss = onDismiss,
        )

        is HomeDialog.DeleteCategory -> ConfirmDialog(
            title = "删除分类",
            message = "删除分类「${dialog.category.name}」后，其中的内容会回到「全部」，文件不会被删除。",
            confirmLabel = "删除",
            destructive = true,
            onConfirm = {
                viewModel.deleteCategory(dialog.category)
                onReplace(null)
            },
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun ComicCategoryPicker(
    comic: Comic,
    categories: List<Category>,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
) {
    var selection by remember(comic.id) { mutableStateOf<Set<Long>?>(null) }
    LaunchedEffect(comic.id) { selection = viewModel.comicCategoryIds(comic.id) }
    val current = selection ?: return
    CategoryPickerDialog(
        categories = categories,
        selectedIds = current,
        typeLabel = "漫画",
        onConfirm = { ids ->
            viewModel.setComicCategories(comic.id, ids)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}

@Composable
private fun VideoCategoryPicker(
    video: Video,
    categories: List<Category>,
    viewModel: HomeViewModel,
    onDismiss: () -> Unit,
) {
    var selection by remember(video.id) { mutableStateOf<Set<Long>?>(null) }
    LaunchedEffect(video.id) { selection = viewModel.videoCategoryIds(video.id) }
    val current = selection ?: return
    CategoryPickerDialog(
        categories = categories,
        selectedIds = current,
        typeLabel = "视频",
        onConfirm = { ids ->
            viewModel.setVideoCategories(video.id, ids)
            onDismiss()
        },
        onDismiss = onDismiss,
    )
}
