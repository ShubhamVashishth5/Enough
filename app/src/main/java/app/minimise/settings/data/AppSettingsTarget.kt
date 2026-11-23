package app.minimise.settings.data

import app.minimise.apps.domain.AppRepository
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.settings.domain.Direction
import app.minimise.settings.domain.SetAppKind
import app.minimise.settings.domain.SetBudget
import app.minimise.settings.domain.SettingChange
import app.minimise.settings.domain.SettingsTarget
import app.minimise.settings.domain.budgetChangeDirection
import app.minimise.settings.domain.kindChangeDirection
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

class AppSettingsTarget @Inject constructor(
    private val appRepository: AppRepository,
    private val settingsStore: SettingsStore,
) : SettingsTarget {
    override suspend fun direction(change: SettingChange): Direction? = when (change) {
        is SetAppKind -> kindChangeDirection(from = appRepository.kindOf(change.packageName), to = change.kind)
        is SetBudget -> budgetChangeDirection(
            fromMinutes = settingsStore.current().dailyBudget.inWholeMinutes.toInt(),
            toMinutes = change.minutes,
        )
    }

    override suspend fun apply(change: SettingChange) = when (change) {
        is SetAppKind -> appRepository.setKind(change.packageName, change.kind)
        is SetBudget -> settingsStore.update { it.copy(dailyBudget = change.minutes.minutes) }
    }
}
