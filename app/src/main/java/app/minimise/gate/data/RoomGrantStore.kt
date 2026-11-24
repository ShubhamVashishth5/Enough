package app.minimise.gate.data

import app.minimise.core.db.GrantDao
import app.minimise.core.db.GrantEntity
import app.minimise.gate.domain.Grant
import app.minimise.gate.domain.GrantStore
import javax.inject.Inject

/** Grants live in Room so a running time box survives the service restarting. */
class RoomGrantStore @Inject constructor(
    private val dao: GrantDao,
) : GrantStore {
    override suspend fun get(packageName: String): Grant? =
        dao.get(packageName)?.let { Grant(it.packageName, it.startAt, it.endAt, it.viaOverride) }

    override suspend fun put(grant: Grant) =
        dao.upsert(GrantEntity(grant.packageName, grant.startAt, grant.endAt, grant.viaOverride))

    override suspend fun remove(packageName: String) = dao.delete(packageName)
}
