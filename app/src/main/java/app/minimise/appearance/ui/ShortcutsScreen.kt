package app.minimise.appearance.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.AppPickerDialog
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow
import app.minimise.launcher.domain.Shortcut
import app.minimise.launcher.domain.ShortcutIcon
import app.minimise.launcher.ui.automaticText
import app.minimise.launcher.ui.drawable
import app.minimise.launcher.ui.label

@Composable
fun ShortcutsRoute(viewModel: ShortcutsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ShortcutsScreen(
        state = state,
        onItemTapped = viewModel::onItemTapped,
        onMove = viewModel::onMove,
        onRemove = viewModel::onRemove,
        onAddTapped = viewModel::onAddTapped,
        onIconChosen = viewModel::onIconChosen,
        onAddDismissed = viewModel::onAddDismissed,
        onAppPicked = viewModel::onAppPicked,
        onRemovePicked = viewModel::onRemovePicked,
        onPickerDismissed = viewModel::onPickerDismissed,
    )
}

@Composable
fun ShortcutsScreen(
    state: ShortcutsUiState,
    onItemTapped: (ShortcutItem) -> Unit,
    onMove: (index: Int, by: Int) -> Unit,
    onRemove: (Int) -> Unit,
    onAddTapped: () -> Unit,
    onIconChosen: (ShortcutIcon) -> Unit,
    onAddDismissed: () -> Unit,
    onAppPicked: (String?) -> Unit,
    onRemovePicked: () -> Unit,
    onPickerDismissed: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.shortcuts_title), scrollable = true) {
        Text(
            text = stringResource(R.string.shortcuts_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        val items = state.items ?: return@ScreenScaffold
        items.forEach { item ->
            ItemRow(
                item = item,
                isFirst = item.index == 0,
                isLast = item.index == items.lastIndex,
                onTap = { onItemTapped(item) },
                onMoveUp = { onMove(item.index, -1) },
                onMoveDown = { onMove(item.index, 1) },
                onRemove = { onRemove(item.index) },
            )
        }
        if (items.size < Shortcut.MAX) {
            SettingsRow(title = stringResource(R.string.shortcuts_add), icon = R.drawable.ic_add, onClick = onAddTapped)
        } else {
            Text(
                text = stringResource(R.string.shortcuts_full, Shortcut.MAX),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 14.dp),
            )
        }
    }

    if (state.adding) IconChooser(onChoose = onIconChosen, onDismiss = onAddDismissed)

    state.picking?.let { item ->
        AppPickerDialog(
            choices = state.choices,
            onPick = { onAppPicked(it.packageName) },
            onClear = onRemovePicked,
            onDismiss = onPickerDismissed,
            title = stringResource(R.string.shortcut_pick_title, stringResource(item.icon.label)),
            clearLabel = stringResource(R.string.shortcut_remove),
            automatic = if (item.icon.default != null) automaticText(item.automatic) else null,
            onAutomatic = { onAppPicked(null) },
        )
    }
}

@Composable
private fun ItemRow(
    item: ShortcutItem,
    isFirst: Boolean,
    isLast: Boolean,
    onTap: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    val name = stringResource(item.icon.label)
    val summary = when {
        item.missing -> stringResource(R.string.shortcuts_app_missing)
        item.chosen != null -> item.chosen.label
        item.icon.default != null -> automaticText(item.automatic)
        else -> stringResource(R.string.shortcuts_choose_app)
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.a11y_change_app), onClick = onTap)
                .padding(vertical = 14.dp),
        ) {
            Icon(painter = painterResource(item.icon.drawable), contentDescription = null, modifier = Modifier.size(26.dp))
            Column {
                Text(text = name, style = MaterialTheme.typography.titleLarge)
                Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        SmallButton(R.drawable.ic_arrow_upward, stringResource(R.string.a11y_shortcut_up, name), enabled = !isFirst, onClick = onMoveUp)
        SmallButton(R.drawable.ic_arrow_downward, stringResource(R.string.a11y_shortcut_down, name), enabled = !isLast, onClick = onMoveDown)
        SmallButton(R.drawable.ic_close, stringResource(R.string.a11y_shortcut_remove, name), enabled = true, onClick = onRemove)
    }
}

@Composable
private fun SmallButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

/** Every icon Enough ships, with its name, to add one. */
@Composable
private fun IconChooser(onChoose: (ShortcutIcon) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, shape = MaterialTheme.shapes.extraLarge) {
            Column(modifier = Modifier.padding(vertical = 20.dp)) {
                Text(
                    text = stringResource(R.string.shortcuts_choose_icon),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp).padding(bottom = 12.dp).semantics { heading() },
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 80.dp),
                    modifier = Modifier.heightIn(max = 440.dp).padding(horizontal = 12.dp),
                ) {
                    items(ShortcutIcon.entries, key = { it.name }) { icon ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(role = Role.Button, onClick = { onChoose(icon) })
                                .padding(vertical = 14.dp, horizontal = 4.dp),
                        ) {
                            Icon(painter = painterResource(icon.drawable), contentDescription = null, modifier = Modifier.size(26.dp))
                            Text(
                                text = stringResource(icon.label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.padding(horizontal = 12.dp).padding(top = 8.dp)) {
                    Text(stringResource(R.string.shortcuts_cancel))
                }
            }
        }
    }
}
