package com.smartsaldo.app.utils

import android.content.Context
import com.smartsaldo.app.R
import java.text.NumberFormat
import java.util.*

object CurrencyHelper {

    private const val PREFS_CURRENCY = "selected_currency"

    // Lista de monedas disponibles
    enum class Currency(
        val code: String,
        val symbol: String,
        val nameResId: Int,
        val locale: Locale
    ) {
        PEN("PEN", "S/", R.string.currency_pen, Locale("es", "PE")),
        USD("USD", "$", R.string.currency_usd, Locale.US),
        EUR("EUR", "€", R.string.currency_eur, Locale.FRANCE),
        GBP("GBP", "£", R.string.currency_gbp, Locale.UK),
        JPY("JPY", "¥", R.string.currency_jpy, Locale.JAPAN),
        CAD("CAD", "C$", R.string.currency_cad, Locale.CANADA),
        AUD("AUD", "A$", R.string.currency_aud, Locale("en", "AU")),
        CHF("CHF", "Fr", R.string.currency_chf, Locale("de", "CH")),
        CNY("CNY", "¥", R.string.currency_cny, Locale.CHINA),
        MXN("MXN", "Mex$", R.string.currency_mxn, Locale("es", "MX")),
        BRL("BRL", "R$", R.string.currency_brl, Locale("pt", "BR")),
        ARS("ARS", "AR$", R.string.currency_ars, Locale("es", "AR")),
        CLP("CLP", "CL$", R.string.currency_clp, Locale("es", "CL")),
        COP("COP", "COL$", R.string.currency_cop, Locale("es", "CO"));

        companion object {
            fun fromCode(code: String): Currency {
                return values().find { it.code == code } ?: PEN
            }
        }
    }

    /**
     * Obtiene la moneda seleccionada
     */
    fun getSelectedCurrency(context: Context): Currency {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val code = prefs.getString(PREFS_CURRENCY, "PEN") ?: "PEN"
        return Currency.fromCode(code)
    }

    /**
     * Guarda la moneda seleccionada
     */
    fun setSelectedCurrency(context: Context, currency: Currency) {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        prefs.edit().putString(PREFS_CURRENCY, currency.code).apply()
    }

    /**
     * Formatea un monto con la moneda seleccionada
     */
    fun formatAmount(context: Context, amount: Double): String {
        val currency = getSelectedCurrency(context)
        return "${currency.symbol} ${String.format(currency.locale, "%.2f", amount)}"
    }

    /**
     * Formatea un monto con separador de miles
     */
    fun formatAmountWithSeparator(context: Context, amount: Double): String {
        val currency = getSelectedCurrency(context)
        val formatter = NumberFormat.getCurrencyInstance(currency.locale)

        return try {
            val javaCurrency = java.util.Currency.getInstance(currency.code)
            formatter.currency = javaCurrency
            formatter.format(amount)
        } catch (e: Exception) {
            formatAmount(context, amount)
        }
    }

    /**
     * Obtiene solo el símbolo de la moneda
     */
    fun getCurrencySymbol(context: Context): String {
        return getSelectedCurrency(context).symbol
    }

    /**
     * Obtiene el nombre de la moneda
     */
    fun getCurrencyName(context: Context): String {
        val currency = getSelectedCurrency(context)
        return context.getString(currency.nameResId)
    }
}