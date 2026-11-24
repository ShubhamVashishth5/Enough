package app.minimise.focus

import app.minimise.focus.data.AndroidEssentialApps
import app.minimise.focus.data.RoomFocusRepository
import app.minimise.focus.data.SystemDoNotDisturb
import app.minimise.focus.domain.DoNotDisturb
import app.minimise.focus.domain.EssentialApps
import app.minimise.focus.domain.FocusRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class FocusModule {
    @Binds
    abstract fun focusRepository(impl: RoomFocusRepository): FocusRepository

    @Binds
    abstract fun doNotDisturb(impl: SystemDoNotDisturb): DoNotDisturb

    @Binds
    abstract fun essentialApps(impl: AndroidEssentialApps): EssentialApps
}
