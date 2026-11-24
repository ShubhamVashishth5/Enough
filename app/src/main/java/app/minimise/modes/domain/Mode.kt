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
 * Work, Study, Home or Sleep: its own values, switched on by hand or by [schedule].
 *
 * @property presetKey which shipped mode this is ("work", "study", "home", "sleep").
 */
data class Mode(val id: Long, val name: String, val values: ModeValues, val schedule: WeeklyWindow?, val presetKey: String? = null) {
    val isSleep: Boolean get() = presetKey == SLEEP

    companion object {
        const val SLEEP = "sleep"
    }
}

interface ModeRepository {
    /** The shipped modes, in order. Seeded on first use. */
    val modes: Flow<List<Mode>>

    suspend fun all(): List<Mode>

    suspend fun get(id: Long): Mode?

    /** Writes directly. Only SettingsGuard should call this. */
    suspend fun update(mode: Mode)
}

/** The one place rules get their values from, so no rule needs to know about modes. */
interface ActiveModeProvider {
    /** The mode in effect now: the one picked by hand, else the first whose schedule is on, else none. */
    suspend fun active(): Mode?

    suspend fun values(): ModeValues

    /** What [values] would be if [manualModeId] were the hand-picked mode (null: follow schedules). */
    suspend fun valuesWith(manualModeId: Long?): ModeValues

    /** Values with no mode at all: the main budget and the standard wait steps. */
    suspend fun baseValues(): ModeValues
}
