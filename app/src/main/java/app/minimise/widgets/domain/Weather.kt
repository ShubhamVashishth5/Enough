package app.minimise.widgets.domain

import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The sky, from the WMO weather codes Open-Meteo reports. */
enum class Sky {
    CLEAR,
    MAINLY_CLEAR,
    PARTLY_CLOUDY,
    OVERCAST,
    FOG,
    DRIZZLE,
    FREEZING_DRIZZLE,
    RAIN,
    FREEZING_RAIN,
    SNOW,
    SNOW_GRAINS,
    RAIN_SHOWERS,
    SNOW_SHOWERS,
    THUNDERSTORM,
    THUNDERSTORM_HAIL,
    UNKNOWN,
    ;

    companion object {
        fun fromWmo(code: Int): Sky = when (code) {
            0 -> CLEAR
            1 -> MAINLY_CLEAR
            2 -> PARTLY_CLOUDY
            3 -> OVERCAST
            45, 48 -> FOG
            51, 53, 55 -> DRIZZLE
            56, 57 -> FREEZING_DRIZZLE
            61, 63, 65 -> RAIN
            66, 67 -> FREEZING_RAIN
            71, 73, 75 -> SNOW
            77 -> SNOW_GRAINS
            80, 81, 82 -> RAIN_SHOWERS
            85, 86 -> SNOW_SHOWERS
            95 -> THUNDERSTORM
            96, 99 -> THUNDERSTORM_HAIL
            else -> UNKNOWN
        }
    }
}

enum class TemperatureUnit { CELSIUS, FAHRENHEIT }

/**
 * Kept in Celsius, as Open-Meteo sends it by default, and turned into the unit shown only for display,
 * so switching units needs no new request.
 */
data class WeatherNow(val celsius: Double, val sky: Sky) {
    /** Rounded to a whole degree. */
    fun temperatureIn(unit: TemperatureUnit): Int = when (unit) {
        TemperatureUnit.CELSIUS -> celsius.roundToInt()
        TemperatureUnit.FAHRENHEIT -> (celsius * 9 / 5 + 32).roundToInt()
    }
}

/** The US AQI bands, the scale most weather apps show. */
enum class AqiLevel {
    GOOD,
    MODERATE,
    UNHEALTHY_FOR_SENSITIVE,
    UNHEALTHY,
    VERY_UNHEALTHY,
    HAZARDOUS,
    ;

    companion object {
        fun ofUsAqi(aqi: Int): AqiLevel = when {
            aqi <= 50 -> GOOD
            aqi <= 100 -> MODERATE
            aqi <= 150 -> UNHEALTHY_FOR_SENSITIVE
            aqi <= 200 -> UNHEALTHY
            aqi <= 300 -> VERY_UNHEALTHY
            else -> HAZARDOUS
        }
    }
}

/**
 * Open-Meteo (open-meteo.com): free, open source, no account or key. Only the rounded location goes
 * in a request; nothing about the user or the phone does.
 */
object OpenMeteo {
    /** Temperatures come in Celsius, Open-Meteo's default. */
    fun forecastUrl(latitude: Double, longitude: Double): String =
        "https://api.open-meteo.com/v1/forecast?latitude=${coordinate(latitude)}&longitude=${coordinate(longitude)}" +
            "&current=temperature_2m,weather_code"

    fun airQualityUrl(latitude: Double, longitude: Double): String =
        "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=${coordinate(latitude)}&longitude=${coordinate(longitude)}" +
            "&current=us_aqi"

    /** Two decimal places, about a kilometre: close enough for the weather, and no closer. */
    fun coordinate(value: Double): String = String.format(Locale.ROOT, "%.2f", value)

    /** Null when the answer isn't the shape asked for. */
    fun parseWeather(json: String): WeatherNow? = runCatching {
        val current = current(json) ?: return null
        val temperature = current["temperature_2m"]?.jsonPrimitive?.doubleOrNull ?: return null
        val code = current["weather_code"]?.jsonPrimitive?.doubleOrNull ?: return null
        WeatherNow(celsius = temperature, sky = Sky.fromWmo(code.toInt()))
    }.getOrNull()

    /** Null when the answer isn't the shape asked for, or there's no reading for the place. */
    fun parseUsAqi(json: String): Int? = runCatching {
        current(json)?.get("us_aqi")?.jsonPrimitive?.doubleOrNull?.roundToInt()
    }.getOrNull()

    private fun current(json: String): JsonObject? = Json.parseToJsonElement(json).jsonObject["current"]?.jsonObject
}
