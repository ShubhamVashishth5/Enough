package app.minimise.browsers

import app.minimise.browsers.data.AssetBrowserRuleSource
import app.minimise.browsers.domain.BrowserRuleSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class BrowsersModule {
    @Binds
    abstract fun browserRuleSource(impl: AssetBrowserRuleSource): BrowserRuleSource
}
