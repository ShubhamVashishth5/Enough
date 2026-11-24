package app.minimise.intervention.domain

import app.minimise.core.Defaults
import kotlin.time.Duration

object TimeBoxOptions {
    /**
     * Every option shorter than [budgetLeft], then [budgetLeft] itself if it's shorter than the longest.
     * With no budget left at all (an override past a budget block), the usual options.
     */
    fun cappedAt(budgetLeft: Duration, options: List<Duration> = Defaults.TIME_BOX_OPTIONS): List<Duration> {
        if (budgetLeft <= Duration.ZERO) return options
        val under = options.filter { it < budgetLeft }
        return if (budgetLeft < options.last()) under + budgetLeft else under
    }
}
