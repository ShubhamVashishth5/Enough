package app.minimise.widgets.data

import app.minimise.core.Defaults
import java.net.HttpURLConnection
import java.net.URI
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * The app's only use of the internet: plain HTTPS GETs to Open-Meteo for home's weather and air quality
 * lines. No library, no cookies, and a User-Agent that names the app and nothing else.
 */
class OpenMeteoClient @Inject constructor() {
    /** The body of a 200 answer, or null on any failure. */
    suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URI(url).toURL().openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = Defaults.WEATHER_TIMEOUT.inWholeMilliseconds.toInt()
                connection.readTimeout = Defaults.WEATHER_TIMEOUT.inWholeMilliseconds.toInt()
                connection.useCaches = false
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.setRequestProperty("Accept", "application/json")
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    Timber.w("Open-Meteo answered %d", connection.responseCode)
                    null
                } else {
                    connection.inputStream.bufferedReader().use { it.readText() }
                }
            } finally {
                connection.disconnect()
            }
        }.onFailure { Timber.w(it, "Open-Meteo request failed") }.getOrNull()
    }

    private companion object {
        const val USER_AGENT = "Enough"
    }
}
