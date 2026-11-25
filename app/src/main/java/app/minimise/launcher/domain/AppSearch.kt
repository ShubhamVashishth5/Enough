package app.minimise.launcher.domain

import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import java.util.Locale

/**
 * @property autoLaunch set when exactly one Tool or Breathe app matches and nothing else does: open it at
 * once (a Breathe app still pauses first). Not while the text could still become a Vault app's name.
 */
data class SearchResult(
    val matches: List<AppEntry>,
    val autoLaunch: AppEntry?,
)

/**
 * Type-to-find, with no app drawer behind it.
 * - Tool and Breathe apps match on any part of the label.
 * - Vault apps match only on their full label (ignoring case), and never auto-launch.
 * - Hidden apps never match.
 */
object AppSearch {
    fun search(query: String, apps: List<AppEntry>): SearchResult {
        val needle = query.trim().normalised()
        if (needle.isEmpty()) return SearchResult(matches = emptyList(), autoLaunch = null)

        val matches = apps.filter { app ->
            val label = app.label.normalised()
            when (app.kind) {
                AppKind.TOOL, AppKind.BREATHE -> needle in label
                AppKind.VAULT -> label == needle
                AppKind.HIDDEN -> false
            }
        }.sortedBy { if (it.label.normalised().startsWith(needle)) 0 else 1 }

        // A Vault name is typed through its first letters ("dis" on the way to "discord"), and those can
        // match a single Tool app ("District"). Opening that would cut the name off before it's whole.
        val vaultAhead = apps.any { it.kind == AppKind.VAULT && it.label.normalised().startsWith(needle) }
        val autoLaunch = matches.singleOrNull()?.takeIf { it.kind in AppKind.VISIBLE && !vaultAhead }
        return SearchResult(matches = matches, autoLaunch = autoLaunch)
    }

    private fun String.normalised(): String = trim().lowercase(Locale.ROOT)
}
