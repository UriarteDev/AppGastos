package com.smartsaldo.app.ui.main

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartsaldo.app.R
import com.smartsaldo.app.ui.shared.AuthState
import com.smartsaldo.app.ui.shared.AuthViewModel
import com.smartsaldo.app.ui.auth.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Verificar autenticación y redirigir
        verificarAutenticacionYRedirigir()
    }

    private fun verificarAutenticacionYRedirigir() {
        lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        // Usuario autenticado → MainActivity
                        val intent = Intent(this@SplashActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    is AuthState.Unauthenticated -> {
                        // No autenticado → LoginActivity
                        val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    is AuthState.Error -> {
                        // Error → LoginActivity
                        val intent = Intent(this@SplashActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    is AuthState.Loading -> {
                        // Esperando verificación
                    }
                }
            }
        }
    }
}