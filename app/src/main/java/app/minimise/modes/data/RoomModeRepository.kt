package app.minimise.modes.data

import android.content.Context
import app.minimise.R
import app.minimise.core.Defaults
import app.minimise.core.db.ModeDao
import app.minimise.core.db.ModeEntity
import app.minimise.core.time.WeeklyWindow
import app.minimise.core.time.minuteOfDay
import app.minimise.modes.domain.Mode
import app.minimise.modes.domain.ModeRepository
import app.minimise.modes.domain.ModeValues
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class RoomModeRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ModeDao,
) : ModeRepository {
    private val seedLock = Mutex()

    override val modes: Flow<List<Mode>> = dao.observeAll()
        .onStart { seedIfEmpty() }
        .map { rows -> rows.map { it.toDomain() } }

    override suspend fun all(): List<Mode> {
        seedIfEmpty()
        return dao.all().map { it.toDomain() }
    }

    override suspend fun get(id: Long): Mode? = dao.get(id)?.toDomain()

    override suspend fun update(mode: Mode) {
        val row = dao.get(mode.id) ?: return
        val schedule = mode.schedule
        dao.update(
            row.copy(
                budgetMinutes = mode.values.budget.inWholeMinutes.toInt(),
                waitStepsSeconds = mode.values.waitSteps.joinToString(",") { it.inWholeSeconds.toString() },
                allowlist = mode.values.allowlist?.joinToString(","),
                scheduleDays = schedule?.daysMask() ?: 0,
                scheduleStartMinute = schedule?.start?.minuteOfDay ?: 0,
                scheduleEndMinute = schedule?.end?.minuteOfDay ?: 0,
            ),
        )
    }

    /** The four modes ship with the app and can't be deleted, so an empty table means first use. */
    private suspend fun seedIfEmpty() = seedLock.withLock {
        if (dao.all().isNotEmpty()) return@withLock
        dao.insertAll(
            Defaults.MODES.map { preset ->
                ModeEntity(
                    presetKey = preset.key,
                    name = context.getString(nameOf(preset.key)),
                    budgetMinutes = preset.budget.inWholeMinutes.toInt(),
                    waitStepsSeconds = preset.waitSteps.joinToString(",") { it.inWholeSeconds.toString() },
                    allowlist = null,
                    scheduleDays = 0,
                    scheduleStartMinute = 0,
                    scheduleEndMinute = 0,
                )
            },
        )
    }

    private fun nameOf(key: String): Int = when (key) {
        "work" -> R.string.mode_work
        "study" -> R.string.mode_study
        "sleep" -> R.string.mode_sleep
        else -> R.string.mode_home
    }

    private fun ModeEntity.toDomain() = Mode(
        id = id,
        name = name,
        values = ModeValues(
            budget = budgetMinutes.minutes,
            waitSteps = waitStepsSeconds.split(',').mapNotNull { it.toLongOrNull()?.seconds }.ifEmpty { Defaults.WAIT_STEPS },
            allowlist = allowlist?.split(',')?.filter { it.isNotBlank() }?.toSet(),
        ),
        schedule = if (scheduleDays == 0) null else WeeklyWindow.fromStored(scheduleDays, scheduleStartMinute, scheduleEndMinute),
    )
}
