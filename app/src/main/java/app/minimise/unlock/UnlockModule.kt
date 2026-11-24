package app.minimise.unlock

import app.minimise.unlock.data.RoomFriendCodeRepository
import app.minimise.unlock.domain.EmergencyPassMethod
import app.minimise.unlock.domain.FriendCodeMethod
import app.minimise.unlock.domain.FriendCodeRepository
import app.minimise.unlock.domain.TypingMethod
import app.minimise.unlock.domain.UnlockMethod
import app.minimise.unlock.domain.UnlockMethodId
import app.minimise.unlock.domain.WaitMethod
import dagger.Binds
import dagger.MapKey
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap

@MapKey
annotation class UnlockMethodKey(val value: UnlockMethodId)

/** The method registry: adding an unlock method means one class and one @IntoMap binding here. */
@Module
@InstallIn(SingletonComponent::class)
abstract class UnlockModule {
    @Binds
    @IntoMap
    @UnlockMethodKey(UnlockMethodId.EMERGENCY_PASS)
    abstract fun emergencyPass(method: EmergencyPassMethod): UnlockMethod

    @Binds
    @IntoMap
    @UnlockMethodKey(UnlockMethodId.WAIT)
    abstract fun waitMethod(method: WaitMethod): UnlockMethod

    @Binds
    @IntoMap
    @UnlockMethodKey(UnlockMethodId.FRIEND_CODE)
    abstract fun friendCode(method: FriendCodeMethod): UnlockMethod

    @Binds
    @IntoMap
    @UnlockMethodKey(UnlockMethodId.TYPING)
    abstract fun typing(method: TypingMethod): UnlockMethod

    @Binds
    abstract fun friendCodeRepository(impl: RoomFriendCodeRepository): FriendCodeRepository
}
