package app.minimise.apps.data

import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.apps.domain.WebTarget
import app.minimise.core.db.AppDao
import app.minimise.core.db.AppEntity
import app.minimise.core.di.ApplicationScope
import java.text.Collator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

@Singleton
class RoomAppRepository @Inject constructor(
    private val source: LauncherAppsSource,
    private val dao: AppDao,
    @ApplicationScope scope: CoroutineScope,
) : AppRepository {
    private val collator = Collator.getInstance()

    override val apps: Flow<List<AppEntry>> = source.installed
        .onEach { installed ->
            // New installs start as TOOL. Rows are never deleted, so a reinstall keeps its old kind.
            dao.insertIfMissing(installed.map { AppEntity(it.packageName, AppKind.TOOL, homeSlot = null) })
        }
        .combine(dao.observeAll()) { installed, rows ->
            val stored = rows.associateBy { it.packageName }
            installed
                .map { app ->
                    val row = stored[app.packageName]
                    AppEntry(
                        packageName = app.packageName,
                        label = app.label,
                        kind = row?.kind ?: AppKind.TOOL,
                        homeSlot = row?.homeSlot,
                        openLimit = row?.openLimit,
                        breatheWait = row?.breatheWaitSeconds?.seconds,
                    )
                }
                .sortedWith { a, b -> collator.compare(a.label, b.label) }
        }
        .shareIn(scope, SharingStarted.WhileSubscribed(5_000), replay = 1)

    override val webTargets: Flow<List<AppEntry>> = dao.observeWebTargets().map { rows -> rows.map { it.toWebEntry() } }

    override val kinds: Flow<Map<String, AppKind>> =
        dao.observeAll().map { rows -> rows.associate { it.packageName to it.kind } }

    override suspend fun entry(packageName: String): AppEntry? {
        if (WebTarget.isWebTarget(packageName)) return dao.get(packageName)?.toWebEntry()
        val label = withContext(Dispatchers.IO) { source.activityFor(packageName)?.label?.toString() } ?: return null
        val row = dao.get(packageName)
        return AppEntry(
            packageName,
            label,
            kind = row?.kind ?: AppKind.TOOL,
            homeSlot = row?.homeSlot,
            openLimit = row?.openLimit,
            breatheWait = row?.breatheWaitSeconds?.seconds,
        )
    }

    override suspend fun kindOf(packageName: String): AppKind = dao.get(packageName)?.kind ?: AppKind.TOOL

    override suspend fun setKind(packageName: String, kind: AppKind) {
        dao.insertIfMissing(listOf(AppEntity(packageName, kind, homeSlot = null)))
        dao.setKind(packageName, kind)
    }

    override suspend fun openLimitOf(packageName: String): Int? = dao.get(packageName)?.openLimit

    override suspend fun openLimits(): Map<String, Int> =
        dao.all().mapNotNull { row -> row.openLimit?.let { row.packageName to it } }.toMap()

    override suspend fun setOpenLimit(packageName: String, limit: Int?) {
        dao.insertIfMissing(listOf(AppEntity(packageName, AppKind.TOOL, homeSlot = null)))
        dao.setOpenLimit(packageName, limit)
    }

    override suspend fun breatheWaitOf(packageName: String): Int? = dao.get(packageName)?.breatheWaitSeconds

    override suspend fun breatheWaits(): Map<String, Int> =
        dao.all().mapNotNull { row -> row.breatheWaitSeconds?.let { row.packageName to it } }.toMap()

    override suspend fun setBreatheWait(packageName: String, seconds: Int?) {
        dao.insertIfMissing(listOf(AppEntity(packageName, AppKind.TOOL, homeSlot = null)))
        dao.setBreatheWait(packageName, seconds)
    }

    override suspend fun setHomeSlot(packageName: String, slot: Int) = dao.assignSlot(packageName, slot)

    override suspend fun clearHomeSlot(slot: Int) = dao.clearSlot(slot)

    private fun AppEntity.toWebEntry() =
        AppEntry(packageName = packageName, label = WebTarget.domainOf(packageName), kind = kind, homeSlot = null, openLimit = openLimit)
}
