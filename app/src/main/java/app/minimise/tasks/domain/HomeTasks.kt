package app.minimise.tasks.domain

import app.minimise.core.Defaults

/**
 * Which of today's tasks home shows: the top ones still to do, in the order the user put them. With
 * room to spare (three tasks or fewer to do), ticked ones stay, struck through, so a tap can undo them.
 *
 * @property more tasks still to do that don't fit, waiting on the Today screen.
 */
data class HomeTasks(val shown: List<TodayTask>, val more: Int) {
    companion object {
        fun from(tasks: List<TodayTask>, count: Int = Defaults.HOME_TASKS): HomeTasks {
            val sorted = tasks.sortedBy { it.position }
            val toDo = sorted.filter { !it.done }.take(count)
            val done = sorted.filter { it.done }.take(count - toDo.size)
            val shown = (toDo + done).sortedBy { it.position }
            return HomeTasks(shown = shown, more = sorted.count { !it.done } - toDo.size)
        }
    }
}
