package com.readplusnext.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.readplusnext.ui.comic.detail.ComicDetailScreen
import com.readplusnext.ui.comic.reader.ComicReaderScreen
import com.readplusnext.ui.home.HomeScreen
import com.readplusnext.ui.settings.SettingsScreen
import com.readplusnext.ui.video.feed.VideoFeedScreen
import com.readplusnext.ui.motion.Motion

object Routes {
    const val HOME = "home"
    const val COMIC_DETAIL = "comicDetail/{comicId}"
    const val COMIC_READER = "comicReader/{comicId}?page={page}"
    const val VIDEO_FEED = "videoFeed/{videoId}"
    const val SETTINGS = "settings"

    fun comicDetail(comicId: Long): String = "comicDetail/$comicId"
    fun comicReader(comicId: Long, page: Int): String = "comicReader/$comicId?page=$page"
    fun videoFeed(videoId: Long): String = "videoFeed/$videoId"
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ReadPlusNextNavHost(navController: NavHostController = rememberNavController()) {
    SharedTransitionLayout {
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            enterTransition = {
                slideInHorizontally(
                    animationSpec = tween(Motion.NAV_ENTER_MS, easing = Motion.Decelerate),
                    initialOffsetX = { it / 6 },
                ) + fadeIn(tween(Motion.NAV_ENTER_MS, easing = Motion.Standard))
            },
            exitTransition = {
                fadeOut(tween(Motion.NAV_EXIT_MS, easing = Motion.Accelerate))
            },
            popEnterTransition = {
                fadeIn(tween(Motion.NAV_ENTER_MS, easing = Motion.Standard))
            },
            popExitTransition = {
                slideOutHorizontally(
                    animationSpec = tween(Motion.NAV_EXIT_MS, easing = Motion.Accelerate),
                    targetOffsetX = { it / 6 },
                ) + fadeOut(tween(Motion.NAV_EXIT_MS, easing = Motion.Accelerate))
            },
        ) {
            composable(Routes.HOME) {
                val animatedVisibilityScope = this
                HomeScreen(
                    onOpenComic = { id -> navController.navigate(Routes.comicDetail(id)) },
                    onOpenVideo = { id -> navController.navigate(Routes.videoFeed(id)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    comicCoverModifier = { comicId ->
                        Modifier.sharedElement(
                            state = rememberSharedContentState(key = "comic-cover-$comicId"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    },
                    videoCoverModifier = { videoId ->
                        Modifier.sharedElement(
                            state = rememberSharedContentState(key = "video-cover-$videoId"),
                            animatedVisibilityScope = animatedVisibilityScope,
                        )
                    },
                )
            }
            composable(
                route = Routes.COMIC_DETAIL,
                arguments = listOf(navArgument("comicId") { type = NavType.LongType }),
            ) { entry ->
                val animatedVisibilityScope = this
                ComicDetailScreen(
                    onBack = { navController.popBackStack() },
                    coverModifier = Modifier.sharedElement(
                        state = rememberSharedContentState(
                            key = "comic-cover-${entry.arguments?.getLong("comicId") ?: 0L}",
                        ),
                        animatedVisibilityScope = animatedVisibilityScope,
                    ),
                    onRead = { startPage ->
                        val comicId = entry.arguments?.getLong("comicId") ?: 0L
                        navController.navigate(Routes.comicReader(comicId, startPage))
                    },
                )
            }
        composable(
            route = Routes.COMIC_READER,
            arguments = listOf(
                navArgument("comicId") { type = NavType.LongType },
                navArgument("page") {
                    type = NavType.IntType
                    defaultValue = -1
                },
            ),
        ) {
            ComicReaderScreen(onBack = { navController.popBackStack() })
        }
        composable(
            route = Routes.VIDEO_FEED,
            arguments = listOf(navArgument("videoId") { type = NavType.LongType }),
        ) {
            VideoFeedScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
    }
}
