package com.smartsaldo.app.utils

import android.util.Patterns
import com.google.android.material.textfield.TextInputLayout

object ValidationUtils {

    /**
     * Valida un email
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Valida una contraseña (mínimo 6 caracteres, al menos una letra y un número)
     */
    fun isValidPassword(password: String): Boolean {
        if (password.length < 6) return false
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }

    /**
     * Valida que un monto sea válido (positivo y no mayor a un límite razonable)
     */
    fun isValidAmount(amount: Double?, maxAmount: Double = 999999999.99): Boolean {
        return amount != null && amount > 0 && amount <= maxAmount
    }

    /**
     * Valida que un texto no esté vacío y tenga longitud válida
     */
    fun isValidText(text: String, minLength: Int = 1, maxLength: Int = 100): Boolean {
        return text.isNotBlank() && text.trim().length >= minLength && text.length <= maxLength
    }

    /**
     * Valida que un nombre sea válido (solo letras, espacios y algunos caracteres)
     */
    fun isValidName(name: String): Boolean {
        if (name.isBlank() || name.length < 2 || name.length > 50) return false
        val regex = Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$")
        return regex.matches(name.trim())
    }

    /**
     * Valida un emoji (al menos 1 carácter Unicode válido)
     */
    fun isValidEmoji(emoji: String): Boolean {
        if (emoji.isBlank()) return false
        // Verificar que tenga al menos un emoji Unicode
        return emoji.codePoints().anyMatch { codePoint ->
            (codePoint in 0x1F600..0x1F64F) || // Emoticons
                    (codePoint in 0x1F300..0x1F5FF) || // Símbolos y pictogramas
                    (codePoint in 0x1F680..0x1F6FF) || // Transporte
                    (codePoint in 0x2600..0x26FF) ||   // Símbolos varios
                    (codePoint in 0x2700..0x27BF) ||   // Dingbats
                    (codePoint in 0x1F900..0x1F9FF)    // Símbolos suplementarios
        }
    }

    /**
     * Muestra error en un TextInputLayout
     */
    fun TextInputLayout.showError(message: String) {
        error = message
        isErrorEnabled = true
    }

    /**
     * Limpia error de un TextInputLayout
     */
    fun TextInputLayout.clearError() {
        error = null
        isErrorEnabled = false
    }

    /**
     * Valida y muestra error si es necesario
     */
    fun TextInputLayout.validateRequired(errorMessage: String = "Este campo es requerido"): Boolean {
        val text = editText?.text.toString().trim()
        return if (text.isBlank()) {
            showError(errorMessage)
            false
        } else {
            clearError()
            true
        }
    }

    /**
     * Valida email y muestra error
     */
    fun TextInputLayout.validateEmail(): Boolean {
        val email = editText?.text.toString().trim()
        return when {
            email.isBlank() -> {
                showError("Ingrese su email")
                false
            }
            !isValidEmail(email) -> {
                showError("Email inválido")
                false
            }
            else -> {
                clearError()
                true
            }
        }
    }

    /**
     * Valida contraseña y muestra error
     */
    fun TextInputLayout.validatePassword(): Boolean {
        val password = editText?.text.toString()
        return when {
            password.isBlank() -> {
                showError("Ingrese su contraseña")
                false
            }
            password.length < 6 -> {
                showError("La contraseña debe tener al menos 6 caracteres")
                false
            }
            !password.any { it.isLetter() } -> {
                showError("La contraseña debe contener al menos una letra")
                false
            }
            !password.any { it.isDigit() } -> {
                showError("La contraseña debe contener al menos un número")
                false
            }
            else -> {
                clearError()
                true
            }
        }
    }

    /**
     * Valida monto y muestra error
     */
    fun TextInputLayout.validateAmount(maxAmount: Double = 999999999.99): Boolean {
        val amountText = editText?.text.toString().trim()
        val amount = amountText.toDoubleOrNull()

        return when {
            amountText.isBlank() -> {
                showError("Ingrese un monto")
                false
            }
            amount == null -> {
                showError("Monto inválido")
                false
            }
            amount <= 0 -> {
                showError("El monto debe ser mayor a 0")
                false
            }
            amount > maxAmount -> {
                showError("El monto es demasiado grande")
                false
            }
            else -> {
                clearError()
                true
            }
        }
    }

    /**
     * Formatea un número con máximo 2 decimales
     */
    fun formatAmount(amount: Double): String {
        return String.format("%.2f", amount)
    }

    /**
     * Limpia y formatea un texto de nombre
     */
    fun cleanName(name: String): String {
        return name.trim().replace(Regex("\\s+"), " ")
    }
}