package app.minimise.digest

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import app.minimise.core.di.ApplicationScope
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.TimeSource
import app.minimise.digest.domain.DigestAction
import app.minimise.digest.domain.DigestFilter
import app.minimise.digest.domain.DigestRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber

/** Holds non-urgent notifications for the next digest. Calls, alarms and exempt apps always pass. */
@AndroidEntryPoint
class DigestListener : NotificationListenerService() {
    @Inject lateinit var settingsStore: SettingsStore
    @Inject lateinit var repository: DigestRepository
    @Inject lateinit var timeSource: TimeSource
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        runCatching {
            val notification = sbn.notification
            val extras = notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val text = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
            appScope.launch {
                runCatching {
                    val settings = settingsStore.current()
                    if (!settings.digestEnabled) return@launch
                    val action = DigestFilter.actionFor(
                        packageName = sbn.packageName,
                        ownPackage = packageName,
                        exempt = settings.digestExempt,
                        category = notification.category,
                        ongoing = sbn.isOngoing,
                        clearable = sbn.isClearable,
                        fullScreen = notification.fullScreenIntent != null,
                        groupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
                    )
                    if (action == DigestAction.HOLD) repository.hold(sbn.packageName, title, text, timeSource.now())
                    if (action != DigestAction.PASS) cancelNotification(sbn.key)
                }.onFailure { Timber.e(it, "Digest failed for %s", sbn.packageName) }
            }
        }.onFailure { Timber.e(it, "Digest listener failed") }
    }
}
