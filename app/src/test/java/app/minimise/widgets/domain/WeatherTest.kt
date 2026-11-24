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
        assertEquals(WeatherNow(23.6, Sky.PARTLY_CLOUDY), OpenMeteo.parseWeather(json))
    }

    @Test
    fun `one reading shows in either unit, without asking again`() {
        val now = WeatherNow(23.6, Sky.CLEAR)
        assertEquals(24, now.temperatureIn(TemperatureUnit.CELSIUS))
        assertEquals(74, now.temperatureIn(TemperatureUnit.FAHRENHEIT))
        assertEquals(32, WeatherNow(0.0, Sky.CLEAR).temperatureIn(TemperatureUnit.FAHRENHEIT))
        assertEquals(-40, WeatherNow(-40.0, Sky.CLEAR).temperatureIn(TemperatureUnit.FAHRENHEIT))
    }

    @Test
    fun `reads the US AQI and PM2_5, and nothing when there's no AQI`() {
        assertEquals(AirQuality(42, 18), OpenMeteo.parseAirQuality("""{"current":{"time":"2026-11-25T10:00","us_aqi":42,"pm2_5":17.6}}"""))
        assertEquals(AirQuality(157, null), OpenMeteo.parseAirQuality("""{"current":{"us_aqi":156.6}}"""))
        assertNull(OpenMeteo.parseAirQuality("""{"current":{"us_aqi":null,"pm2_5":40.0}}"""))
    }

    @Test
    fun `an unexpected answer is no reading, not a crash`() {
        assertNull(OpenMeteo.parseWeather("""{"error":true,"reason":"Latitude must be in range"}"""))
        assertNull(OpenMeteo.parseWeather("<html>busy</html>"))
        assertNull(OpenMeteo.parseAirQuality(""))
    }

    @Test
    fun `requests carry only a location rounded to two places`() {
        val url = OpenMeteo.forecastUrl(12.971599, -77.594566)
        assertTrue(url.startsWith("https://api.open-meteo.com/v1/forecast?"))
        assertTrue("latitude=12.97&longitude=-77.59" in url)
        assertFalse("temperature_unit" in url)
        assertFalse("12.9715" in url)
        assertTrue("latitude=12.97&longitude=-77.59&current=us_aqi,pm2_5" in OpenMeteo.airQualityUrl(12.971599, -77.594566))
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
