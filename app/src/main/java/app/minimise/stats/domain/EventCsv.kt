package app.minimise.stats.domain

import app.minimise.usage.domain.Event

/** The whole event log as CSV, one row per event, times in ISO-8601 UTC. */
object EventCsv {
    private val HEADER = listOf("id", "type", "package", "start", "end", "data")

    fun write(events: List<Event>, out: Appendable) {
        out.append(HEADER.joinToString(",")).append("\r\n")
        events.forEach { event ->
            val fields = listOf(
                event.id.toString(),
                event.type.name,
                event.packageName.orEmpty(),
                event.startAt.toString(),
                event.endAt?.toString().orEmpty(),
                event.data.orEmpty(),
            )
            out.append(fields.joinToString(",", transform = ::escape)).append("\r\n")
        }
    }

    /** RFC 4180: quote fields with commas, quotes or line breaks, doubling any quotes. */
    fun escape(field: String): String =
        if (field.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + field.replace("\"", "\"\"") + "\"" else field
}
