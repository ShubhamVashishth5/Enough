package app.minimise.onboarding.domain

import app.minimise.core.prefs.SettingsStore
import app.minimise.core.prefs.current
import javax.inject.Inject
import kotlin.time.Duration.Companion.minutes

/**
 * Writes the first-run budget directly. Nothing exists to protect yet, so it skips the loosen-later
 * rule; once onboarding is done, it refuses and every change goes through SettingsGuard.
 */
class InitialSetup @Inject constructor(
    private val settingsStore: SettingsStore,
) {
    suspend fun isDone(): Boolean = settingsStore.current().onboardingDone

    /** Returns false (and changes nothing) after onboarding. */
    suspend fun setInitialBudget(minutes: Int): Boolean {
        if (isDone()) return false
        settingsStore.update { it.copy(dailyBudget = minutes.minutes) }
        return true
    }

    /** The step onboarding was on, by name, if it's under way. */
    suspend fun savedStep(): String? = settingsStore.current().onboardingStep

    /** Remembers the step on screen, so a fresh start of the home screen carries on from it. */
    suspend fun saveStep(step: String) {
        settingsStore.update { it.copy(onboardingStep = step) }
    }

    suspend fun finish() {
        settingsStore.update { it.copy(onboardingDone = true, homeSlotsChosen = true, onboardingStep = null) }
    }
}
