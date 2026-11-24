package app.minimise.testing

import app.minimise.apps.domain.AppRepository
import app.minimise.settings.domain.Direction
import app.minimise.settings.domain.SetAppKind
import app.minimise.settings.domain.SettingChange
import app.minimise.settings.domain.SettingsTarget
import app.minimise.settings.domain.kindChangeDirection

/** Applies app kinds to a (fake) AppRepository; other changes are ignored. */
class AppSettingsTargetForTest(private val apps: AppRepository) : SettingsTarget {
    override suspend fun direction(change: SettingChange): Direction? =
        (change as? SetAppKind)?.let { kindChangeDirection(apps.kindOf(it.packageName), it.kind) }

    override suspend fun apply(change: SettingChange) {
        (change as? SetAppKind)?.let { apps.setKind(it.packageName, it.kind) }
    }
}
