package app.minimise.onboarding

import app.minimise.onboarding.domain.InitialSetup
import app.minimise.onboarding.ui.OnboardingStep
import app.minimise.testing.FakeSettingsStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Making Enough the default home app starts a fresh home screen part way through onboarding. */
class OnboardingProgressTest {
    private val store = FakeSettingsStore()
    private val setup = InitialSetup(store)

    @Test
    fun `a fresh start carries on from the saved step`() = runTest {
        setup.saveStep(OnboardingStep.PERMISSIONS.name)

        assertEquals(OnboardingStep.PERMISSIONS, OnboardingStep.fromSaved(setup.savedStep()))
    }

    @Test
    fun `nothing saved, or a step this version doesn't have, starts at the welcome`() {
        assertEquals(OnboardingStep.WELCOME, OnboardingStep.fromSaved(null))
        assertEquals(OnboardingStep.WELCOME, OnboardingStep.fromSaved("A_STEP_FROM_ELSEWHERE"))
    }

    @Test
    fun `finishing forgets the step`() = runTest {
        setup.saveStep(OnboardingStep.BUDGET.name)

        setup.finish()

        assertNull(setup.savedStep())
        assertTrue(store.state.value.onboardingDone)
    }
}
