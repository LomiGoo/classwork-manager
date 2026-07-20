package com.lomigoo.classworkmanager.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = JRU_Blue_Light,
    secondary = JRU_Gold_Light,
    tertiary = JRU_Gold_Light,
)

private val LightColorScheme = lightColorScheme(
    primary = JRU_Blue,
    secondary = JRU_Gold,
    tertiary = JRU_Gold,
)

@Composable
fun ClassworkManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is disabled by default to keep JRU branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val targetColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val colorScheme = animateColorScheme(targetColorScheme)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
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
