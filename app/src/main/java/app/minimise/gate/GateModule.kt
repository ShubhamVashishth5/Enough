package app.minimise.gate

import app.minimise.gate.data.RoomGrantStore
import app.minimise.gate.domain.GrantStore
import app.minimise.gate.domain.Rule
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
}
