package app.minimise.usage.domain

import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.modes.domain.ActiveModeProvider
import javax.inject.Inject
import kotlin.time.Duration

/** The one shared allowance across all Vault apps, so hopping between them can't dodge it. */
class DailyBudget @Inject constructor(
    private val activeMode: ActiveModeProvider,
    private val todayUsage: TodayUsageProvider,
    private val settingsStore: SettingsStore,
) {
    /** Never negative. Uses the active mode's budget, plus minutes earned today when earn-it is on. */
    suspend fun remaining(): Duration {
        val today = todayUsage.today()
        return remaining(allowance(today), today)
    }

    /** Today's whole allowance: the active mode's budget, plus [TodayUsage.bonus] when earn-it is on. */
    suspend fun allowance(today: TodayUsage): Duration =
        activeMode.values().budget + if (settingsStore.current().earnIt) today.bonus else Duration.ZERO

    companion object {
        fun remaining(budget: Duration, today: TodayUsage): Duration =
            (budget - today.vaultTime).coerceAtLeast(Duration.ZERO)
    }
}
