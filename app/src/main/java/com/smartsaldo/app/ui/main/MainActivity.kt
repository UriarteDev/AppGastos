package com.smartsaldo.app.ui.main

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdView
import com.smartsaldo.app.R
import com.smartsaldo.app.ads.AdManager
import com.smartsaldo.app.databinding.ActivityMainBinding
import com.smartsaldo.app.ui.ahorros.AhorroViewModel
import com.smartsaldo.app.ui.ahorros.AhorrosFragment
import com.smartsaldo.app.ui.shared.AuthViewModel
import com.smartsaldo.app.ui.categorias.CategoriasFragment
import com.smartsaldo.app.ui.estadisticas.EstadisticasFragment
import com.smartsaldo.app.ui.home.HomeFragment
import com.smartsaldo.app.ui.profile.ProfileFragment
import com.smartsaldo.app.ui.home.TransaccionViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adView: AdView
    private val adManager = AdManager.Companion.getInstance()

    private val transaccionViewModel: TransaccionViewModel by viewModels()
    private val ahorroViewModel: AhorroViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupAdMob()
        setupBottomNavigation()
        cargarDatosUsuario()
    }

    private fun cargarDatosUsuario() {
        lifecycleScope.launch {
            authViewModel.usuario.collect { usuario ->
                usuario?.let {
                    transaccionViewModel.setUsuarioId(it.uid)
                    ahorroViewModel.setUsuarioId(it.uid)

                    // ✅ SINCRONIZAR AL INICIAR LA APP
                    authViewModel.sincronizarDatos()

                    if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
                        loadHomeFragment()
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
                R.id.nav_stats -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, EstadisticasFragment())
                        .commit()
                    true
                }
                R.id.nav_categories -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, CategoriasFragment())
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
        if (::adView.isInitialized) {
            adView.pause()
        }
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        if (::adView.isInitialized) {
            adView.resume()
        }
    }

    override fun onDestroy() {
        if (::adView.isInitialized) {
            adView.destroy()
        }
        super.onDestroy()
    }
}