package com.example.gestor_colecciones.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestor_colecciones.adapters.ColeccionAdapter
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentColeccionesBinding
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.repository.ColeccionRepository
import com.example.gestor_colecciones.viewmodel.ColeccionViewModel
import com.example.gestor_colecciones.viewmodel.ColeccionViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ColeccionesFragment : Fragment() {

    private var _binding: FragmentColeccionesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ColeccionViewModel
    private lateinit var adapter: ColeccionAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColeccionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView
        adapter = ColeccionAdapter(emptyList()) { coleccion ->
            // Acción al pulsar una colección (ir a items de esa colección)
            val fragment = ItemListByCollectionFragment.newInstance(coleccion.id)
            parentFragmentManager.beginTransaction()
                .replace(id, fragment)
                .addToBackStack(null)
                .commit()
        }
        binding.rvColecciones.layoutManager = LinearLayoutManager(requireContext())
        binding.rvColecciones.adapter = adapter

        // ViewModel
        val repo = ColeccionRepository(DatabaseProvider.getColeccionDao(requireContext()))
        viewModel = ViewModelProvider(
            this,
            ColeccionViewModelFactory(repo)
        )[ColeccionViewModel::class.java]

        // Observar colecciones
        lifecycleScope.launch {
            viewModel.colecciones.collectLatest { lista ->
                adapter.updateList(lista)
            }
        }

        // Botón añadir colección
        binding.btnAddColeccion.setOnClickListener {
            // Abrir un diálogo o fragmento para crear nueva colección
            val nueva = Coleccion(nombre = "Nueva colección", descripcion = "", fechaCreacion = java.util.Date())
            viewModel.insert(nueva)
        }

        // Botón editar o eliminar se puede implementar dentro del adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}