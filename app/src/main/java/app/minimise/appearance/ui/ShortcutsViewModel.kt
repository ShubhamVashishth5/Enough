package app.minimise.appearance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.prefs.SettingsStore
import app.minimise.launcher.data.ShortcutDefaults
import app.minimise.launcher.domain.DefaultApp
import app.minimise.launcher.domain.Shortcut
import app.minimise.launcher.domain.ShortcutIcon
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * One of home's icons as the editor shows it.
 *
 * @property chosen the app the user picked, or null on Automatic.
 * @property missing an app was picked but isn't installed or is no longer a Tool or Breathe app, so home
 * leaves the icon out.
 * @property automatic the phone's own app for the icon, when it has one.
 */
data class ShortcutItem(
    val index: Int,
    val icon: ShortcutIcon,
    val chosen: AppEntry?,
    val missing: Boolean,
    val automatic: AppEntry?,
)

/** Null [items] until settings load. [picking] is the icon whose app is being chosen. */
data class ShortcutsUiState(
    val items: List<ShortcutItem>? = null,
    val choices: List<AppEntry> = emptyList(),
    val picking: ShortcutItem? = null,
    val adding: Boolean = false,
)

/** Home's icons. Not rules: changes apply at once. */
@HiltViewModel
class ShortcutsViewModel @Inject constructor(
    private val settingsStore: SettingsStore,
    appRepository: AppRepository,
    private val shortcutDefaults: ShortcutDefaults,
) : ViewModel() {
    private val defaultApps = MutableStateFlow<Map<DefaultApp, String>>(emptyMap())
    private val picking = MutableStateFlow<Int?>(null)
    private val adding = MutableStateFlow(false)

    val uiState: StateFlow<ShortcutsUiState> =
        combine(settingsStore.settings, appRepository.apps, defaultApps, picking, adding) { settings, apps, defaults, picking, adding ->
            val visible = apps.filter { it.kind in AppKind.VISIBLE }
            fun appFor(packageName: String?) = packageName?.let { name -> visible.firstOrNull { it.packageName == name } }
            val items = settings.shortcuts.mapIndexed { index, shortcut ->
                val chosen = appFor(shortcut.packageName)
                ShortcutItem(
                    index = index,
                    icon = shortcut.icon,
                    chosen = chosen,
                    missing = shortcut.packageName != null && chosen == null,
                    automatic = appFor(shortcut.icon.default?.let(defaults::get)),
                )
            }
            ShortcutsUiState(items = items, choices = visible, picking = picking?.let(items::getOrNull), adding = adding)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShortcutsUiState())

    init {
        viewModelScope.launch { defaultApps.value = shortcutDefaults.resolve(DefaultApp.entries) }
    }

    fun onItemTapped(item: ShortcutItem) {
        picking.value = item.index
    }

    /** [packageName] null: Automatic. */
    fun onAppPicked(packageName: String?) {
        val index = picking.value ?: return
        picking.value = null
        edit { list -> list.also { if (index in it.indices) it[index] = it[index].copy(packageName = packageName) } }
    }

    fun onPickerDismissed() {
        picking.value = null
    }

    /** From the app picker, whose other button removes the icon it was opened for. */
    fun onRemovePicked() {
        val index = picking.value ?: return
        picking.value = null
        onRemove(index)
    }

    fun onRemove(index: Int) {
        edit { list -> list.also { if (index in it.indices) it.removeAt(index) } }
    }

    fun onMove(index: Int, by: Int) {
        edit { list ->
            val to = index + by
            if (index in list.indices && to in list.indices) list.add(to, list.removeAt(index))
            list
        }
    }

    fun onAddTapped() {
        adding.value = true
    }

    fun onAddDismissed() {
        adding.value = false
    }

    /** Adds [icon] on Automatic. An icon with no Automatic goes straight on to choosing its app. */
    fun onIconChosen(icon: ShortcutIcon) {
        adding.value = false
        viewModelScope.launch {
            var added: Int? = null
            settingsStore.update { settings ->
                if (settings.shortcuts.size >= Shortcut.MAX) return@update settings
                added = settings.shortcuts.size
                settings.copy(shortcuts = settings.shortcuts + Shortcut(icon))
            }
            if (icon.default == null) added?.let { picking.value = it }
        }
    }

    private fun edit(change: (MutableList<Shortcut>) -> List<Shortcut>) {
        viewModelScope.launch { settingsStore.update { it.copy(shortcuts = change(it.shortcuts.toMutableList())) } }
    }
}
