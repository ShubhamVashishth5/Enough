package app.minimise.core.prefs

/**
 * How the app looks: its typeface and text sizes. Not rules, so changes apply at once and a lock period
 * doesn't hold them. Every choice is stored by name, so never rename an entry.
 */
data class Appearance(
    val font: FontChoice = FontChoice.SYSTEM,
    /** Every piece of text in the app, on top of the phone's own font size. */
    val textSize: TextSize = TextSize.DEFAULT,
    /** Home's clock. */
    val clockSize: HomeTextSize = HomeTextSize.MEDIUM,
    /** The app names on home. */
    val appNameSize: HomeTextSize = HomeTextSize.MEDIUM,
    /** Home's icons on the right, and its lock button. */
    val iconSize: HomeTextSize = HomeTextSize.MEDIUM,
)

/** Fonts every Android phone has, so nothing is downloaded or bundled. */
enum class FontChoice {
    /** The phone's own font, whatever the maker or the user chose. */
    SYSTEM,

    /** The same font in thin and light weights for large text. */
    LIGHT,
    CONDENSED,
    SERIF,
    MONO,
}

enum class TextSize(val scale: Float) {
    SMALLER(0.9f),
    DEFAULT(1f),
    LARGER(1.12f),
    LARGEST(1.25f),
}

enum class HomeTextSize {
    SMALL,
    MEDIUM,
    LARGE,
}

/** [T]'s entry called [name], or [fallback] for a name this version doesn't know. */
inline fun <reified T : Enum<T>> enumByName(name: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: fallback
