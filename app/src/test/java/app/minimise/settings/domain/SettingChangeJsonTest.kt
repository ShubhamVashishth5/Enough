package app.minimise.settings.domain

import app.minimise.apps.domain.AppKind
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pending changes are stored as JSON, so the format must round-trip and carry its type. */
class SettingChangeJsonTest {
    private fun roundTrip(change: SettingChange): SettingChange =
        Json.decodeFromString(SettingChange.serializer(), Json.encodeToString(SettingChange.serializer(), change))

    @Test
    fun `changes survive a round trip`() {
        assertEquals(SetAppKind("com.instagram.android", AppKind.TOOL), roundTrip(SetAppKind("com.instagram.android", AppKind.TOOL)))
        assertEquals(SetBudget(60), roundTrip(SetBudget(60)))
    }

    @Test
    fun `a mode switch keeps its end, and one stored before ends existed reads without one`() {
        assertEquals(SetManualMode(3, untilEpochMillis = 1_000L), roundTrip(SetManualMode(3, untilEpochMillis = 1_000L)))
        val old = Json.decodeFromString(SettingChange.serializer(), """{"type":"set_manual_mode","modeId":3}""")
        assertEquals(SetManualMode(3, untilEpochMillis = null), old)
    }

    @Test
    fun `new modes are told apart by name, edits by id`() {
        val add = SaveMode(0, 20, null, 0, 0, 0, name = "Commute")
        assertEquals(add, roundTrip(add))
        assertEquals("mode:new:Commute", add.key)
        assertEquals("mode:5", SaveMode(5, 20, null, 0, 0, 0).key)
        assertEquals("mode:5", RemoveMode(5).key)
        assertEquals(SetBreatheWait("pkg.whatsapp", 30), roundTrip(SetBreatheWait("pkg.whatsapp", 30)))
    }

    @Test
    fun `json carries a stable type name`() {
        val json = Json.encodeToString(SettingChange.serializer(), SetBudget(30))
        assertEquals("set_budget", Json.parseToJsonElement(json).jsonObject["type"]?.jsonPrimitive?.content)
    }
}
