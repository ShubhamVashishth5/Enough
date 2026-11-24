package app.minimise.settings.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.BuildConfig
import app.minimise.core.prefs.SettingsStore
import app.minimise.display.GrayscaleController
import app.minimise.display.domain.DisplayFilter
import app.minimise.display.domain.GrayscaleMode
import app.minimise.modes.domain.ActiveModeProvider
import app.minimise.monitor.AccessibilityActions
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import app.minimise.core.time.TimeSource
import java.time.LocalDateTime
import android.net.Uri
import app.minimise.stats.data.CsvExporter
import app.minimise.apps.domain.PrivateSpace
import app.minimise.backup.data.BackupExporter
import app.minimise.backup.data.BackupImporter
import app.minimise.protection.ProtectionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val budgetMinutes: Int? = null,
    val activeModeName: String? = null,
    val pendingCount: Int = 0,
    val grayscaleMode: GrayscaleMode = GrayscaleMode.OFF,
    /** False until the one-time ADB grant is made. */
    val grayscaleAvailable: Boolean = false,
    /** Debug builds only. */
    val showDebug: Boolean = false,
    val debugShortLoosenDelay: Boolean = false,
    /** The end of the lock period in force, if any. */
    val lockedUntil: LocalDateTime? = null,
    /** Events written by the last export (-1 when it failed), until the toast has shown. */
    val exported: Int? = null,
    /** Whether Private space is locked; null when there is none (or below Android 15). */
    val privateSpace: Boolean? = null,
    /** Feed blocking and the lock guard read screens, which only Full protection can. */
    val screensCovered: Boolean = true,
    /** The outcome of the last settings backup or restore, until it has been shown. */
    val backupMessage: BackupMessage? = null,
)

sealed interface BackupMessage {
    data object Saved : BackupMessage

    data object SaveFailed : BackupMessage

    data class Restored(val result: BackupImporter.Result) : BackupMessage
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    settingsGuard: SettingsGuard,
    private val settingsStore: SettingsStore,
    private val displayFilter: Provider<DisplayFilter>,
    private val activeMode: ActiveModeProvider,
    private val accessibilityActions: AccessibilityActions,
    private val timeSource: TimeSource,
    private val csvExporter: CsvExporter,
    private val privateSpace: PrivateSpace,
    private val protectionStatus: ProtectionStatus,
    private val backupExporter: BackupExporter,
    private val backupImporter: BackupImporter,
) : ViewModel() {
    private val exported = MutableStateFlow<Int?>(null)
    private val backupMessage = MutableStateFlow<BackupMessage?>(null)
    private val messages = combine(exported, backupMessage) { exported, backup -> exported to backup }

    val uiState: StateFlow<SettingsUiState> = combine(settingsGuard.pending, settingsStore.settings, messages) { pending, settings, (exported, backup) ->
        SettingsUiState(
            budgetMinutes = settings.dailyBudget.inWholeMinutes.toInt(),
            activeModeName = activeMode.active()?.name,
            pendingCount = pending.size,
            grayscaleMode = settings.grayscaleMode,
            grayscaleAvailable = displayFilter.get().isAvailable(),
            showDebug = BuildConfig.DEBUG,
            debugShortLoosenDelay = settings.debugShortLoosenDelay,
            exported = exported,
            privateSpace = if (privateSpace.exists()) privateSpace.isLocked() else null,
            screensCovered = protectionStatus.current().readsScreens,
            backupMessage = backup,
            lockedUntil = settings.lockedUntil?.takeIf { it.isAfter(timeSource.now()) }?.atZone(timeSource.zone())?.toLocalDateTime(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    /** Cycles Off → Vault apps → Always. Applies at once here too, in case the accessibility service is off. */
    fun onGrayscaleTapped() {
        val next = GrayscaleMode.entries[(uiState.value.grayscaleMode.ordinal + 1) % GrayscaleMode.entries.size]
        viewModelScope.launch {
            settingsStore.update { it.copy(grayscaleMode = next) }
            displayFilter.get().setGrayscale(GrayscaleController.wanted(next, inVaultApp = false))
        }
    }

    fun onDebugShortLoosenDelayToggled() {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch { settingsStore.update { it.copy(debugShortLoosenDelay = !it.debugShortLoosenDelay) } }
    }

    fun onExport(uri: Uri) {
        viewModelScope.launch { exported.value = csvExporter.export(uri) ?: -1 }
    }

    fun onExportShown() {
        exported.value = null
    }

    fun onBackup(uri: Uri) {
        viewModelScope.launch { backupMessage.value = if (backupExporter.export(uri)) BackupMessage.Saved else BackupMessage.SaveFailed }
    }

    fun onRestore(uri: Uri) {
        viewModelScope.launch { backupMessage.value = BackupMessage.Restored(backupImporter.import(uri)) }
    }

    fun onBackupMessageShown() {
        backupMessage.value = null
    }

    fun onDebugDumpScreen() {
        accessibilityActions.dumpScreenSoon()
    }
}
