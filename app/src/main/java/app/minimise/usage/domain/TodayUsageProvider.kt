package app.minimise.usage.domain

import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.time.DayBoundary
import app.minimise.core.time.TimeSource
import javax.inject.Inject
import kotlinx.coroutines.flow.first

class TodayUsageProvider @Inject constructor(
    private val eventLog: EventLog,
    private val dayBoundary: DayBoundary,
    private val timeSource: TimeSource,
    private val appRepository: AppRepository,
    private val usageTracker: UsageTracker,
) {
    suspend fun today(): TodayUsage {
        val now = timeSource.now()
        val day = dayBoundary.dayOf(now)
        val start = dayBoundary.startOf(day)
        val end = dayBoundary.endOf(day)
        val vault = appRepository.kinds.first().filterValues { it == AppKind.VAULT }.keys
        return TodayUsage.from(
            events = eventLog.between(start, end),
            dayStart = start,
            dayEnd = end,
            now = now,
            vaultPackages = vault,
            openSpanId = usageTracker.openSpan.value?.eventId,
        )
    }
}
