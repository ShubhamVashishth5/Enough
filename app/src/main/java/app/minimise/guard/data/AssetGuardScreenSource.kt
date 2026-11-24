package app.minimise.guard.data

import android.content.Context
import app.minimise.guard.domain.GuardScreen
import app.minimise.guard.domain.GuardScreenSource
import app.minimise.guard.domain.GuardScreens
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

class AssetGuardScreenSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : GuardScreenSource {
    override fun screens(): List<GuardScreen> =
        runCatching { context.assets.open(FILE).bufferedReader().use { GuardScreens.parse(it.readText()) } }
            .onFailure { Timber.e(it, "Could not read %s", FILE) }
            .getOrDefault(emptyList())

    private companion object {
        const val FILE = "guard_rules.json"
    }
}
