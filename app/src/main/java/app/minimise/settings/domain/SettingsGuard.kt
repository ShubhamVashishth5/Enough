package app.minimise.settings.domain

import app.minimise.core.time.TimeSource
import app.minimise.core.time.plus
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The only way to change a rule setting. Tightening applies at once; loosening waits ([LoosenDelay],
 * 24 hours by default) so a weak moment can't undo a calm decision.
 *
 * Pending changes apply lazily: call [applyDue] on app start, on screen unlock and before every gate check.
 */
@Singleton
class SettingsGuard @Inject constructor(
    private val target: SettingsTarget,
    private val store: PendingChangeStore,
    private val timeSource: TimeSource,
    private val loosenDelay: LoosenDelay,
    private val eventLog: EventLog,
) {
    private val mutex = Mutex()

    val pending: Flow<List<PendingChange>> get() = store.observeAll()

    suspend fun request(change: SettingChange): GuardResult = mutex.withLock {
        applyDueLocked()
        // A new request for the same setting replaces any pending one. It can't shorten a wait:
        // a new loosen starts its own full delay.
        store.removeKey(change.key)
        when (target.direction(change)) {
            null -> GuardResult.AppliedNow
            Direction.TIGHTEN -> {
                applyLocked(change, Direction.TIGHTEN)
                GuardResult.AppliedNow
            }
            Direction.LOOSEN -> {
                val now = timeSource.now()
                val applyAt = now + loosenDelay.current()
                store.add(change, requestedAt = now, applyAt = applyAt)
                GuardResult.Scheduled(applyAt)
            }
        }
    }

    /** Cancelling a pending loosen is always allowed: it can only keep things stricter. */
    suspend fun cancel(pendingId: Long) = mutex.withLock { store.remove(pendingId) }

    suspend fun applyDue() = mutex.withLock { applyDueLocked() }

    private suspend fun applyDueLocked() {
        val now = timeSource.now()
        store.all()
            .filter { !it.applyAt.isAfter(now) }
            .forEach { due ->
                store.remove(due.id)
                // Re-check: the setting may already be at the new value.
                val direction = target.direction(due.change) ?: return@forEach
                applyLocked(due.change, direction)
            }
    }

    private suspend fun applyLocked(change: SettingChange, direction: Direction) {
        target.apply(change)
        eventLog.log(
            Event(
                type = EventType.SETTING_CHANGED,
                startAt = timeSource.now(),
                packageName = when (change) {
                    is SetAppKind -> change.packageName
                    is SetOpenLimit -> change.packageName
                    else -> null
                },
                data = buildJsonObject {
                    put("direction", direction.name)
                    put("change", Json.encodeToJsonElement(SettingChange.serializer(), change))
                }.toString(),
            ),
        )
    }
}
