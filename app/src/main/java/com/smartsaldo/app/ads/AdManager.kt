package com.smartsaldo.app.ads

import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.*

class AdManager private constructor() {

    companion object {
        @Volatile
        private var INSTANCE: AdManager? = null

        fun getInstance(): AdManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AdManager()
                INSTANCE = instance
                instance
            }
        }

        // IDs de anuncios (reemplazar con los reales de AdMob)
        const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111" // Test ID
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712" // Test ID
    }

    fun initializeAds(context: Context) {
        MobileAds.initialize(context) { initializationStatus ->
            // Configurar como app para familias si es necesario
            val requestConfiguration = RequestConfiguration.Builder()
                .setTagForChildDirectedTreatment(RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE)
                .setTagForUnderAgeOfConsent(RequestConfiguration.TAG_FOR_UNDER_AGE_OF_CONSENT_FALSE)
                .build()

            MobileAds.setRequestConfiguration(requestConfiguration)
        }
    }

    fun loadBannerAd(adView: AdView) {
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    fun loadInterstitialAd(
        context: Context,
        onAdLoaded: (InterstitialAd) -> Unit,
        onAdFailedToLoad: () -> Unit
    ) {
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    onAdLoaded(interstitialAd)
                }

                override fun onAdFailedToLoad(loadAdError: com.google.android.gms.ads.LoadAdError) {
                    onAdFailedToLoad()
                }
            }
        )
    }
}