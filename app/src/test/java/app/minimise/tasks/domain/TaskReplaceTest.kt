package app.minimise.tasks.domain

import app.minimise.tasks.domain.TaskReplace.Step
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskReplaceTest {
    @Test
    fun `moving tasks keeps every one of them`() {
        val plan = TaskReplace.plan(existing = listOf("a", "b", "c"), texts = listOf("c", "a", "b"))
        assertEquals(listOf(Step.Keep(2, 0), Step.Keep(0, 1), Step.Keep(1, 2)), plan.steps)
        assertTrue(plan.removed.isEmpty())
    }

    @Test
    fun `a changed text takes over a task that went, as an edit`() {
        val plan = TaskReplace.plan(existing = listOf("a", "b", "c"), texts = listOf("a", "B!", "c"))
        assertEquals(listOf(Step.Keep(0, 0), Step.Edit(1, 1, "B!"), Step.Keep(2, 2)), plan.steps)
        assertTrue(plan.removed.isEmpty())
    }

    @Test
    fun `more tasks are added and fewer are removed`() {
        assertEquals(
            listOf(Step.Keep(0, 0), Step.Add(1, "new")),
            TaskReplace.plan(existing = listOf("a"), texts = listOf("a", "new")).steps,
        )
        val fewer = TaskReplace.plan(existing = listOf("a", "b", "c"), texts = listOf("c"))
        assertEquals(listOf(Step.Keep(2, 0)), fewer.steps)
        assertEquals(listOf(0, 1), fewer.removed)
    }

    @Test
    fun `the same text twice matches two tasks, not one`() {
        val plan = TaskReplace.plan(existing = listOf("walk", "walk"), texts = listOf("walk", "walk"))
        assertEquals(listOf(Step.Keep(0, 0), Step.Keep(1, 1)), plan.steps)
    }
}
