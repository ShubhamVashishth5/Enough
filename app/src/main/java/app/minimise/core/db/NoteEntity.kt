package app.minimise.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** A captured thought, kept for later instead of becoming a reason to open a browser. */
@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val createdAt: Instant,
    val done: Boolean = false,
)
