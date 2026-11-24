package app.minimise.core.ui

import kotlin.time.Duration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** Emits at once, then every [period]. For screens that show a countdown. */
fun ticker(period: Duration): Flow<Unit> = flow {
    while (true) {
        emit(Unit)
        delay(period)
    }
}
