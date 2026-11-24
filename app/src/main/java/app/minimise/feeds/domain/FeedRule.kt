package app.minimise.feeds.domain

import app.minimise.monitor.domain.NodeMatcher
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What happens when a blocked feed comes on screen. */
@Serializable
enum class FeedAction {
    /** Step back out of the feed, staying in the app. */
    BACK,

    /** Leave the app for the home screen. */
    HOME,

    /** Step back, then show a "Feed blocked" screen. */
    SCREEN,
}

/**
 * The endless part of an app (YouTube Shorts, Instagram Reels). It is on screen when any of
 * [matchers] matches a visible node. Kept in assets/feed_rules.json, so a redesign of the app is a
 * data change, not a code change.
 */
@Serializable
data class FeedRule(
    /** Stable: stored in settings when the user turns the block on. */
    val id: String,
    @SerialName("package") val packageName: String,
    val name: String,
    val matchers: List<NodeMatcher>,
    val action: FeedAction = FeedAction.BACK,
)

@Serializable
private data class FeedRuleFile(val feeds: List<FeedRule> = emptyList())

object FeedRules {
    private val json = Json { ignoreUnknownKeys = true }

    /** Rules without matchers are dropped, so a half-written entry can't block a whole app. */
    fun parse(text: String): List<FeedRule> =
        json.decodeFromString(FeedRuleFile.serializer(), text).feeds.filter { it.matchers.isNotEmpty() }
}

/** Where the rules come from (an asset file). */
interface FeedRuleSource {
    fun rules(): List<FeedRule>
}
