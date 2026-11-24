package app.minimise.settings.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow
import app.minimise.core.ui.dateTimeText
import app.minimise.keys.ui.KeyCheck
import app.minimise.unlock.domain.TypingPenance
import app.minimise.unlock.domain.UnlockMethodId

@Composable
fun ApplyNowRoute(onDone: () -> Unit, viewModel: ApplyNowViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.done) { if (state.done) onDone() }
    ApplyNowScreen(
        state = state,
        onMethodChosen = viewModel::onMethodChosen,
        onBack = viewModel::onBack,
        onTyped = viewModel::onTyped,
        onFriendCode = viewModel::onFriendCode,
        onNfcTag = viewModel::onNfcTag,
        onQrScanned = viewModel::onQrScanned,
    )
}

@Composable
fun ApplyNowScreen(
    state: ApplyNowUiState,
    onMethodChosen: (UnlockMethodId) -> Unit,
    onBack: () -> Unit,
    onTyped: (String) -> Unit,
    onFriendCode: (String) -> Unit,
    onNfcTag: (ByteArray) -> Unit,
    onQrScanned: (String) -> Unit,
) {
    ScreenScaffold(title = stringResource(R.string.apply_now_title)) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            val locked = state.lockedUntil
            if (locked != null) {
                Text(stringResource(R.string.lock_refused, dateTimeText(locked)), style = MaterialTheme.typography.bodyLarge)
                return@Column
            }
            when (val method = state.method) {
                null -> {
                    Text(
                        text = stringResource(R.string.apply_now_explainer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    state.methods.forEach { option ->
                        SettingsRow(title = methodLabel(option), onClick = { onMethodChosen(option.method) })
                    }
                }
                UnlockMethodId.TYPING -> Typing(state, onTyped)
                UnlockMethodId.FRIEND_CODE -> FriendCode(failed = state.failed, onFriendCode = onFriendCode)
                else -> KeyCheck(method = method, failed = state.failed, onNfcTag = onNfcTag, onQrScanned = onQrScanned)
            }
            if (state.method != null) TextButton(onClick = onBack) { Text(stringResource(R.string.blocked_back)) }
        }
    }
}

@Composable
private fun methodLabel(option: SkipMethod): String = when (option.method) {
    UnlockMethodId.FRIEND_CODE -> pluralStringResource(R.plurals.override_friend_code, option.usesLeft ?: 0, option.usesLeft ?: 0)
    UnlockMethodId.TYPING -> stringResource(R.string.override_typing)
    UnlockMethodId.NFC_KEY -> stringResource(R.string.override_nfc_key)
    UnlockMethodId.QR_KEY -> stringResource(R.string.override_qr_key)
    else -> option.method.name
}

@Composable
private fun Typing(state: ApplyNowUiState, onTyped: (String) -> Unit) {
    val right = TypingPenance.correctSoFar(state.passage, state.typed)
    val muted = MaterialTheme.colorScheme.outline
    Text(
        text = buildAnnotatedString {
            append(state.passage.take(right))
            withStyle(SpanStyle(color = muted)) { append(state.passage.drop(right)) }
        },
        style = MaterialTheme.typography.bodyLarge,
    )
    OutlinedTextField(
        value = state.typed,
        onValueChange = onTyped,
        // A password keyboard turns off suggestions, autocorrect and swipe typing on most phones.
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            capitalization = KeyboardCapitalization.None,
            autoCorrectEnabled = false,
        ),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    )
    Text(
        text = if (right < state.typed.length) {
            stringResource(R.string.typing_mistake, right + 1)
        } else {
            stringResource(R.string.typing_progress, right, state.passage.length)
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun FriendCode(failed: Boolean, onFriendCode: (String) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    Text(stringResource(R.string.friend_code_ask), style = MaterialTheme.typography.bodyLarge)
    OutlinedTextField(
        value = code,
        onValueChange = { code = it.take(MAX_CODE_LENGTH) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    )
    if (failed) {
        Text(
            text = stringResource(R.string.friend_code_wrong),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    OutlinedButton(onClick = { onFriendCode(code) }, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.friend_code_check)) }
}

private const val MAX_CODE_LENGTH = 16
