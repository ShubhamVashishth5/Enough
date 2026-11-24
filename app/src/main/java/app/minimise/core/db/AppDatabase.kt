package app.minimise.core.db

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [EventEntity::class, AppEntity::class, PendingChangeEntity::class, GrantEntity::class, TaskEntity::class, SwapEntity::class, ScheduleEntity::class, KeyEntity::class, FocusSessionEntity::class, ModeEntity::class, BufferedNotificationEntity::class, NoteEntity::class],
    version = 13,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6),
        AutoMigration(from = 6, to = 7),
        AutoMigration(from = 7, to = 8),
        AutoMigration(from = 8, to = 9),
        AutoMigration(from = 9, to = 10),
        AutoMigration(from = 10, to = 11),
        AutoMigration(from = 11, to = 12),
        AutoMigration(from = 12, to = 13),
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

    abstract fun scheduleDao(): ScheduleDao

    abstract fun keyDao(): KeyDao

    abstract fun focusSessionDao(): FocusSessionDao

    abstract fun modeDao(): ModeDao

    abstract fun bufferedNotificationDao(): BufferedNotificationDao

    abstract fun noteDao(): NoteDao

    companion object {
        const val NAME = "minimise.db"
    }
}
