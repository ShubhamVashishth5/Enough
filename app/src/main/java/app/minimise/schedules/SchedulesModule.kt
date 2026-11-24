package app.minimise.schedules

import app.minimise.schedules.data.RoomScheduleRepository
import app.minimise.schedules.domain.ScheduleRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulesModule {
    @Binds
    abstract fun scheduleRepository(impl: RoomScheduleRepository): ScheduleRepository
}
