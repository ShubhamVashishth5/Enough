package app.minimise.protection.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLockPlanTest {
    private val vault = setOf("pkg.instagram", "pkg.tiktok", "web:youtube.com")

    @Test
    fun `below full protection every unlocked vault app is locked`() {
        val locked = mapOf("pkg.instagram" to false, "pkg.tiktok" to true)
        val plan = AppLockPlan.of(ProtectionLevel.LITE, vault, locked, lockedByEnough = emptySet())
        assertEquals(listOf("pkg.instagram"), plan.toLock)
        assertEquals(emptyList<String>(), plan.toUnlock)
    }

    @Test
    fun `websites and apps without app lock support are left alone`() {
        val plan = AppLockPlan.of(ProtectionLevel.LAUNCHER_ONLY, vault, locked = emptyMap(), lockedByEnough = emptySet())
        assertEquals(AppLockPlan(emptyList(), emptyList()), plan)
    }

    @Test
    fun `back at full protection only enough's own locks come off`() {
        val locked = mapOf("pkg.instagram" to true, "pkg.tiktok" to true)
        val plan = AppLockPlan.of(ProtectionLevel.FULL, vault, locked, lockedByEnough = setOf("pkg.instagram"))
        assertEquals(emptyList<String>(), plan.toLock)
        assertEquals(listOf("pkg.instagram"), plan.toUnlock)
    }

    @Test
    fun `an app that left the vault is unlocked even below full protection`() {
        val locked = mapOf("pkg.maps" to true, "pkg.instagram" to true)
        val plan = AppLockPlan.of(ProtectionLevel.LITE, vault, locked, lockedByEnough = setOf("pkg.maps", "pkg.instagram"))
        assertEquals(listOf("pkg.maps"), plan.toUnlock)
    }

    @Test
    fun `locks taken off in system settings are forgotten`() {
        val locked = mapOf("pkg.instagram" to false, "pkg.tiktok" to true)
        assertEquals(setOf("pkg.tiktok"), AppLockPlan.stillLocked(setOf("pkg.instagram", "pkg.tiktok", "pkg.gone"), locked))
    }
}
