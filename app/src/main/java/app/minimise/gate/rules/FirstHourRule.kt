package app.minimise.gate.rules

import app.minimise.apps.domain.AppKind
import app.minimise.core.Defaults
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.plus
import app.minimise.gate.domain.Decision
import app.minimise.gate.domain.GateContext
import app.minimise.gate.domain.Rule
import app.minimise.unlock.domain.UnlockMethodId
import javax.inject.Inject
import kotlin.time.Duration

/** When switched on, no Vault apps until an hour after the day's first unlock. */
class FirstHourRule @Inject constructor(
    private val settingsStore: SettingsStore,
) : Rule {
    override val id: String = "first_hour"

    override suspend fun evaluate(ctx: GateContext): Decision =
        if (settingsStore.current().firstHourRule) evaluate(ctx) else Decision.Allow

    companion object {
        const val REASON = "first_hour"

        fun evaluate(ctx: GateContext, length: Duration = Defaults.FIRST_HOUR): Decision {
            val firstUnlock = ctx.today.firstUnlockAt ?: return Decision.Allow
            return if (ctx.app.kind == AppKind.VAULT && ctx.now.isBefore(firstUnlock + length)) {
                Decision.Block(REASON, listOf(UnlockMethodId.EMERGENCY_PASS))
            } else {
                Decision.Allow
            }
        }
    }
}
