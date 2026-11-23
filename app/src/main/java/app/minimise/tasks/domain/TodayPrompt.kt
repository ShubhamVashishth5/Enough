package app.minimise.tasks.domain

import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.DayBoundary
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** The Today screen opens once per usage day, on the first unlock (or the first visit home). */
@Singleton
class TodayPrompt @Inject constructor(
    private val settingsStore: SettingsStore,
    private val dayBoundary: DayBoundary,
    private val taskRepository: TaskRepository,
) {
    private val mutex = Mutex()

    /** True the first time it's asked each usage day, unless today's tasks are already set. */
    suspend fun claim(): Boolean = mutex.withLock {
        val today = dayBoundary.today()
        if (settingsStore.current().todayPromptedDay == today) return@withLock false
        settingsStore.update { it.copy(todayPromptedDay = today) }
        taskRepository.tasksFor(today).isEmpty()
    }
}
