package app.minimise.monitor.domain

import kotlin.time.Duration
import kotlinx.serialization.Serializable

/**
 * One element of another app's screen, as the accessibility service sees it. Plain Kotlin so the
 * matching code can be tested without a phone.
 */
interface ScreenNode {
    /** Full resource name, such as "com.google.android.youtube:id/reel_recycler". */
    val viewId: String?
    val text: CharSequence?
    val contentDescription: CharSequence?
    val visible: Boolean

    /** The user is typing into it (an address bar being edited). */
    val focused: Boolean
    val childCount: Int

    fun child(index: Int): ScreenNode?

    /**
     * Every node under this one with [viewId], found by the system in one call. Null when that isn't
     * supported, and the tree must be walked instead.
     */
    fun findByViewId(viewId: String): List<ScreenNode>? = null
}

/**
 * Describes one node. Every field that is set must match; an empty matcher matches nothing.
 * `{app_label}` and `{service_label}` in text fields are replaced with this app's own names.
 *
 * Stored in the JSON rule files under assets/, so keep field names stable.
 */
@Serializable
data class NodeMatcher(
    val viewId: String? = null,
    val text: String? = null,
    val textContains: String? = null,
    val contentDescription: String? = null,
    val descriptionContains: String? = null,
) {
    private val isEmpty: Boolean
        get() = viewId == null && text == null && textContains == null && contentDescription == null && descriptionContains == null

    fun matches(node: ScreenNode): Boolean {
        if (isEmpty || !node.visible) return false
        if (viewId != null && node.viewId != viewId) return false
        if (text != null && !node.text.toString().equals(text, ignoreCase = true)) return false
        if (textContains != null && node.text?.contains(textContains, ignoreCase = true) != true) return false
        if (contentDescription != null && !node.contentDescription.toString().equals(contentDescription, ignoreCase = true)) return false
        if (descriptionContains != null && node.contentDescription?.contains(descriptionContains, ignoreCase = true) != true) return false
        return true
    }

    fun withLabels(labels: Map<String, String>): NodeMatcher {
        fun String?.fill(): String? = this?.let { labels.entries.fold(it) { acc, (key, value) -> acc.replace("{$key}", value) } }
        return copy(text = text.fill(), textContains = textContains.fill(), contentDescription = contentDescription.fill(), descriptionContains = descriptionContains.fill())
    }
}

/** Bounded walks of a screen's node tree, so a huge or deep screen can't stall the service. */
object ScreenTree {
    /** Visits nodes depth-first until [visit] returns true. Returns whether it did. */
    fun any(root: ScreenNode, maxDepth: Int, maxNodes: Int, visit: (ScreenNode) -> Boolean): Boolean {
        var seen = 0
        val stack = ArrayDeque<Pair<ScreenNode, Int>>()
        stack.addLast(root to 0)
        while (stack.isNotEmpty()) {
            val (node, depth) = stack.removeLast()
            if (++seen > maxNodes) return false
            if (visit(node)) return true
            if (depth >= maxDepth) continue
            // Push in reverse so children are visited in order.
            for (i in node.childCount - 1 downTo 0) node.child(i)?.let { stack.addLast(it to depth + 1) }
        }
        return false
    }

    fun firstMatch(root: ScreenNode, matchers: List<NodeMatcher>, maxDepth: Int, maxNodes: Int): ScreenNode? {
        val (byId, walked) = fastLookups(root, matchers)
        byId.firstNotNullOfOrNull { (matcher, nodes) -> nodes.firstOrNull(matcher::matches) }?.let { return it }
        if (walked.isEmpty()) return null
        var found: ScreenNode? = null
        any(root, maxDepth, maxNodes) { node -> walked.any { it.matches(node) }.also { if (it) found = node } }
        return found
    }

    /** True when every one of [matchers] matches some node. */
    fun matchesAll(root: ScreenNode, matchers: List<NodeMatcher>, maxDepth: Int, maxNodes: Int): Boolean {
        if (matchers.isEmpty()) return false
        val (byId, walked) = fastLookups(root, matchers)
        if (byId.any { (matcher, nodes) -> nodes.none(matcher::matches) }) return false
        if (walked.isEmpty()) return true
        val left = walked.toMutableSet()
        any(root, maxDepth, maxNodes) { node ->
            left.removeAll { it.matches(node) }
            left.isEmpty()
        }
        return left.isEmpty()
    }

    /** Matchers with a view ID, paired with the system's lookup when the node supports it; the rest need a walk. */
    private fun fastLookups(root: ScreenNode, matchers: List<NodeMatcher>): Pair<List<Pair<NodeMatcher, List<ScreenNode>>>, List<NodeMatcher>> {
        val byId = mutableListOf<Pair<NodeMatcher, List<ScreenNode>>>()
        val walked = mutableListOf<NodeMatcher>()
        matchers.forEach { matcher ->
            val found = matcher.viewId?.let(root::findByViewId)
            if (found != null) byId += matcher to found else walked += matcher
        }
        return byId to walked
    }
}

/** At most one check per [interval]; a burst of events ends with one check after the last of them. */
class Throttle(private val interval: Duration) {
    private var lastRun: Duration? = null

    /** How long to wait before the next check may run, given the monotonic time [now]. */
    fun waitBeforeNext(now: Duration): Duration {
        val last = lastRun ?: return Duration.ZERO
        return (last + interval - now).coerceAtLeast(Duration.ZERO)
    }

    fun markRun(now: Duration) {
        lastRun = now
    }
}
