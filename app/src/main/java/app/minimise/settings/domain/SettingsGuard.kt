package app.minimise.settings.domain

import app.minimise.core.time.TimeSource
import app.minimise.core.time.plus
import app.minimise.unlock.domain.UnlockMethodId
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import javax.inject.Inject
import javax.inject.Singleton
import java.time.Instant
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The only way to change a rule setting. Tightening applies at once; loosening waits ([LoosenDelay],
 * 24 hours by default) so a weak moment can't undo a calm decision. During a [LockPeriod] nothing
 * loosens at all: requests are refused and waiting changes keep waiting until the lock ends.
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
    private val lockPeriod: LockPeriod,
) {
    private val mutex = Mutex()
    private val _refusals = MutableSharedFlow<GuardResult.Refused>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val pending: Flow<List<PendingChange>> get() = store.observeAll()

    /** Every refused request, so the app can say why nothing happened wherever the user tapped. */
    val refusals: SharedFlow<GuardResult.Refused> = _refusals.asSharedFlow()

    /** The end of the lock period in force now, or null. */
    suspend fun lockedUntil(): Instant? = lockPeriod.lockedUntil()?.takeIf { it.isAfter(timeSource.now()) }

    suspend fun request(change: SettingChange): GuardResult = mutex.withLock {
        applyDueLocked()
        val direction = target.direction(change)
        if (direction == Direction.LOOSEN) lockedUntil()?.let { return@withLock refuse(it) }
        // A new request for the same setting replaces any pending one. It can't shorten a wait:
        // a new loosen starts its own full delay.
        store.removeKey(change.key)
        when (direction) {
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

    /**
     * Applies a waiting loosen now, skipping the rest of its wait. Only call once [method] (a friend
     * code, typing penance or a physical key) has been passed. Refused during a lock period.
     */
    suspend fun applyNow(pendingId: Long, method: UnlockMethodId): GuardResult = mutex.withLock {
        lockedUntil()?.let { return@withLock refuse(it) }
        val pending = store.all().firstOrNull { it.id == pendingId } ?: return@withLock GuardResult.AppliedNow
        store.remove(pending.id)
        target.direction(pending.change)?.let { applyLocked(pending.change, it, method) }
        GuardResult.AppliedNow
    }

    suspend fun applyDue() = mutex.withLock { applyDueLocked() }

    private suspend fun applyDueLocked() {
        val now = timeSource.now()
        val locked = lockedUntil() != null
        store.all()
            .filter { !it.applyAt.isAfter(now) }
            .forEach { due ->
                // Re-check: the setting may already be at the new value.
                val direction = target.direction(due.change)
                if (direction == Direction.LOOSEN && locked) return@forEach
                store.remove(due.id)
                direction?.let { applyLocked(due.change, it) }
            }
    }

    private fun refuse(until: Instant): GuardResult.Refused =
        GuardResult.Refused(GuardResult.LOCKED, until).also { _refusals.tryEmit(it) }

    private suspend fun applyLocked(change: SettingChange, direction: Direction, method: UnlockMethodId? = null) {
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
                    method?.let { put("method", it.name) }
                    put("change", Json.encodeToJsonElement(SettingChange.serializer(), change))
                }.toString(),
            ),
        )
    }
}
