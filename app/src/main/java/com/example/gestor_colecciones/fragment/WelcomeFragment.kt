package com.example.gestor_colecciones.fragment

/*
 * WelcomeFragment.kt
 *
 * Fragmento de bienvenida que sirve como punto de entrada a la app. Gestiona
 * la comprobación del estado de autenticación y del onboarding, sincroniza
 * datos iniciales y navega al fragmento correspondiente (login, onboarding o
 * pantalla principal). También anima la entrada de elementos de la UI.
 *
 * Nota: Solo se añaden comentarios explicativos en español; no se modifica la lógica.
 */

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.auth.AuthStore
import com.example.gestor_colecciones.repository.RepositoryProvider
import com.google.android.material.button.MaterialButton
import com.google.android.material.transition.MaterialFadeThrough
import kotlinx.coroutines.launch

class WelcomeFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configurar transiciones visuales para la navegación entre fragments
        exitTransition = MaterialFadeThrough().apply { duration = 220 }
        reenterTransition = MaterialFadeThrough().apply { duration = 220 }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)
        // Botón principal de entrada: comprobar onboarding y autenticación
        view.findViewById<MaterialButton>(R.id.btnEnter).setOnClickListener { button ->
            // Leer si el usuario ya completó el onboarding
            val onboardingCompleted = requireContext()
                .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .getBoolean("onboarding_completed", false)

            val authStore = AuthStore(requireContext())
            // Si no hay token, navegar al fragment de autenticación
            if (authStore.getToken().isNullOrBlank()) {
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace(R.id.fragment_container, AuthFragment())
                    .addToBackStack(null)
                    .commit()
            } else {
                // Si hay token, desactivar el botón y sincronizar datos antes de navegar
                button.isEnabled = false
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        RepositoryProvider.syncRepository(requireContext()).syncAll()

                        // Elegir destino: colecciones o onboarding según el flag
                        val destino = if (onboardingCompleted) ColeccionesFragment()
                        else OnboardingFragment()

                        parentFragmentManager.beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container, destino)
                            .addToBackStack(null)
                            .commit()
                    } catch (e: Exception) {
                        // En caso de error (p. ej. token inválido), limpiar credenciales y volver al login
                        authStore.clear()
                        parentFragmentManager.beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container, AuthFragment())
                            .addToBackStack(null)
                            .commit()
                    } finally {
                        button.isEnabled = true
                    }
                }
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Ejecutar animaciones de entrada para los elementos del layout
        animateEntrance(view)
    }

    private fun animateEntrance(view: View) {
        val interpolator = DecelerateInterpolator(2f)
        val duration = 500L

        val layoutTop = view.findViewById<View>(R.id.layoutTop)
        val tvTagline = view.findViewById<View>(R.id.tvTagline)
        val layoutFeatures = view.findViewById<View>(R.id.layoutFeatures)
        val layoutBottom = view.findViewById<View>(R.id.layoutBottom)

        fun animateView(v: View, delay: Long) {
            v.translationY = 60f
            v.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(duration)
                .setStartDelay(delay)
                .setInterpolator(interpolator)
                .start()
        }

        // Animar secuencialmente cada grupo de vistas con pequeños delays para un efecto de entrada
        animateView(layoutTop, 100)
        animateView(tvTagline, 250)
        animateView(layoutFeatures, 380)
        animateView(layoutBottom, 500)
    }
}
