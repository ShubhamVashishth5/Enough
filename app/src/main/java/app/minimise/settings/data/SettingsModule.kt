package app.minimise.settings.data

import app.minimise.settings.domain.LoosenDelay
import app.minimise.settings.domain.PendingChangeStore
import app.minimise.settings.domain.SettingsTarget
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {
    @Binds
    abstract fun settingsTarget(impl: AppSettingsTarget): SettingsTarget

    @Binds
    abstract fun pendingChangeStore(impl: RoomPendingChangeStore): PendingChangeStore

    @Binds
    abstract fun loosenDelay(impl: SettingsLoosenDelay): LoosenDelay
}
