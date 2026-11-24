package app.minimise.widgets.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherTest {
    @Test
    fun `reads Open-Meteo's current weather`() {
        val json = """{"latitude":12.98,"current_units":{"temperature_2m":"°C"},"current":{"time":"2026-11-25T10:00","interval":900,"temperature_2m":23.6,"weather_code":2}}"""
        assertEquals(WeatherNow(24, Sky.PARTLY_CLOUDY, TemperatureUnit.CELSIUS), OpenMeteo.parseWeather(json, TemperatureUnit.CELSIUS))
    }

    @Test
    fun `reads the US AQI, and none when there's no reading`() {
        assertEquals(42, OpenMeteo.parseUsAqi("""{"current":{"time":"2026-11-25T10:00","us_aqi":42}}"""))
        assertEquals(157, OpenMeteo.parseUsAqi("""{"current":{"us_aqi":156.6}}"""))
        assertNull(OpenMeteo.parseUsAqi("""{"current":{"us_aqi":null}}"""))
    }

    @Test
    fun `an unexpected answer is no reading, not a crash`() {
        assertNull(OpenMeteo.parseWeather("""{"error":true,"reason":"Latitude must be in range"}""", TemperatureUnit.CELSIUS))
        assertNull(OpenMeteo.parseWeather("<html>busy</html>", TemperatureUnit.CELSIUS))
        assertNull(OpenMeteo.parseUsAqi(""))
    }

    @Test
    fun `requests carry only a location rounded to two places`() {
        val url = OpenMeteo.forecastUrl(12.971599, -77.594566, TemperatureUnit.FAHRENHEIT)
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?"))
        assertTrue("latitude=12.97&longitude=-77.59" in url)
        assertTrue("temperature_unit=fahrenheit" in url)
        assertFalse("12.9715" in url)
        assertTrue("latitude=12.97&longitude=-77.59&current=us_aqi" in OpenMeteo.airQualityUrl(12.971599, -77.594566))
    }

    @Test
    fun `weather codes map to the sky`() {
        assertEquals(Sky.CLEAR, Sky.fromWmo(0))
        assertEquals(Sky.FOG, Sky.fromWmo(48))
        assertEquals(Sky.RAIN, Sky.fromWmo(63))
        assertEquals(Sky.RAIN_SHOWERS, Sky.fromWmo(81))
        assertEquals(Sky.THUNDERSTORM_HAIL, Sky.fromWmo(99))
        assertEquals(Sky.UNKNOWN, Sky.fromWmo(42))
    }

    @Test
    fun `AQI bands follow the US scale`() {
        assertEquals(AqiLevel.GOOD, AqiLevel.ofUsAqi(50))
        assertEquals(AqiLevel.MODERATE, AqiLevel.ofUsAqi(51))
        assertEquals(AqiLevel.UNHEALTHY_FOR_SENSITIVE, AqiLevel.ofUsAqi(150))
        assertEquals(AqiLevel.UNHEALTHY, AqiLevel.ofUsAqi(151))
        assertEquals(AqiLevel.VERY_UNHEALTHY, AqiLevel.ofUsAqi(300))
        assertEquals(AqiLevel.HAZARDOUS, AqiLevel.ofUsAqi(301))
    }
}
