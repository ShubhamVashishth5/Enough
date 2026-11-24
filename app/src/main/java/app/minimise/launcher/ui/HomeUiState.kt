package app.minimise.launcher.ui

import app.minimise.apps.domain.AppEntry
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
)

data class HomeSlot(val index: Int, val app: AppEntry?)

data class SlotPicker(val slot: Int, val choices: List<AppEntry>)

data class TodayNumbers(val vaultTime: Duration, val unlocks: Int)
