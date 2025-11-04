package com.smartsaldo.app.ui.welcome

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.smartsaldo.app.R
import com.smartsaldo.app.databinding.ActivityWelcomeSetupBinding
import com.smartsaldo.app.ui.auth.LoginActivity
import com.smartsaldo.app.utils.CurrencyHelper
import com.smartsaldo.app.utils.LocaleHelper

class WelcomeSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeSetupBinding
    private var idiomaSeleccionado = "en"
    private var monedaSeleccionada = CurrencyHelper.Currency.USD
    private var isInitializing = true // ✅ Flag para evitar recreate durante inicialización

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val setupCompleted = prefs.getBoolean("setup_completed", false)

        val context = if (!setupCompleted) {
            // Obtener el idioma ya guardado si existe (por si recrea)
            val savedLanguage = prefs.getString("temp_language", "en") ?: "en"
            LocaleHelper.setLocale(newBase, savedLanguage)
        } else {
            LocaleHelper.onAttach(newBase)
        }

        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Verificar si ya configuró antes
        if (isSetupCompleted()) {
            irALogin()
            return
        }

        // ✅ Cargar idioma temporal guardado (si existe)
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        idiomaSeleccionado = prefs.getString("temp_language", "en") ?: "en"

        setupUI()

        // ✅ Permitir interacción después de 500ms
        binding.root.postDelayed({
            isInitializing = false
        }, 500)
    }

    private fun setupUI() {
        // Configurar spinner de idiomas
        setupIdiomaSpinner()

        // Configurar spinner de monedas
        setupMonedaSpinner()

        // Botón continuar
        binding.btnContinuar.setOnClickListener {
            guardarConfiguracion()
            irALogin()
        }
    }

    private fun setupIdiomaSpinner() {
        val idiomas = listOf(
            Idioma("en", "English"),
            Idioma("es", "Español")
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            idiomas.map { it.nombre }
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerIdioma.adapter = adapter

        // ✅ Seleccionar idioma actual
        val posicionActual = idiomas.indexOfFirst { it.codigo == idiomaSeleccionado }
        if (posicionActual >= 0) {
            binding.spinnerIdioma.setSelection(posicionActual)
        }

        binding.spinnerIdioma.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                // ✅ Ignorar si está inicializando
                if (isInitializing) return

                val nuevoIdioma = idiomas[position].codigo

                // Solo cambiar si es diferente
                if (nuevoIdioma != idiomaSeleccionado) {
                    idiomaSeleccionado = nuevoIdioma

                    // Guardar temporalmente
                    val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
                    prefs.edit().putString("temp_language", idiomaSeleccionado).apply()

                    // Aplicar idioma
                    LocaleHelper.setLocale(this@WelcomeSetupActivity, idiomaSeleccionado)

                    // Marcar que va a recrear
                    isInitializing = true

                    // Recrear activity
                    recreate()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupMonedaSpinner() {
        val monedas = CurrencyHelper.Currency.values().toList()

        val nombresMonedas = monedas.map { currency ->
            when (currency) {
                CurrencyHelper.Currency.PEN -> "Peruvian Sol (S/)"
                CurrencyHelper.Currency.USD -> "US Dollar ($)"
                CurrencyHelper.Currency.EUR -> "Euro (€)"
                CurrencyHelper.Currency.GBP -> "British Pound (£)"
                CurrencyHelper.Currency.JPY -> "Japanese Yen (¥)"
                CurrencyHelper.Currency.CAD -> "Canadian Dollar (C$)"
                CurrencyHelper.Currency.AUD -> "Australian Dollar (A$)"
                CurrencyHelper.Currency.CHF -> "Swiss Franc (Fr)"
                CurrencyHelper.Currency.CNY -> "Chinese Yuan (¥)"
                CurrencyHelper.Currency.MXN -> "Mexican Peso (Mex$)"
                CurrencyHelper.Currency.BRL -> "Brazilian Real (R$)"
                CurrencyHelper.Currency.ARS -> "Argentine Peso (AR$)"
                CurrencyHelper.Currency.CLP -> "Chilean Peso (CL$)"
                CurrencyHelper.Currency.COP -> "Colombian Peso (COL$)"
            }
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            nombresMonedas
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spinnerMoneda.adapter = adapter

        // ✅ USD por defecto
        val posicionUSD = monedas.indexOf(CurrencyHelper.Currency.USD)
        if (posicionUSD >= 0) {
            binding.spinnerMoneda.setSelection(posicionUSD)
        }

        binding.spinnerMoneda.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                monedaSeleccionada = monedas[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun guardarConfiguracion() {
        // Guardar idioma definitivo
        LocaleHelper.setLocale(this, idiomaSeleccionado)

        // Guardar moneda
        CurrencyHelper.setSelectedCurrency(this, monedaSeleccionada)

        // Marcar configuración como completada
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean("setup_completed", true)
            .remove("temp_language") // ✅ Limpiar idioma temporal
            .apply()
    }

    private fun isSetupCompleted(): Boolean {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        return prefs.getBoolean("setup_completed", false)
    }

    private fun irALogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    data class Idioma(val codigo: String, val nombre: String)
}