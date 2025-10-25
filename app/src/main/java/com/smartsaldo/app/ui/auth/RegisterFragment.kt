package com.smartsaldo.app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.smartsaldo.app.R
import com.smartsaldo.app.databinding.FragmentRegisterBinding
import com.smartsaldo.app.ui.shared.AuthState
import com.smartsaldo.app.ui.shared.AuthViewModel
import com.smartsaldo.app.utils.ValidationUtils
import com.smartsaldo.app.utils.ValidationUtils.clearError
import com.smartsaldo.app.utils.ValidationUtils.validateEmail
import com.smartsaldo.app.utils.ValidationUtils.validatePassword
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        observeAuthState()
    }

    private fun setupUI() {
        binding.apply {
            // Limpiar errores al escribir
            etNombre.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) layoutNombre.clearError()
            }

            etEmail.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) layoutEmail.clearError()
            }

            etPassword.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) layoutPassword.clearError()
            }

            etConfirmPassword.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) layoutConfirmPassword.clearError()
            }

            btnRegistrar.setOnClickListener {
                if (validateRegistration()) {
                    val nombre = ValidationUtils.cleanName(etNombre.text.toString())
                    val email = etEmail.text.toString().trim()
                    val password = etPassword.text.toString()

                    authViewModel.signUpWithEmail(email, password, nombre)
                }
            }

            tvYaTienesCuenta.setOnClickListener {
                findNavController().navigateUp()
            }
        }
    }

    private fun validateRegistration(): Boolean {
        binding.apply {
            // Validar nombre
            val isNameValid = layoutNombre.run {
                val nombre = etNombre.text.toString().trim()
                when {
                    nombre.isBlank() -> {
                        error = getString(R.string.ingrese_nombre)
                        false
                    }
                    nombre.length < 2 -> {
                        error = getString(R.string.nombre_muy_corto)
                        false
                    }
                    nombre.length > 50 -> {
                        error = getString(R.string.nombre_muy_largo)
                        false
                    }
                    !ValidationUtils.isValidName(nombre) -> {
                        error = getString(R.string.nombre_caracteres_invalidos)
                        false
                    }
                    else -> {
                        clearError()
                        true
                    }
                }
            }

            // Validar email
            val isEmailValid = layoutEmail.validateEmail()

            // Validar contraseña
            val isPasswordValid = layoutPassword.validatePassword()

            // Validar confirmación de contraseña
            val isConfirmPasswordValid = layoutConfirmPassword.run {
                val password = etPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()
                when {
                    confirmPassword.isBlank() -> {
                        error = getString(R.string.confirme_password)
                        false
                    }
                    confirmPassword != password -> {
                        error = getString(R.string.passwords_no_coinciden)
                        false
                    }
                    else -> {
                        clearError()
                        true
                    }
                }
            }

            return isNameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnRegistrar.isEnabled = false
                    }
                    is AuthState.Authenticated -> {
                        binding.progressBar.visibility = View.GONE
                        findNavController().navigate(R.id.nav_home)
                    }
                    is AuthState.Unauthenticated -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRegistrar.isEnabled = true
                    }
                    is AuthState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnRegistrar.isEnabled = true
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}