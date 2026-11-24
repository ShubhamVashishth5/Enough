package app.minimise.testing

import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAppRepository(initial: List<AppEntry> = emptyList()) : AppRepository {
    val state = MutableStateFlow(initial)

    override val apps: Flow<List<AppEntry>> = state

    override val kinds: Flow<Map<String, AppKind>> = state.map { list -> list.associate { it.packageName to it.kind } }

    override suspend fun entry(packageName: String): AppEntry? = state.value.firstOrNull { it.packageName == packageName }

    override suspend fun kindOf(packageName: String): AppKind = entry(packageName)?.kind ?: AppKind.TOOL

    override suspend fun setKind(packageName: String, kind: AppKind) {
        state.value = state.value.map { if (it.packageName == packageName) it.copy(kind = kind) else it }
    }

    override suspend fun setHomeSlot(packageName: String, slot: Int) {
        state.value = state.value.map {
            when {
                it.packageName == packageName -> it.copy(homeSlot = slot)
                it.homeSlot == slot -> it.copy(homeSlot = null)
                else -> it
            }
        }
    }

    override suspend fun clearHomeSlot(slot: Int) {
        state.value = state.value.map { if (it.homeSlot == slot) it.copy(homeSlot = null) else it }
    }
}

fun app(label: String, kind: AppKind = AppKind.TOOL) =
    AppEntry(packageName = "pkg.${label.lowercase()}", label = label, kind = kind, homeSlot = null)
