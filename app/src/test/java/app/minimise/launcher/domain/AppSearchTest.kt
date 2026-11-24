package app.minimise.launcher.domain

import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSearchTest {
    private fun app(label: String, kind: AppKind = AppKind.TOOL) =
        AppEntry(packageName = "pkg.${label.lowercase()}", label = label, kind = kind, homeSlot = null)

    private val maps = app("Maps")
    private val camera = app("Camera")
    private val calendar = app("Calendar")
    private val instagram = app("Instagram", AppKind.VAULT)
    private val secret = app("Secret", AppKind.HIDDEN)
    private val all = listOf(maps, camera, calendar, instagram, secret)

    @Test
    fun `empty query shows nothing`() {
        val result = AppSearch.search("  ", all)
        assertTrue(result.matches.isEmpty())
        assertNull(result.autoLaunch)
    }

    @Test
    fun `a unique tool match auto-launches`() {
        val result = AppSearch.search("ma", all)
        assertEquals(listOf(maps), result.matches)
        assertEquals(maps, result.autoLaunch)
    }

    @Test
    fun `tool apps match any part of the label, prefix matches first`() {
        val result = AppSearch.search("a", listOf(maps, camera, calendar))
        assertEquals(setOf(maps, camera, calendar), result.matches.toSet())
        assertNull(result.autoLaunch)
    }

    @Test
    fun `vault apps stay invisible on partial input`() {
        assertTrue(AppSearch.search("insta", all).matches.isEmpty())
        assertTrue(AppSearch.search("instagra", all).matches.isEmpty())
    }

    @Test
    fun `vault app shows on its full name, ignoring case, and never auto-launches`() {
        val result = AppSearch.search("INSTAGRAM", all)
        assertEquals(listOf(instagram), result.matches)
        assertNull(result.autoLaunch)
    }

    @Test
    fun `hidden apps never show, even on the full name`() {
        assertTrue(AppSearch.search("secret", all).matches.isEmpty())
    }

    @Test
    fun `breathe apps are found like tools and auto-launch (into their pause)`() {
        val whatsapp = app("WhatsApp", AppKind.BREATHE)
        val result = AppSearch.search("whats", listOf(whatsapp, instagram))
        assertEquals(listOf(whatsapp), result.matches)
        assertEquals(whatsapp, result.autoLaunch)
    }

    @Test
    fun `a tool match alongside a vault match does not auto-launch`() {
        val lite = app("Instagram Lite")
        val result = AppSearch.search("instagram", listOf(instagram, lite))
        assertEquals(2, result.matches.size)
        assertNull(result.autoLaunch)
    }
}
