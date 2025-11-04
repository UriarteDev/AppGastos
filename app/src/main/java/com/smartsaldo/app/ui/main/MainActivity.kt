package com.smartsaldo.app.ui.main

import android.content.Context
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
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.smartsaldo.app.utils.LocaleHelper

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
        observarConectividad()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase))
    }
    private fun observarConectividad() {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) {
                android.util.Log.d("MainActivity", "📶 Internet disponible - Sincronizando...")

                // Sincronizar cuando se conecte
                lifecycleScope.launch {
                    authViewModel.usuario.value?.let { usuario ->
                        sincronizarDatosBidireccional(usuario.uid)
                    }
                }
            }

            override fun onLost(network: android.net.Network) {
                android.util.Log.d("MainActivity", "📵 Sin internet")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } else {
            val request = android.net.NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager.registerNetworkCallback(request, networkCallback)
        }
    }
    private fun cargarDatosUsuario() {
        lifecycleScope.launch {
            authViewModel.usuario.collect { usuario ->
                usuario?.let {
                    transaccionViewModel.setUsuarioId(it.uid)
                    ahorroViewModel.setUsuarioId(it.uid)

                    // 🔄 SINCRONIZACIÓN BIDIRECCIONAL
                    sincronizarDatosBidireccional(it.uid)

                    // Cargar HomeFragment si no hay nada cargado
                    if (supportFragmentManager.findFragmentById(R.id.fragment_container) == null) {
                        loadHomeFragment()
                    }
                }
            }
        }
    }

    private fun sincronizarDatosBidireccional(usuarioId: String) {
        lifecycleScope.launch {
            try {
                // Verificar conectividad
                if (!com.smartsaldo.app.utils.NetworkUtils.isNetworkAvailable(this@MainActivity)) {
                    android.util.Log.w("MainActivity", "⚠️ Sin conexión a internet")
                    // Mostrar mensaje opcional al usuario
                    return@launch
                }

                android.util.Log.d("MainActivity", "🔄 Iniciando sincronización bidireccional...")

                // 1️⃣ Descargar datos de Firestore a Room
                authViewModel.sincronizarDatos()

                // Esperar un poco para que Room se actualice
                kotlinx.coroutines.delay(1000)

                // 2️⃣ Subir datos de Room a Firestore
                authViewModel.sincronizarTransaccionesAFirestore(usuarioId)
                authViewModel.sincronizarAhorrosAFirestore(usuarioId)

                android.util.Log.d("MainActivity", "✅ Sincronización bidireccional completada")

            } catch (e: Exception) {
                android.util.Log.e("MainActivity", "❌ Error en sincronización", e)
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