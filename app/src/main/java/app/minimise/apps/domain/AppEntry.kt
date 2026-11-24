package app.minimise.apps.domain

/**
 * An installed, launchable app merged with what the user chose for it.
 *
 * @property homeSlot 0-based position on the home screen, or null when it isn't on home.
 * @property openLimit most proceeded opens a day, or null for no limit.
 */
data class AppEntry(
    val packageName: String,
    val label: String,
    val kind: AppKind,
    val homeSlot: Int?,
    val openLimit: Int? = null,
)
