package app.minimise.modes.data

import app.minimise.core.Defaults
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.TimeSource
import app.minimise.modes.domain.ActiveModeProvider
import app.minimise.modes.domain.Mode
import app.minimise.modes.domain.ModeRepository
import app.minimise.modes.domain.ModeValues
import javax.inject.Inject

class DefaultActiveModeProvider @Inject constructor(
    private val modes: ModeRepository,
    private val settingsStore: SettingsStore,
    private val timeSource: TimeSource,
) : ActiveModeProvider {
    override suspend fun active(): Mode? = activeWith(settingsStore.current().manualModeId)

    override suspend fun values(): ModeValues = active()?.values ?: baseValues()

    override suspend fun valuesWith(manualModeId: Long?): ModeValues = activeWith(manualModeId)?.values ?: baseValues()

    override suspend fun baseValues(): ModeValues =
        ModeValues(budget = settingsStore.current().dailyBudget, waitSteps = Defaults.WAIT_STEPS, allowlist = null)

    private suspend fun activeWith(manualModeId: Long?): Mode? {
        val all = modes.all()
        manualModeId?.let { id -> all.firstOrNull { it.id == id }?.let { return it } }
        val now = timeSource.now().atZone(timeSource.zone()).toLocalDateTime()
        return all.firstOrNull { it.schedule?.isActive(now) == true }
    }
}
