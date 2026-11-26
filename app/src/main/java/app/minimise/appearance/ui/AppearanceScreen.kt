package app.minimise.appearance.ui

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.prefs.Appearance
import app.minimise.core.prefs.FontChoice
import app.minimise.core.prefs.HomeTextSize
import app.minimise.core.prefs.TextSize
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow
import app.minimise.core.ui.SettingsSection
import app.minimise.core.ui.theme.family
import app.minimise.launcher.domain.ShortcutIcon
import app.minimise.launcher.ui.appNameStyle
import app.minimise.launcher.ui.clockStyle
import app.minimise.launcher.ui.drawable
import app.minimise.launcher.ui.iconSize
import app.minimise.launcher.ui.label
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun AppearanceRoute(onOpenShortcuts: () -> Unit, viewModel: AppearanceViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    AppearanceScreen(state = state, onChange = viewModel::onChange, onOpenShortcuts = onOpenShortcuts)
}

@Composable
fun AppearanceScreen(state: AppearanceUiState, onChange: ((Appearance) -> Appearance) -> Unit, onOpenShortcuts: () -> Unit) {
    ScreenScaffold(title = stringResource(R.string.appearance_title), scrollable = true) {
        val appearance = state.appearance ?: return@ScreenScaffold
        HomePreview(appearance = appearance, appNames = state.appNames, icons = state.icons)

        SettingsSection(stringResource(R.string.appearance_font))
        Choices(
            options = FontChoice.entries,
            selected = appearance.font,
            label = { stringResource(it.labelRes()) },
            // Each font's name in that font, so the choice shows itself.
            style = { MaterialTheme.typography.bodyLarge.copy(fontFamily = it.family(), fontWeight = if (it == FontChoice.LIGHT) FontWeight.Light else FontWeight.Normal) },
            onSelect = { font -> onChange { it.copy(font = font) } },
        )

        SettingsSection(stringResource(R.string.appearance_text_size))
        Choices(
            options = TextSize.entries,
            selected = appearance.textSize,
            label = { stringResource(it.labelRes()) },
            onSelect = { size -> onChange { it.copy(textSize = size) } },
        )
        Note(stringResource(R.string.appearance_text_size_note))

        SettingsSection(stringResource(R.string.appearance_clock_size))
        Choices(
            options = HomeTextSize.entries,
            selected = appearance.clockSize,
            label = { stringResource(it.labelRes()) },
            onSelect = { size -> onChange { it.copy(clockSize = size) } },
        )

        SettingsSection(stringResource(R.string.appearance_app_name_size))
        Choices(
            options = HomeTextSize.entries,
            selected = appearance.appNameSize,
            label = { stringResource(it.labelRes()) },
            onSelect = { size -> onChange { it.copy(appNameSize = size) } },
        )

        SettingsSection(stringResource(R.string.appearance_icon_size))
        Choices(
            options = HomeTextSize.entries,
            selected = appearance.iconSize,
            label = { stringResource(it.labelRes()) },
            onSelect = { size -> onChange { it.copy(iconSize = size) } },
        )

        SettingsSection(stringResource(R.string.appearance_on_home))
        val names = state.icons.map { stringResource(it.label) }
        SettingsRow(
            title = stringResource(R.string.shortcuts_title),
            summary = if (names.isEmpty()) stringResource(R.string.shortcuts_none) else names.joinToString(", "),
            icon = R.drawable.ic_glyph_star,
            onClick = onOpenShortcuts,
        )
        Spacer(Modifier.height(16.dp))
    }
}

/** Home in miniature, in the chosen font and sizes. Read out as one line. */
@Composable
private fun HomePreview(appearance: Appearance, appNames: List<String>, icons: List<ShortcutIcon>) {
    val context = LocalContext.current
    val now = remember { LocalDateTime.now() }
    val time = remember(now) { now.format(DateTimeFormatter.ofPattern(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm")) }
    val date = remember(now) { now.format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEdMMMM"))) }
    val description = stringResource(R.string.appearance_preview_description)
    Column(
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(PREVIEW_CORNER))
            .padding(horizontal = 24.dp, vertical = 20.dp)
            .clearAndSetSemantics { contentDescription = description },
    ) {
        Text(text = time, style = clockStyle(appearance.clockSize).shrunk())
        Text(text = date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        // As on home: the app names, and the icons in a small column on the right.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                appNames.forEach { Text(text = it, style = appNameStyle(appearance.appNameSize).shrunk(), maxLines = 1) }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                icons.forEach {
                    Icon(painter = painterResource(it.drawable), contentDescription = null, modifier = Modifier.size(iconSize(appearance.iconSize).glyph * PREVIEW_SCALE))
                }
            }
        }
    }
}

/** Home's sizes at a scale that fits a preview. */
private fun TextStyle.shrunk(): TextStyle = copy(fontSize = fontSize * PREVIEW_SCALE, lineHeight = lineHeight * PREVIEW_SCALE)

/** One choice of several, as a row of pills; the chosen one is filled. TalkBack reads them as radio buttons. */
@Composable
private fun <T> Choices(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    style: @Composable (T) -> TextStyle = { MaterialTheme.typography.bodyLarge },
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp).selectableGroup(),
    ) {
        options.forEach { option ->
            val chosen = option == selected
            val shape = RoundedCornerShape(50)
            Box(
                modifier = Modifier
                    .clip(shape)
                    .then(
                        if (chosen) {
                            Modifier.background(MaterialTheme.colorScheme.onSurface)
                        } else {
                            Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                        },
                    )
                    .selectable(selected = chosen, role = Role.RadioButton, onClick = { onSelect(option) })
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Text(
                    text = label(option),
                    style = style(option),
                    color = if (chosen) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
    }
}

@Composable
private fun Note(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}

private fun FontChoice.labelRes(): Int = when (this) {
    FontChoice.SYSTEM -> R.string.appearance_font_system
    FontChoice.LIGHT -> R.string.appearance_font_light
    FontChoice.CONDENSED -> R.string.appearance_font_condensed
    FontChoice.SERIF -> R.string.appearance_font_serif
    FontChoice.MONO -> R.string.appearance_font_mono
}

private fun TextSize.labelRes(): Int = when (this) {
    TextSize.SMALLER -> R.string.appearance_text_smaller
    TextSize.DEFAULT -> R.string.appearance_text_default
    TextSize.LARGER -> R.string.appearance_text_larger
    TextSize.LARGEST -> R.string.appearance_text_largest
}

private fun HomeTextSize.labelRes(): Int = when (this) {
    HomeTextSize.SMALL -> R.string.appearance_size_small
    HomeTextSize.MEDIUM -> R.string.appearance_size_medium
    HomeTextSize.LARGE -> R.string.appearance_size_large
}

private const val PREVIEW_SCALE = 0.6f
private val PREVIEW_CORNER = 24.dp
