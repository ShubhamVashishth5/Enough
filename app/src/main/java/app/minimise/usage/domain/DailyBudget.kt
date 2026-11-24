package app.minimise.usage.domain

import app.minimise.modes.domain.ActiveModeProvider
import javax.inject.Inject
import kotlin.time.Duration

/** The one shared allowance across all Vault apps, so hopping between them can't dodge it. */
class DailyBudget @Inject constructor(
    private val activeMode: ActiveModeProvider,
    private val todayUsage: TodayUsageProvider,
) {
    /** Never negative. Uses the active mode's budget. */
    suspend fun remaining(): Duration = remaining(activeMode.values().budget, todayUsage.today())

    companion object {
        fun remaining(budget: Duration, today: TodayUsage): Duration =
            (budget - today.vaultTime).coerceAtLeast(Duration.ZERO)
    }
}
