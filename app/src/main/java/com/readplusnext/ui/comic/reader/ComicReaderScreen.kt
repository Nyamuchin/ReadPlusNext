package com.readplusnext.ui.comic.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.ViewStream
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.readplusnext.data.image.ComicPageImage
import com.readplusnext.domain.model.ReadingMode
import com.readplusnext.ui.motion.Motion
import com.readplusnext.ui.util.findActivity

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ComicReaderScreen(
    onBack: () -> Unit,
    viewModel: ComicReaderViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    var chromeVisible by remember { mutableStateOf(true) }
    var currentPage by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.startPage) { currentPage = state.startPage }

    DisposableEffect(Unit) {
        val controller = context.findActivity()?.let { WindowCompat.getInsetsController(it.window, view) }
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    LaunchedEffect(chromeVisible) {
        val activity = context.findActivity() ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(activity.window, view)
        if (chromeVisible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        val comic = state.comic
        when {
            state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator(color = Color.White)
            }

            comic == null || state.pages.isEmpty() -> ReaderMessage(
                message = "无法打开这篇漫画",
                onBack = onBack,
            )

            state.readingMode == ReadingMode.VERTICAL_SCROLL -> {
                val listState = rememberLazyListState(initialFirstVisibleItemIndex = state.startPage)
                LaunchedEffect(listState) {
                    snapshotFlow { listState.firstVisibleItemIndex }.collect { index ->
                        currentPage = index
                        viewModel.onPageChanged(index)
                    }
                }
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(items = state.pages, key = { it.key }) { page ->
                        ZoomablePage(
                            model = ComicPageImage(
                                sourcePath = comic.zipPath,
                                pageKey = page.key,
                                isPdf = comic.isPdf,
                            ),
                            contentDescription = "第 ${page.index + 1} 页",
                            onTap = { chromeVisible = !chromeVisible },
                            onZoomChanged = {},
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            else -> {
                var canScroll by remember { mutableStateOf(true) }
                val pagerState = rememberPagerState(initialPage = state.startPage) {
                    state.pages.size
                }
                LaunchedEffect(pagerState.settledPage) {
                    currentPage = pagerState.settledPage
                    viewModel.onPageChanged(pagerState.settledPage)
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = state.readingMode == ReadingMode.RIGHT_TO_LEFT,
                    userScrollEnabled = canScroll,
                ) { index ->
                    ZoomablePage(
                        model = ComicPageImage(
                            sourcePath = comic.zipPath,
                            pageKey = state.pages[index].key,
                            isPdf = comic.isPdf,
                        ),
                        contentDescription = "第 ${index + 1} 页",
                        onTap = { chromeVisible = !chromeVisible },
                        onZoomChanged = { canScroll = !it },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = chromeVisible && state.pages.isNotEmpty(),
            enter = fadeIn(tween(Motion.CONTROL_MS)) +
                slideInVertically(tween(Motion.CONTROL_MS, easing = Motion.Decelerate)) { -it / 4 },
            exit = fadeOut(tween(Motion.CONTROL_MS)) +
                slideOutVertically(tween(Motion.CONTROL_MS, easing = Motion.Accelerate)) { -it / 4 },
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                ReaderTopBar(
                    title = state.comic?.title.orEmpty(),
                    page = currentPage,
                    total = state.pages.size,
                    onBack = onBack,
                )
                Spacer(Modifier.weight(1f))
                ReaderBottomBar(
                    readingMode = state.readingMode,
                    page = currentPage,
                    total = state.pages.size,
                    onCycleMode = viewModel::cycleReadingMode,
                )
            }
        }
    }
}

@Composable
private fun ReaderTopBar(
    title: String,
    page: Int,
    total: Int,
    onBack: () -> Unit,
) {
    Surface(
        color = Color.Black.copy(alpha = 0.55f),
        contentColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${page + 1} / $total",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun ReaderBottomBar(
    readingMode: ReadingMode,
    page: Int,
    total: Int,
    onCycleMode: () -> Unit,
) {
    val label = when (readingMode) {
        ReadingMode.LEFT_TO_RIGHT -> "从左到右"
        ReadingMode.RIGHT_TO_LEFT -> "从右到左"
        ReadingMode.VERTICAL_SCROLL -> "纵向滚动"
    }
    Surface(
        color = Color.Black.copy(alpha = 0.55f),
        contentColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCycleMode) {
                Icon(
                    imageVector = when (readingMode) {
                        ReadingMode.LEFT_TO_RIGHT -> Icons.Outlined.SwapHoriz
                        ReadingMode.RIGHT_TO_LEFT -> Icons.Outlined.ScreenRotation
                        ReadingMode.VERTICAL_SCROLL -> Icons.Outlined.ViewStream
                    },
                    contentDescription = null,
                    tint = Color.White,
                )
                Spacer(Modifier.size(8.dp))
                Text(text = label, color = Color.White)
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${page + 1} / $total",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
            )
        }
    }
}

@Composable
private fun ReaderMessage(message: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text(text = message, color = Color.White, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.size(12.dp))
        TextButton(onClick = onBack) { Text("返回", color = Color.White) }
    }
}
