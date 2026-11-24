package app.minimise.unlock.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.core.Defaults
import app.minimise.unlock.domain.FriendCodeRepository
import app.minimise.unlock.domain.FriendCodes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** @property shownCodes the new codes, shown once to share and then forgotten. Only their hashes are kept. */
data class FriendKeyUiState(val unused: Int = 0, val shownCodes: List<String>? = null)

@HiltViewModel
class FriendKeyViewModel @Inject constructor(
    private val codes: FriendCodeRepository,
) : ViewModel() {
    private val shown = MutableStateFlow<List<String>?>(null)

    val uiState: StateFlow<FriendKeyUiState> = combine(codes.unused, shown) { unused, shown -> FriendKeyUiState(unused, shown) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FriendKeyUiState())

    /** Makes a fresh set. The old codes stop working. */
    fun onMakeCodes() {
        val fresh = FriendCodes.generate(Defaults.FRIEND_CODE_COUNT)
        viewModelScope.launch {
            codes.replaceAll(fresh.map(FriendCodes::hashOf))
            shown.value = fresh
        }
    }

    fun onDone() {
        shown.value = null
    }

    /** The codes go with the screen: leaving it means they can't be shown again. */
    override fun onCleared() {
        shown.value = null
    }
}
