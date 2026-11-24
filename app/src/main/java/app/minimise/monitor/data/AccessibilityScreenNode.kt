package app.minimise.monitor.data

import android.view.accessibility.AccessibilityNodeInfo
import app.minimise.monitor.domain.ScreenNode
import timber.log.Timber

/** Adapts the platform's node to [ScreenNode], so matching code stays plain Kotlin. */
class AccessibilityScreenNode(private val info: AccessibilityNodeInfo) : ScreenNode {
    override val viewId: String? get() = info.viewIdResourceName
    override val text: CharSequence? get() = info.text
    override val contentDescription: CharSequence? get() = info.contentDescription
    override val visible: Boolean get() = info.isVisibleToUser
    override val focused: Boolean get() = info.isFocused
    override val childCount: Int get() = info.childCount

    override fun child(index: Int): ScreenNode? = runCatching { info.getChild(index) }.getOrNull()?.let(::AccessibilityScreenNode)

    override fun findByViewId(viewId: String): List<ScreenNode> =
        runCatching { info.findAccessibilityNodeInfosByViewId(viewId) }.getOrNull().orEmpty().map(::AccessibilityScreenNode)
}

/** Debug builds: logs a window's node tree, to find the view IDs a rule file needs. */
object ScreenDump {
    private const val TAG = "ScreenDump"

    fun log(root: AccessibilityNodeInfo) {
        Timber.tag(TAG).d("Window of %s", root.packageName)
        walk(root, depth = 0)
    }

    private fun walk(node: AccessibilityNodeInfo, depth: Int) {
        if (depth > 80) return
        Timber.tag(TAG).d(
            "%s%s id=%s text=%s desc=%s visible=%s",
            "  ".repeat(depth),
            node.className?.toString()?.substringAfterLast('.'),
            node.viewIdResourceName,
            node.text,
            node.contentDescription,
            node.isVisibleToUser,
        )
        for (i in 0 until node.childCount) runCatching { node.getChild(i) }.getOrNull()?.let { walk(it, depth + 1) }
    }
}
