package app.minimise.launcher.ui

import app.minimise.apps.domain.AppEntry
import app.minimise.core.prefs.HomeOptions
import app.minimise.launcher.domain.ShortcutIcon
import app.minimise.protection.domain.ProtectionLevel
import app.minimise.tasks.domain.TodayTask
import java.time.LocalDateTime
import kotlin.time.Duration

data class HomeUiState(
    val now: LocalDateTime? = null,
    val tasks: List<TodayTask> = emptyList(),
    val slots: List<HomeSlot> = emptyList(),
    /** The two numbers: "Vault 23 min · Unlock 31". Null until loaded. */
    val numbers: TodayNumbers? = null,
    val isDefaultHome: Boolean = true,
    /** How much the gate can see. Below Full, home says what isn't covered. */
    val protection: ProtectionLevel = ProtectionLevel.FULL,
    /** Non-null while the user is choosing an app for a slot. */
    val slotPicker: SlotPicker? = null,
    /** The day's first visit: open the Today screen. */
    val promptToday: Boolean = false,
    /** The home screen's switchable parts. */
    val options: HomeOptions = HomeOptions(),
    /** The icons on the right, each with the app it opens; icons with no app to open are left out. */
    val shortcuts: List<HomeShortcut> = emptyList(),
    /** Non-null while the user is choosing the app for an icon. */
    val shortcutPicker: ShortcutPicker? = null,
)

data class HomeSlot(val index: Int, val app: AppEntry?)

data class SlotPicker(val slot: Int, val choices: List<AppEntry>)

data class TodayNumbers(val vaultTime: Duration, val unlocks: Int)

/** [index] is the icon's place in the user's list, counting icons that aren't shown. */
data class HomeShortcut(val index: Int, val icon: ShortcutIcon, val app: AppEntry)

/** [automatic] is the phone's own app for the icon, when it has one. */
data class ShortcutPicker(val index: Int, val icon: ShortcutIcon, val automatic: AppEntry?, val choices: List<AppEntry>)
