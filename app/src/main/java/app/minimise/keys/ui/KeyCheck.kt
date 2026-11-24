package app.minimise.keys.ui

import android.nfc.NfcAdapter
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.minimise.R
import app.minimise.unlock.domain.UnlockMethodId
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import timber.log.Timber

/**
 * Listens for an NFC tag while on screen and reports its ID. Reader mode keeps other apps from
 * reacting to the tag meanwhile.
 */
@Composable
fun NfcReader(onTag: (ByteArray) -> Unit) {
    val activity = LocalActivity.current ?: return
    val latest = rememberUpdatedState(onTag)
    DisposableEffect(activity) {
        val adapter = NfcAdapter.getDefaultAdapter(activity)
        runCatching {
            adapter?.enableReaderMode(
                activity,
                { tag -> activity.runOnUiThread { latest.value(tag.id) } },
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or
                    NfcAdapter.FLAG_READER_NFC_V or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK,
                null,
            )
        }.onFailure { Timber.w(it, "NFC reader mode failed") }
        onDispose { runCatching { adapter?.disableReaderMode(activity) } }
    }
}

/** True when the phone has NFC and it's switched on. */
@Composable
fun nfcReady(): Boolean = LocalActivity.current?.let { NfcAdapter.getDefaultAdapter(it)?.isEnabled } == true

/**
 * The check for a physical key: hold the sticker to the phone, or scan the printed code.
 *
 * @param onScanStarting called just before the scanner opens over this screen.
 */
@Composable
fun KeyCheck(
    method: UnlockMethodId,
    failed: Boolean,
    onNfcTag: (ByteArray) -> Unit,
    onQrScanned: (String) -> Unit,
    onScanStarting: () -> Unit = {},
) {
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result -> result.contents?.let(onQrScanned) }
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (method) {
            UnlockMethodId.NFC_KEY -> {
                NfcReader(onTag = onNfcTag)
                Text(
                    text = stringResource(if (nfcReady()) R.string.key_hold_sticker else R.string.key_nfc_off),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
            }
            UnlockMethodId.QR_KEY -> {
                Text(
                    text = stringResource(R.string.key_scan_code),
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                )
                OutlinedButton(
                    onClick = {
                        onScanStarting()
                        scanner.launch(
                            ScanOptions()
                                .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                .setPrompt("")
                                .setBeepEnabled(false)
                                .setOrientationLocked(false),
                        )
                    },
                ) { Text(stringResource(R.string.key_scan)) }
            }
            else -> Unit
        }
        if (failed) {
            Text(
                text = stringResource(R.string.key_not_yours),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
