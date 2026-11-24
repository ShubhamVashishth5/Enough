package app.minimise.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/** A paired physical key. Only a hash of the sticker ID or QR secret is kept. */
@Entity(tableName = "keys")
data class KeyEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,
    val label: String,
    val secretHash: String,
    val createdAt: Instant,
)
