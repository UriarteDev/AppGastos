package com.smartsaldo.app.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdView
import com.smartsaldo.app.R
import com.smartsaldo.app.databinding.ActivityMainBinding
import com.smartsaldo.app.ads.AdManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adView: AdView
    private val adManager = AdManager.getInstance()

    // Agregar el ViewModel
    private val transaccionViewModel: TransaccionViewModel by viewModels()
    private val ahorroViewModel: AhorroViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupAdMob()
        inicializarUsuario() // Cambiar el orden
        loadHomeFragment()
        setupBottomNavigation()
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
                else -> false
            }
        }
    }

    private fun inicializarUsuario() {
        lifecycleScope.launch {
            val database = com.smartsaldo.app.db.AppDatabase.getDatabase(this@MainActivity)
            val usuarioDao = database.usuarioDao()
            val categoriaDao = database.categoriaDao()

            // Verificar si ya existe un usuario
            var usuario = usuarioDao.getUsuarioActivo()

            if (usuario == null) {
                // Crear usuario de prueba
                val usuarioPrueba = com.smartsaldo.app.db.entities.Usuario(
                    uid = "test-user-123",
                    email = "prueba@smartsaldo.com",
                    displayName = "Usuario de Prueba",
                    photoURL = null,
                    provider = "email",
                    isActive = true
                )
                usuarioDao.insertOrUpdate(usuarioPrueba)
                usuario = usuarioPrueba

                // Crear categorías por defecto
                crearCategoriasDefault(categoriaDao)

                android.widget.Toast.makeText(
                    this@MainActivity,
                    "Usuario de prueba creado",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }

            // IMPORTANTE: Establecer el usuarioId en el ViewModel
            transaccionViewModel.setUsuarioId(usuario.uid)
            ahorroViewModel.setUsuarioId(usuario.uid)
        }
    }

    private suspend fun crearCategoriasDefault(categoriaDao: com.smartsaldo.app.db.dao.CategoriaDao) {
        val categorias = listOf(
            com.smartsaldo.app.db.entities.Categoria(nombre = "Comida", icono = "🍔", color = "#FF5722", tipo = "GASTO", esDefault = true),
            com.smartsaldo.app.db.entities.Categoria(nombre = "Transporte", icono = "🚗", color = "#2196F3", tipo = "GASTO", esDefault = true),
            com.smartsaldo.app.db.entities.Categoria(nombre = "Ocio", icono = "🎮", color = "#9C27B0", tipo = "GASTO", esDefault = true),
            com.smartsaldo.app.db.entities.Categoria(nombre = "Salud", icono = "🏥", color = "#F44336", tipo = "GASTO", esDefault = true),
            com.smartsaldo.app.db.entities.Categoria(nombre = "Sueldo", icono = "💼", color = "#4CAF50", tipo = "INGRESO", esDefault = true),
            com.smartsaldo.app.db.entities.Categoria(nombre = "Freelance", icono = "💻", color = "#00BCD4", tipo = "INGRESO", esDefault = true)
        )

        categoriaDao.insertCategorias(categorias)
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