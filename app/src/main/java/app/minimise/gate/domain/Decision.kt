package app.minimise.gate.domain

import app.minimise.apps.domain.AppEntry
import app.minimise.unlock.domain.UnlockMethodId
import app.minimise.usage.domain.TodayUsage
import java.time.Instant
import kotlin.time.Duration

data class GateContext(val app: AppEntry, val today: TodayUsage, val now: Instant)

sealed interface Decision {
    data object Allow : Decision

    /**
     * @property breatheOnly a Breathe app's pause: after the wait it just opens, with no reason asked, no
     * time box and nothing taken from the budget.
     */
    data class Pause(val wait: Duration, val breatheOnly: Boolean = false) : Decision

    data class Block(val reason: String, val overrides: List<UnlockMethodId>) : Decision
}

/** Every feature that delays or blocks an app is a Rule, registered with a Hilt @IntoSet binding. */
interface Rule {
    val id: String

    suspend fun evaluate(ctx: GateContext): Decision
}
