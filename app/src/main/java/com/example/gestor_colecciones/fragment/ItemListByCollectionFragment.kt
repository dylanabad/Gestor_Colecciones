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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

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

        val db = DatabaseProvider.getDatabase(requireContext())
        val itemRepo = ItemRepository(db.itemDao())
        val categoriaRepo = CategoriaRepository(db.categoriaDao())

        viewModel = ViewModelProvider(this, ItemViewModelFactory(itemRepo))[ItemViewModel::class.java]

        // Cargar categorías
        lifecycleScope.launch {
            val categorias: List<Categoria> = categoriaRepo.allCategoriasOnce()
            categoriasMap = categorias.associate { it.id to it.nombre }

            // Inicializar adapter
            adapter = ItemAdapter(fullItemList, categoriasMap)

            // Asignar listeners
            adapter.onItemClick = { item: Item ->
                val fragment = ItemDetailFragment.newInstance(item.id)
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
            adapter.onItemLongClick = { item: Item ->
                showEditItemDialog(item)
            }

            // Configurar RecyclerView
            binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
            binding.rvItems.adapter = adapter
        }

        // Mostrar nombre de la colección
        lifecycleScope.launch {
            val collection = db.coleccionDao().getColeccionById(collectionId)
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

        // Observar items de la colección
        lifecycleScope.launch {
            viewModel.getItemsByCollection(collectionId).collectLatest { items ->
                fullItemList = items
                adapter.updateList(items)
            }
        }

        // FAB para crear nuevo item
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
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_item, null)

        val etTitulo = view.findViewById<EditText>(R.id.etTitulo)
        val etValor = view.findViewById<EditText>(R.id.etValor)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        val spinnerCategoria = view.findViewById<Spinner>(R.id.spinnerCategoria)

        val categoriasList = categoriasMap.entries.toList()
        val adapterSpinner = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoriasList.map { it.value }
        )
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapterSpinner

        AlertDialog.Builder(requireContext())
            .setTitle("Nuevo item")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                lifecycleScope.launch {
                    val titulo = etTitulo.text.toString()
                    val valor = etValor.text.toString().toDoubleOrNull() ?: 0.0
                    val descripcion = etDescripcion.text.toString().takeIf { it.isNotBlank() }
                    val categoriaId = if (categoriasList.isNotEmpty()) {
                        categoriasList[spinnerCategoria.selectedItemPosition].key
                    } else 0

                    if (titulo.isNotBlank()) {
                        val newItem = Item(
                            titulo = titulo,
                            categoriaId = categoriaId,
                            collectionId = collectionId,
                            fechaAdquisicion = Date(),
                            valor = valor,
                            imagenPath = null,
                            estado = "Nuevo",
                            descripcion = descripcion,
                            calificacion = 0f
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
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditItemDialog(item: Item) {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_item, null)

        val etTitulo = view.findViewById<EditText>(R.id.etTitulo)
        val etValor = view.findViewById<EditText>(R.id.etValor)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        val spinnerCategoria = view.findViewById<Spinner>(R.id.spinnerCategoria)

        val categoriasList = categoriasMap.entries.toList()
        val adapterSpinner = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoriasList.map { it.value }
        )
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapterSpinner

        val selectedIndex = categoriasList.indexOfFirst { it.key == item.categoriaId }
        if (selectedIndex >= 0) spinnerCategoria.setSelection(selectedIndex)

        etTitulo.setText(item.titulo)
        etValor.setText(item.valor.toString())
        etDescripcion.setText(item.descripcion)

        AlertDialog.Builder(requireContext())
            .setTitle("Editar item")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                lifecycleScope.launch {
                    val titulo = etTitulo.text.toString()
                    val valor = etValor.text.toString().toDoubleOrNull() ?: item.valor
                    val descripcion = etDescripcion.text.toString().takeIf { it.isNotBlank() }
                    val categoriaId = if (categoriasList.isNotEmpty()) {
                        categoriasList[spinnerCategoria.selectedItemPosition].key
                    } else item.categoriaId

                    val actualizado = item.copy(
                        titulo = titulo,
                        valor = valor,
                        descripcion = descripcion,
                        categoriaId = categoriaId
                    )
                    viewModel.update(actualizado)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
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