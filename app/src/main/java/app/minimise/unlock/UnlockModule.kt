package app.minimise.unlock

import app.minimise.unlock.domain.EmergencyPassMethod
import app.minimise.unlock.domain.UnlockMethod
import app.minimise.unlock.domain.UnlockMethodId
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
}
