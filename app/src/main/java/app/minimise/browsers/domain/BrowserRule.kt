package app.minimise.browsers.domain

import app.minimise.monitor.domain.NodeMatcher
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A browser and how to find its address bar. Kept in assets/browser_rules.json. */
@Serializable
data class BrowserRule(
    @SerialName("package") val packageName: String,
    val name: String,
    /** The address bar: the first visible node any of these match. */
    val matchers: List<NodeMatcher>,
)

@Serializable
private data class BrowserRuleFile(val browsers: List<BrowserRule> = emptyList())

object BrowserRules {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): List<BrowserRule> =
        json.decodeFromString(BrowserRuleFile.serializer(), text).browsers.filter { it.matchers.isNotEmpty() }
}

interface BrowserRuleSource {
    fun rules(): List<BrowserRule>
}
