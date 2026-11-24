package app.minimise.testing

import app.minimise.core.prefs.Settings
import app.minimise.core.prefs.SettingsStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsStore(initial: Settings = Settings()) : SettingsStore {
    val state = MutableStateFlow(initial)

    override val settings: Flow<Settings> = state

    override suspend fun update(transform: (Settings) -> Settings) {
        state.value = transform(state.value)
    }
}
