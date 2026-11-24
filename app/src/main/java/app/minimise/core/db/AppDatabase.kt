package app.minimise.core.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [EventEntity::class, AppEntity::class, PendingChangeEntity::class, GrantEntity::class, TaskEntity::class, SwapEntity::class],
    version = 7,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    abstract fun appDao(): AppDao

    abstract fun pendingChangeDao(): PendingChangeDao

    abstract fun grantDao(): GrantDao

    abstract fun taskDao(): TaskDao

    abstract fun swapDao(): SwapDao

    companion object {
        const val NAME = "minimise.db"
    }
}
