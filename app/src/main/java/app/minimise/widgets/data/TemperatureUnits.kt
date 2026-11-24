package app.minimise.widgets.data

import androidx.core.text.util.LocalePreferences
import app.minimise.core.prefs.TemperatureChoice
import app.minimise.widgets.domain.TemperatureUnit

object TemperatureUnits {
    /** The unit to show for the user's choice. Automatic reads the phone's regional preference each time. */
    fun resolve(choice: TemperatureChoice): TemperatureUnit = when (choice) {
        TemperatureChoice.CELSIUS -> TemperatureUnit.CELSIUS
        TemperatureChoice.FAHRENHEIT -> TemperatureUnit.FAHRENHEIT
        TemperatureChoice.AUTOMATIC ->
            if (LocalePreferences.getTemperatureUnit() == LocalePreferences.TemperatureUnit.FAHRENHEIT) TemperatureUnit.FAHRENHEIT else TemperatureUnit.CELSIUS
    }
}
