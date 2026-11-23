package app.minimise.monitor.data

import app.minimise.monitor.domain.ForegroundAppSource
import app.minimise.monitor.domain.ForegroundEvent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Fed only by GateAccessibilityService. Everything else reads it through [ForegroundAppSource]. */
@Singleton
class AccessibilityForegroundSource @Inject constructor() : ForegroundAppSource {
    private val _events = MutableSharedFlow<ForegroundEvent>(extraBufferCapacity = 64)
    private val _current = MutableStateFlow<ForegroundEvent?>(null)

    override val events: Flow<ForegroundEvent> = _events.asSharedFlow()
    override val current: StateFlow<ForegroundEvent?> = _current.asStateFlow()

    /** The last app that was in front before the screen went off, so it can resume on unlock. */
    var lastAppBeforeScreenOff: String? = null
        private set

    fun emit(event: ForegroundEvent) {
        if (_current.value?.packageName == event.packageName) return
        _current.value = event
        _events.tryEmit(event)
    }

    fun onScreenOff(event: ForegroundEvent) {
        lastAppBeforeScreenOff = _current.value?.packageName
        emit(event)
    }
}
