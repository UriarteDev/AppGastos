package com.smartsaldo.app.utils

import android.content.Context
import android.util.Patterns
import com.google.android.material.textfield.TextInputLayout
import com.smartsaldo.app.R

object ValidationUtils {

    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isValidPassword(password: String): Boolean {
        if (password.length < 6) return false
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }

    fun isValidAmount(amount: Double?, maxAmount: Double = 999999999.99): Boolean {
        return amount != null && amount > 0 && amount <= maxAmount
    }

    fun isValidText(text: String, minLength: Int = 1, maxLength: Int = 100): Boolean {
        return text.isNotBlank() && text.trim().length >= minLength && text.length <= maxLength
    }

    fun isValidName(name: String): Boolean {
        if (name.isBlank() || name.length < 2 || name.length > 50) return false
        val regex = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$")
        return regex.matches(name.trim())
    }

    fun TextInputLayout.showError(message: String) {
        error = message
        isErrorEnabled = true
    }

    fun TextInputLayout.clearError() {
        error = null
        isErrorEnabled = false
    }

    fun TextInputLayout.validateRequired(context: Context, errorMessage: String? = null): Boolean {
        val text = editText?.text.toString().trim()
        return if (text.isBlank()) {
            showError(errorMessage ?: context.getString(com.smartsaldo.app.R.string.campo_requerido))
            false
        } else {
            clearError()
            true
        }
    }

    // Versión sin Context - usa directamente en el Fragment
    fun TextInputLayout.validateEmail(): Boolean {
        val email = editText?.text.toString().trim()
        return when {
            email.isBlank() -> {
                error = context.getString(R.string.ingrese_email)
                false
            }
            !ValidationUtils.isValidEmail(email) -> {
                error = context.getString(R.string.email_invalido)
                false
            }
            else -> {
                clearError()
                true
            }
        }
    }

    fun TextInputLayout.validatePassword(): Boolean {
        val password = editText?.text.toString()
        return when {
            password.isBlank() -> {
                error = context.getString(R.string.ingrese_password)
                false
            }
            password.length < 6 -> {
                error = context.getString(R.string.password_min_6)
                false
            }
            !password.any { it.isLetter() } -> {
                error = context.getString(R.string.password_debe_letra)
                false
            }
            !password.any { it.isDigit() } -> {
                error = context.getString(R.string.password_debe_numero)
                false
            }
            else -> {
                clearError()
                true
            }
        }
    }

    fun TextInputLayout.validateAmount(maxAmount: Double = 999999999.99): Boolean {
        val amountText = editText?.text.toString().trim()
        val amount = amountText.toDoubleOrNull()

        return when {
            amountText.isBlank() -> {
                error = context.getString(R.string.ingrese_monto)
                false
            }
            amount == null -> {
                error = context.getString(R.string.monto_invalido)
                false
            }
            amount <= 0 -> {
                error = context.getString(R.string.monto_mayor_cero)
                false
            }
            amount > maxAmount -> {
                error = context.getString(R.string.monto_muy_grande)
                false
            }
            else -> {
                clearError()
                true
            }
        }
    }

    fun formatAmount(amount: Double): String {
        return String.format("%.2f", amount)
    }

    fun cleanName(name: String): String {
        return name.trim().replace(Regex("\\s+"), " ")
    }
}