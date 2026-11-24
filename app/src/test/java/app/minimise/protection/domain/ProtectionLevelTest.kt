package app.minimise.protection.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionLevelTest {
    @Test
    fun `accessibility wins whatever else is granted`() {
        assertEquals(ProtectionLevel.FULL, ProtectionLevel.from(accessibility = true, usageAccess = false, overlay = false))
        assertEquals(ProtectionLevel.FULL, ProtectionLevel.from(accessibility = true, usageAccess = true, overlay = true))
    }

    @Test
    fun `lite needs both usage access and the overlay`() {
        assertEquals(ProtectionLevel.LITE, ProtectionLevel.from(accessibility = false, usageAccess = true, overlay = true))
        assertEquals(ProtectionLevel.LAUNCHER_ONLY, ProtectionLevel.from(accessibility = false, usageAccess = true, overlay = false))
        assertEquals(ProtectionLevel.LAUNCHER_ONLY, ProtectionLevel.from(accessibility = false, usageAccess = false, overlay = true))
        assertEquals(ProtectionLevel.LAUNCHER_ONLY, ProtectionLevel.from(accessibility = false, usageAccess = false, overlay = false))
    }

    @Test
    fun `only full protection reads screens`() {
        assertTrue(ProtectionLevel.FULL.readsScreens)
        assertFalse(ProtectionLevel.LITE.readsScreens)
        assertFalse(ProtectionLevel.LAUNCHER_ONLY.readsScreens)
    }
}
