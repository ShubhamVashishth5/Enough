package app.minimise.tasks.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeTasksTest {
    private val day = LocalDate.of(2026, 11, 25)

    private fun tasks(vararg done: Boolean) =
        done.mapIndexed { i, isDone -> TodayTask(id = i.toLong(), text = "t$i", day = day, done = isDone, position = i) }

    private fun HomeTasks.ids() = shown.map { it.id }

    @Test
    fun `three or fewer show as they are, ticked ones in place`() {
        val home = HomeTasks.from(tasks(true, false, false))
        assertEquals(listOf(0L, 1L, 2L), home.ids())
        assertEquals(0, home.more)
    }

    @Test
    fun `with more to do, the top three still to do show and the rest wait`() {
        val home = HomeTasks.from(tasks(false, false, false, false, false))
        assertEquals(listOf(0L, 1L, 2L), home.ids())
        assertEquals(2, home.more)
    }

    @Test
    fun `ticking one off brings the next one up`() {
        val home = HomeTasks.from(tasks(true, false, false, false, false))
        assertEquals(listOf(1L, 2L, 3L), home.ids())
        assertEquals(1, home.more)
    }

    @Test
    fun `near the end, ticked ones fill the spare rows`() {
        val home = HomeTasks.from(tasks(true, true, false, true, false))
        assertEquals(listOf(0L, 2L, 4L), home.ids())
        assertEquals(0, home.more)
    }

    @Test
    fun `order follows position, not the list`() {
        val shuffled = tasks(false, false, false).reversed()
        assertEquals(listOf(0L, 1L, 2L), HomeTasks.from(shuffled).ids())
    }
}
