package com.example.gestor_colecciones.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.fragment.WelcomeFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Activa edge-to-edge — el contenido se dibuja detrás de las barras del sistema
        WindowCompat.setDecorFitsSystemWindows(window, false)

        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, WelcomeFragment())
                .commit()
        }
    }
}