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
    fun `json carries a stable type name`() {
        val json = Json.encodeToString(SettingChange.serializer(), SetBudget(30))
        assertEquals("set_budget", Json.parseToJsonElement(json).jsonObject["type"]?.jsonPrimitive?.content)
    }
}
