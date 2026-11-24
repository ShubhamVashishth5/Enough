package app.minimise.focus.domain

import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.time.plus
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Starts and ends focus sessions. Ending is lazy: [finishIfDue] runs on unlock, on app start and from the service. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class FocusSessions @Inject constructor(
    private val repository: FocusRepository,
    private val settingsStore: SettingsStore,
    private val appRepository: AppRepository,
    private val doNotDisturb: DoNotDisturb,
    private val eventLog: EventLog,
    private val timeSource: TimeSource,
) {
    private val mutex = Mutex()

    /** The running session, turning to null the moment it ends. */
    val active: Flow<FocusSession?> = repository.open.flatMapLatest { session ->
        if (session == null) {
            flowOf(null)
        } else {
            flow {
                val left = durationBetween(timeSource.now(), session.endAt)
                if (left > Duration.ZERO) {
                    emit(session)
                    delay(left)
                }
                emit(null)
            }
        }
    }

    val lastEnded: Flow<FocusSession?> = repository.lastEnded

    suspend fun current(): FocusSession? = repository.open()?.takeIf { it.isActive(timeSource.now()) }

    suspend fun start(length: Duration, locked: Boolean) = mutex.withLock {
        finishIfDueLocked()
        if (repository.open() != null) return@withLock
        val now = timeSource.now()
        repository.start(
            startAt = now,
            endAt = now + length,
            locked = locked,
            allowlist = allowlist(),
            previousInterruptionFilter = if (doNotDisturb.isAvailable()) doNotDisturb.enable() else null,
        )
    }

    /** Ends the running session now. For a locked one, call only after a physical key was checked. */
    suspend fun endEarly() = mutex.withLock {
        repository.open()?.let { finish(it, timeSource.now(), early = true) }
    }

    suspend fun finishIfDue() = mutex.withLock { finishIfDueLocked() }

    private suspend fun finishIfDueLocked() {
        val session = repository.open() ?: return
        if (!timeSource.now().isBefore(session.endAt)) finish(session, session.endAt, early = false)
    }

    private suspend fun finish(session: FocusSession, at: Instant, early: Boolean) {
        if (!repository.end(session.id, at)) return
        session.previousInterruptionFilter?.let(doNotDisturb::restore)
        eventLog.log(
            Event(
                type = EventType.FOCUS_SESSION,
                startAt = session.startAt,
                endAt = at,
                data = buildJsonObject {
                    put("plannedMinutes", durationBetween(session.startAt, session.endAt).inWholeMinutes)
                    put("locked", session.locked)
                    put("endedEarly", early)
                }.toString(),
            ),
        )
    }

    /** The chosen allowlist, or the Tool apps on the home screen until one is chosen. */
    private suspend fun allowlist(): Set<String> =
        settingsStore.current().focusAllowlist
            ?: appRepository.apps.first().filter { it.homeSlot != null && it.kind == AppKind.TOOL }.map { it.packageName }.toSet()
}
