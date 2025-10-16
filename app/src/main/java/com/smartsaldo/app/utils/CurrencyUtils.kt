package com.smartsaldo.app.utils

import java.text.NumberFormat
import java.util.*

object CurrencyUtils {

    private const val CURRENCY_SYMBOL = "S/"
    private val locale = Locale("es", "PE")

    /**
     * Formatea un monto con símbolo de moneda
     */
    fun formatAmount(amount: Double): String {
        return "$CURRENCY_SYMBOL ${String.format(locale, "%.2f", amount)}"
    }

    /**
     * Formatea un monto sin símbolo
     */
    fun formatAmountWithoutSymbol(amount: Double): String {
        return String.format(locale, "%.2f", amount)
    }

    /**
     * Formatea un monto para mostrar en tarjetas (con separador de miles)
     */
    fun formatAmountWithSeparator(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = Currency.getInstance("PEN")
        return formatter.format(amount).replace("PEN", CURRENCY_SYMBOL)
    }

    /**
     * Convierte un string a double de manera segura
     */
    fun parseAmount(amountString: String): Double? {
        return try {
            amountString.trim()
                .replace(CURRENCY_SYMBOL, "")
                .replace(",", "")
                .toDoubleOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Valida que un monto sea válido
     */
    fun isValidAmount(amount: Double?, maxAmount: Double = Constants.MAX_AMOUNT): Boolean {
        return amount != null && amount > 0 && amount <= maxAmount
    }

    /**
     * Calcula porcentaje
     */
    fun calculatePercentage(current: Double, total: Double): Int {
        return if (total > 0) {
            ((current / total) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
    }

    /**
     * Formatea porcentaje
     */
    fun formatPercentage(current: Double, total: Double): String {
        val percentage = calculatePercentage(current, total)
        return "$percentage%"
    }

    /**
     * Obtiene el color según el tipo de monto (ingreso/gasto)
     */
    fun getAmountColor(isIncome: Boolean): Int {
        return if (isIncome) {
            android.graphics.Color.parseColor("#4CAF50") // Verde
        } else {
            android.graphics.Color.parseColor("#F44336") // Rojo
        }
    }

    /**
     * Formatea monto con signo según tipo
     */
    fun formatAmountWithSign(amount: Double, isIncome: Boolean): String {
        val sign = if (isIncome) "+" else "-"
        return "$sign${formatAmount(amount)}"
    }

    /**
     * Calcula el cambio entre dos montos
     */
    fun calculateChange(previous: Double, current: Double): Double {
        return current - previous
    }

    /**
     * Calcula el porcentaje de cambio
     */
    fun calculatePercentageChange(previous: Double, current: Double): Double {
        return if (previous != 0.0) {
            ((current - previous) / previous) * 100
        } else {
            0.0
        }
    }

    /**
     * Formatea el cambio con signo
     */
    fun formatChange(change: Double): String {
        val sign = if (change >= 0) "+" else ""
        return "$sign${formatAmount(change)}"
    }
}