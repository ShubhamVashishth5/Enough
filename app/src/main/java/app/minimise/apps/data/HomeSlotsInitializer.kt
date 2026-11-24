package app.minimise.apps.data

import app.minimise.apps.domain.AppRepository
import app.minimise.core.Defaults
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Fills the home slots with the phone's default tools once, the first time the launcher runs. */
class HomeSlotsInitializer @Inject constructor(
    private val appRepository: AppRepository,
    private val defaultHomeApps: DefaultHomeApps,
    private val settingsStore: SettingsStore,
) {
    suspend fun initialiseIfNeeded() {
        if (settingsStore.current().homeSlotsChosen) return
        val installed = appRepository.apps.first { it.isNotEmpty() }.map { it.packageName }.toSet()
        val defaults = withContext(Dispatchers.IO) { defaultHomeApps.packages() }
        defaults.filter { it in installed }
            .take(Defaults.HOME_SLOT_COUNT)
            .forEachIndexed { slot, packageName -> appRepository.setHomeSlot(packageName, slot) }
        settingsStore.update { it.copy(homeSlotsChosen = true) }
    }
}
