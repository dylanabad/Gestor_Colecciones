package com.example.gestor_colecciones.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import nl.dionsegijn.konfetti.xml.KonfettiView
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.fragment.WelcomeFragment
import com.example.gestor_colecciones.widget.ColeccionesWidgetProvider
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

// Activity principal de la aplicación
class MainActivity : AppCompatActivity() {

    // Vista encargada de mostrar animaciones de confeti
    private lateinit var konfettiView: KonfettiView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Permite que la app ocupe toda la pantalla (edge-to-edge)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Oculta la ActionBar superior para una UI más limpia
        supportActionBar?.hide()

        // Establece el layout principal
        setContentView(R.layout.activity_main)

        // Referencia al componente de confeti en el layout
        konfettiView = findViewById(R.id.konfettiView)

        // FrameLayout que contiene los fragments
        val fragmentContainer = findViewById<android.widget.FrameLayout>(R.id.fragment_container)

        // Ajusta el padding para evitar solapamiento con system bars (status/nav bar)
        ViewCompat.setOnApplyWindowInsetsListener(fragmentContainer) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Carga inicial del fragment de bienvenida si no hay estado previo
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, WelcomeFragment())
                .commit()
        }

        // --- ACTUALIZACIÓN DEL WIDGET ---

        // Manager de widgets del sistema
        val appWidgetManager = AppWidgetManager.getInstance(this)

        // Identificador del widget de la app
        val component = ComponentName(this, ColeccionesWidgetProvider::class.java)

        // Obtiene todos los IDs de instancias del widget
        val ids = appWidgetManager.getAppWidgetIds(component)

        // Si hay widgets activos, los actualiza en segundo plano
        if (ids.isNotEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                ColeccionesWidgetProvider.updateWidgets(this@MainActivity, appWidgetManager, ids)
            }
        }
    }

    // Función que lanza una animación de confeti en pantalla
    fun lanzarConfeti() {

        // Configuración del “evento” de confeti
        val party = Party(
            emitter = Emitter(duration = 3000, TimeUnit.MILLISECONDS).max(120),

            // Posición desde donde aparece el confeti (centro superior)
            position = Position.Relative(0.5, 0.0),

            // Dispersión del confeti
            spread = 300,

            // Tamaños de las partículas
            size = listOf(Size.SMALL, Size.MEDIUM),

            // Colores del confeti
            colors = listOf(
                0xFFF44336.toInt(),
                0xFF2196F3.toInt(),
                0xFF4CAF50.toInt(),
                0xFFFF9800.toInt(),
                0xFF9C27B0.toInt(),
                0xFFFFEB3B.toInt()
            )
        )

        // Inicia la animación de confeti
        konfettiView.start(party)
    }
}