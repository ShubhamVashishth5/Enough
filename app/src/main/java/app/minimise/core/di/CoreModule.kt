package app.minimise.core.di

import android.content.Context
import androidx.room.Room
import app.minimise.apps.data.RoomAppRepository
import app.minimise.apps.domain.AppRepository
import app.minimise.core.db.AppDao
import app.minimise.core.db.AppDatabase
import app.minimise.core.db.EventDao
import app.minimise.core.db.PendingChangeDao
import app.minimise.core.prefs.DataStoreSettingsStore
import app.minimise.core.prefs.SettingsStore
import app.minimise.core.time.SystemTimeSource
import app.minimise.core.time.TimeSource
import app.minimise.usage.data.RoomEventLog
import app.minimise.usage.domain.EventLog
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
abstract class CoreBindings {
    @Binds
    abstract fun timeSource(impl: SystemTimeSource): TimeSource

    @Binds
    abstract fun settingsStore(impl: DataStoreSettingsStore): SettingsStore

    @Binds
    abstract fun eventLog(impl: RoomEventLog): EventLog

    @Binds
    abstract fun appRepository(impl: RoomAppRepository): AppRepository
}

@Module
@InstallIn(SingletonComponent::class)
object CoreProviders {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    @Provides
    fun eventDao(db: AppDatabase): EventDao = db.eventDao()

    @Provides
    fun appDao(db: AppDatabase): AppDao = db.appDao()

    @Provides
    fun pendingChangeDao(db: AppDatabase): PendingChangeDao = db.pendingChangeDao()

    @Provides
    @Singleton
    @ApplicationScope
    fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
