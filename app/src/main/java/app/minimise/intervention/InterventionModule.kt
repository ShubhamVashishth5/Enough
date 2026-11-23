package app.minimise.intervention

import app.minimise.intervention.domain.InterventionLauncher
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class InterventionModule {
    @Binds
    abstract fun interventionLauncher(impl: ActivityInterventionLauncher): InterventionLauncher
}
