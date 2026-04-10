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

/**
 * Fragment que gestiona el login y registro de usuarios.
 * Tras autenticarse correctamente, sincroniza los datos del servidor
 * y navega al onboarding o a la pantalla principal según corresponda.
 */
class AuthFragment : Fragment() {

    // View Binding: se anula en onDestroyView para evitar memory leaks
    private var _binding: FragmentAuthBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: AuthViewModel

    // Evita procesar el estado Success más de una vez si el flow emite varias veces
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

        // AuthRepository necesita la API para llamadas de red y AuthStore para persistir el token
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

        // Anima la tarjeta de login al entrar en la pantalla
        animateEntry()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collect { state ->
                when (state) {

                    // Estado inicial: oculta el indicador de carga
                    is AuthState.Idle -> setLoading(false)

                    // Operación en curso: muestra el indicador y bloquea los botones
                    is AuthState.Loading -> setLoading(true)

                    // Error de autenticación: muestra el mensaje devuelto por el servidor
                    is AuthState.Error -> {
                        setLoading(false)
                        binding.tvError.text = state.message
                        binding.tvError.isVisible = true
                    }

                    // Autenticación correcta: sincroniza datos y navega al siguiente destino
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

    /**
     * Valida los campos de login y lanza la llamada al ViewModel.
     * Acepta email o nombre de usuario en el mismo campo etEmail.
     */
    private fun handleLogin() {
        val emailInput = binding.etEmail.text?.toString()?.trim().orEmpty()
        val usernameInput = binding.etUsername.text?.toString()?.trim().orEmpty()

        // Si hay email lo usa; si no, intenta con el nombre de usuario
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

    /**
     * Valida los campos de registro y lanza la llamada al ViewModel.
     * El registro requiere los tres campos: usuario, email y contraseña.
     */
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

    /**
     * Muestra u oculta el indicador de carga y habilita o bloquea
     * los botones de acción para evitar envíos duplicados.
     */
    private fun setLoading(loading: Boolean) {
        binding.progressBar.isVisible = loading
        binding.btnLogin.isEnabled = !loading
        binding.btnRegister.isEnabled = !loading
    }

    /**
     * Anima la tarjeta de autenticación con un fade + deslizamiento hacia arriba
     * al entrar en la pantalla, usando DecelerateInterpolator para suavizar el final.
     */
    private fun animateEntry() {
        val card = binding.authCard

        // Parte invisible y ligeramente desplazada hacia abajo
        card.alpha = 0f
        card.translationY = (16 * resources.displayMetrics.density)

        // Anima hasta su posición y opacidad natural
        card.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(320)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    /**
     * Tras el login exitoso, sincroniza todos los datos del servidor antes de navegar.
     * Si la sincronización falla muestra el error sin navegar.
     */
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

    /**
     * Decide el destino de navegación según si el usuario ya completó el onboarding.
     * Usa SharedPreferences para leer el flag guardado en el primer uso de la app.
     */
    private fun navigateToNext() {
        val onboardingCompleted = requireContext()
            .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("onboarding_completed", false)

        // Si ya vio el onboarding va a colecciones; si no, al onboarding primero
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
        _binding = null // Limpia la referencia al binding para evitar memory leaks
    }
}