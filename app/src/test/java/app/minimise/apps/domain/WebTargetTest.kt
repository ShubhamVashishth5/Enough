package app.minimise.apps.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebTargetTest {
    @Test
    fun `address bar text becomes a host`() {
        assertEquals("instagram.com", WebTarget.hostOf("instagram.com"))
        assertEquals("m.youtube.com", WebTarget.hostOf("https://m.youtube.com/shorts/abc?x=1"))
        assertEquals("www.reddit.com", WebTarget.hostOf("  HTTPS://www.Reddit.com:443/r/all  "))
    }

    @Test
    fun `searches and hints aren't addresses`() {
        assertNull(WebTarget.hostOf("cat videos"))
        assertNull(WebTarget.hostOf("Search or type web address"))
        assertNull(WebTarget.hostOf(""))
        assertNull(WebTarget.hostOf("localhost"))
    }

    @Test
    fun `typed websites are cleaned to a bare domain`() {
        assertEquals("instagram.com", WebTarget.normalise("https://www.Instagram.com/reels/"))
        assertEquals("youtube.com", WebTarget.normalise("m.youtube.com"))
        assertNull(WebTarget.normalise("not a site"))
    }

    @Test
    fun `subdomains count as their site`() {
        val domains = setOf("youtube.com", "instagram.com")
        assertEquals("youtube.com", WebTarget.match("m.youtube.com", domains))
        assertEquals("instagram.com", WebTarget.match("instagram.com", domains))
        assertNull(WebTarget.match("notyoutube.com", domains))
        assertNull(WebTarget.match("example.org", domains))
    }

    @Test
    fun `the most specific site wins`() {
        assertEquals("news.example.com", WebTarget.match("a.news.example.com", setOf("example.com", "news.example.com")))
    }

    @Test
    fun `ids round-trip`() {
        val id = WebTarget.idFor("x.com")
        assertEquals("web:x.com", id)
        assertEquals("x.com", WebTarget.domainOf(id))
    }
}
