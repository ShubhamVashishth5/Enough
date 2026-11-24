package app.minimise.apps

import app.minimise.apps.data.AndroidPrivateSpace
import app.minimise.apps.data.NotificationAutoVaultNotice
import app.minimise.apps.domain.PrivateSpace
import app.minimise.apps.domain.AutoVaultNotice
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AppsModule {
    @Binds
    abstract fun autoVaultNotice(impl: NotificationAutoVaultNotice): AutoVaultNotice

    @Binds
    abstract fun privateSpace(impl: AndroidPrivateSpace): PrivateSpace
}
