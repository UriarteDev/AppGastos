package com.smartsaldo.app.utils

object Constants {

    // Validaciones
    const val MIN_PASSWORD_LENGTH = 6
    const val MIN_NAME_LENGTH = 2
    const val MAX_NAME_LENGTH = 50
    const val MIN_DESCRIPTION_LENGTH = 3
    const val MAX_DESCRIPTION_LENGTH = 100
    const val MAX_NOTES_LENGTH = 500
    const val MAX_AHORRO_NAME_LENGTH = 50
    const val MAX_APORTE_NOTE_LENGTH = 200

    // Montos
    const val MAX_AMOUNT = 99999999.99
    const val MAX_AHORRO_AMOUNT = 999999999.99

    // Database
    const val DATABASE_NAME = "smartsaldo_db"
    const val DATABASE_VERSION = 6

    // SharedPreferences
    const val PREFS_NAME = "smartsaldo_prefs"
    const val KEY_USER_ID = "user_id"
    const val KEY_IS_LOGGED_IN = "is_logged_in"
    const val KEY_LANGUAGE = "idioma"

    // Date Formats
    const val DATE_FORMAT_DISPLAY = "dd/MM/yyyy"
    const val TIME_FORMAT_DISPLAY = "HH:mm"
    const val DATE_TIME_FORMAT = "dd/MM/yyyy HH:mm"

    // AdMob
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

    // Animation
    const val ANIMATION_DURATION_SHORT = 200L
    const val ANIMATION_DURATION_MEDIUM = 400L
    const val ANIMATION_DURATION_LONG = 600L
    const val ITEM_ANIMATION_DELAY = 50L

    // Pull to Refresh
    const val REFRESH_DELAY = 500L
}