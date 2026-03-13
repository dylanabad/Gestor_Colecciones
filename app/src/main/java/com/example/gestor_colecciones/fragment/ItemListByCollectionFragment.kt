package com.example.gestor_colecciones.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestor_colecciones.adapters.ItemAdapter
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentItemListBinding
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.repository.CategoriaRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date
import com.example.gestor_colecciones.R

class ItemListByCollectionFragment : Fragment() {

    private var collectionId: Int = 0
    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter
    private var fullItemList: List<Item> = emptyList()

    private var categoriaList: List<Categoria> = emptyList()

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

        // Adapter
        adapter = ItemAdapter(emptyList(),
            onItemClick = { item -> /* Detalle del item */ },
            onItemLongClick = { item -> showEditItemDialog(item) }
        )
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        // Swipe para eliminar
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                target: androidx.recyclerview.widget.RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val item = adapter.getItem(position)
                viewHolder.itemView.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        lifecycleScope.launch { viewModel.delete(item) }
                    }
                    .start()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvItems)

        // ViewModel
        val repo = ItemRepository(DatabaseProvider.getDatabase(requireContext()).itemDao())
        viewModel = ViewModelProvider(this, ItemViewModelFactory(repo))[ItemViewModel::class.java]

        // Observamos items de la colección
        lifecycleScope.launch {
            viewModel.getItemsByCollection(collectionId).collectLatest { items ->
                fullItemList = items
                adapter.updateList(items)
            }
        }

        // Categorías
        val categoriaRepo = CategoriaRepository(DatabaseProvider.getDatabase(requireContext()).categoriaDao())
        lifecycleScope.launch {
            categoriaRepo.allCategorias.collectLatest { list ->
                categoriaList = list
            }
        }

        // FAB para añadir item
        binding.fabAddItem.setOnClickListener { showCreateItemDialog() }

        // SearchView
        binding.searchItems.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val texto = newText ?: ""
                adapter.updateList(fullItemList.filter { it.titulo.contains(texto, ignoreCase = true) })
                return true
            }
        })
    }

    private fun showCreateItemDialog() {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_item, null)

        val etTitulo = view.findViewById<EditText>(R.id.etTitulo)
        val etValor = view.findViewById<EditText>(R.id.etValor)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        val spinner = view.findViewById<Spinner>(R.id.spinnerCategoria)

        // Spinner con categorías
        val adapterSpinner = ArrayAdapter(requireContext(),
            android.R.layout.simple_spinner_item,
            categoriaList.map { it.nombre })
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapterSpinner

        AlertDialog.Builder(requireContext())
            .setTitle("Nuevo item")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                val titulo = etTitulo.text.toString()
                val valor = etValor.text.toString().toDoubleOrNull() ?: 0.0
                val descripcion = etDescripcion.text.toString()
                val selectedIndex = spinner.selectedItemPosition

                if (titulo.isNotBlank() && selectedIndex >= 0) {
                    val categoriaId = categoriaList[selectedIndex].id
                    val item = Item(
                        titulo = titulo,
                        valor = valor,
                        descripcion = descripcion,
                        categoriaId = categoriaId,
                        collectionId = collectionId,
                        fechaAdquisicion = Date(),
                        imagenPath = null,
                        estado = "Nuevo",
                        calificacion = 0f
                    )
                    lifecycleScope.launch { viewModel.insert(item) }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditItemDialog(item: Item) {
        // Similar al create, pero con prellenado y update
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_COLLECTION_ID = "collection_id"
        fun newInstance(collectionId: Int): ItemListByCollectionFragment {
            val fragment = ItemListByCollectionFragment()
            fragment.arguments = Bundle().apply { putInt(ARG_COLLECTION_ID, collectionId) }
            return fragment
        }
    }
}