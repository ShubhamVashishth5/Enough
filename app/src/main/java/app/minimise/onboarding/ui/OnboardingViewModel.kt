package app.minimise.onboarding.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.minimise.apps.data.HomeSlotsInitializer
import app.minimise.apps.domain.AppEntry
import app.minimise.apps.domain.AppKind
import app.minimise.apps.domain.AppRepository
import app.minimise.apps.domain.CommonTimeSinks
import app.minimise.apps.domain.WebTarget
import app.minimise.core.Defaults
import app.minimise.onboarding.domain.InitialSetup
import app.minimise.settings.domain.SetAppKind
import app.minimise.settings.domain.SettingsGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep { WELCOME, HONEST_LIMITS, PERMISSIONS, HOME_APPS, VAULT, BUDGET }

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.WELCOME,
    val apps: List<AppEntry> = emptyList(),
    /** In slot order. */
    val homePicks: List<String> = emptyList(),
    val vaultPicks: Set<String> = emptySet(),
    val budgetMinutes: Int = Defaults.DAILY_BUDGET.inWholeMinutes.toInt(),
    val saving: Boolean = false,
    val finished: Boolean = false,
) {
    /** Pre-ticked apps first, so the likely time sinks are at the top. */
    val vaultChoices: List<AppEntry> get() = apps.sortedBy { if (it.packageName in vaultPicks) 0 else 1 }
}

/** First run: welcome, honest limits, permissions, home apps, Vault apps, budget. */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val settingsGuard: SettingsGuard,
    private val initialSetup: InitialSetup,
    homeSlotsInitializer: HomeSlotsInitializer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            homeSlotsInitializer.initialiseIfNeeded()
            val apps = appRepository.apps.first()
            _uiState.update { state ->
                state.copy(
                    apps = apps,
                    homePicks = apps.filter { it.homeSlot != null }.sortedBy { it.homeSlot }.map { it.packageName },
                    vaultPicks = apps
                        .filter { it.kind == AppKind.VAULT || it.packageName in CommonTimeSinks.packages }
                        .map { it.packageName }
                        .toSet(),
                )
            }
        }
    }

    fun onHomePickToggled(app: AppEntry) = _uiState.update { state ->
        val picks = state.homePicks
        when {
            app.packageName in picks -> state.copy(homePicks = picks - app.packageName)
            picks.size < Defaults.HOME_SLOT_COUNT -> state.copy(homePicks = picks + app.packageName)
            else -> state
        }
    }

    fun onVaultPickToggled(app: AppEntry) = _uiState.update { state ->
        val picks = state.vaultPicks
        state.copy(vaultPicks = if (app.packageName in picks) picks - app.packageName else picks + app.packageName)
    }

    fun onBudgetStep(deltaMinutes: Int) = _uiState.update {
        it.copy(budgetMinutes = (it.budgetMinutes + deltaMinutes).coerceIn(0, MAX_BUDGET_MINUTES))
    }

    fun onBack() = _uiState.update { state ->
        val previous = OnboardingStep.entries.getOrNull(state.step.ordinal - 1) ?: return@update state
        state.copy(step = previous)
    }

    fun onNext() {
        val state = _uiState.value
        if (state.saving) return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            when (state.step) {
                OnboardingStep.HOME_APPS -> saveHomePicks(state.homePicks)
                OnboardingStep.VAULT -> saveVaultPicks(state.apps, state.vaultPicks)
                OnboardingStep.BUDGET -> {
                    initialSetup.setInitialBudget(state.budgetMinutes)
                    initialSetup.finish()
                }
                OnboardingStep.WELCOME, OnboardingStep.HONEST_LIMITS, OnboardingStep.PERMISSIONS -> Unit
            }
            _uiState.update {
                val next = OnboardingStep.entries.getOrNull(it.step.ordinal + 1)
                if (next == null) it.copy(saving = false, finished = true) else it.copy(saving = false, step = next)
            }
        }
    }

    private suspend fun saveHomePicks(picks: List<String>) {
        (0 until Defaults.HOME_SLOT_COUNT).forEach { slot ->
            val packageName = picks.getOrNull(slot)
            if (packageName == null) appRepository.clearHomeSlot(slot) else appRepository.setHomeSlot(packageName, slot)
        }
    }

    /** Through the guard like any other change: ticking is tightening, so it applies at once. Their websites go in too. */
    private suspend fun saveVaultPicks(apps: List<AppEntry>, picks: Set<String>) {
        apps.forEach { app ->
            val wanted = if (app.packageName in picks) AppKind.VAULT else if (app.kind == AppKind.VAULT) AppKind.TOOL else null
            if (wanted != null && wanted != app.kind) settingsGuard.request(SetAppKind(app.packageName, wanted))
        }
        picks.flatMap { CommonTimeSinks.websites[it].orEmpty() }.toSet().forEach { domain ->
            settingsGuard.request(SetAppKind(WebTarget.idFor(domain), AppKind.VAULT))
        }
    }

    private companion object {
        const val MAX_BUDGET_MINUTES = 8 * 60
    }
}
