package app.minimise.feeds.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.TimeSource
import app.minimise.core.time.durationBetween
import app.minimise.core.ui.ticker
import app.minimise.feeds.domain.FeedBlocker
import app.minimise.feeds.domain.FeedRule
import app.minimise.settings.domain.SetFeedBlocked
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FeedsUiState(val rows: List<FeedRow> = emptyList())

/** @property offIn how long until a waiting switch-off applies. */
data class FeedRow(val rule: FeedRule, val blocked: Boolean, val offIn: Duration?)

@HiltViewModel
class FeedsViewModel @Inject constructor(
    feedBlocker: FeedBlocker,
    settingsStore: SettingsStore,
    private val settingsGuard: SettingsGuard,
    private val timeSource: TimeSource,
) : ViewModel() {
    private val rules = feedBlocker.allRules()

    val uiState: StateFlow<FeedsUiState> = combine(
        settingsStore.settings,
        settingsGuard.pending,
        ticker(REFRESH).onEach { settingsGuard.applyDue() },
    ) { settings, pending, _ ->
        val now = timeSource.now()
        FeedsUiState(
            rows = rules.map { rule ->
                FeedRow(
                    rule = rule,
                    blocked = rule.id in settings.blockedFeeds,
                    offIn = pending.firstOrNull { it.change == SetFeedBlocked(rule.id, blocked = false) }?.let { durationBetween(now, it.applyAt) },
                )
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FeedsUiState())

    /** On applies at once. Off waits; tapping again while it waits keeps the block on. */
    fun onFeedTapped(row: FeedRow) {
        val blocked = !row.blocked || row.offIn != null
        viewModelScope.launch { settingsGuard.request(SetFeedBlocked(row.rule.id, blocked)) }
    }

    private companion object {
        val REFRESH = 15.seconds
    }
}
