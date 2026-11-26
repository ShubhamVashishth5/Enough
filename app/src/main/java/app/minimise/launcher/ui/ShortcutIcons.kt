package app.minimise.launcher.ui

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.minimise.R
import app.minimise.launcher.domain.ShortcutIcon

/** The white glyph Enough ships for each icon (Material Icons, outlined). */
@get:DrawableRes
val ShortcutIcon.drawable: Int
    get() = when (this) {
        ShortcutIcon.PHONE -> R.drawable.ic_glyph_phone
        ShortcutIcon.MESSAGES -> R.drawable.ic_glyph_messages
        ShortcutIcon.CAMERA -> R.drawable.ic_glyph_camera
        ShortcutIcon.BROWSER -> R.drawable.ic_glyph_browser
        ShortcutIcon.MUSIC -> R.drawable.ic_glyph_music
        ShortcutIcon.MAPS -> R.drawable.ic_glyph_maps
        ShortcutIcon.MAIL -> R.drawable.ic_glyph_mail
        ShortcutIcon.CALENDAR -> R.drawable.ic_glyph_calendar
        ShortcutIcon.CLOCK -> R.drawable.ic_glyph_clock
        ShortcutIcon.PHOTOS -> R.drawable.ic_glyph_photos
        ShortcutIcon.CONTACTS -> R.drawable.ic_glyph_contacts
        ShortcutIcon.CALCULATOR -> R.drawable.ic_glyph_calculator
        ShortcutIcon.FILES -> R.drawable.ic_glyph_files
        ShortcutIcon.SETTINGS -> R.drawable.ic_glyph_settings
        ShortcutIcon.WEATHER -> R.drawable.ic_glyph_weather
        ShortcutIcon.FITNESS -> R.drawable.ic_glyph_fitness
        ShortcutIcon.CHAT -> R.drawable.ic_glyph_chat
        ShortcutIcon.NOTES -> R.drawable.ic_glyph_notes
        ShortcutIcon.TASKS -> R.drawable.ic_glyph_tasks
        ShortcutIcon.PODCASTS -> R.drawable.ic_glyph_podcasts
        ShortcutIcon.RADIO -> R.drawable.ic_glyph_radio
        ShortcutIcon.VIDEO -> R.drawable.ic_glyph_video
        ShortcutIcon.BOOKS -> R.drawable.ic_glyph_books
        ShortcutIcon.NEWS -> R.drawable.ic_glyph_news
        ShortcutIcon.RECORDER -> R.drawable.ic_glyph_recorder
        ShortcutIcon.TRANSLATE -> R.drawable.ic_glyph_translate
        ShortcutIcon.WALLET -> R.drawable.ic_glyph_wallet
        ShortcutIcon.BANK -> R.drawable.ic_glyph_bank
        ShortcutIcon.SHOPPING -> R.drawable.ic_glyph_shopping
        ShortcutIcon.FOOD -> R.drawable.ic_glyph_food
        ShortcutIcon.RIDES -> R.drawable.ic_glyph_rides
        ShortcutIcon.GAMES -> R.drawable.ic_glyph_games
        ShortcutIcon.CLOUD -> R.drawable.ic_glyph_cloud
        ShortcutIcon.KEY -> R.drawable.ic_glyph_key
        ShortcutIcon.HOME -> R.drawable.ic_glyph_home
        ShortcutIcon.STAR -> R.drawable.ic_glyph_star
    }

/** What the icon stands for, as the user reads it when choosing one. */
@get:StringRes
val ShortcutIcon.label: Int
    get() = when (this) {
        ShortcutIcon.PHONE -> R.string.shortcut_phone
        ShortcutIcon.MESSAGES -> R.string.shortcut_messages
        ShortcutIcon.CAMERA -> R.string.shortcut_camera
        ShortcutIcon.BROWSER -> R.string.shortcut_browser
        ShortcutIcon.MUSIC -> R.string.shortcut_music
        ShortcutIcon.MAPS -> R.string.shortcut_maps
        ShortcutIcon.MAIL -> R.string.shortcut_mail
        ShortcutIcon.CALENDAR -> R.string.shortcut_calendar
        ShortcutIcon.CLOCK -> R.string.shortcut_clock
        ShortcutIcon.PHOTOS -> R.string.shortcut_photos
        ShortcutIcon.CONTACTS -> R.string.shortcut_contacts
        ShortcutIcon.CALCULATOR -> R.string.shortcut_calculator
        ShortcutIcon.FILES -> R.string.shortcut_files
        ShortcutIcon.SETTINGS -> R.string.shortcut_settings
        ShortcutIcon.WEATHER -> R.string.shortcut_weather
        ShortcutIcon.FITNESS -> R.string.shortcut_fitness
        ShortcutIcon.CHAT -> R.string.shortcut_chat
        ShortcutIcon.NOTES -> R.string.shortcut_notes
        ShortcutIcon.TASKS -> R.string.shortcut_tasks
        ShortcutIcon.PODCASTS -> R.string.shortcut_podcasts
        ShortcutIcon.RADIO -> R.string.shortcut_radio
        ShortcutIcon.VIDEO -> R.string.shortcut_video
        ShortcutIcon.BOOKS -> R.string.shortcut_books
        ShortcutIcon.NEWS -> R.string.shortcut_news
        ShortcutIcon.RECORDER -> R.string.shortcut_recorder
        ShortcutIcon.TRANSLATE -> R.string.shortcut_translate
        ShortcutIcon.WALLET -> R.string.shortcut_wallet
        ShortcutIcon.BANK -> R.string.shortcut_bank
        ShortcutIcon.SHOPPING -> R.string.shortcut_shopping
        ShortcutIcon.FOOD -> R.string.shortcut_food
        ShortcutIcon.RIDES -> R.string.shortcut_rides
        ShortcutIcon.GAMES -> R.string.shortcut_games
        ShortcutIcon.CLOUD -> R.string.shortcut_cloud
        ShortcutIcon.KEY -> R.string.shortcut_key
        ShortcutIcon.HOME -> R.string.shortcut_home
        ShortcutIcon.STAR -> R.string.shortcut_star
    }
