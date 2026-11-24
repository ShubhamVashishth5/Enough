package app.minimise.display.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.display.domain.DisplayFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

/**
 * Grayscale through Android's colour correction ("daltonizer") set to monochrome. Needs
 * WRITE_SECURE_SETTINGS, which only ADB or Shizuku can grant. The user's own colour correction is
 * saved before the first switch and put back when grayscale goes off.
 */
@Singleton
class SecureSettingsDisplayFilter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsStore: SettingsStore,
) : DisplayFilter {
    private val mutex = Mutex()

    override fun isAvailable(): Boolean =
        context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) == PackageManager.PERMISSION_GRANTED

    override suspend fun setGrayscale(on: Boolean) = mutex.withLock {
        if (!isAvailable()) return@withLock
        runCatching {
            val resolver = context.contentResolver
            val saved = settingsStore.current().savedColourCorrection
            if (on) {
                if (saved == null) {
                    val enabled = Settings.Secure.getInt(resolver, ENABLED, 0)
                    val mode = Settings.Secure.getInt(resolver, MODE, DEFAULT_MODE)
                    settingsStore.update { it.copy(savedColourCorrection = listOf(enabled, mode)) }
                }
                Settings.Secure.putInt(resolver, MODE, MONOCHROME)
                Settings.Secure.putInt(resolver, ENABLED, 1)
            } else if (saved != null) {
                Settings.Secure.putInt(resolver, MODE, saved[1])
                Settings.Secure.putInt(resolver, ENABLED, saved[0])
                settingsStore.update { it.copy(savedColourCorrection = null) }
            }
        }.onFailure { Timber.e(it, "Could not change colour correction") }
        Unit
    }

    private companion object {
        const val ENABLED = "accessibility_display_daltonizer_enabled"
        const val MODE = "accessibility_display_daltonizer"
        const val MONOCHROME = 0

        /** What Android uses when no mode has ever been picked (deuteranomaly). */
        const val DEFAULT_MODE = 12
    }
}

/** For when grayscale isn't possible: changes nothing. */
class NoOpDisplayFilter @Inject constructor() : DisplayFilter {
    override fun isAvailable(): Boolean = false

    override suspend fun setGrayscale(on: Boolean) = Unit
}
