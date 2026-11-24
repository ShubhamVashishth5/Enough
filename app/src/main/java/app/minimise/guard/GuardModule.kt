package app.minimise.guard

import app.minimise.guard.data.AssetGuardScreenSource
import app.minimise.guard.domain.GuardScreenSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class GuardModule {
    @Binds
    abstract fun guardScreenSource(impl: AssetGuardScreenSource): GuardScreenSource
}
