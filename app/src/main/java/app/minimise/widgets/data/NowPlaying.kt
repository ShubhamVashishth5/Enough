package app.minimise.widgets.data

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import app.minimise.digest.DigestListener
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import timber.log.Timber

/** @property packageName the player, which tapping the track opens. */
data class Track(val packageName: String, val title: String, val artist: String?, val playing: Boolean)

/**
 * What's playing, from the system's media sessions (Spotify, YouTube Music, podcasts, the browser).
 * Android lists other apps' sessions only to an app with notification access, which the digest's
 * listener already has once it's allowed; without it there is simply no track.
 */
class NowPlaying @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val sessions: MediaSessionManager? = context.getSystemService(MediaSessionManager::class.java)
    private val listener = ComponentName(context, DigestListener::class.java)

    /** Null when nothing is playing or paused, or when notification access is off. */
    val track: Flow<Track?> = callbackFlow {
        val manager = sessions
        if (manager == null) {
            trySend(null)
            awaitClose {}
            return@callbackFlow
        }
        val handler = Handler(Looper.getMainLooper())
        var watched: MediaController? = null
        lateinit var callback: MediaController.Callback

        fun watchList(list: List<MediaController>) {
            watched?.unregisterCallback(callback)
            watched = pick(list)?.also { it.registerCallback(callback, handler) }
            trySend(watched?.toTrack())
        }
        callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                trySend(watched?.toTrack())
            }

            override fun onPlaybackStateChanged(state: PlaybackState?) {
                trySend(watched?.toTrack())
            }

            override fun onSessionDestroyed() {
                watchList(controllers())
            }
        }
        val changes = MediaSessionManager.OnActiveSessionsChangedListener { watchList(it.orEmpty()) }
        val registered = runCatching { manager.addOnActiveSessionsChangedListener(changes, listener, handler) }
            .onFailure { Timber.i("No media sessions without notification access") }
            .isSuccess
        watchList(controllers())
        awaitClose {
            if (registered) runCatching { manager.removeOnActiveSessionsChangedListener(changes) }
            watched?.unregisterCallback(callback)
        }
    }.distinctUntilChanged()

    fun playPause(packageName: String) {
        val controller = controllerFor(packageName) ?: return
        val controls = controller.transportControls
        if (controller.playbackState?.isPlaying() == true) controls.pause() else controls.play()
    }

    fun next(packageName: String) {
        controllerFor(packageName)?.transportControls?.skipToNext()
    }

    fun previous(packageName: String) {
        controllerFor(packageName)?.transportControls?.skipToPrevious()
    }

    private fun controllerFor(packageName: String): MediaController? = controllers().firstOrNull { it.packageName == packageName }

    private fun controllers(): List<MediaController> =
        runCatching { sessions?.getActiveSessions(listener).orEmpty() }.getOrDefault(emptyList())

    /** The one playing, or else the most recent session with something to show. */
    private fun pick(list: List<MediaController>): MediaController? =
        list.firstOrNull { it.playbackState?.isPlaying() == true && it.title() != null } ?: list.firstOrNull { it.title() != null }

    private fun MediaController.title(): String? =
        metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() }

    private fun MediaController.toTrack(): Track? {
        val title = title() ?: return null
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() }
            ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)?.takeIf { it.isNotBlank() }
        return Track(packageName = packageName, title = title, artist = artist, playing = playbackState?.isPlaying() == true)
    }

    private fun PlaybackState.isPlaying(): Boolean =
        state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
}
