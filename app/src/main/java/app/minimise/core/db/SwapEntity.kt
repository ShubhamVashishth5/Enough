package app.minimise.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One entry of the Instead list. @property packageName an optional Tool app that "Do this instead" opens. */
@Entity(tableName = "swaps")
data class SwapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val text: String,
    val packageName: String?,
)
