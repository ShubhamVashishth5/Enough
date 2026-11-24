package app.minimise.apps.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateSpaceAppsTest {
    @Test
    fun `a locked private space hides its apps, nothing else does`() {
        assertFalse(PrivateSpaceApps.showsProfile(isPrivate = true, locked = true))
        assertTrue(PrivateSpaceApps.showsProfile(isPrivate = true, locked = false))
        assertTrue(PrivateSpaceApps.showsProfile(isPrivate = false, locked = true))
        assertTrue(PrivateSpaceApps.showsProfile(isPrivate = false, locked = false))
    }
}
