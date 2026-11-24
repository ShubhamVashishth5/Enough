package app.minimise.browsers.data

import android.content.Context
import app.minimise.browsers.domain.BrowserRule
import app.minimise.browsers.domain.BrowserRuleSource
import app.minimise.browsers.domain.BrowserRules
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

class AssetBrowserRuleSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : BrowserRuleSource {
    override fun rules(): List<BrowserRule> =
        runCatching { context.assets.open(FILE).bufferedReader().use { BrowserRules.parse(it.readText()) } }
            .onFailure { Timber.e(it, "Could not read %s", FILE) }
            .getOrDefault(emptyList())

    private companion object {
        const val FILE = "browser_rules.json"
    }
}
