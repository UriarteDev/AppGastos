package com.smartsaldo.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.smartsaldo.app.R
import com.smartsaldo.app.databinding.ActivityMainBinding
import com.smartsaldo.app.ads.AdManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adView: AdView
    private val adManager = AdManager.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupAdMob()
        setupFAB()
    }

    private fun setupNavigation() {
        val navController = findNavController(R.id.nav_host_fragment)
        binding.bottomNavigation.setupWithNavController(navController)

        setSupportActionBar(binding.toolbar)
    }

    private fun setupAdMob() {
        // Inicializar AdMob
        adManager.initializeAds(this)

        // Configurar banner
        adView = binding.adView
        adManager.loadBannerAd(adView)
    }

    private fun setupFAB() {
        binding.fabAddTransaction.setOnClickListener {
            // Navegar a dialog de agregar transacción
            findNavController(R.id.nav_host_fragment)
                .navigate(R.id.action_to_add_transaction_dialog)
        }
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
