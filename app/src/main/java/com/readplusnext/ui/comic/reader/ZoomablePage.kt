package com.readplusnext.ui.comic.reader

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import coil.compose.AsyncImage
import com.readplusnext.data.image.ComicPageImage
import kotlin.math.abs

/**
 * A single comic page with pinch to zoom (1x–5x) and double tap zoom (2.5x).
 *
 * Gestures are only consumed while the page is actually zoomed in (or while a
 * two-finger pinch is in progress). A single finger drag at 1x is left to the
 * parent pager / vertical list so page turning and scrolling keep working.
 * [onZoomChanged] reports whether the page is zoomed in so the surrounding pager
 * can stop intercepting horizontal swipes.
 */
@Composable
fun ZoomablePage(
    model: ComicPageImage,
    contentDescription: String?,
    onTap: () -> Unit,
    onZoomChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var viewSize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(scale) { onZoomChanged(scale > 1.01f) }

    /** Keep the zoomed page from being panned completely off screen. */
    fun clampOffset(candidate: Offset, zoom: Float): Offset {
        if (viewSize.width == 0 || viewSize.height == 0) return candidate
        val maxX = (viewSize.width * (zoom - 1f)) / 2f
        val maxY = (viewSize.height * (zoom - 1f)) / 2f
        return Offset(
            x = candidate.x.coerceIn(-maxX, maxX),
            y = candidate.y.coerceIn(-maxY, maxY),
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .onSizeChanged { viewSize = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1.01f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    while (true) {
                        val event = awaitPointerEvent()
                        val zoomChange = event.calculateZoom()
                        val panChange = event.calculatePan()
                        val currentScale = scale
                        val isPinching = abs(zoomChange - 1f) > 0.0001f

                        if (isPinching) {
                            // A pinch always belongs to the page, even at 1x.
                            val newScale = (currentScale * zoomChange).coerceIn(1f, 5f)
                            scale = newScale
                            if (newScale > 1.01f) {
                                offset = clampOffset(offset + panChange, newScale)
                            } else {
                                offset = Offset.Zero
                            }
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        } else if (currentScale > 1.01f && panChange != Offset.Zero) {
                            // Panned content: keep the drag here instead of turning the page.
                            offset = clampOffset(offset + panChange, currentScale)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }

                        if (event.changes.none { it.pressed }) break
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )
    }
}
