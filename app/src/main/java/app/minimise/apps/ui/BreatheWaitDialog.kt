package app.minimise.apps.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.minimise.R
import app.minimise.apps.domain.AppEntry
import app.minimise.core.Defaults
import kotlin.time.Duration

/** Picks how long one Breathe app pauses: the usual wait, or one of [Defaults.BREATHE_WAITS]. */
@Composable
fun BreatheWaitDialog(app: AppEntry, onChosen: (Duration?) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.breathe_wait_title, app.label)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = stringResource(R.string.breathe_wait_explainer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                WaitChoice(
                    text = stringResource(R.string.breathe_wait_usual, Defaults.WAIT_STEPS.first().inWholeSeconds),
                    selected = app.breatheWait == null,
                    onClick = { onChosen(null) },
                )
                Defaults.BREATHE_WAITS.forEach { wait ->
                    val seconds = wait.inWholeSeconds.toInt()
                    WaitChoice(
                        text = pluralStringResource(R.plurals.breathe_wait_seconds, seconds, seconds),
                        selected = app.breatheWait == wait,
                        onClick = { onChosen(wait) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.swap_cancel)) } },
    )
}

@Composable
private fun WaitChoice(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
