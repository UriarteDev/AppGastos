package com.smartsaldo.app.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {

    private const val SELECTED_LANGUAGE = "idioma"

    /**
     * Aplica el idioma guardado en SharedPreferences
     */
    fun onAttach(context: Context): Context {
        val lang = getPersistedLanguage(context)
        return setLocale(context, lang)
    }

    /**
     * Guarda y aplica un nuevo idioma
     */
    fun setLocale(context: Context, language: String): Context {
        persist(context, language)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            updateResources(context, language)
        } else {
            updateResourcesLegacy(context, language)
        }
    }

    /**
     * Obtiene el idioma guardado (español por defecto)
     */
    fun getPersistedLanguage(context: Context): String {
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

        // Buscar primero el idioma definitivo, luego el temporal, por defecto español
        return preferences.getString("idioma", null)
            ?: preferences.getString(SELECTED_LANGUAGE, null)
            ?: preferences.getString("temp_language", "es")
            ?: "es"
    }

    /**
     * Guarda el idioma en SharedPreferences
     */
    private fun persist(context: Context, language: String) {
        val preferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        preferences.edit().putString(SELECTED_LANGUAGE, language).apply()
    }

    /**
     * Actualiza recursos para Android N+
     */
    @Suppress("DEPRECATION")
    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val configuration = context.resources.configuration
        configuration.setLocale(locale)
        configuration.setLayoutDirection(locale)

        return context.createConfigurationContext(configuration)
    }

    /**
     * Actualiza recursos para Android < N (Legacy)
     */
    @Suppress("DEPRECATION")
    private fun updateResourcesLegacy(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = resources.configuration
        configuration.locale = locale

        resources.updateConfiguration(configuration, resources.displayMetrics)

        return context
    }
}