package app.minimise.core.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [EventEntity::class, AppEntity::class, PendingChangeEntity::class, GrantEntity::class, TaskEntity::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    abstract fun appDao(): AppDao

    abstract fun pendingChangeDao(): PendingChangeDao

    abstract fun grantDao(): GrantDao

    abstract fun taskDao(): TaskDao

    companion object {
        const val NAME = "minimise.db"
    }
}
