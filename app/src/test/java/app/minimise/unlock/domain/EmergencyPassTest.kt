package app.minimise.unlock.domain

import app.minimise.usage.domain.Event
import app.minimise.usage.domain.EventType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class EmergencyPassTest {
    private fun used(method: UnlockMethodId) =
        Event(type = EventType.OVERRIDE_USED, startAt = Instant.EPOCH, data = """{"method":"${method.name}","timeBoxMinutes":5}""")

    @Test
    fun `three a week, counting only emergency passes`() {
        assertEquals(3, EmergencyPassMethod.remaining(emptyList()))
        assertEquals(2, EmergencyPassMethod.remaining(listOf(used(UnlockMethodId.EMERGENCY_PASS), used(UnlockMethodId.NFC_KEY))))
    }

    @Test
    fun `never below zero`() {
        assertEquals(0, EmergencyPassMethod.remaining(List(5) { used(UnlockMethodId.EMERGENCY_PASS) }))
    }
}
