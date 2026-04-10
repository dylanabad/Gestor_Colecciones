package com.example.gestor_colecciones.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.gestor_colecciones.auth.AuthRepository
import com.example.gestor_colecciones.auth.AuthState
import com.example.gestor_colecciones.auth.AuthStore
import com.example.gestor_colecciones.auth.AuthViewModel
import com.example.gestor_colecciones.auth.AuthViewModelFactory
import com.example.gestor_colecciones.databinding.FragmentAuthBinding
import com.example.gestor_colecciones.network.ApiProvider
import com.example.gestor_colecciones.repository.RepositoryProvider
import kotlinx.coroutines.launch

class AuthFragment : Fragment() {

    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AuthViewModel
    private var handledSuccess = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val authRepo = AuthRepository(
            ApiProvider.getApi(requireContext()),
            AuthStore(requireContext())
        )

        viewModel = ViewModelProvider(
            this,
            AuthViewModelFactory(authRepo)
        )[AuthViewModel::class.java]

        binding.btnLogin.setOnClickListener { handleLogin() }
        binding.btnRegister.setOnClickListener { handleRegister() }

        animateEntry()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {

                    is AuthState.Idle -> setLoading(false)

                    is AuthState.Loading -> setLoading(true)

                    is AuthState.Error -> {
                        setLoading(false)
                        binding.tvError.text = state.message
                        binding.tvError.isVisible = true
                    }

                    is AuthState.Success -> {
                        binding.tvError.isVisible = false
                        if (!handledSuccess) {
                            handledSuccess = true
                            syncAndNavigate()
                        }
                    }
                }
            }
        }
    }

    private fun handleLogin() {
        val emailInput = binding.etEmail.text?.toString()?.trim().orEmpty()
        val usernameInput = binding.etUsername.text?.toString()?.trim().orEmpty()

        val email = if (emailInput.isNotBlank()) emailInput else usernameInput
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()

        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Email/usuario y contrasena son obligatorios",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        binding.tvError.isVisible = false
        viewModel.login(email, password)
    }

    private fun handleRegister() {
        val username = binding.etUsername.text?.toString()?.trim().orEmpty()
        val email = binding.etEmail.text?.toString()?.trim().orEmpty()
        val password = binding.etPassword.text?.toString()?.trim().orEmpty()

        if (username.isBlank() || email.isBlank() || password.isBlank()) {
            Toast.makeText(
                requireContext(),
                "Usuario, email y contrasena son obligatorios",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        binding.tvError.isVisible = false
        viewModel.register(username, email, password)
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.isVisible = loading
        binding.btnLogin.isEnabled = !loading
        binding.btnRegister.isEnabled = !loading
    }

    private fun animateEntry() {
        val card = binding.authCard

        card.alpha = 0f
        card.translationY = (16 * resources.displayMetrics.density)

        card.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(320)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun syncAndNavigate() {
        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                RepositoryProvider.syncRepository(requireContext()).syncAll()
                navigateToNext()
            } catch (e: Exception) {
                binding.tvError.text = e.message ?: "Error al sincronizar datos"
                binding.tvError.isVisible = true
            } finally {
                setLoading(false)
            }
        }
    }

    private fun navigateToNext() {
        val onboardingCompleted = requireContext()
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("onboarding_completed", false)

        val destino =
            if (onboardingCompleted) ColeccionesFragment()
            else OnboardingFragment()

        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace((view?.parent as ViewGroup).id, destino)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}