package app.minimise.usage.domain

import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import javax.inject.Inject
import kotlin.time.Duration

/** The one shared allowance across all Vault apps, so hopping between them can't dodge it. */
class DailyBudget @Inject constructor(
    private val settingsStore: SettingsStore,
    private val todayUsage: TodayUsageProvider,
) {
    /** Never negative. */
    suspend fun remaining(): Duration = remaining(settingsStore.current().dailyBudget, todayUsage.today())

    companion object {
        fun remaining(budget: Duration, today: TodayUsage): Duration =
            (budget - today.vaultTime).coerceAtLeast(Duration.ZERO)
    }
}
