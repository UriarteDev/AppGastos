package com.smartsaldo.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdView
import com.smartsaldo.app.R
import com.smartsaldo.app.databinding.ActivityMainBinding
import com.smartsaldo.app.ads.AdManager
import com.smartsaldo.app.ui.auth.LoginActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adView: AdView
    private val adManager = AdManager.getInstance()

    private val transaccionViewModel: TransaccionViewModel by viewModels()
    private val ahorroViewModel: AhorroViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupAdMob()
        verificarAutenticacion()
        setupBottomNavigation()
    }

    private fun verificarAutenticacion() {
        lifecycleScope.launch {
            authViewModel.authState.collect { state ->
                when (state) {
                    is AuthState.Authenticated -> {
                        // Usuario autenticado, cargar datos
                        authViewModel.usuario.collect { usuario ->
                            usuario?.let {
                                transaccionViewModel.setUsuarioId(it.uid)
                                ahorroViewModel.setUsuarioId(it.uid)

                                // Cargar HomeFragment si no hay nada cargado
                                if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
                                    loadHomeFragment()
                                }
                            }
                        }
                    }
                    is AuthState.Unauthenticated -> {
                        // Mostrar pantalla de login
                        val intent = Intent(this@MainActivity, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    else -> {
                        // Loading, no hacer nada
                    }
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .commit()
                    true
                }
                R.id.nav_ahorros -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, AhorrosFragment())
                        .commit()
                    true
                }
                R.id.nav_profile -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, ProfileFragment())
                        .commit()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupAdMob() {
        adManager.initializeAds(this)
        adView = binding.adView
        adManager.loadBannerAd(adView)
    }

    private fun loadHomeFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()
    }

    override fun onPause() {
        adView.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView.resume()
    }

    override fun onDestroy() {
        adView.destroy()
        super.onDestroy()
    }
}