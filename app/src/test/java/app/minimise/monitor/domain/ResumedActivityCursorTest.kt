package app.minimise.monitor.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ResumedActivityCursorTest {
    private fun at(millis: Long, packageName: String = "pkg.a") = ResumedActivity(packageName, "$packageName.Main", millis)

    @Test
    fun `reads overlap the last one, and an event is handled once`() {
        val cursor = ResumedActivityCursor(startMillis = 10_000, overlapMillis = 2_000)
        assertEquals(8_000L, cursor.readFrom())
        assertEquals(listOf(at(10_500)), cursor.accept(listOf(at(10_500)), readTo = 11_000))
        assertEquals(9_000L, cursor.readFrom())
        // The next read sees the same event again: it isn't handled twice.
        assertEquals(listOf(at(11_200, "pkg.b")), cursor.accept(listOf(at(10_500), at(11_200, "pkg.b")), readTo = 12_000))
    }

    @Test
    fun `an event written late is still caught by the overlap`() {
        val cursor = ResumedActivityCursor(startMillis = 10_000, overlapMillis = 2_000)
        assertEquals(emptyList<ResumedActivity>(), cursor.accept(emptyList(), readTo = 11_000))
        // Stamped 10 900, but only visible after the first read ended at 11 000.
        assertEquals(listOf(at(10_900)), cursor.accept(listOf(at(10_900)), readTo = 12_000))
    }

    @Test
    fun `events from before the start, or older than the newest handled, are dropped`() {
        val cursor = ResumedActivityCursor(startMillis = 10_000, overlapMillis = 2_000)
        assertEquals(emptyList<ResumedActivity>(), cursor.accept(listOf(at(9_000)), readTo = 11_000))
        cursor.accept(listOf(at(11_500, "pkg.b")), readTo = 12_000)
        // pkg.a came to the front before pkg.b did, so it isn't in front any more.
        assertEquals(emptyList<ResumedActivity>(), cursor.accept(listOf(at(11_400)), readTo = 13_000))
    }

    @Test
    fun `events come back oldest first`() {
        val cursor = ResumedActivityCursor(startMillis = 10_000, overlapMillis = 2_000)
        val read = listOf(at(10_700, "pkg.c"), at(10_200, "pkg.b"), at(10_400, "pkg.a"))
        assertEquals(listOf("pkg.b", "pkg.a", "pkg.c"), cursor.accept(read, readTo = 11_000).map { it.packageName })
    }

    @Test
    fun `two apps in the same millisecond are both kept`() {
        val cursor = ResumedActivityCursor(startMillis = 10_000, overlapMillis = 2_000)
        assertEquals(2, cursor.accept(listOf(at(10_500), at(10_500, "pkg.b")), readTo = 11_000).size)
        assertEquals(0, cursor.accept(listOf(at(10_500), at(10_500, "pkg.b")), readTo = 12_000).size)
    }
}
