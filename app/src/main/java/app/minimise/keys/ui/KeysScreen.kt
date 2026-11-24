package app.minimise.keys.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.minimise.R
import app.minimise.core.ui.ScreenScaffold
import app.minimise.core.ui.SettingsRow
import app.minimise.core.ui.durationText
import app.minimise.keys.domain.KeyType
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

@Composable
fun KeysRoute(viewModel: KeysViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val stickerLabel = stringResource(R.string.key_label_sticker)
    val codeLabel = stringResource(R.string.key_label_code)
    ScreenScaffold(title = stringResource(R.string.keys_title)) {
        Text(
            text = stringResource(R.string.keys_explainer),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
            items(state.rows, key = { it.key.id }) { row ->
                SettingsRow(
                    title = row.key.label,
                    summary = row.removalIn?.let { stringResource(R.string.key_removed_in, durationText(it)) }
                        ?: stringResource(if (row.key.type == KeyType.NFC) R.string.key_type_nfc else R.string.key_type_qr),
                    onClick = { viewModel.onRemove(row.key) },
                )
            }
        }
        if (nfcReady()) {
            TextButton(onClick = viewModel::onPairNfc) { Text(stringResource(R.string.keys_pair_nfc), style = MaterialTheme.typography.titleMedium) }
        }
        TextButton(onClick = viewModel::onMakeQr) { Text(stringResource(R.string.keys_make_qr), style = MaterialTheme.typography.titleMedium) }
    }

    when (val pairing = state.pairing) {
        is Pairing.Nfc -> AlertDialog(
            onDismissRequest = viewModel::onPairingDismissed,
            text = {
                NfcReader(onTag = { viewModel.onNfcTag(it, stickerLabel) })
                Text(stringResource(if (pairing.alreadyPaired) R.string.key_already_paired else R.string.key_hold_sticker))
            },
            confirmButton = { TextButton(onClick = viewModel::onPairingDismissed) { Text(stringResource(R.string.swap_cancel)) } },
        )
        is Pairing.Qr -> AlertDialog(
            onDismissRequest = viewModel::onPairingDismissed,
            text = { QrCode(pairing.secret) },
            confirmButton = { TextButton(onClick = { viewModel.onQrSaved(codeLabel) }) { Text(stringResource(R.string.key_qr_saved)) } },
            dismissButton = { TextButton(onClick = viewModel::onPairingDismissed) { Text(stringResource(R.string.swap_cancel)) } },
        )
        null -> Unit
    }

    state.confirmingRemoval?.let { key ->
        AlertDialog(
            onDismissRequest = viewModel::onRemoveDismissed,
            title = { Text(key.label) },
            text = { Text(stringResource(R.string.key_remove_body)) },
            confirmButton = { TextButton(onClick = viewModel::onRemoveConfirmed) { Text(stringResource(R.string.key_remove)) } },
            dismissButton = { TextButton(onClick = viewModel::onRemoveDismissed) { Text(stringResource(R.string.swap_cancel)) } },
        )
    }
}

@Composable
private fun QrCode(secret: String) {
    val bitmap = remember(secret) { BarcodeEncoder().encodeBitmap(secret, BarcodeFormat.QR_CODE, QR_SIZE, QR_SIZE) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.key_qr_explainer), style = MaterialTheme.typography.bodyMedium)
    }
}

private const val QR_SIZE = 768
