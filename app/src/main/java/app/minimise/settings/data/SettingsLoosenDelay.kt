package app.minimise.settings.data

import app.minimise.BuildConfig
import app.minimise.core.Defaults
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.settings.domain.LockPeriod
import app.minimise.settings.domain.LoosenDelay
import java.time.Instant
import javax.inject.Inject
import kotlin.time.Duration

/** 24 hours, or 1 minute in debug builds when the hidden debug switch is on. */
class SettingsLoosenDelay @Inject constructor(
    private val settingsStore: SettingsStore,
) : LoosenDelay {
    override suspend fun current(): Duration =
        if (BuildConfig.DEBUG && settingsStore.current().debugShortLoosenDelay) {
            Defaults.DEBUG_LOOSEN_DELAY
        } else {
            Defaults.LOOSEN_DELAY
        }
}

class SettingsLockPeriod @Inject constructor(
    private val settingsStore: SettingsStore,
) : LockPeriod {
    override suspend fun lockedUntil(): Instant? = settingsStore.current().lockedUntil
}
