package app.minimise.usage.domain

import java.time.Instant

/**
 * One row of the event log. Every stat in the app is computed from these; never store a counter
 * that can be derived from events.
 *
 * @property endAt only set for spans ([EventType.APP_SPAN]).
 * @property data small JSON for extras, such as the chosen reason or time box minutes.
 */
data class Event(
    val type: EventType,
    val startAt: Instant,
    val endAt: Instant? = null,
    val packageName: String? = null,
    val data: String? = null,
    val id: Long = 0,
)
