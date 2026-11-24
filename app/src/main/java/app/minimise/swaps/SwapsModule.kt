package app.minimise.swaps

import app.minimise.swaps.data.RoomSwapRepository
import app.minimise.swaps.domain.SwapRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class SwapsModule {
    @Binds
    abstract fun swapRepository(impl: RoomSwapRepository): SwapRepository
}
