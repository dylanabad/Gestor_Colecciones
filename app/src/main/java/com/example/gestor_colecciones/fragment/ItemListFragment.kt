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
import com.example.gestor_colecciones.repository.CategoriaRepository
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

    private var categoriasMap: Map<Int, String> = emptyMap() // 🔥 clave

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = DatabaseProvider.getDatabase(requireContext())

        val itemRepo = ItemRepository(db.itemDao())
        val categoriaRepo = CategoriaRepository(db.categoriaDao())

        viewModel = ViewModelProvider(
            this,
            ItemViewModelFactory(itemRepo)
        )[ItemViewModel::class.java]

        // 1. Cargar categorías primero
        lifecycleScope.launch {
            val categorias = categoriaRepo.allCategoriasOnce()
            categoriasMap = categorias.associate { it.id to it.nombre }

            // 2. Crear adapter DESPUÉS
            adapter = ItemAdapter(
                items = emptyList(),
                categoriasMap = categoriasMap,
                onItemClick = { item ->
                    val fragment = ItemDetailFragment.newInstance(item.id)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                },
                onItemLongClick = { item ->
                    // aquí puedes meter edición si quieres
                }
            )

            binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
            binding.rvItems.adapter = adapter

            //  3. Observar items (ya con adapter listo)
            lifecycleScope.launch {
                viewModel.items.collectLatest { items ->
                    adapter.updateList(items)
                }
            }
        }

        //Búsqueda
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

        // ➕ FAB
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

            viewModel.insert(newItem) { id ->
                val fragment = ItemDetailFragment.newInstance(id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
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