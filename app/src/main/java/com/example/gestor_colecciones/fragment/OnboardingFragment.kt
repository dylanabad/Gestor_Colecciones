package com.example.gestor_colecciones.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.transition.MaterialFadeThrough
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.adapters.OnboardingAdapter
import com.example.gestor_colecciones.databinding.FragmentOnboardingBinding
import com.example.gestor_colecciones.model.OnboardingData

class OnboardingFragment : Fragment() {

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

        setupDots(pages.size, 0)

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                setupDots(pages.size, position)
                updateButtons(position, pages.size)
            }
        })

        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < pages.size - 1) {
                binding.viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }

        binding.btnSkip.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun setupDots(total: Int, selected: Int) {
        binding.layoutDots.removeAllViews()
        val context = requireContext()

        for (i in 0 until total) {
            val dot = ImageView(context).apply {
                val size = if (i == selected) 24 else 16
                val params = LinearLayout.LayoutParams(size.dpToPx(), 8.dpToPx()).apply {
                    setMargins(4.dpToPx(), 0, 4.dpToPx(), 0)
                }
                layoutParams = params
                setImageResource(R.drawable.dot_onboarding)
                alpha = if (i == selected) 1f else 0.35f
                scaleType = ImageView.ScaleType.FIT_XY
            }
            binding.layoutDots.addView(dot)
        }
    }

    private fun updateButtons(position: Int, total: Int) {
        if (position == total - 1) {
            binding.btnNext.text = "¡Empezar!"
            binding.btnSkip.visibility = View.GONE
        } else {
            binding.btnNext.text = "Siguiente"
            binding.btnSkip.visibility = View.VISIBLE
        }
    }

    private fun finishOnboarding() {
        requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("onboarding_completed", true).apply()

        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, ColeccionesFragment())
            .commit()
    }

    private fun Int.dpToPx(): Int =
        (this * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}