package app.minimise.testing

import app.minimise.core.Defaults
import app.minimise.modes.domain.ActiveModeProvider
import app.minimise.modes.domain.Mode
import app.minimise.modes.domain.ModeValues

/** No mode on: the default budget and wait steps. */
class FakeActiveMode(var mode: Mode? = null) : ActiveModeProvider {
    private val base = ModeValues(Defaults.DAILY_BUDGET, Defaults.WAIT_STEPS, allowlist = null)

    override suspend fun active(): Mode? = mode

    override suspend fun values(): ModeValues = mode?.values ?: base

    override suspend fun baseValues(): ModeValues = base
}
