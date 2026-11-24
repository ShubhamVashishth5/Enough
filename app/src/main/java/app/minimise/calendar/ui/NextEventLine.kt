package app.minimise.calendar.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import app.minimise.R
import app.minimise.calendar.domain.CalendarEvent
import app.minimise.calendar.domain.CalendarSource
import app.minimise.calendar.domain.NextEvent
import app.minimise.core.time.TimeSource
import app.minimise.core.time.plus
import app.minimise.core.ui.ticker
import app.minimise.core.ui.timeText
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** @property startsAt null when the event is already under way. */
data class NextEventUi(val title: String, val startsAt: LocalTime?)

@HiltViewModel
class NextEventViewModel @Inject constructor(
    private val calendar: CalendarSource,
    private val timeSource: TimeSource,
) : ViewModel() {
    val next: StateFlow<NextEventUi?> = ticker(1.minutes)
        .map { if (calendar.isAvailable()) load() else null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private suspend fun load(): NextEventUi? {
        val now = timeSource.now()
        val event: CalendarEvent = NextEvent.pick(calendar.eventsBetween(now, now + 24.hours), now) ?: return null
        val startsAt = if (event.begin.isAfter(now)) event.begin.atZone(timeSource.zone()).toLocalTime() else null
        return NextEventUi(event.title, startsAt)
    }
}

/** The Today line's last part: the next calendar item, under the clock. */
@Composable
fun NextEventLine(viewModel: NextEventViewModel = hiltViewModel()) {
    val next by viewModel.next.collectAsStateWithLifecycle()
    val event = next ?: return
    Text(
        text = event.startsAt?.let { stringResource(R.string.event_next, timeText(it), event.title) }
            ?: stringResource(R.string.event_now, event.title),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
}
