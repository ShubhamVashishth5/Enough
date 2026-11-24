package app.minimise.monitor.domain

import app.minimise.testing.FakeScreenNode
import app.minimise.testing.node
import kotlin.time.Duration.Companion.milliseconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTreeTest {
    private val screen = node(
        "app:id/root",
        null,
        node("app:id/toolbar", "Home"),
        node("app:id/list", null, node("app:id/item", "Hello"), node("app:id/reel_recycler")),
    )

    @Test
    fun `finds a node by view id anywhere in the tree`() {
        assertNotNull(ScreenTree.firstMatch(screen, listOf(NodeMatcher(viewId = "app:id/reel_recycler")), maxDepth = 10, maxNodes = 100))
    }

    @Test
    fun `an empty matcher matches nothing`() {
        assertNull(ScreenTree.firstMatch(screen, listOf(NodeMatcher()), maxDepth = 10, maxNodes = 100))
    }

    @Test
    fun `hidden nodes don't match`() {
        val hidden = FakeScreenNode(viewId = "app:id/reel_recycler", visible = false)
        assertNull(ScreenTree.firstMatch(hidden, listOf(NodeMatcher(viewId = "app:id/reel_recycler")), 10, 100))
    }

    @Test
    fun `every set field must match`() {
        assertTrue(NodeMatcher(viewId = "app:id/item", text = "hello").matches(node("app:id/item", "Hello")))
        assertFalse(NodeMatcher(viewId = "app:id/item", text = "bye").matches(node("app:id/item", "Hello")))
        assertTrue(NodeMatcher(textContains = "ell").matches(node(text = "Hello")))
    }

    @Test
    fun `the depth limit stops the walk`() {
        assertNull(ScreenTree.firstMatch(screen, listOf(NodeMatcher(viewId = "app:id/reel_recycler")), maxDepth = 1, maxNodes = 100))
    }

    @Test
    fun `the node limit stops the walk`() {
        assertNull(ScreenTree.firstMatch(screen, listOf(NodeMatcher(viewId = "app:id/reel_recycler")), maxDepth = 10, maxNodes = 3))
    }

    @Test
    fun `matchesAll needs every matcher somewhere`() {
        val both = listOf(NodeMatcher(text = "Home"), NodeMatcher(text = "Hello"))
        assertTrue(ScreenTree.matchesAll(screen, both, 10, 100))
        assertFalse(ScreenTree.matchesAll(screen, both + NodeMatcher(text = "Missing"), 10, 100))
    }

    @Test
    fun `labels are filled in`() {
        val filled = NodeMatcher(text = "{app_label}").withLabels(mapOf("app_label" to "Enough"))
        assertEquals("Enough", filled.text)
    }

    @Test
    fun `throttle allows one check per interval`() {
        val throttle = Throttle(300.milliseconds)
        assertEquals(0.milliseconds, throttle.waitBeforeNext(1000.milliseconds))
        throttle.markRun(1000.milliseconds)
        assertEquals(200.milliseconds, throttle.waitBeforeNext(1100.milliseconds))
        assertEquals(0.milliseconds, throttle.waitBeforeNext(1400.milliseconds))
    }
}
