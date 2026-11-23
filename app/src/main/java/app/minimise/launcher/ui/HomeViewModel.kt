package app.minimise.launcher.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.data.AppLauncher
import app.minimise.apps.data.HomeSlotsInitializer
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.Defaults
import app.minimise.core.time.TimeSource
import app.minimise.launcher.HomeRole
import app.minimise.settings.domain.SettingsGuard
import app.minimise.settings.permissions.PermissionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDateTime
import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val appLauncher: AppLauncher,
    private val homeRole: HomeRole,
    private val permissionStatus: PermissionStatus,
    private val timeSource: TimeSource,
    private val settingsGuard: SettingsGuard,
    homeSlotsInitializer: HomeSlotsInitializer,
) : ViewModel() {
    private val editingSlot = MutableStateFlow<Int?>(null)
    private val permissions = MutableStateFlow(Permissions())

    val uiState: StateFlow<HomeUiState> = combine(
        appRepository.apps,
        minuteTicks(),
        editingSlot,
        permissions,
    ) { apps, now, editing, permissions ->
        HomeUiState(
            now = now,
            slots = homeSlots(apps),
            isDefaultHome = permissions.defaultHome,
            protectionOff = !permissions.accessibility,
            slotPicker = editing?.let { SlotPicker(slot = it, choices = apps.filter { app -> app.kind == AppKind.TOOL }) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { homeSlotsInitializer.initialiseIfNeeded() }
    }

    fun onResume() {
        permissions.value = Permissions(
            defaultHome = permissionStatus.isDefaultHome(),
            accessibility = permissionStatus.isAccessibilityEnabled(),
        )
        viewModelScope.launch { settingsGuard.applyDue() }
    }

    fun homeRoleRequestIntent(): Intent = homeRole.requestIntent()

    fun onSlotTapped(slot: HomeSlot) {
        val app = slot.app
        if (app == null) editingSlot.value = slot.index else appLauncher.launch(app.packageName)
    }

    fun onSlotLongPressed(slot: HomeSlot) {
        editingSlot.value = slot.index
    }

    fun onSlotAppPicked(packageName: String) {
        val slot = editingSlot.value ?: return
        editingSlot.value = null
        viewModelScope.launch { appRepository.setHomeSlot(packageName, slot) }
    }

    fun onSlotCleared() {
        val slot = editingSlot.value ?: return
        editingSlot.value = null
        viewModelScope.launch { appRepository.clearHomeSlot(slot) }
    }

    fun onPickerDismissed() {
        editingSlot.update { null }
    }

    /** Called when the Home button is pressed while home is already showing. */
    fun onHomePressed() {
        editingSlot.value = null
    }

    private fun homeSlots(apps: List<AppEntry>): List<HomeSlot> =
        (0 until Defaults.HOME_SLOT_COUNT).map { index ->
            // Vault and Hidden apps never sit on home, even if they were placed there before.
            HomeSlot(index = index, app = apps.firstOrNull { it.homeSlot == index && it.kind == AppKind.TOOL })
        }

    /** The local time now, then again at the start of every minute. */
    private fun minuteTicks(): Flow<LocalDateTime> = flow {
        while (true) {
            val now = timeSource.now()
            emit(now.atZone(timeSource.zone()).toLocalDateTime())
            delay(MILLIS_PER_MINUTE - now.toEpochMilli() % MILLIS_PER_MINUTE)
        }
    }

    private data class Permissions(val defaultHome: Boolean = true, val accessibility: Boolean = true)

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
