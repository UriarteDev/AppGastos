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
import com.smartsaldo.app.ui.AuthState
import com.smartsaldo.app.ui.AuthViewModel
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
            btnRegistrar.setOnClickListener {
                val nombre = etNombre.text.toString().trim()
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString()
                val confirmPassword = etConfirmPassword.text.toString()

                if (nombre.isBlank()) {
                    etNombre.error = "Ingrese su nombre"
                    return@setOnClickListener
                }

                if (email.isBlank()) {
                    etEmail.error = "Ingrese su email"
                    return@setOnClickListener
                }

                if (password.length < 6) {
                    etPassword.error = "La contraseña debe tener al menos 6 caracteres"
                    return@setOnClickListener
                }

                if (password != confirmPassword) {
                    etConfirmPassword.error = "Las contraseñas no coinciden"
                    return@setOnClickListener
                }

                authViewModel.signUpWithEmail(email, password, nombre)
            }

            tvYaTienesCuenta.setOnClickListener {
                findNavController().navigateUp()
            }
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