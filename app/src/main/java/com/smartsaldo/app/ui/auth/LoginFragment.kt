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
import com.smartsaldo.app.ui.shared.AuthState
import com.smartsaldo.app.ui.shared.AuthViewModel
import com.smartsaldo.app.ui.main.MainActivity
import com.smartsaldo.app.utils.ValidationUtils.clearError
import com.smartsaldo.app.utils.ValidationUtils.validateEmail
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
        binding.progressBar.visibility = View.GONE
        binding.btnLogin.isEnabled = true
        binding.btnGoogleSignIn.isEnabled = true

        when (result.resultCode) {
            Activity.RESULT_OK -> {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                try {
                    val account = task.getResult(ApiException::class.java)
                    val idToken = account?.idToken

                    if (idToken != null) {
                        binding.progressBar.visibility = View.VISIBLE
                        binding.btnLogin.isEnabled = false
                        binding.btnGoogleSignIn.isEnabled = false

                        authViewModel.signInWithGoogle(idToken)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_token_google),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: ApiException) {
                    android.util.Log.e("LoginFragment", "Google sign-in failed", e)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_google_signin, e.message ?: "Unknown"),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            Activity.RESULT_CANCELED -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.login_cancelado),
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.error_inicializar_google),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)

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
            // Limpiar errores al escribir
            etEmail.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) layoutEmail.clearError()
            }

            etPassword.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) layoutPassword.clearError()
            }

            btnLogin.setOnClickListener {
                if (validateLogin()) {
                    val email = etEmail.text.toString().trim()
                    val password = etPassword.text.toString()
                    authViewModel.signInWithEmail(email, password)
                }
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

    private fun validateLogin(): Boolean {
        binding.apply {
            val isEmailValid = layoutEmail.validateEmail()
            val isPasswordValid = layoutPassword.run {
                val password = etPassword.text.toString()
                when {
                    password.isBlank() -> {
                        error = getString(R.string.ingrese_password)
                        false
                    }
                    password.length < 6 -> {
                        error = getString(R.string.password_min_6)
                        false
                    }
                    else -> {
                        clearError()
                        true
                    }
                }
            }

            return isEmailValid && isPasswordValid
        }
    }

    private fun iniciarGoogleSignIn() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnLogin.isEnabled = false
        binding.btnGoogleSignIn.isEnabled = false

        googleSignInClient?.signOut()?.addOnCompleteListener { signOutTask ->
            if (signOutTask.isSuccessful) {
                googleSignInClient?.revokeAccess()?.addOnCompleteListener {
                    val signInIntent = googleSignInClient?.signInIntent
                    if (signInIntent != null) {
                        googleSignInLauncher.launch(signInIntent)
                    } else {
                        binding.progressBar.visibility = View.GONE
                        binding.btnLogin.isEnabled = true
                        binding.btnGoogleSignIn.isEnabled = true
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.error_inicializar_google),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                val signInIntent = googleSignInClient?.signInIntent
                if (signInIntent != null) {
                    googleSignInLauncher.launch(signInIntent)
                } else {
                    binding.progressBar.visibility = View.GONE
                    binding.btnLogin.isEnabled = true
                    binding.btnGoogleSignIn.isEnabled = true
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_inicializar_google),
                        Toast.LENGTH_SHORT
                    ).show()
                }
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

                        kotlinx.coroutines.delay(500)

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

        dialogBinding.apply {
            etEmail.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) layoutEmail.clearError()
            }

            btnEnviar.setOnClickListener {
                if (layoutEmail.validateEmail()) {
                    val email = etEmail.text.toString().trim()
                    authViewModel.resetPassword(email)
                    dialog.dismiss()
                    Snackbar.make(
                        binding.root,
                        getString(R.string.email_recuperacion_enviado, email),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

            btnCancelar.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        googleSignInClient?.signOut()
        googleSignInClient = null
    }
}