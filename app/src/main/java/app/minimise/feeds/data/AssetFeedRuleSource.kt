package app.minimise.feeds.data

import android.content.Context
import app.minimise.feeds.domain.FeedRule
import app.minimise.feeds.domain.FeedRuleSource
import app.minimise.feeds.domain.FeedRules
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import timber.log.Timber

/** Reads assets/feed_rules.json. A broken file means no feed blocking, never a crash. */
class AssetFeedRuleSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : FeedRuleSource {
    override fun rules(): List<FeedRule> =
        runCatching { context.assets.open(FILE).bufferedReader().use { FeedRules.parse(it.readText()) } }
            .onFailure { Timber.e(it, "Could not read %s", FILE) }
            .getOrDefault(emptyList())

    private companion object {
        const val FILE = "feed_rules.json"
    }
}
