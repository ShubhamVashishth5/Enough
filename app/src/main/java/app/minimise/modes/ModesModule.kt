package app.minimise.modes

import app.minimise.modes.data.DefaultActiveModeProvider
import app.minimise.modes.data.RoomModeRepository
import app.minimise.modes.domain.ActiveModeProvider
import app.minimise.modes.domain.ModeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class ModesModule {
    @Binds
    abstract fun modeRepository(impl: RoomModeRepository): ModeRepository

    @Binds
    abstract fun activeModeProvider(impl: DefaultActiveModeProvider): ActiveModeProvider
}
