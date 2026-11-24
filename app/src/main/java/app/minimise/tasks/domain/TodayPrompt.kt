package app.minimise.tasks.domain

import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import app.minimise.core.time.DayBoundary
import app.minimise.modes.domain.ActiveModeProvider
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
    private val activeMode: ActiveModeProvider,
) {
    private val mutex = Mutex()

    /**
     * True the first time it's asked each usage day, unless today's tasks are already set. During
     * wind-down (the Sleep mode) it waits, so the first unlock after the wake time gets it.
     */
    suspend fun claim(): Boolean = mutex.withLock {
        val today = dayBoundary.today()
        val settings = settingsStore.current()
        if (!settings.onboardingDone || settings.todayPromptedDay == today) return@withLock false
        if (activeMode.active()?.isSleep == true) return@withLock false
        settingsStore.update { it.copy(todayPromptedDay = today) }
        taskRepository.tasksFor(today).isEmpty()
    }
}
