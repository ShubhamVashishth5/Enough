package app.minimise.core.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import app.minimise.core.prefs.Appearance
import app.minimise.core.prefs.FontChoice

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

/**
 * The look in force, for the few places that size text from it rather than from [MaterialTheme]: home's
 * clock and app names.
 */
val LocalAppearance = staticCompositionLocalOf { Appearance() }

/** Weights for each part of the type scale; a typeface without a weight shows its nearest. */
private data class Weights(
    val display: FontWeight,
    val headline: FontWeight,
    val title: FontWeight,
    val titleStrong: FontWeight,
    val body: FontWeight,
)

/** The typeface itself, also for showing each choice in its own font. */
fun FontChoice.family(): FontFamily = when (this) {
    FontChoice.SYSTEM -> FontFamily.Default
    FontChoice.LIGHT -> FontFamily.SansSerif
    FontChoice.CONDENSED -> FontFamily(
        Font(Condensed, FontWeight.Light),
        Font(Condensed, FontWeight.Normal),
        Font(Condensed, FontWeight.Medium),
    )
    FontChoice.SERIF -> FontFamily.Serif
    FontChoice.MONO -> FontFamily.Monospace
}

private fun FontChoice.weights(): Weights = when (this) {
    // Large text a step lighter than the stock Material scale: a calmer clock and headings.
    FontChoice.SYSTEM, FontChoice.CONDENSED -> Weights(FontWeight.Light, FontWeight.Normal, FontWeight.Normal, FontWeight.Medium, FontWeight.Normal)
    FontChoice.LIGHT -> Weights(FontWeight.ExtraLight, FontWeight.Light, FontWeight.Light, FontWeight.Normal, FontWeight.Normal)
    FontChoice.SERIF -> Weights(FontWeight.Normal, FontWeight.Normal, FontWeight.Normal, FontWeight.Medium, FontWeight.Normal)
    FontChoice.MONO -> Weights(FontWeight.Normal, FontWeight.Normal, FontWeight.Normal, FontWeight.Normal, FontWeight.Normal)
}

/** Android's own condensed family, on every phone (it falls back to the usual font if a maker dropped it). */
private val Condensed = DeviceFontFamilyName("sans-serif-condensed")

/**
 * The type scale. Sizes follow Material's, but titles are a step smaller and letters a little closer
 * than Material's defaults, which read as loose in long settings lists. A monospaced face keeps its own
 * spacing.
 */
private fun typography(font: FontChoice): Typography {
    val family = font.family()
    val w = font.weights()
    val mono = font == FontChoice.MONO
    fun style(size: Int, line: Int, weight: FontWeight, tracking: Double) = TextStyle(
        fontFamily = family,
        fontWeight = weight,
        fontSize = size.sp,
        lineHeight = line.sp,
        letterSpacing = if (mono) 0.sp else tracking.sp,
    )
    return Typography(
        displayLarge = style(57, 64, w.display, -0.5),
        displayMedium = style(45, 52, w.display, -0.25),
        displaySmall = style(36, 44, w.display, 0.0),
        headlineLarge = style(32, 40, w.headline, -0.25),
        headlineMedium = style(28, 36, w.headline, -0.2),
        headlineSmall = style(24, 32, w.headline, 0.0),
        titleLarge = style(20, 28, w.title, 0.0),
        titleMedium = style(16, 24, w.titleStrong, 0.1),
        titleSmall = style(14, 20, w.titleStrong, 0.1),
        bodyLarge = style(16, 24, w.body, 0.15),
        bodyMedium = style(14, 20, w.body, 0.15),
        bodySmall = style(12, 16, w.body, 0.2),
        labelLarge = style(14, 20, w.titleStrong, 0.1),
        labelMedium = style(12, 16, w.titleStrong, 0.4),
        labelSmall = style(11, 16, w.titleStrong, 0.4),
    )
}

/**
 * The app's only theme: monochrome and dark, in the font and text size chosen in Settings → Appearance.
 * Until settings are first read (a moment, once per process) it shows only black.
 */
@Composable
fun MinimiseTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val source = remember(context) { AppearanceSource.from(context) }
    val appearance by source.appearance.collectAsState()
    val current = appearance
    if (current == null) {
        Box(Modifier.fillMaxSize().background(Black))
        return
    }
    MinimiseTheme(appearance = current, content = content)
}

@Composable
fun MinimiseTheme(appearance: Appearance, content: @Composable () -> Unit) {
    val typography = remember(appearance.font) { typography(appearance.font) }
    val density = LocalDensity.current
    // Text size scales every sp in the app on top of the phone's own font size, dialogs included.
    val scaled = remember(density, appearance.textSize) { Density(density.density, density.fontScale * appearance.textSize.scale) }
    CompositionLocalProvider(LocalAppearance provides appearance, LocalDensity provides scaled) {
        MaterialTheme(
            colorScheme = MonochromeDark,
            typography = typography,
            content = content,
        )
    }
}
