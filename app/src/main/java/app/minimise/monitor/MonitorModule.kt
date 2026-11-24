package app.minimise.monitor

import app.minimise.monitor.data.ForegroundEventBus
import app.minimise.monitor.domain.ForegroundAppSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class MonitorModule {
    @Binds
    abstract fun foregroundAppSource(impl: ForegroundEventBus): ForegroundAppSource
}
