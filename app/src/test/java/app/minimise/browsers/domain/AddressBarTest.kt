package app.minimise.browsers.domain

import app.minimise.monitor.domain.NodeMatcher
import app.minimise.testing.FakeScreenNode
import app.minimise.testing.node
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AddressBarTest {
    private val chrome = BrowserRule("com.android.chrome", "Chrome", listOf(NodeMatcher(viewId = "com.android.chrome:id/url_bar")))
    private val vault = setOf("instagram.com")

    private fun screen(bar: FakeScreenNode) = node("com.android.chrome:id/root", null, node("com.android.chrome:id/toolbar", null, bar))

    @Test
    fun `a vault website in the address bar is that website`() {
        val read = AddressBar.read(screen(FakeScreenNode(viewId = "com.android.chrome:id/url_bar", text = "instagram.com/reels")), chrome, vault)
        assertEquals(AddressBarRead.Page("web:instagram.com"), read)
    }

    @Test
    fun `any other page is the browser itself`() {
        val read = AddressBar.read(screen(FakeScreenNode(viewId = "com.android.chrome:id/url_bar", text = "wikipedia.org")), chrome, vault)
        assertEquals(AddressBarRead.Page(null), read)
    }

    @Test
    fun `while the user types in the address bar nothing changes`() {
        val typing = FakeScreenNode(viewId = "com.android.chrome:id/url_bar", text = "instagram.com", focused = true)
        assertEquals(AddressBarRead.Unknown, AddressBar.read(screen(typing), chrome, vault))
    }

    @Test
    fun `no address bar on screen means unknown`() {
        assertEquals(AddressBarRead.Unknown, AddressBar.read(node("com.android.chrome:id/root"), chrome, vault))
    }

    @Test
    fun `the shipped browser file parses`() {
        val rules = BrowserRules.parse(File("src/main/assets/browser_rules.json").readText())
        assertTrue(rules.any { it.packageName == "com.android.chrome" })
        assertEquals(rules.size, rules.map { it.packageName }.toSet().size)
    }
}
