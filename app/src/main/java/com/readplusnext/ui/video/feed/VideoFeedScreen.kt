package com.readplusnext.ui.video.feed

import android.view.LayoutInflater
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.readplusnext.R
import com.readplusnext.domain.model.Video
import com.readplusnext.ui.motion.Motion
import com.readplusnext.ui.util.findActivity
import com.readplusnext.ui.util.formatDuration
import java.io.File
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoFeedScreen(
    onBack: () -> Unit,
    viewModel: VideoFeedViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    var chromeVisible by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val controller = context.findActivity()?.let { WindowCompat.getInsetsController(it.window, view) }
        controller?.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose { controller?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    LaunchedEffect(chromeVisible) {
        val activity = context.findActivity() ?: return@LaunchedEffect
        val controller = WindowCompat.getInsetsController(activity.window, view)
        if (chromeVisible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator(color = Color.White)
            }
            return@Box
        }
        if (state.videos.isEmpty()) {
            Text(
                text = "视频库还是空的",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center),
            )
            return@Box
        }

        val pagerState = rememberPagerState(initialPage = state.startIndex) {
            state.videos.size
        }
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { index ->
            val video = state.videos[index]
            VideoPage(
                video = video,
                startPositionMs = state.startPositions[video.id] ?: 0L,
                isActive = pagerState.currentPage == index,
                onTap = { chromeVisible = !chromeVisible },
                onSavePosition = { positionMs ->
                    viewModel.savePlaybackPosition(video.id, positionMs)
                },
            )
        }

        AnimatedVisibility(
            visible = chromeVisible,
            enter = fadeIn(tween(Motion.CONTROL_MS)),
            exit = fadeOut(tween(Motion.CONTROL_MS)),
        ) {
            val current = state.videos.getOrNull(pagerState.currentPage)
            Surface(
                color = Color.Black.copy(alpha = 0.45f),
                contentColor = Color.White,
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = current?.title.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val categories = current?.let { state.categoryNames[it.id] }.orEmpty()
                        Text(
                            text = if (categories.isEmpty()) {
                                "本地视频 · ${formatDuration(current?.duration ?: 0L)}"
                            } else {
                                categories.joinToString(" · ")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPage(
    video: Video,
    startPositionMs: Long,
    isActive: Boolean,
    onTap: () -> Unit,
    onSavePosition: (Long) -> Unit,
) {
    val context = LocalContext.current
    val player = remember(video.id) {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
            setSeekParameters(SeekParameters.EXACT)
            setMediaItem(MediaItem.fromUri(video.uri))
            prepare()
            if (startPositionMs > 0L) seekTo(startPositionMs)
        }
    }
    var firstFrameRendered by remember(video.id) { mutableStateOf(false) }
    var isPlaying by remember(video.id) { mutableStateOf(false) }
    var positionMs by remember(video.id) { mutableLongStateOf(0L) }
    var durationMs by remember(video.id) { mutableLongStateOf(0L) }
    var scrubFraction by remember(video.id) { mutableFloatStateOf(0f) }
    var scrubbing by remember(video.id) { mutableStateOf(false) }

    DisposableEffect(player, video.id) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                firstFrameRendered = true
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    durationMs = player.duration.coerceAtLeast(0L)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            onSavePosition(player.currentPosition)
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, isActive) {
        if (isActive) {
            player.play()
        } else {
            player.pause()
        }
    }

    LaunchedEffect(player) {
        while (true) {
            positionMs = player.currentPosition
            if (durationMs <= 0L) durationMs = player.duration.coerceAtLeast(0L)
            delay(80)
        }
    }

    val coverAlpha by animateFloatAsState(
        targetValue = if (firstFrameRendered) 0f else 1f,
        animationSpec = tween(50),
        label = "cover-alpha",
    )
    val progressFraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else {
        0f
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                LayoutInflater.from(ctx).inflate(R.layout.view_player, null)
            },
            update = { root ->
                root.findViewById<PlayerView>(R.id.player_view).player = player
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (coverAlpha > 0f && video.coverPath != null) {
            AsyncImage(
                model = File(video.coverPath),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .graphicsLayerAlpha(coverAlpha),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onTap,
                ),
        )

        if (!isPlaying) {
            Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "播放",
                tint = Color.White.copy(alpha = 0.92f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(84.dp)
                    .clickable { player.play() },
            )
        }

        if (isPlaying) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(Color.White.copy(alpha = 0.25f)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressFraction)
                        .height(2.dp)
                        .background(Color.White),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = "${formatDuration((scrubFraction * durationMs).toLong())} / " +
                        formatDuration(durationMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                )
                Slider(
                    value = if (scrubbing) scrubFraction else progressFraction,
                    onValueChange = { value ->
                        scrubbing = true
                        scrubFraction = value
                    },
                    onValueChangeFinished = {
                        val target = (scrubFraction * durationMs).toLong()
                        player.seekTo(target)
                        scrubbing = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private fun Modifier.graphicsLayerAlpha(value: Float): Modifier =
    this.graphicsLayer { alpha = value }
