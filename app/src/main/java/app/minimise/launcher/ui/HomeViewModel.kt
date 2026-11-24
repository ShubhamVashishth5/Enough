package app.minimise.launcher.ui

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.data.HomeSlotsInitializer
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.Defaults
import app.minimise.core.prefs.HomeOptions
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.DayBoundary
import app.minimise.core.time.TimeSource
import app.minimise.launcher.AppOpener
import app.minimise.launcher.HomeRole
import app.minimise.launcher.NotificationShade
import app.minimise.monitor.AccessibilityActions
import app.minimise.protection.ProtectionStatus
import app.minimise.protection.domain.ProtectionLevel
import app.minimise.settings.domain.SettingsGuard
import app.minimise.settings.permissions.PermissionStatus
import app.minimise.tasks.domain.TaskRepository
import app.minimise.tasks.domain.TodayPrompt
import app.minimise.tasks.domain.TodayTask
import app.minimise.usage.domain.EarnIt
import app.minimise.usage.domain.TodayUsageProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val appOpener: AppOpener,
    private val homeRole: HomeRole,
    private val permissionStatus: PermissionStatus,
    private val protectionStatus: ProtectionStatus,
    private val timeSource: TimeSource,
    private val settingsGuard: SettingsGuard,
    private val taskRepository: TaskRepository,
    private val todayPrompt: TodayPrompt,
    private val accessibilityActions: AccessibilityActions,
    private val notificationShade: NotificationShade,
    private val earnIt: EarnIt,
    settingsStore: SettingsStore,
    dayBoundary: DayBoundary,
    todayUsage: TodayUsageProvider,
    homeSlotsInitializer: HomeSlotsInitializer,
) : ViewModel() {
    private val editingSlot = MutableStateFlow<Int?>(null)
    private val permissions = MutableStateFlow(Permissions())
    private val promptToday = MutableStateFlow(false)

    /** Bumped on resume so the numbers refresh at once rather than at the next minute. */
    private val resumes = MutableStateFlow(0)

    private val ticks: Flow<Instant> = minuteTicks().shareIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    private val tasks: Flow<List<TodayTask>> = ticks
        .map { dayBoundary.dayOf(it) }
        .distinctUntilChanged()
        .flatMapLatest { taskRepository.observeDay(it) }

    private val numbers: Flow<TodayNumbers> = combine(ticks, resumes) { _, _ -> todayUsage.today() }
        .map { TodayNumbers(vaultTime = it.vaultTime, unlocks = it.unlocks) }

    private val options = settingsStore.settings.map { it.home }.distinctUntilChanged()

    private val status = combine(editingSlot, permissions, promptToday, options) { editing, permissions, prompt, options ->
        Status(editing, permissions, prompt, options)
    }

    val uiState: StateFlow<HomeUiState> = combine(appRepository.apps, ticks, tasks, numbers, status) { apps, now, tasks, numbers, status ->
        HomeUiState(
            now = now.atZone(timeSource.zone()).toLocalDateTime(),
            tasks = tasks,
            slots = homeSlots(apps),
            numbers = numbers,
            isDefaultHome = status.permissions.defaultHome,
            protection = status.permissions.protection,
            slotPicker = status.editingSlot?.let { SlotPicker(slot = it, choices = apps.filter { app -> app.kind in AppKind.VISIBLE }) },
            promptToday = status.promptToday,
            options = status.options,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { homeSlotsInitializer.initialiseIfNeeded() }
    }

    fun onResume() {
        protectionStatus.refresh()
        permissions.value = Permissions(
            defaultHome = permissionStatus.isDefaultHome(),
            protection = protectionStatus.current(),
        )
        resumes.value++
        viewModelScope.launch {
            settingsGuard.applyDue()
            if (todayPrompt.claim()) promptToday.value = true
        }
    }

    fun onTodayPromptShown() {
        promptToday.value = false
    }

    fun homeRoleRequestIntent(): Intent = homeRole.requestIntent()

    fun onTaskTapped(task: TodayTask) {
        viewModelScope.launch {
            taskRepository.setDone(task.id, !task.done)
            if (!task.done) earnIt.onTaskDone(task.id)
        }
    }

    fun onSlotTapped(slot: HomeSlot) {
        val app = slot.app
        if (app == null) editingSlot.value = slot.index else viewModelScope.launch { appOpener.open(app) }
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
        editingSlot.value = null
    }

    /** Double-tap or the lock icon locks the screen, through the accessibility service. False when it's off. */
    fun onLock(): Boolean = accessibilityActions.lockScreen()

    /** Swipe down: the notification shade. False when it couldn't be opened. */
    fun onSwipeDown(): Boolean = notificationShade.open()

    /** Called when the Home button is pressed while home is already showing. */
    fun onHomePressed() {
        editingSlot.value = null
    }

    private fun homeSlots(apps: List<AppEntry>): List<HomeSlot> =
        (0 until Defaults.HOME_SLOT_COUNT).map { index ->
            // Vault and Hidden apps never sit on home, even if they were placed there before. Breathe apps do.
            HomeSlot(index = index, app = apps.firstOrNull { it.homeSlot == index && it.kind in AppKind.VISIBLE })
        }

    /** Now, then again at the start of every minute. */
    private fun minuteTicks(): Flow<Instant> = flow {
        while (true) {
            val now = timeSource.now()
            emit(now)
            delay(MILLIS_PER_MINUTE - now.toEpochMilli() % MILLIS_PER_MINUTE)
        }
    }

    private data class Permissions(val defaultHome: Boolean = true, val protection: ProtectionLevel = ProtectionLevel.FULL)

    private data class Status(
        val editingSlot: Int?,
        val permissions: Permissions,
        val promptToday: Boolean,
        val options: HomeOptions,
    )

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
