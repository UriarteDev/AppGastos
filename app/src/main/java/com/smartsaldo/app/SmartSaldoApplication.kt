package com.smartsaldo.app

import android.app.Application
import android.content.Context
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartSaldoApplication : Application(){

    override fun onCreate() {
        super.onCreate()

        // Cargar idioma guardado
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val idioma = prefs.getString("idioma", "es") ?: "es"

        val locale = java.util.Locale(idioma)
        java.util.Locale.setDefault(locale)

        val config = resources.configuration
        config.setLocale(locale)
        createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}