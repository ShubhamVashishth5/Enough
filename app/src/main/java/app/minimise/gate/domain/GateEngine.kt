package app.minimise.gate.domain

import app.minimise.apps.domain.AppEntry
import app.minimise.core.time.TimeSource
import app.minimise.settings.domain.SettingsGuard
import app.minimise.usage.domain.TodayUsageProvider
import javax.inject.Inject

/**
 * Decides what happens when an app comes to the front. Runs every rule and keeps the strictest result;
 * an active grant (a running time box) short-circuits to [Decision.Allow].
 */
class GateEngine @Inject constructor(
    private val rules: Set<@JvmSuppressWildcards Rule>,
    private val grantStore: GrantStore,
    private val todayUsage: TodayUsageProvider,
    private val timeSource: TimeSource,
    private val settingsGuard: SettingsGuard,
) {
    suspend fun check(app: AppEntry): Decision {
        settingsGuard.applyDue()
        val now = timeSource.now()
        if (grantStore.get(app.packageName)?.isActive(now) == true) return Decision.Allow
        val ctx = GateContext(app = app, today = todayUsage.today(), now = now)
        return strictest(rules.map { it.evaluate(ctx) })
    }

    companion object {
        /**
         * Any Block wins; with several, the first reason is kept and only overrides every Block allows
         * remain. Otherwise the longest Pause (a full Vault pause over a breathe-only one of the same
         * length), otherwise Allow.
         */
        fun strictest(decisions: List<Decision>): Decision {
            val blocks = decisions.filterIsInstance<Decision.Block>()
            if (blocks.isNotEmpty()) {
                val overrides = blocks.map { it.overrides.toSet() }.reduce { a, b -> a intersect b }
                return Decision.Block(reason = blocks.first().reason, overrides = blocks.first().overrides.filter { it in overrides })
            }
            return decisions.filterIsInstance<Decision.Pause>().maxWithOrNull(compareBy({ it.wait }, { !it.breatheOnly }))
                ?: Decision.Allow
        }
    }
}
