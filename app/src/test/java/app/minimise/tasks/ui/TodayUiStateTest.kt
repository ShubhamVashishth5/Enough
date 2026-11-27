package app.minimise.tasks.ui

import app.minimise.core.Defaults
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayUiStateTest {
    private val fresh = TodayUiState(loaded = true, yesterday = listOf("Call the bank", "Write the report"))

    @Test
    fun `yesterday's undone tasks show until they are carried over`() {
        assertEquals(listOf("Call the bank", "Write the report"), fresh.leftovers)
        val carried = fresh.carriedOver("Write the report")
        assertEquals(listOf("Call the bank"), carried.leftovers)
    }

    @Test
    fun `a carried task fills the first empty line`() {
        val state = fresh.copy(drafts = listOf(TaskDraft("Run"), TaskDraft(), TaskDraft()))
        assertEquals(listOf(TaskDraft("Run"), TaskDraft("Call the bank"), TaskDraft()), state.carriedOver("Call the bank").drafts)
    }

    @Test
    fun `with no empty line a carried task is added at the end`() {
        val state = fresh.copy(drafts = listOf(TaskDraft("Run"), TaskDraft("Read"), TaskDraft("Cook")))
        assertEquals(TaskDraft("Call the bank"), state.carriedOver("Call the bank").drafts.last())
        assertEquals(4, state.carriedOver("Call the bank").drafts.size)
    }

    @Test
    fun `a full day takes nothing more`() {
        val full = fresh.copy(drafts = List(Defaults.MAX_TODAY_TASKS) { TaskDraft("Task $it") })
        assertFalse(full.canCarryOver)
        assertSame(full, full.carriedOver("Call the bank"))
    }

    @Test
    fun `a task typed again today counts as carried`() {
        val state = fresh.copy(drafts = listOf(TaskDraft("Call the bank "), TaskDraft(), TaskDraft()))
        assertEquals(listOf("Write the report"), state.leftovers)
        assertTrue(state.canCarryOver)
    }
}
