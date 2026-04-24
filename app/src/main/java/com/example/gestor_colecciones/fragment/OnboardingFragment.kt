package com.example.gestor_colecciones.fragment

/*
 * OnboardingFragment.kt
 *
 * Fragmento que muestra una serie de pantallas de bienvenida (onboarding)
 * usando ViewPager2. Gestiona los puntos (dots), botones Siguiente/Skip y
 * marca en SharedPreferences cuando el onboarding se ha completado.
 *
 */

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import androidx.core.view.isGone
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.adapters.OnboardingAdapter
import com.example.gestor_colecciones.databinding.FragmentOnboardingBinding
import com.example.gestor_colecciones.model.OnboardingData
import com.google.android.material.transition.MaterialFadeThrough
import kotlin.math.abs

// Fragment que gestiona el flujo de onboarding (pantallas de bienvenida)
/**
 * Fragment de bienvenida inicial usado para introducir la propuesta de valor de la app.
 */
class OnboardingFragment : Fragment() {

    // ViewBinding para acceder a las vistas del layout
    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough().apply { duration = 300 }
        exitTransition = MaterialFadeThrough().apply { duration = 300 }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOnboardingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pages = OnboardingData.PAGES
        val adapter = OnboardingAdapter(pages)
        binding.viewPager.adapter = adapter

        // Aplicar Transformador para Efecto Parallax y Fade
        setupPageTransformer()

        setupDots(pages.size, 0)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupDots(pages.size, position)
                updateButtons(position, pages.size)
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)
                // Eliminada la transición de color de fondo para mantener el tema de la app
            }
        })

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < pages.size - 1) {
                binding.viewPager.setCurrentItem(current + 1, true)
            } else {
                finishOnboarding()
            }
        }

        binding.btnSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun setupPageTransformer() {
        binding.viewPager.setPageTransformer { page, position ->
            val absPos = abs(position)
            
            // Elementos dentro de item_onboarding_page
            val emoji = page.findViewById<View>(R.id.tvEmoji)
            val titulo = page.findViewById<View>(R.id.tvTitulo)
            val descripcion = page.findViewById<View>(R.id.tvDescripcion)
            val card = page.findViewById<View>(R.id.cardDetalle)

            if (position < -1 || position > 1) {
                page.alpha = 0f
            } else {
                // Efecto Parallax (diferentes velocidades de movimiento)
                emoji?.translationX = position * (page.width / 1.5f)
                titulo?.translationX = position * (page.width / 2.5f)
                descripcion?.translationX = position * (page.width / 3.5f)
                card?.translationX = position * (page.width / 4.5f)

                // Fade out mientras se aleja del centro
                page.alpha = 1f - absPos
            }
        }
    }

    private fun setupDots(total: Int, selected: Int) {
        binding.layoutDots.removeAllViews()
        for (i in 0 until total) {
            val dot = ImageView(requireContext()).apply {
                // El dot seleccionado es más ancho (efecto píldora)
                val width = if (i == selected) 24.dpToPx() else 8.dpToPx()
                val height = 8.dpToPx()
                val params = LinearLayout.LayoutParams(width, height).apply {
                    setMargins(4.dpToPx(), 0, 4.dpToPx(), 0)
                }
                layoutParams = params
                setImageResource(R.drawable.dot_onboarding)
                // Usar color del tema para los dots
                alpha = if (i == selected) 1f else 0.4f
            }
            binding.layoutDots.addView(dot)
        }
    }

    private fun updateButtons(position: Int, total: Int) {
        if (position == total - 1) {
            binding.btnNext.text = "¡Empezar!"
            binding.btnSkip.animate().alpha(0f).setDuration(200).withEndAction { 
                binding.btnSkip.isGone = true 
            }
        } else {
            binding.btnNext.text = "Siguiente"
            if (binding.btnSkip.isGone) {
                binding.btnSkip.visibility = View.VISIBLE
                binding.btnSkip.animate().alpha(1f).setDuration(200)
            }
        }
    }

    private fun finishOnboarding() {
        // Marcar en SharedPreferences que el onboarding se completó
        requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit { putBoolean("onboarding_completed", true) }

        // Volver al fragmento de autenticación (Login)
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, AuthFragment())
            .commit()
    }

    private fun Int.dpToPx(): Int =
        // Convierte dp a px usando la densidad de pantalla
        (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar binding para evitar fugas de memoria
        _binding = null
    }
}