package com.example.gestor_colecciones.fragment

/*
 * LogrosFragment.kt
 *
 * Fragmento que muestra la lista de logros del usuario. Consulta el repositorio
 * de logros y el estado mediante un ViewModel, muestra el conteo de logros
 * desbloqueados y permite volver atrás.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialFadeThrough
import com.example.gestor_colecciones.adapters.LogroAdapter
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentLogrosBinding
import com.example.gestor_colecciones.model.LogroDefinicion
import com.example.gestor_colecciones.model.LogroManager
import com.example.gestor_colecciones.repository.LogroRepository
import com.example.gestor_colecciones.network.ApiProvider
import com.example.gestor_colecciones.repository.RepositoryProvider
import com.example.gestor_colecciones.repository.PrestamoRepository
import com.example.gestor_colecciones.viewmodel.LogroViewModel
import com.example.gestor_colecciones.viewmodel.LogroViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LogrosFragment : Fragment() {

    // ViewBinding para acceder a las vistas del layout
    private var _binding: FragmentLogrosBinding? = null
    private val binding get() = _binding!!

    // ViewModel que expone la lista de logros y la lógica asociada
    private lateinit var viewModel: LogroViewModel
    // Adaptador del RecyclerView que muestra los logros
    private lateinit var adapter: LogroAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configurar transiciones de entrada/salida para el fragmento
        enterTransition = MaterialFadeThrough().apply { duration = 220 }
        returnTransition = MaterialFadeThrough().apply { duration = 200 }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflar layout y preparar binding
        _binding = FragmentLogrosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Inicializar repositorios y manager de logros
        val logroRepo = LogroRepository(
            DatabaseProvider.getDatabase(requireContext()).logroDao(),
            ApiProvider.getApi(requireContext())
        )
        val coleccionRepo = RepositoryProvider.coleccionRepository(requireContext())
        val itemRepo = RepositoryProvider.itemRepository(requireContext())
        val prestamoRepo = RepositoryProvider.prestamoRepository(requireContext())
        val manager = LogroManager(logroRepo, coleccionRepo, itemRepo, prestamoRepo)

        // Crear ViewModel usando factory que inyecta repo y manager
        viewModel = ViewModelProvider(
            this, LogroViewModelFactory(logroRepo, manager)
        )[LogroViewModel::class.java]

        // Preparar RecyclerView y adaptador de logros (Estilo Lista)
        adapter = LogroAdapter(emptyList())
        binding.rvLogros.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        binding.rvLogros.adapter = adapter

        // Botón para volver atrás en el stack de fragments
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Observar cambios en la lista de logros (Flow) y actualizar UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.logros.collectLatest { lista ->
                adapter.updateList(lista)
                val desbloqueados = lista.count { it.desbloqueado }
                val total = LogroDefinicion.TODOS.size
                
                // Actualizar contador de texto
                binding.tvContador.text = "$desbloqueados/$total"
                
                // Actualizar barra de progreso (0-100)
                if (total > 0) {
                    val porcentaje = (desbloqueados * 100) / total
                    binding.progressLogros.setProgress(porcentaje, true)
                }
            }
        }

        // Forzar comprobación de logros al abrir la pantalla
        viewModel.checkLogros()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar binding para evitar fugas de memoria
        _binding = null
    }
}
