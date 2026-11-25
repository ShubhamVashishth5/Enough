package app.minimise.core.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.minimise.apps.domain.AppKind

/** What the user chose for a package. Rows outlive uninstalls, so a reinstalled app keeps its kind. */
@Entity(tableName = "apps")
data class AppEntity(
    @PrimaryKey val packageName: String,
    val kind: AppKind,
    val homeSlot: Int?,
    /** Most proceeded opens a day, or null for no limit. */
    val openLimit: Int? = null,
    /** A Breathe app's own pause in seconds, or null for the usual one. */
    val breatheWaitSeconds: Int? = null,
)
