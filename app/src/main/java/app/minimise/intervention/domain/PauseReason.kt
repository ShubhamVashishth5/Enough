package app.minimise.intervention.domain

/**
 * The answers to "What are you here for?". Stored by name in the event log.
 *
 * @property offersSwap the pause offers something from the Instead list before the app.
 */
enum class PauseReason(val offersSwap: Boolean) {
    REPLY(offersSwap = false),
    LOOK_UP(offersSwap = false),
    BORED(offersSwap = true),
    HABIT(offersSwap = true),
}
