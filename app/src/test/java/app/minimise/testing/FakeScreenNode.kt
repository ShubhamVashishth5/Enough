package app.minimise.testing

import app.minimise.monitor.domain.ScreenNode

/** A hand-built screen for matcher tests. */
data class FakeScreenNode(
    override val viewId: String? = null,
    override val text: CharSequence? = null,
    override val contentDescription: CharSequence? = null,
    override val visible: Boolean = true,
    override val focused: Boolean = false,
    val children: List<FakeScreenNode> = emptyList(),
) : ScreenNode {
    override val childCount: Int get() = children.size

    override fun child(index: Int): ScreenNode? = children.getOrNull(index)
}

fun node(viewId: String? = null, text: String? = null, vararg children: FakeScreenNode) =
    FakeScreenNode(viewId = viewId, text = text, children = children.toList())
