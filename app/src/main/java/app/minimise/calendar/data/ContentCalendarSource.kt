package app.minimise.calendar.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Instances
import app.minimise.calendar.domain.CalendarEvent
import app.minimise.calendar.domain.CalendarSource
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

class ContentCalendarSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : CalendarSource {
    override fun isAvailable(): Boolean =
        context.checkSelfPermission(Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    override suspend fun eventsBetween(from: Instant, until: Instant): List<CalendarEvent> {
        if (!isAvailable()) return emptyList()
        return withContext(Dispatchers.IO) {
            runCatching {
                val uri = Instances.CONTENT_URI.buildUpon()
                    .also { ContentUris.appendId(it, from.toEpochMilli()) }
                    .also { ContentUris.appendId(it, until.toEpochMilli()) }
                    .build()
                context.contentResolver.query(
                    uri,
                    arrayOf(Instances.TITLE, Instances.BEGIN, Instances.END),
                    "${Instances.ALL_DAY} = 0 AND ${Instances.SELF_ATTENDEE_STATUS} != ${Attendees.ATTENDEE_STATUS_DECLINED}",
                    null,
                    "${Instances.BEGIN} ASC",
                )?.use { cursor ->
                    buildList {
                        while (cursor.moveToNext()) {
                            add(
                                CalendarEvent(
                                    title = cursor.getString(0).orEmpty(),
                                    begin = Instant.ofEpochMilli(cursor.getLong(1)),
                                    end = Instant.ofEpochMilli(cursor.getLong(2)),
                                ),
                            )
                        }
                    }
                }.orEmpty()
            }.onFailure { Timber.w(it, "Calendar query failed") }.getOrDefault(emptyList())
        }
    }
}
