package com.smartsaldo.app.ui.auth

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.smartsaldo.app.R
import com.smartsaldo.app.databinding.DialogRecuperarPasswordBinding
import com.smartsaldo.app.databinding.FragmentLoginBinding
import com.smartsaldo.app.ui.AuthState
import com.smartsaldo.app.ui.AuthViewModel
import com.smartsaldo.app.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    private val authViewModel: AuthViewModel by viewModels()
    private var googleSignInClient: GoogleSignInClient? = null

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("LoginFragment", "Google Sign-In result: ${result.resultCode}")

        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account?.idToken

                android.util.Log.d("LoginFragment", "Token obtenido: ${idToken != null}")

                if (idToken != null) {
                    // Pasar el token al ViewModel
                    authViewModel.signInWithGoogle(idToken)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No se pudo obtener el token de Google",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    binding.btnGoogleSignIn.isEnabled = true
                }
            } catch (e: ApiException) {
                android.util.Log.e("LoginFragment", "Error de Google Sign-In", e)
                Toast.makeText(
                    requireContext(),
                    "Error de Google Sign-In: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.progressBar.visibility = View.GONE
                binding.btnLogin.isEnabled = true
                binding.btnGoogleSignIn.isEnabled = true
            }
        } else {
            android.util.Log.d("LoginFragment", "Login cancelado por el usuario")
            Toast.makeText(
                requireContext(),
                "Inicio de sesión cancelado",
                Toast.LENGTH_SHORT
            ).show()
            binding.progressBar.visibility = View.GONE
            binding.btnLogin.isEnabled = true
            binding.btnGoogleSignIn.isEnabled = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

        // Inicializar Google Sign-In Client
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
        observeAuthState()
    }

    private fun setupUI() {
        binding.apply {
            btnLogin.setOnClickListener {
                val email = etEmail.text.toString().trim()
                val password = etPassword.text.toString()

                if (email.isBlank()) {
                    etEmail.error = "Ingrese su email"
                    return@setOnClickListener
                }
                if (password.isBlank()) {
                    etPassword.error = "Ingrese su contraseña"
                    return@setOnClickListener
                }

                authViewModel.signInWithEmail(email, password)
            }

            btnGoogleSignIn.setOnClickListener {
                iniciarGoogleSignIn()
            }

            tvRegistrarse.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, RegisterFragment())
                    .addToBackStack(null)
                    .commit()
            }

            tvOlvidePassword.setOnClickListener {
                mostrarDialogRecuperarPassword()
            }
        }
    }

    private fun iniciarGoogleSignIn() {
        // Cerrar sesión previa para forzar selección de cuenta
        googleSignInClient?.signOut()?.addOnCompleteListener {
            val signInIntent = googleSignInClient?.signInIntent
            if (signInIntent != null) {
                googleSignInLauncher.launch(signInIntent)
            } else {
                Toast.makeText(
                    requireContext(),
                    "Error inicializando Google Sign-In",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun observeAuthState() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Loading -> {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnLogin.isEnabled = false
                        binding.btnGoogleSignIn.isEnabled = false
                    }
                    is AuthState.Authenticated -> {
                        binding.progressBar.visibility = View.GONE

                        // Esperar un momento para asegurar que todo se guarde
                        kotlinx.coroutines.delay(500)

                        // Navegar a MainActivity
                        val intent = Intent(requireContext(), MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        requireActivity().finish()
                    }
                    is AuthState.Unauthenticated -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                        binding.btnGoogleSignIn.isEnabled = true
                    }
                    is AuthState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                        binding.btnGoogleSignIn.isEnabled = true
                        Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun mostrarDialogRecuperarPassword() {
        val dialogBinding = DialogRecuperarPasswordBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnEnviar.setOnClickListener {
            val email = dialogBinding.etEmail.text.toString().trim()
            if (email.isBlank()) {
                dialogBinding.etEmail.error = "Ingrese su email"
                return@setOnClickListener
            }

            authViewModel.resetPassword(email)
            dialog.dismiss()
            Snackbar.make(
                binding.root,
                "Email de recuperación enviado",
                Snackbar.LENGTH_LONG
            ).show()
        }

        dialogBinding.btnCancelar.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        googleSignInClient?.signOut()
        googleSignInClient = null
    }
}