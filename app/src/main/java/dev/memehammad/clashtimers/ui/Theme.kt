package dev.memehammad.clashtimers.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Material You colours from the wallpaper, with bolder type and rounder shapes. */
@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

    val base = Typography()
    val typography = base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        displayMedium = base.displayMedium.copy(fontWeight = FontWeight.Black),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(36.dp),
    )
    MaterialTheme(colorScheme = scheme, typography = typography, shapes = shapes, content = content)
}

/**
 * Boost theme: while a potion/snack runs, the tertiary palette takes over the primary role
 * (cross-fading), and surfaces pick up a hint of it.
 */
@Composable
fun BoostTheme(active: Boolean, content: @Composable () -> Unit) {
    val s = MaterialTheme.colorScheme
    val spec = tween<Color>(700)
    @Composable
    fun pick(normal: Color, boosted: Color) =
        animateColorAsState(if (active) boosted else normal, spec, label = "boost").value

    val scheme = s.copy(
        primary = pick(s.primary, s.tertiary),
        onPrimary = pick(s.onPrimary, s.onTertiary),
        primaryContainer = pick(s.primaryContainer, s.tertiaryContainer),
        onPrimaryContainer = pick(s.onPrimaryContainer, s.onTertiaryContainer),
        surface = pick(s.surface, s.tertiaryContainer.copy(alpha = 0.18f).compositeOver(s.surface)),
        surfaceContainer = pick(s.surfaceContainer, s.tertiaryContainer.copy(alpha = 0.28f).compositeOver(s.surfaceContainer)),
        surfaceContainerHigh = pick(
            s.surfaceContainerHigh,
            s.tertiaryContainer.copy(alpha = 0.35f).compositeOver(s.surfaceContainerHigh),
        ),
    )
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes, content = content)
}
