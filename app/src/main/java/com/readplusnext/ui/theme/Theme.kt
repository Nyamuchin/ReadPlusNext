package com.readplusnext.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.readplusnext.domain.model.ThemeMode
import com.readplusnext.domain.model.ThemeStyle

/** Shared Expressive motion specification used by all screens. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
val ExpressiveMotion: MotionScheme = MotionScheme.expressive()

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ReadPlusNextTheme(
    themeStyle: ThemeStyle = ThemeStyle.MATERIAL,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colorScheme = when (themeStyle) {
        ThemeStyle.MATERIAL -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else if (dark) {
                ExpressiveDarkColorScheme
            } else {
                ExpressiveLightColorScheme
            }
        }

        ThemeStyle.MIUIX -> if (dark) MiuixDarkColorScheme else MiuixLightColorScheme
    }

    if (themeStyle == ThemeStyle.MATERIAL) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = ExpressiveMotion,
            shapes = ExpressiveShapes,
            typography = ExpressiveTypography,
            content = content,
        )
    } else {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = ExpressiveShapes,
            typography = ExpressiveTypography,
            content = content,
        )
    }
}
