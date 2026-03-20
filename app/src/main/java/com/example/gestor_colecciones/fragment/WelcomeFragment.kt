package com.example.gestor_colecciones.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import com.example.gestor_colecciones.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.transition.MaterialFadeThrough

class WelcomeFragment : Fragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialFadeThrough().apply { duration = 220 }
        reenterTransition = MaterialFadeThrough().apply { duration = 220 }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)

        view.findViewById<MaterialButton>(R.id.btnEnter).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, ColeccionesFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

        animateView(layoutTop, 100)
        animateView(tvTagline, 250)
        animateView(layoutFeatures, 380)
        animateView(layoutBottom, 500)
    }
}