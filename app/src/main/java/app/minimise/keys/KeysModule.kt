package app.minimise.keys

import app.minimise.keys.data.RoomKeyRepository
import app.minimise.keys.domain.KeyRepository
import app.minimise.keys.domain.NfcKeyMethod
import app.minimise.keys.domain.QrKeyMethod
import app.minimise.unlock.UnlockMethodKey
import app.minimise.unlock.domain.UnlockMethod
import app.minimise.unlock.domain.UnlockMethodId
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@Module
@InstallIn(SingletonComponent::class)
abstract class KeysModule {
    @Binds
    abstract fun keyRepository(impl: RoomKeyRepository): KeyRepository

    @Binds
    @IntoMap
    @UnlockMethodKey(UnlockMethodId.NFC_KEY)
    abstract fun nfcKey(method: NfcKeyMethod): UnlockMethod

    @Binds
    @IntoMap
    @UnlockMethodKey(UnlockMethodId.QR_KEY)
    abstract fun qrKey(method: QrKeyMethod): UnlockMethod
}
