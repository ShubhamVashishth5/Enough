package app.minimise.apps.domain

/**
 * A website treated like an app: "web:instagram.com". Stored in the apps table like any package, so
 * every rule, the budget and the usage spans handle it without knowing it's a website.
 */
object WebTarget {
    private const val PREFIX = "web:"
    private val SCHEME = Regex("^[a-z][a-z0-9+.-]*://")
    private val DOMAIN = Regex("^([a-z0-9]([a-z0-9-]*[a-z0-9])?\\.)+[a-z]{2,}$")
    private val MOBILE_PREFIXES = listOf("www.", "m.", "mobile.")

    fun idFor(domain: String): String = PREFIX + domain

    fun isWebTarget(id: String): Boolean = id.startsWith(PREFIX)

    fun domainOf(id: String): String = id.removePrefix(PREFIX)

    /** What the user typed ("https://www.Instagram.com/reels") as a bare domain ("instagram.com"), or null. */
    fun normalise(input: String): String? {
        var host = hostOf(input) ?: return null
        MOBILE_PREFIXES.firstOrNull { host.startsWith(it) && host.length > it.length }?.let { host = host.removePrefix(it) }
        return host
    }

    /**
     * The host in an address bar's text, or null when it isn't an address (a search being typed, the
     * new-tab hint). Browsers show "instagram.com", "https://m.instagram.com/x" or the full URL.
     */
    fun hostOf(addressBar: String): String? {
        val text = addressBar.trim().lowercase()
        if (text.isEmpty() || text.any { it.isWhitespace() }) return null
        val host = text.replaceFirst(SCHEME, "").substringBefore('/').substringBefore('?').substringBefore('#').substringBefore(':')
            .substringAfterLast('@')
            .trimEnd('.')
        return host.takeIf { DOMAIN.matches(it) }
    }

    /** The most specific of [domains] that [host] is, or is part of ("m.youtube.com" is youtube.com). */
    fun match(host: String, domains: Set<String>): String? =
        domains.filter { host == it || host.endsWith(".$it") }.maxByOrNull { it.length }
}
