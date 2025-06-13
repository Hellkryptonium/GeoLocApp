package com.mohdharish.geolocapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.mohdharish.geolocapp.MainActivity
import android.os.Handler
import android.os.Looper

class SplashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Show the splash screen (Android 12+ API)
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Delay for 1.5 seconds, then start MainActivity
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 1500)
    }
}
