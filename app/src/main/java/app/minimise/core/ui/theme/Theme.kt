package app.minimise.core.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

private val Black = Color(0xFF000000)
private val White = Color(0xFFFFFFFF)
private val Grey90 = Color(0xFF141414)
private val Grey80 = Color(0xFF1F1F1F)
private val Grey70 = Color(0xFF2B2B2B)
private val Grey50 = Color(0xFF6B6B6B)
private val Grey30 = Color(0xFFB3B3B3)

/** Monochrome: black background, white text, greys for everything secondary. No accent colour. */
private val MonochromeDark: ColorScheme = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = Grey80,
    onPrimaryContainer = White,
    inversePrimary = Black,
    secondary = Grey30,
    onSecondary = Black,
    secondaryContainer = Grey80,
    onSecondaryContainer = White,
    tertiary = Grey30,
    onTertiary = Black,
    tertiaryContainer = Grey80,
    onTertiaryContainer = White,
    background = Black,
    onBackground = White,
    surface = Black,
    onSurface = White,
    surfaceVariant = Grey80,
    onSurfaceVariant = Grey30,
    surfaceTint = Black,
    inverseSurface = White,
    inverseOnSurface = Black,
    error = White,
    onError = Black,
    errorContainer = Grey70,
    onErrorContainer = White,
    outline = Grey50,
    outlineVariant = Grey70,
    scrim = Black,
    surfaceBright = Grey70,
    surfaceDim = Black,
    surfaceContainer = Grey90,
    surfaceContainerHigh = Grey80,
    surfaceContainerHighest = Grey70,
    surfaceContainerLow = Grey90,
    surfaceContainerLowest = Black,
)

private val AppFont = FontFamily.SansSerif

private fun TextStyle.withAppFont() = copy(fontFamily = AppFont)

private val MonochromeTypography: Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.withAppFont(),
        displayMedium = displayMedium.withAppFont(),
        displaySmall = displaySmall.withAppFont(),
        headlineLarge = headlineLarge.withAppFont(),
        headlineMedium = headlineMedium.withAppFont(),
        headlineSmall = headlineSmall.withAppFont(),
        titleLarge = titleLarge.withAppFont(),
        titleMedium = titleMedium.withAppFont(),
        titleSmall = titleSmall.withAppFont(),
        bodyLarge = bodyLarge.withAppFont(),
        bodyMedium = bodyMedium.withAppFont(),
        bodySmall = bodySmall.withAppFont(),
        labelLarge = labelLarge.withAppFont(),
        labelMedium = labelMedium.withAppFont(),
        labelSmall = labelSmall.withAppFont(),
    )
}

/** The app's only theme. Dark is the default and, for now, the only mode. */
@Composable
fun MinimiseTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MonochromeDark,
        typography = MonochromeTypography,
        content = content,
    )
}
