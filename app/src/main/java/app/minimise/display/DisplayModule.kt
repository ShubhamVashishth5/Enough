package app.minimise.display

import app.minimise.display.data.NoOpDisplayFilter
import app.minimise.display.data.SecureSettingsDisplayFilter
import app.minimise.display.domain.DisplayFilter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object DisplayModule {
    /** Not a singleton: inject a Provider to notice an ADB grant made while the app runs. */
    @Provides
    fun displayFilter(secure: SecureSettingsDisplayFilter, noOp: NoOpDisplayFilter): DisplayFilter =
        if (secure.isAvailable()) secure else noOp
}
