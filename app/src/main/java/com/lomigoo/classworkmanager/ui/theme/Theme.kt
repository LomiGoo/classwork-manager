package com.lomigoo.classworkmanager.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.lomigoo.classworkmanager.data.AppPreferences

// Sharp modern shapes
private val SharpShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp)
)

@Composable
fun ClassworkManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    selectedTheme: String = AppPreferences.THEME_NEUTRAL,
    // Dynamic color is disabled by default to keep branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val targetColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getThemeColorScheme(selectedTheme, darkTheme)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as android.app.Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    val colorScheme = animateColorScheme(targetColorScheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = SharpShapes,
        content = content,
    )
}

private fun getThemeColorScheme(themeId: String, isDark: Boolean): ColorScheme {
    return if (isDark) {
        val primary = when (themeId) {
            AppPreferences.THEME_EMERALD -> Color(0xFF6EE7B7)
            AppPreferences.THEME_CRIMSON -> Color(0xFFFCA5A5)
            AppPreferences.THEME_AMETHYST -> Color(0xFFC4B5FD)
            AppPreferences.THEME_NEUTRAL -> Color(0xFF94A3B8)
            else -> Color(0xFFADC6FF) // JRU Blue Light
        }
        val secondary = when (themeId) {
            AppPreferences.THEME_EMERALD -> Color(0xFFA7F3D0)
            AppPreferences.THEME_CRIMSON -> Color(0xFFFDE047)
            AppPreferences.THEME_AMETHYST -> Color(0xFFDDD6FE)
            AppPreferences.THEME_NEUTRAL -> Color(0xFFCBD5E1)
            else -> JRU_Gold_Light
        }

        darkColorScheme(
            primary = primary,
            onPrimary = Color(0xFF001D4D),
            primaryContainer = Color(0xFF004494),
            onPrimaryContainer = Color(0xFFD8E2FF),
            secondary = secondary,
            onSecondary = Color(0xFF451A03),
            secondaryContainer = Color(0xFF78350F),
            onSecondaryContainer = Color(0xFFFEF3C7),
            background = Dark_Background,
            onBackground = Color(0xFFF1F5F9), // Fix: direct color for stability
            surface = Dark_Surface,
            onSurface = Dark_OnSurface,
            surfaceVariant = Dark_SurfaceVariant,
            onSurfaceVariant = Dark_OnSurfaceVariant,
            outline = Dark_Outline,
            error = ErrorRed,
            onError = OnErrorRed
        )
    } else {
        val primary = when (themeId) {
            AppPreferences.THEME_EMERALD -> Emerald_Primary
            AppPreferences.THEME_CRIMSON -> Crimson_Primary
            AppPreferences.THEME_AMETHYST -> Amethyst_Primary
            AppPreferences.THEME_NEUTRAL -> Neutral_Primary
            else -> JRU_Blue
        }
        val secondary = when (themeId) {
            AppPreferences.THEME_EMERALD -> Emerald_Secondary
            AppPreferences.THEME_CRIMSON -> Color(0xFFD97706)
            AppPreferences.THEME_AMETHYST -> Amethyst_Secondary
            AppPreferences.THEME_NEUTRAL -> Neutral_Secondary
            else -> JRU_Gold
        }
        val container = when (themeId) {
            AppPreferences.THEME_EMERALD -> Emerald_Light
            AppPreferences.THEME_CRIMSON -> Crimson_Light
            AppPreferences.THEME_AMETHYST -> Amethyst_Light
            AppPreferences.THEME_NEUTRAL -> Neutral_Light
            else -> Color(0xFFD8E2FF) // Fix: direct color for stability
        }

        lightColorScheme(
            primary = primary,
            onPrimary = White,
            primaryContainer = container,
            onPrimaryContainer = Color(0xFF001A41),
            secondary = secondary,
            onSecondary = White,
            secondaryContainer = Color(0xFFFFE08C),
            onSecondaryContainer = Color(0xFF241A00),
            background = Light_Background,
            onBackground = Black_Slate, // Fix: direct color for stability
            surface = Light_Surface,
            onSurface = Light_OnSurface,
            surfaceVariant = Light_SurfaceVariant,
            onSurfaceVariant = Light_OnSurfaceVariant,
            outline = Light_Outline,
            error = ErrorRed,
            onError = OnErrorRed
        )
    }
}

@Composable
private fun animateColorScheme(targetScheme: ColorScheme): ColorScheme {
    val duration = 500 // Animation duration in milliseconds

    return targetScheme.copy(
        primary = animateColorAsState(targetScheme.primary, tween(duration), label = "primary").value,
        onPrimary = animateColorAsState(targetScheme.onPrimary, tween(duration), label = "onPrimary").value,
        primaryContainer = animateColorAsState(
            targetScheme.primaryContainer,
            tween(duration),
            label = "primaryContainer",
        ).value,
        onPrimaryContainer = animateColorAsState(
            targetScheme.onPrimaryContainer,
            tween(duration),
            label = "onPrimaryContainer",
        ).value,
        secondary = animateColorAsState(targetScheme.secondary, tween(duration), label = "secondary").value,
        onSecondary = animateColorAsState(targetScheme.onSecondary, tween(duration), label = "onSecondary").value,
        secondaryContainer = animateColorAsState(
            targetScheme.secondaryContainer,
            tween(duration),
            label = "secondaryContainer",
        ).value,
        onSecondaryContainer = animateColorAsState(
            targetScheme.onSecondaryContainer,
            tween(duration),
            label = "onSecondaryContainer",
        ).value,
        tertiary = animateColorAsState(targetScheme.tertiary, tween(duration), label = "tertiary").value,
        onTertiary = animateColorAsState(targetScheme.onTertiary, tween(duration), label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(
            targetScheme.tertiaryContainer,
            tween(duration),
            label = "tertiaryContainer",
        ).value,
        onTertiaryContainer = animateColorAsState(
            targetScheme.onTertiaryContainer,
            tween(duration),
            label = "onTertiaryContainer",
        ).value,
        error = animateColorAsState(targetScheme.error, tween(duration), label = "error").value,
        onError = animateColorAsState(targetScheme.onError, tween(duration), label = "onError").value,
        errorContainer = animateColorAsState(
            targetScheme.errorContainer,
            tween(duration),
            label = "errorContainer",
        ).value,
        onErrorContainer = animateColorAsState(
            targetScheme.onErrorContainer,
            tween(duration),
            label = "onErrorContainer",
        ).value,
        background = animateColorAsState(targetScheme.background, tween(duration), label = "background").value,
        onBackground = animateColorAsState(targetScheme.onBackground, tween(duration), label = "onBackground").value,
        surface = animateColorAsState(targetScheme.surface, tween(duration), label = "surface").value,
        onSurface = animateColorAsState(targetScheme.onSurface, tween(duration), label = "onSurface").value,
        surfaceVariant = animateColorAsState(
            targetScheme.surfaceVariant,
            tween(duration),
            label = "surfaceVariant",
        ).value,
        onSurfaceVariant = animateColorAsState(
            targetScheme.onSurfaceVariant,
            tween(duration),
            label = "onSurfaceVariant",
        ).value,
        outline = animateColorAsState(targetScheme.outline, tween(duration), label = "outline").value,
        outlineVariant = animateColorAsState(
            targetScheme.outlineVariant,
            tween(duration),
            label = "outlineVariant",
        ).value,
        scrim = animateColorAsState(targetScheme.scrim, tween(duration), label = "scrim").value,
        inverseSurface = animateColorAsState(
            targetScheme.inverseSurface,
            tween(duration),
            label = "inverseSurface",
        ).value,
        inverseOnSurface = animateColorAsState(
            targetScheme.inverseOnSurface,
            tween(duration),
            label = "inverseOnSurface",
        ).value,
        inversePrimary = animateColorAsState(
            targetScheme.inversePrimary,
            tween(duration),
            label = "inversePrimary",
        ).value,
    )
}
