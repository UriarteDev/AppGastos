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

    // Error Messages
    const val ERROR_GENERIC = "Ha ocurrido un error"
    const val ERROR_NO_INTERNET = "No hay conexión a internet"
    const val ERROR_LOAD_DATA = "Error al cargar datos"
    const val ERROR_SAVE_DATA = "Error al guardar datos"
}

object ErrorMessages {
    const val EMPTY_EMAIL = "Ingrese su email"
    const val INVALID_EMAIL = "Email inválido"
    const val EMPTY_PASSWORD = "Ingrese su contraseña"
    const val SHORT_PASSWORD = "La contraseña debe tener al menos 6 caracteres"
    const val PASSWORD_NO_LETTER = "La contraseña debe contener al menos una letra"
    const val PASSWORD_NO_DIGIT = "La contraseña debe contener al menos un número"
    const val PASSWORDS_DONT_MATCH = "Las contraseñas no coinciden"
    const val EMPTY_NAME = "Ingrese su nombre"
    const val SHORT_NAME = "El nombre debe tener al menos 2 caracteres"
    const val LONG_NAME = "El nombre es demasiado largo"
    const val INVALID_NAME = "El nombre contiene caracteres inválidos"
    const val EMPTY_AMOUNT = "Ingrese un monto"
    const val INVALID_AMOUNT = "Monto inválido"
    const val AMOUNT_ZERO = "El monto debe ser mayor a 0"
    const val AMOUNT_TOO_LARGE = "El monto es demasiado grande"
    const val EMPTY_DESCRIPTION = "Ingrese una descripción"
    const val SHORT_DESCRIPTION = "La descripción es muy corta (mínimo 3 caracteres)"
    const val LONG_DESCRIPTION = "La descripción es muy larga (máximo 100 caracteres)"
    const val EMPTY_CATEGORY = "Seleccione una categoría"
    const val NOTES_TOO_LONG = "Las notas son muy largas (máximo 500 caracteres)"
}

object SuccessMessages {
    const val TRANSACTION_CREATED = "Transacción guardada ✅"
    const val TRANSACTION_UPDATED = "Transacción actualizada ✅"
    const val TRANSACTION_DELETED = "Transacción eliminada ✅"
    const val CATEGORY_CREATED = "Categoría creada ✅"
    const val CATEGORY_UPDATED = "Categoría actualizada ✅"
    const val CATEGORY_DELETED = "Categoría eliminada ✅"
    const val AHORRO_CREATED = "Meta de ahorro creada ✅"
    const val AHORRO_DELETED = "Meta de ahorro eliminada"
    const val APORTE_ADDED = "Aporte registrado ✅"
    const val DATA_REFRESHED = "Actualizado ✅"
    const val PASSWORD_RESET_SENT = "Email de recuperación enviado"
}