package app.minimise.apps.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppFilterTest {
    private val labels = listOf("Google Maps", "Gmail", "Météo", "Instagram", "instagram.com")

    @Test
    fun `a blank search keeps every app, in order`() {
        assertEquals(labels, AppFilter.filter(labels, "") { it })
        assertEquals(labels, AppFilter.filter(labels, "   ") { it })
    }

    @Test
    fun `any part of the name matches, whatever the case`() {
        assertEquals(listOf("Google Maps", "Gmail"), AppFilter.filter(labels, "MA") { it })
        assertEquals(listOf("Instagram", "instagram.com"), AppFilter.filter(labels, "gram") { it })
    }

    @Test
    fun `accents and surrounding spaces don't count`() {
        assertTrue(AppFilter.matches("Météo", "meteo"))
        assertTrue(AppFilter.matches("Meteo", "météo"))
        assertTrue(AppFilter.matches("Gmail", "  gma "))
    }

    @Test
    fun `nothing matches a name that isn't there`() {
        assertFalse(AppFilter.matches("Gmail", "maps"))
        assertEquals(emptyList<String>(), AppFilter.filter(labels, "zzz") { it })
    }
}
