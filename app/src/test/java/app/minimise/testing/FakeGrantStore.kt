package app.minimise.testing

import app.minimise.gate.domain.Grant
import app.minimise.gate.domain.GrantStore

class FakeGrantStore : GrantStore {
    val grants = mutableMapOf<String, Grant>()

    override suspend fun get(packageName: String): Grant? = grants[packageName]

    override suspend fun put(grant: Grant) {
        grants[grant.packageName] = grant
    }

    override suspend fun remove(packageName: String) {
        grants.remove(packageName)
    }
}
