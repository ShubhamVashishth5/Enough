package app.minimise.tasks.domain

/**
 * How saving the Today screen maps the new list of texts onto the day's existing tasks. A text that's
 * still there keeps its task (its done mark and earn-it record) wherever it moved. A new text takes
 * over a task whose text went, as an edit, starting undone. Tasks left over are deleted.
 */
object TaskReplace {
    sealed interface Step {
        val position: Int

        /** The task at [from] in the old list, unchanged apart from its position. */
        data class Keep(val from: Int, override val position: Int) : Step

        /** The task at [from] gets a new text and starts undone. */
        data class Edit(val from: Int, override val position: Int, val text: String) : Step

        data class Add(override val position: Int, val text: String) : Step
    }

    /** @property removed indices into the old list. */
    data class Plan(val steps: List<Step>, val removed: List<Int>)

    fun plan(existing: List<String>, texts: List<String>): Plan {
        val unmatched = existing.indices.toMutableList()
        val kept = texts.map { text -> unmatched.firstOrNull { existing[it] == text }?.also { unmatched.remove(it) } }
        val steps = texts.mapIndexed { position, text ->
            val same = kept[position]
            when {
                same != null -> Step.Keep(same, position)
                unmatched.isNotEmpty() -> Step.Edit(unmatched.removeAt(0), position, text)
                else -> Step.Add(position, text)
            }
        }
        return Plan(steps, removed = unmatched)
    }
}
