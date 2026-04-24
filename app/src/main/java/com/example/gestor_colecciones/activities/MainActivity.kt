package com.example.gestor_colecciones.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.fragment.WelcomeFragment
import com.example.gestor_colecciones.util.ThemeManager
import com.example.gestor_colecciones.widget.ColeccionesWidgetProvider
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.xml.KonfettiView
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var konfettiView: KonfettiView

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        supportActionBar?.hide()
        setContentView(R.layout.activity_main)

        konfettiView = findViewById(R.id.konfettiView)

        val fragmentContainer = findViewById<android.widget.FrameLayout>(R.id.fragment_container)
        ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, WelcomeFragment())
                .commit()
        }

        ColeccionesWidgetProvider.refreshAllWidgets(this)
    }

    fun lanzarConfeti() {
        val party = Party(
            emitter = Emitter(duration = 3000, TimeUnit.MILLISECONDS).max(120),
            position = Position.Relative(0.5, 0.0),
            spread = 300,
            size = listOf(Size.SMALL, Size.MEDIUM),
            colors = listOf(
                0xFFF44336.toInt(),
                0xFF2196F3.toInt(),
                0xFF4CAF50.toInt(),
                0xFFFF9800.toInt(),
                0xFF9C27B0.toInt(),
                0xFFFFEB3B.toInt()
            )
        )

        konfettiView.start(party)
    }
}
