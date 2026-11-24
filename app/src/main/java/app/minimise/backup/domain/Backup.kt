package app.minimise.backup.domain

import app.minimise.apps.domain.AppKind
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Every setting in one file, for moving to a new phone. The event log isn't in it; that's the CSV
 * export. Keys and friend codes are there only as the hashes the app keeps. Field names are part of the
 * file format: add fields with defaults, never rename one, and bump [BackupCodec.VERSION] when an old
 * app couldn't read the new file correctly.
 *
 * @property apps every app or website ever given a kind or a limit, installed or not.
 * @property manualMode the preset key ("work", "sleep") of the mode picked by hand, if any.
 */
@Serializable
data class Backup(
    val format: String = BackupCodec.FORMAT,
    val version: Int = BackupCodec.VERSION,
    val exportedAtMillis: Long,
    val apps: List<App> = emptyList(),
    val budgetMinutes: Int,
    val taperFloorMinutes: Int? = null,
    val firstHourRule: Boolean = false,
    val earnIt: Boolean = false,
    val calendarFocus: Boolean = false,
    val lockedUntilMillis: Long? = null,
    val blockedFeeds: List<String> = emptyList(),
    val modes: List<Mode> = emptyList(),
    val manualMode: String? = null,
    val schedules: List<Schedule> = emptyList(),
    val keys: List<Key> = emptyList(),
    val friendCodeHashes: List<String> = emptyList(),
    val swaps: List<Swap> = emptyList(),
    val digestEnabled: Boolean = false,
    val digestExempt: List<String> = emptyList(),
    val grayscaleMode: String = "OFF",
    val focusAllowlist: List<String>? = null,
    /** Missing from files made before version 2; a restore then leaves this phone's home screen alone. */
    val home: Home? = null,
) {
    @Serializable
    data class App(val packageName: String, val kind: AppKind, val openLimit: Int? = null)

    /** One of the shipped modes, matched by [preset] on import since ids differ between phones. */
    @Serializable
    data class Mode(
        val preset: String,
        val budgetMinutes: Int,
        val waitStepsSeconds: List<Long>,
        val allowlist: List<String>? = null,
        val scheduleDays: Int = 0,
        val scheduleStartMinute: Int = 0,
        val scheduleEndMinute: Int = 0,
    )

    /** Matched by [name] on import. Days have Monday as bit 0; times are minutes after midnight. */
    @Serializable
    data class Schedule(val name: String, val days: Int, val startMinute: Int, val endMinute: Int)

    /** A paired physical key: its type (NFC or QR), label and the SHA-256 of its secret. */
    @Serializable
    data class Key(val type: String, val label: String, val secretHash: String)

    @Serializable
    data class Swap(val text: String, val packageName: String? = null)

    /** The home screen's optional parts (HomeOptions). */
    @Serializable
    data class Home(
        val lockButton: Boolean = true,
        val swipeDownShade: Boolean = true,
        val hideStatusBar: Boolean = false,
        val battery: Boolean = true,
        val nextAlarm: Boolean = true,
        val music: Boolean = true,
        val weather: Boolean = false,
        val airQuality: Boolean = false,
        /** TemperatureChoice by name. */
        val temperatureUnit: String = "AUTOMATIC",
    )
}

object BackupCodec {
    const val FORMAT = "enough-settings"

    /** 2: the Breathe app kind, which version 1 can't read, and the home screen's options. */
    const val VERSION = 2

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    sealed interface Decoded {
        data class Ok(val backup: Backup) : Decoded

        /** Made by a newer version of the app, which may mean things this one would get wrong. */
        data class TooNew(val version: Int) : Decoded

        /** Not a settings file, or damaged. */
        data object Unreadable : Decoded
    }

    fun encode(backup: Backup): String = json.encodeToString(Backup.serializer(), backup)

    fun decode(text: String): Decoded = runCatching {
        val root = json.parseToJsonElement(text).jsonObject
        if (root["format"]?.jsonPrimitive?.content != FORMAT) return Decoded.Unreadable
        val version = root["version"]?.jsonPrimitive?.intOrNull ?: return Decoded.Unreadable
        if (version > VERSION) return Decoded.TooNew(version)
        Decoded.Ok(json.decodeFromJsonElement(Backup.serializer(), root))
    }.getOrDefault(Decoded.Unreadable)
}
