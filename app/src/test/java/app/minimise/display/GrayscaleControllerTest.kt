package app.minimise.display

import app.minimise.display.domain.GrayscaleMode
import org.junit.Assert.assertEquals
import org.junit.Test

class GrayscaleControllerTest {
    @Test
    fun `grayscale follows the mode and the app in front`() {
        assertEquals(false, GrayscaleController.wanted(GrayscaleMode.OFF, inVaultApp = true))
        assertEquals(true, GrayscaleController.wanted(GrayscaleMode.VAULT_APPS, inVaultApp = true))
        assertEquals(false, GrayscaleController.wanted(GrayscaleMode.VAULT_APPS, inVaultApp = false))
        assertEquals(true, GrayscaleController.wanted(GrayscaleMode.ALWAYS, inVaultApp = false))
    }
}
