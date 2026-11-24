package app.minimise.core.prefs

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

interface SettingsStore {
    val settings: Flow<Settings>

    suspend fun update(transform: (Settings) -> Settings)
}

suspend fun SettingsStore.current(): Settings = settings.first()
