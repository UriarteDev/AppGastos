package com.smartsaldo.app

import android.app.Application
import android.content.Context
import com.smartsaldo.app.utils.LocaleHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SmartSaldoApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Aplicar idioma guardado al iniciar la app
        LocaleHelper.onAttach(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(base))
    }
}