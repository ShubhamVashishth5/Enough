package app.minimise.tasks.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.ScreenScaffold

@Composable
fun TodayRoute(onClose: () -> Unit, viewModel: TodayViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.finished) { if (state.finished) onClose() }
    TodayScreen(state = state, onDraftChange = viewModel::onDraftChange, onSave = viewModel::onSave, onSkip = viewModel::onSkip)
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    onDraftChange: (Int, String) -> Unit,
    onSave: () -> Unit,
    onSkip: () -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.today_title), modifier = Modifier.imePadding()) {
        Text(
            text = stringResource(R.string.today_question),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        if (!state.loaded) return@ScreenScaffold
        state.drafts.forEachIndexed { index, text ->
            TaskField(
                text = text,
                hint = stringResource(R.string.today_hint, index + 1),
                onChange = { onDraftChange(index, it) },
                last = index == state.drafts.lastIndex,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 24.dp)) {
            OutlinedButton(onClick = onSave) { Text(stringResource(R.string.today_save)) }
            TextButton(onClick = onSkip) { Text(stringResource(R.string.today_skip)) }
        }
    }
}

@Composable
private fun TaskField(text: String, hint: String, onChange: (String) -> Unit, last: Boolean) {
    val style = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface)
    BasicTextField(
        value = text,
        onValueChange = onChange,
        singleLine = true,
        textStyle = style,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            imeAction = if (last) ImeAction.Done else ImeAction.Next,
        ),
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        decorationBox = { field ->
            Box {
                if (text.isEmpty()) Text(text = hint, style = style, color = MaterialTheme.colorScheme.outline)
                field()
            }
        },
    )
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
