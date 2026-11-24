package app.minimise.swaps.data

import android.content.Context
import app.minimise.R
import app.minimise.core.db.SwapDao
import app.minimise.core.db.SwapEntity
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.swaps.domain.Swap
import app.minimise.swaps.domain.SwapRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomSwapRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: SwapDao,
    private val settingsStore: SettingsStore,
) : SwapRepository {
    override val swaps: Flow<List<Swap>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override suspend fun all(): List<Swap> = dao.all().map { it.toDomain() }

    override suspend fun save(swap: Swap) {
        val text = swap.text.trim()
        if (text.isEmpty()) return
        dao.upsert(SwapEntity(id = swap.id, text = text, packageName = swap.packageName))
    }

    override suspend fun delete(id: Long) = dao.delete(id)

    override suspend fun seedDefaultsOnce() {
        if (settingsStore.current().swapsSeeded) return
        settingsStore.update { it.copy(swapsSeeded = true) }
        if (dao.all().isNotEmpty()) return
        val defaults = context.resources.getStringArray(R.array.default_swaps)
        dao.insertAll(defaults.map { SwapEntity(text = it, packageName = null) })
    }

    private fun SwapEntity.toDomain() = Swap(id = id, text = text, packageName = packageName)
}
