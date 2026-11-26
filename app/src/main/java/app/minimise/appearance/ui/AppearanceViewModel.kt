package app.minimise.appearance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.Defaults
import app.minimise.core.prefs.Appearance
import app.minimise.core.prefs.SettingsStore
import app.minimise.launcher.domain.ShortcutIcon
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Null [appearance] until settings load. [appNames] are home's first few apps and [icons] its icons, for
 * the preview.
 */
data class AppearanceUiState(
    val appearance: Appearance? = null,
    val appNames: List<String> = emptyList(),
    val icons: List<ShortcutIcon> = emptyList(),
)

/** The look. None of it is a rule, so it changes at once (no SettingsGuard, and a lock period doesn't hold it). */
@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    appRepository: AppRepository,
) : ViewModel() {
    val uiState: StateFlow<AppearanceUiState> = combine(settingsStore.settings, appRepository.apps) { settings, apps ->
        val onHome = (0 until Defaults.HOME_SLOT_COUNT).mapNotNull { slot ->
            apps.firstOrNull { it.homeSlot == slot && it.kind in AppKind.VISIBLE }?.label
        }
        AppearanceUiState(
            appearance = settings.appearance,
            appNames = onHome.take(PREVIEW_APPS),
            icons = settings.shortcuts.map { it.icon },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppearanceUiState())

    fun onChange(change: (Appearance) -> Appearance) {
        viewModelScope.launch { settingsStore.update { it.copy(appearance = change(it.appearance)) } }
    }

    private companion object {
        const val PREVIEW_APPS = 3
    }
}
