package app.minimise.monitor.domain

/** One activity coming to the front, as usage access reports it. [atMillis] is wall-clock time. */
data class ResumedActivity(val packageName: String, val className: String?, val atMillis: Long)

/**
 * Keeps each usage access event to one pass while reads overlap. The system can write an event a
 * moment after its timestamp, so every read starts [overlapMillis] before the last one ended; events
 * already handled, and ones older than the newest handled, are dropped (the app in front has moved on).
 */
class ResumedActivityCursor(startMillis: Long, private val overlapMillis: Long) {
    private var readUpTo = startMillis
    private var newest = startMillis
    private var seen: Set<ResumedActivity> = emptySet()

    /** Where the next read starts. */
    fun readFrom(): Long = readUpTo - overlapMillis

    /** Takes what a read from [readFrom] to [readTo] found, and returns the events not handled yet, oldest first. */
    fun accept(read: List<ResumedActivity>, readTo: Long): List<ResumedActivity> {
        val fresh = read.filter { it.atMillis >= newest && it !in seen }.sortedBy { it.atMillis }
        fresh.lastOrNull()?.let { newest = it.atMillis }
        readUpTo = maxOf(readUpTo, readTo)
        seen = (seen + fresh).filterTo(HashSet()) { it.atMillis >= readUpTo - overlapMillis }
        return fresh
    }
}
