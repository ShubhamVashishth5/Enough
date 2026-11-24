package app.minimise.calendar

import app.minimise.calendar.data.AlarmCalendarFocusScheduler
import app.minimise.calendar.data.ContentCalendarSource
import app.minimise.calendar.domain.CalendarFocusScheduler
import app.minimise.calendar.domain.CalendarSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class CalendarModule {
    @Binds
    abstract fun calendarSource(impl: ContentCalendarSource): CalendarSource

    @Binds
    abstract fun calendarFocusScheduler(impl: AlarmCalendarFocusScheduler): CalendarFocusScheduler
}
