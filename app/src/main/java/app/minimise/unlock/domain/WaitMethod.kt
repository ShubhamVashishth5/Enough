package app.minimise.unlock.domain

import app.minimise.core.Defaults
import javax.inject.Inject
import kotlin.time.Duration

/** Wait it out. Offered by unlocked focus sessions; the Blocked screen runs the countdown. */
class WaitMethod @Inject constructor() : UnlockMethod {
    override val id: UnlockMethodId = UnlockMethodId.WAIT

    val wait: Duration = Defaults.FOCUS_WAIT

    override suspend fun isAvailable(): Boolean = true
}
