package app.minimise.protection

import android.content.Context
import app.minimise.core.Defaults
import app.minimise.core.di.ApplicationScope
import app.minimise.core.time.TimeSource
import app.minimise.protection.domain.ProtectionLevel
import app.minimise.settings.permissions.PermissionStatus
import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventLog
import app.minimise.usage.domain.EventType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Works out the [ProtectionLevel] from the permissions, logs [EventType.PROTECTION_CHANGED] when it
 * changes, and starts or stops the Lite watcher to match. There's no broadcast for most of these
 * permissions, so [refresh] runs on app start, on every visit home, on unlock and whenever the
 * accessibility service connects or stops.
 */
@Singleton
class ProtectionStatus @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissions: PermissionStatus,
    private val runtime: Provider<ProtectionRuntime>,
    private val eventLog: EventLog,
    private val timeSource: TimeSource,
    @ApplicationScope private val appScope: CoroutineScope,
) {
    private val logLock = Mutex()

    fun current(): ProtectionLevel = level(accessibility = permissions.isAccessibilityEnabled())

    /**
     * Re-reads the permissions and brings the Lite watcher in line.
     *
     * @param accessibilityStopped the accessibility service just stopped; the system's list of enabled
     * services can lag a moment behind, so treat it as off now and look again shortly.
     */
    fun refresh(accessibilityStopped: Boolean = false) {
        val level = if (accessibilityStopped) level(accessibility = false) else current()
        runCatching {
            when {
                level != ProtectionLevel.LITE -> ProtectionService.stop(context)
                runtime.get().running != ProtectionLevel.LITE -> ProtectionService.start(context)
            }
        }.onFailure { Timber.w(it, "Could not switch the Lite watcher") }
        appScope.launch { logIfChanged(level) }
        if (accessibilityStopped) {
            appScope.launch {
                delay(Defaults.ACCESSIBILITY_SETTLE)
                refresh()
            }
        }
    }

    private fun level(accessibility: Boolean): ProtectionLevel =
        ProtectionLevel.from(accessibility = accessibility, usageAccess = permissions.hasUsageAccess(), overlay = permissions.canDrawOverlays())

    private suspend fun logIfChanged(level: ProtectionLevel) = logLock.withLock {
        val now = timeSource.now()
        if (lastLogged(now) == level) return@withLock
        eventLog.log(
            Event(
                type = EventType.PROTECTION_CHANGED,
                startAt = now,
                data = buildJsonObject {
                    put(KEY_LEVEL, level.name)
                    if (level != ProtectionLevel.FULL && permissions.isAdvancedProtectionOn()) put(KEY_ADVANCED_PROTECTION, true)
                }.toString(),
            ),
        )
    }

    private suspend fun lastLogged(now: Instant): ProtectionLevel? =
        eventLog.latest(EventType.PROTECTION_CHANGED, before = now.plusMillis(1))?.data?.let(::levelOf)

    companion object {
        private const val KEY_LEVEL = "level"
        private const val KEY_ADVANCED_PROTECTION = "advancedProtection"

        /** The level a [EventType.PROTECTION_CHANGED] event's data records. */
        fun levelOf(data: String): ProtectionLevel? = runCatching {
            val name = Json.parseToJsonElement(data).jsonObject[KEY_LEVEL]?.jsonPrimitive?.content
            ProtectionLevel.entries.firstOrNull { it.name == name }
        }.getOrNull()
    }
}
