package app.minimise.core.time

import java.time.Instant
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

operator fun Instant.plus(duration: Duration): Instant = plus(duration.toJavaDuration())

operator fun Instant.minus(duration: Duration): Instant = minus(duration.toJavaDuration())

/** Time from [start] to [end]; negative if [end] is earlier. */
fun durationBetween(start: Instant, end: Instant): Duration =
    java.time.Duration.between(start, end).toKotlinDuration()
