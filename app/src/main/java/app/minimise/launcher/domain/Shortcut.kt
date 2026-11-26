package app.minimise.launcher.domain

/**
 * One of the small icons on the right of home: one of Enough's own [icon]s, opening [packageName], or
 * when that's null, the phone's default app for what the icon stands for (its [ShortcutIcon.default]).
 */
data class Shortcut(val icon: ShortcutIcon, val packageName: String? = null) {
    companion object {
        /** More than this and the icons stop being a short, neat column. */
        const val MAX = 5

        val DEFAULTS: List<Shortcut> = listOf(
            Shortcut(ShortcutIcon.PHONE),
            Shortcut(ShortcutIcon.MESSAGES),
            Shortcut(ShortcutIcon.CAMERA),
        )
    }
}

/** How a phone finds its own app for an icon, when the user hasn't chosen one. */
enum class DefaultApp {
    DIALER,
    SMS,
    CAMERA,
    BROWSER,
    MUSIC,
    MAPS,
    EMAIL,
    CALENDAR,
    CONTACTS,
    GALLERY,
    CALCULATOR,
    FILES,
    WEATHER,
    FITNESS,
    CLOCK,
    SETTINGS,
}

/**
 * Enough's own icons, each for a kind of app. Stored by name, so never rename an entry. Icons with no
 * [default] open only an app the user chooses.
 */
enum class ShortcutIcon(val default: DefaultApp?) {
    PHONE(DefaultApp.DIALER),
    MESSAGES(DefaultApp.SMS),
    CAMERA(DefaultApp.CAMERA),
    BROWSER(DefaultApp.BROWSER),
    MUSIC(DefaultApp.MUSIC),
    MAPS(DefaultApp.MAPS),
    MAIL(DefaultApp.EMAIL),
    CALENDAR(DefaultApp.CALENDAR),
    CLOCK(DefaultApp.CLOCK),
    PHOTOS(DefaultApp.GALLERY),
    CONTACTS(DefaultApp.CONTACTS),
    CALCULATOR(DefaultApp.CALCULATOR),
    FILES(DefaultApp.FILES),
    SETTINGS(DefaultApp.SETTINGS),
    WEATHER(DefaultApp.WEATHER),
    FITNESS(DefaultApp.FITNESS),
    CHAT(null),
    NOTES(null),
    TASKS(null),
    PODCASTS(null),
    RADIO(null),
    VIDEO(null),
    BOOKS(null),
    NEWS(null),
    RECORDER(null),
    TRANSLATE(null),
    WALLET(null),
    BANK(null),
    SHOPPING(null),
    FOOD(null),
    RIDES(null),
    GAMES(null),
    CLOUD(null),
    KEY(null),
    HOME(null),
    STAR(null),
}

/**
 * Shortcuts as one stored string: "PHONE:,MESSAGES:org.thoughtcrime.securesms". Package names never hold
 * ':' or ','. An icon this version doesn't know is skipped, and so is anything past [Shortcut.MAX].
 */
object ShortcutCodec {
    fun encode(shortcuts: List<Shortcut>): String =
        shortcuts.joinToString(",") { "${it.icon.name}:${it.packageName.orEmpty()}" }

    fun decode(text: String): List<Shortcut> =
        text.split(',')
            .filter { it.isNotBlank() }
            .mapNotNull { item ->
                val icon = ShortcutIcon.entries.firstOrNull { it.name == item.substringBefore(':') } ?: return@mapNotNull null
                Shortcut(icon, item.substringAfter(':', "").takeIf { it.isNotBlank() })
            }
            .take(Shortcut.MAX)
}
