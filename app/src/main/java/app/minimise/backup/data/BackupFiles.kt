package app.minimise.backup.data

import android.content.Context
import android.net.Uri
import app.minimise.backup.domain.Backup
import app.minimise.backup.domain.BackupCodec
import app.minimise.backup.domain.BackupDiff
import app.minimise.core.prefs.HomeOptions
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.TemperatureChoice
import app.minimise.core.time.TimeSource
import app.minimise.display.domain.GrayscaleMode
import app.minimise.settings.domain.GuardResult
import app.minimise.settings.domain.SettingsGuard
import app.minimise.swaps.domain.Swap
import app.minimise.swaps.domain.SwapRepository
import app.minimise.unlock.domain.FriendCodeRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/** Writes every setting to a file the user picked (ACTION_CREATE_DOCUMENT). */
class BackupExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reader: LocalSettingsReader,
) {
    /** False when the file couldn't be written. */
    suspend fun export(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val text = BackupCodec.encode(reader.read().settings)
            context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use { it.write(text) } ?: return@runCatching false
            true
        }.onFailure { Timber.e(it, "Settings backup failed") }.getOrDefault(false)
    }
}

/**
 * Restores a backup file (ACTION_OPEN_DOCUMENT). Every rule setting that differs goes through
 * [SettingsGuard]: tighter applies now, looser waits 24 hours, and nothing loosens during a lock.
 * The few settings the app never guards (the Instead list, digest, grayscale, focus apps, friend
 * codes, the home screen's options) are written as they are, as their own screens do.
 */
class BackupImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val reader: LocalSettingsReader,
    private val settingsGuard: SettingsGuard,
    private val settingsStore: SettingsStore,
    private val swapRepository: SwapRepository,
    private val friendCodes: FriendCodeRepository,
    private val timeSource: TimeSource,
) {
    sealed interface Result {
        /** [waiting] looser changes wait 24 hours; [refused] were turned down because a lock period is on. */
        data class Restored(val applied: Int, val waiting: Int, val refused: Int) : Result

        data class TooNew(val version: Int) : Result

        data object Unreadable : Result
    }

    suspend fun import(uri: Uri): Result = withContext(Dispatchers.IO) {
        val text = runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }
            .onFailure { Timber.w(it, "Could not read the backup") }
            .getOrNull()
            ?: return@withContext Result.Unreadable
        when (val decoded = BackupCodec.decode(text)) {
            is BackupCodec.Decoded.Ok -> restore(decoded.backup)
            is BackupCodec.Decoded.TooNew -> Result.TooNew(decoded.version)
            BackupCodec.Decoded.Unreadable -> Result.Unreadable
        }
    }

    private suspend fun restore(incoming: Backup): Result.Restored {
        val local = reader.read()
        var applied = 0
        var waiting = 0
        var refused = 0
        BackupDiff.changes(incoming, local, nowMillis = timeSource.now().toEpochMilli()).forEach { change ->
            when (settingsGuard.request(change)) {
                GuardResult.AppliedNow -> applied++
                is GuardResult.Scheduled -> waiting++
                is GuardResult.Refused -> refused++
            }
        }
        restoreUnguarded(incoming, local.settings)
        return Result.Restored(applied, waiting, refused)
    }

    private suspend fun restoreUnguarded(incoming: Backup, here: Backup) {
        settingsStore.update { settings ->
            settings.copy(
                digestEnabled = incoming.digestEnabled,
                digestExempt = incoming.digestExempt.toSet(),
                grayscaleMode = GrayscaleMode.entries.firstOrNull { it.name == incoming.grayscaleMode } ?: settings.grayscaleMode,
                focusAllowlist = incoming.focusAllowlist?.toSet(),
                swapsSeeded = true,
                home = incoming.home?.let {
                    HomeOptions(
                        lockButton = it.lockButton,
                        swipeDownShade = it.swipeDownShade,
                        hideStatusBar = it.hideStatusBar,
                        battery = it.battery,
                        nextAlarm = it.nextAlarm,
                        music = it.music,
                        weather = it.weather,
                        airQuality = it.airQuality,
                        temperatureUnit = TemperatureChoice.entries.firstOrNull { choice -> choice.name == it.temperatureUnit }
                            ?: settings.home.temperatureUnit,
                    )
                } ?: settings.home,
            )
        }
        if (incoming.swaps != here.swaps) {
            swapRepository.all().forEach { swapRepository.delete(it.id) }
            incoming.swaps.forEach { swapRepository.save(Swap(text = it.text, packageName = it.packageName)) }
        }
        // A backup without codes leaves the ones here alone, so the friend's codes keep working.
        if (incoming.friendCodeHashes.isNotEmpty() && incoming.friendCodeHashes.toSet() != here.friendCodeHashes.toSet()) {
            friendCodes.replaceAll(incoming.friendCodeHashes)
        }
    }
}
