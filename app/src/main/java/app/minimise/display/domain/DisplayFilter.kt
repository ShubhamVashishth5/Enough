package app.minimise.display.domain

/** Turns the whole screen grayscale and back. */
interface DisplayFilter {
    /** False when the app can't change the screen's colours (no WRITE_SECURE_SETTINGS grant). */
    fun isAvailable(): Boolean

    /** Off restores whatever colour correction the user had before. */
    suspend fun setGrayscale(on: Boolean)
}

enum class GrayscaleMode {
    OFF,

    /** Grayscale while a Vault app is in front, colour everywhere else. */
    VAULT_APPS,

    ALWAYS,
}
