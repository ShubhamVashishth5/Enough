package app.minimise.display

import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.display.domain.DisplayFilter
import app.minimise.display.domain.GrayscaleMode
import app.minimise.core.ui.ticker
import app.minimise.focus.domain.FocusSessions
import app.minimise.modes.domain.ActiveModeProvider
import kotlin.time.Duration.Companion.minutes
import app.minimise.monitor.domain.ForegroundAppSource
import javax.inject.Inject
import javax.inject.Provider
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** Runs in the accessibility service: grayscale while a Vault app is in front, or always, per the setting. */
class GrayscaleController @Inject constructor(
    private val foreground: ForegroundAppSource,
    private val appRepository: AppRepository,
    private val settingsStore: SettingsStore,
    private val filter: Provider<DisplayFilter>,
    private val focusSessions: FocusSessions,
    private val activeMode: ActiveModeProvider,
) {
    suspend fun run() {
        // Wind-down: the Sleep mode comes on by schedule, so look once a minute.
        val sleeping = ticker(1.minutes).map { activeMode.active()?.isSleep == true }.distinctUntilChanged()
        val quiet = combine(focusSessions.active.map { it != null }, sleeping) { focusing, asleep -> focusing || asleep }
        combine(
            foreground.current,
            appRepository.kinds,
            settingsStore.settings.map { it.grayscaleMode }.distinctUntilChanged(),
            quiet,
        ) { current, kinds, mode, focusing ->
            wanted(mode, inVaultApp = current?.packageName?.let { kinds[it] == AppKind.VAULT } == true, focusing = focusing)
        }
            .distinctUntilChanged()
            .collect { filter.get().setGrayscale(it) }
    }

    /** Without the service nothing follows the app in front, so "Vault apps only" goes back to colour. */
    suspend fun onServiceStopped() {
        if (settingsStore.current().grayscaleMode == GrayscaleMode.VAULT_APPS) filter.get().setGrayscale(false)
    }

    companion object {
        /** A focus session and wind-down go grayscale too, unless the feature is off. */
        fun wanted(mode: GrayscaleMode, inVaultApp: Boolean, focusing: Boolean = false): Boolean = when (mode) {
            GrayscaleMode.OFF -> false
            GrayscaleMode.VAULT_APPS -> inVaultApp || focusing
            GrayscaleMode.ALWAYS -> true
        }
    }
}
