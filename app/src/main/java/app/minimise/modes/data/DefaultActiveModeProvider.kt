package app.minimise.modes.data

import app.minimise.core.Defaults
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.TimeSource
import app.minimise.modes.domain.ActiveModeProvider
import app.minimise.modes.domain.Mode
import app.minimise.modes.domain.ModeRepository
import app.minimise.modes.domain.ModeTimeline
import app.minimise.modes.domain.ModeValues
import app.minimise.modes.domain.manualPickAt
import javax.inject.Inject

class DefaultActiveModeProvider @Inject constructor(
    private val modes: ModeRepository,
    private val settingsStore: SettingsStore,
    private val timeSource: TimeSource,
) : ActiveModeProvider {
    override suspend fun active(): Mode? {
        val now = timeSource.now()
        return ModeTimeline.activeAt(now, timeSource.zone(), modes.all(), settingsStore.current().manualPickAt(now))
    }

    override suspend fun values(): ModeValues = active()?.values ?: baseValues()

    override suspend fun baseValues(): ModeValues =
        ModeValues(budget = settingsStore.current().dailyBudget, waitSteps = Defaults.WAIT_STEPS, allowlist = null)
}
