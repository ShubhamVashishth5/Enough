package app.minimise.feeds

import app.minimise.feeds.data.AssetFeedRuleSource
import app.minimise.feeds.domain.FeedRuleSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class FeedsModule {
    @Binds
    abstract fun feedRuleSource(impl: AssetFeedRuleSource): FeedRuleSource
}
