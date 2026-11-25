package app.minimise.modes.domain

import app.minimise.core.time.WeeklyWindow
import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow

/**
 * The values rules read. Outside any mode they come from the main settings.
 *
 * @property allowlist when set, only these apps open (Vault apps on it still pause); null keeps every
 * app's usual kind.
 */
data class ModeValues(val budget: Duration, val waitSteps: List<Duration>, val allowlist: Set<String>?)

/**
 * Work, Study, Home, Sleep or one the user made: its own values, switched on by hand or by [schedule].
 *
 * @property presetKey which shipped mode this is ("work", "study", "home", "sleep"); null for the user's own.
 */
data class Mode(val id: Long, val name: String, val values: ModeValues, val schedule: WeeklyWindow?, val presetKey: String? = null) {
    val isSleep: Boolean get() = presetKey == SLEEP

    /** The user's own: it can be renamed and removed. The shipped ones can't. */
    val isCustom: Boolean get() = presetKey == null

    companion object {
        const val SLEEP = "sleep"

        /** Longest name a mode of the user's own can have. */
        const val MAX_NAME_LENGTH = 30
    }
}

interface ModeRepository {
    /** The shipped modes, in order (seeded on first use), then the user's own in the order they were made. */
    val modes: Flow<List<Mode>>

    suspend fun all(): List<Mode>

    suspend fun get(id: Long): Mode?

    /** Writes directly. Only SettingsGuard should call this. */
    suspend fun update(mode: Mode)

    /** Adds one of the user's own modes and returns its id. Only SettingsGuard should call this. */
    suspend fun add(mode: Mode): Long

    /** Removes one of the user's own modes; the shipped ones stay. Only SettingsGuard should call this. */
    suspend fun delete(id: Long)
}

/** The one place rules get their values from, so no rule needs to know about modes. */
interface ActiveModeProvider {
    /** The mode in effect now: the one picked by hand while it lasts, else the first whose schedule is on, else none. */
    suspend fun active(): Mode?

    suspend fun values(): ModeValues

    /** Values with no mode at all: the main budget and the standard wait steps. */
    suspend fun baseValues(): ModeValues
}
