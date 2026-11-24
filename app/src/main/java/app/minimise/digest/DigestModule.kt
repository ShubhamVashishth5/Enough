package app.minimise.digest

import app.minimise.digest.data.AlarmDigestScheduler
import app.minimise.digest.data.RoomDigestRepository
import app.minimise.digest.domain.DigestRepository
import app.minimise.digest.domain.DigestScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DigestModule {
    @Binds
    abstract fun digestRepository(impl: RoomDigestRepository): DigestRepository

    @Binds
    abstract fun digestScheduler(impl: AlarmDigestScheduler): DigestScheduler
}
