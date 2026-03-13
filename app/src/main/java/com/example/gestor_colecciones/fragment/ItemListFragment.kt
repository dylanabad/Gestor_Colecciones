package com.example.gestor_colecciones.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentItemListBinding
import com.example.gestor_colecciones.adapters.ItemAdapter
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*
import com.example.gestor_colecciones.R

class ItemListFragment : Fragment() {

    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView
        adapter = ItemAdapter(emptyList())
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        // ViewModel
        val db = DatabaseProvider.getDatabase(requireContext())
        val repo = ItemRepository(db.itemDao())
        viewModel = ViewModelProvider(
            this,
            ItemViewModelFactory(repo)
        )[ItemViewModel::class.java]

        // Observar lista de items
        lifecycleScope.launch {
            viewModel.items.collectLatest { items ->
                adapter.updateList(items)
            }
        }

        // Configurar búsqueda usando SearchView
        binding.searchItems.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { search(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { search(it) }
                return true
            }

            private fun search(query: String) {
                lifecycleScope.launch {
                    viewModel.searchItems(query).collectLatest { items ->
                        adapter.updateList(items)
                    }
                }
            }
        })

        // --- FAB: agregar item y abrir detalle ---
        binding.fabAddItem.setOnClickListener {
            val newItem = Item(
                titulo = "Nuevo item",
                categoriaId = 1,
                collectionId = 1,
                fechaAdquisicion = Date(),
                valor = 50.0,
                imagenPath = null,
                estado = "Nuevo",
                descripcion = "Descripción de prueba",
                calificacion = 4.5f
            )

            // Insertar usando ViewModel y abrir detalle con callback
            viewModel.insert(newItem) { id ->
                val fragment = ItemDetailFragment.newInstance(id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment) // coincide con tu Activity
                    .addToBackStack(null)
                    .commit()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}