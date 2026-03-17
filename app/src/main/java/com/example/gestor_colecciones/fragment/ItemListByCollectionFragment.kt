package com.example.gestor_colecciones.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.adapters.ItemAdapter
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentItemListBinding
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.repository.CategoriaRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import kotlinx.coroutines.launch
import java.util.Date

class ItemListByCollectionFragment : Fragment() {

    private var collectionId: Int = 0
    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter
    private var fullItemList: List<Item> = emptyList()
    private var categoriasMap: Map<Int, String> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionId = arguments?.getInt(ARG_COLLECTION_ID) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemRepo = ItemRepository(DatabaseProvider.getDatabase(requireContext()).itemDao())
        val categoriaRepo = CategoriaRepository(DatabaseProvider.getDatabase(requireContext()).categoriaDao())

        viewModel = ViewModelProvider(this, ItemViewModelFactory(itemRepo))[ItemViewModel::class.java]

        // Cargar categorías y crear el mapa categoriaId -> nombre
        lifecycleScope.launch {
            val categorias = categoriaRepo.allCategoriasOnce()
            categoriasMap = categorias.associate { it.id to it.nombre }

            adapter = ItemAdapter(fullItemList, categoriasMap,
                onItemClick = { item ->
                    val fragment = ItemDetailFragment.newInstance(item.id)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                },
                onItemLongClick = { item -> showEditItemDialog(item) }
            )

            binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
            binding.rvItems.adapter = adapter
        }

        // Mostrar nombre de la colección
        lifecycleScope.launch {
            val collection = DatabaseProvider.getDatabase(requireContext()).coleccionDao()
                .getColeccionById(collectionId)
            collection?.let {
                binding.tvCollectionName.text = it.nombre
                binding.tvCollectionName.visibility = View.VISIBLE
            }
        }

        // Swipe para eliminar
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                target: androidx.recyclerview.widget.RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val item = adapter.getItem(viewHolder.adapterPosition)
                viewHolder.itemView.animate().alpha(0f).setDuration(300).withEndAction {
                    lifecycleScope.launch { viewModel.delete(item) }
                }.start()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvItems)

        // Observar items
        lifecycleScope.launch {
            viewModel.getItemsByCollection(collectionId).collect { items ->
                fullItemList = items
                adapter.updateList(items)
            }
        }

        // Crear item
        binding.fabAddItem.setOnClickListener { showCreateItemDialog() }

        // Buscador
        binding.searchItems.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val texto = newText ?: ""
                adapter.updateList(fullItemList.filter { it.titulo.contains(texto, ignoreCase = true) })
                return true
            }
        })
    }

    private fun showCreateItemDialog() {
    }

    private fun showEditItemDialog(item: Item) {
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_COLLECTION_ID = "collection_id"
        fun newInstance(collectionId: Int) = ItemListByCollectionFragment().apply {
            arguments = Bundle().apply { putInt(ARG_COLLECTION_ID, collectionId) }
        }
    }
}