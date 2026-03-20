package com.example.gestor_colecciones.fragment

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
import com.example.gestor_colecciones.repository.ColeccionRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.repository.LogroRepository
import com.example.gestor_colecciones.viewmodel.LogroViewModel
import com.example.gestor_colecciones.viewmodel.LogroViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class LogrosFragment : Fragment() {

    private var _binding: FragmentLogrosBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LogroViewModel
    private lateinit var adapter: LogroAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough().apply { duration = 220 }
        returnTransition = MaterialFadeThrough().apply { duration = 200 }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogrosBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val logroRepo = LogroRepository(DatabaseProvider.getDatabase(requireContext()).logroDao())
        val coleccionRepo = ColeccionRepository(DatabaseProvider.getColeccionDao(requireContext()))
        val itemRepo = ItemRepository(DatabaseProvider.getItemDao(requireContext()))
        val manager = LogroManager(logroRepo, coleccionRepo, itemRepo)

        viewModel = ViewModelProvider(
            this, LogroViewModelFactory(logroRepo, manager)
        )[LogroViewModel::class.java]

        adapter = LogroAdapter(emptyList())
        binding.rvLogros.layoutManager = LinearLayoutManager(requireContext())
        binding.rvLogros.adapter = adapter

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.logros.collectLatest { lista ->
                adapter.updateList(lista)
                val desbloqueados = lista.count { it.desbloqueado }
                val total = LogroDefinicion.TODOS.size
                binding.tvContador.text = "$desbloqueados/$total"
            }
        }

        // Comprobar logros al abrir la pantalla
        viewModel.checkLogros()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}