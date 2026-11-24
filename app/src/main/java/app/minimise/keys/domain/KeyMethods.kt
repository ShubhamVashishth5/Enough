package app.minimise.keys.domain

import app.minimise.unlock.domain.UnlockMethod
import app.minimise.unlock.domain.UnlockMethodId
import javax.inject.Inject

/** Tap a paired NFC sticker. Checks the tag's ID against the stored hashes. */
class NfcKeyMethod @Inject constructor(private val keys: KeyRepository) : UnlockMethod {
    override val id: UnlockMethodId = UnlockMethodId.NFC_KEY

    override suspend fun isAvailable(): Boolean = keys.all().any { it.type == KeyType.NFC }

    suspend fun verify(tagId: ByteArray): Boolean = matches(keys.all(), KeyType.NFC, KeySecrets.hashOf(tagId))
}

/** Scan the printed QR code. Checks the scanned text against the stored hashes. */
class QrKeyMethod @Inject constructor(private val keys: KeyRepository) : UnlockMethod {
    override val id: UnlockMethodId = UnlockMethodId.QR_KEY

    override suspend fun isAvailable(): Boolean = keys.all().any { it.type == KeyType.QR }

    suspend fun verify(scanned: String): Boolean = matches(keys.all(), KeyType.QR, KeySecrets.hashOf(scanned))
}

fun matches(keys: List<PhysicalKey>, type: KeyType, hash: String): Boolean = keys.any { it.type == type && it.secretHash == hash }
