package app.minimise.protection

import android.content.Context
import android.os.Build
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object ProtectionModule {
    @Provides
    fun appLocker(@ApplicationContext context: Context): AppLocker =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) PlatformAppLocker(context) else NoOpAppLocker()
}
