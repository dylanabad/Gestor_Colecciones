package com.example.gestor_colecciones.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.fragment.WelcomeFragment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        // Aplica los insets del sistema al fragment_container
        val fragmentContainer = findViewById<android.widget.FrameLayout>(R.id.fragment_container)
        ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, WelcomeFragment())
                .commit()
        }
    }
}