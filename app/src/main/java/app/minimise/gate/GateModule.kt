package app.minimise.gate

import app.minimise.gate.data.RoomGrantStore
import app.minimise.gate.domain.GrantStore
import app.minimise.gate.domain.Rule
import app.minimise.gate.rules.BreathePauseRule
import app.minimise.gate.rules.BudgetRule
import app.minimise.gate.rules.FirstHourRule
import app.minimise.gate.rules.FocusRule
import app.minimise.gate.rules.ModeAllowlistRule
import app.minimise.gate.rules.OpenLimitRule
import app.minimise.gate.rules.ScheduleRule
import app.minimise.gate.rules.VaultPauseRule
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Adding a rule means adding one class and one @IntoSet binding here. */
@Module
@InstallIn(SingletonComponent::class)
abstract class GateModule {
    @Binds
    abstract fun grantStore(impl: RoomGrantStore): GrantStore

    @Binds
    @IntoSet
    abstract fun vaultPauseRule(rule: VaultPauseRule): Rule

    @Binds
    @IntoSet
    abstract fun breathePauseRule(rule: BreathePauseRule): Rule

    @Binds
    @IntoSet
    abstract fun budgetRule(rule: BudgetRule): Rule

    @Binds
    @IntoSet
    abstract fun openLimitRule(rule: OpenLimitRule): Rule

    @Binds
    @IntoSet
    abstract fun scheduleRule(rule: ScheduleRule): Rule

    @Binds
    @IntoSet
    abstract fun firstHourRule(rule: FirstHourRule): Rule

    @Binds
    @IntoSet
    abstract fun focusRule(rule: FocusRule): Rule

    @Binds
    @IntoSet
    abstract fun modeAllowlistRule(rule: ModeAllowlistRule): Rule
}
