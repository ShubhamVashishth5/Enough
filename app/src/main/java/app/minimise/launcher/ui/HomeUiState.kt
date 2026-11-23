package app.minimise.launcher.ui

import app.minimise.apps.domain.AppEntry
import java.time.LocalDateTime

data class HomeUiState(
    val now: LocalDateTime? = null,
    val slots: List<HomeSlot> = emptyList(),
    val isDefaultHome: Boolean = true,
    /** Non-null while the user is choosing an app for a slot. */
    val slotPicker: SlotPicker? = null,
)

data class HomeSlot(val index: Int, val app: AppEntry?)

data class SlotPicker(val slot: Int, val choices: List<AppEntry>)
