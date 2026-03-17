package com.example.gestor_colecciones.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.adapters.ItemAdapter
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.repository.CategoriaRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import kotlinx.coroutines.launch
import java.util.*

class ItemListByCollectionFragment : Fragment() {

    private var collectionId: Int = 0
    private var _binding: View? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter
    private var fullItemList: List<Item> = emptyList()
    private var categoriasMap: Map<Int, String> = emptyMap()
    private var categoriasList: List<Categoria> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionId = arguments?.getInt(ARG_COLLECTION_ID) ?: 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = inflater.inflate(R.layout.fragment_item_list, container, false)
        return binding
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val itemRepo = ItemRepository(DatabaseProvider.getDatabase(requireContext()).itemDao())
        val categoriaRepo = CategoriaRepository(DatabaseProvider.getDatabase(requireContext()).categoriaDao())
        viewModel = ViewModelProvider(this, ItemViewModelFactory(itemRepo))[ItemViewModel::class.java]

        lifecycleScope.launch {
            // Cargar categorías
            categoriasList = categoriaRepo.allCategoriasOnce()
            categoriasMap = categoriasList.associate { it.id to it.nombre }

            // Adapter de items
            adapter = ItemAdapter(fullItemList, categoriasMap).apply {
                // click normal
                this.onItemClick = { item: Item ->
                    val fragment = ItemDetailFragment.newInstance(item.id)
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit()
                }
                // click largo
                this.onItemLongClick = { item: Item ->
                    showEditItemDialog(item)
                }
            }

            val rvItems = binding.findViewById<RecyclerView>(R.id.rvItems)
            rvItems.layoutManager = LinearLayoutManager(requireContext())
            rvItems.adapter = adapter
        }

        // Swipe para borrar
        val rvItems = binding.findViewById<RecyclerView>(R.id.rvItems)
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false
            override fun onSwiped(vh: RecyclerView.ViewHolder, direction: Int) {
                val item = adapter.getItem(vh.adapterPosition)
                vh.itemView.animate().alpha(0f).setDuration(300).withEndAction {
                    lifecycleScope.launch { viewModel.delete(item) }
                }.start()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(rvItems)

        // Observar items
        lifecycleScope.launch {
            viewModel.getItemsByCollection(collectionId).collect { items ->
                fullItemList = items
                if (::adapter.isInitialized) adapter.updateList(items)
            }
        }

        // FAB para crear items
        val fabAddItem = binding.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddItem)
        fabAddItem.setOnClickListener {
            showCreateItemDialog()
        }

        // FAB para crear categorías
        val fabAddCategory = binding.findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAddCategory)
        fabAddCategory.setOnClickListener {
            showCreateCategoriaDialog()
        }
    }

    private fun showCreateItemDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_item, null)
        val etTitulo = dialogView.findViewById<EditText>(R.id.etTitulo)
        val etValor = dialogView.findViewById<EditText>(R.id.etValor)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcion)
        val spinnerCategoria = dialogView.findViewById<Spinner>(R.id.spinnerCategoria)

        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoriasList.map { it.nombre })
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = spinnerAdapter

        AlertDialog.Builder(requireContext())
            .setTitle("Crear item")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val titulo = etTitulo.text.toString()
                val valor = etValor.text.toString().toDoubleOrNull() ?: 0.0
                val descripcion = etDescripcion.text.toString().takeIf { it.isNotBlank() }
                val categoriaId = categoriasList.getOrNull(spinnerCategoria.selectedItemPosition)?.id ?: 0

                if (titulo.isNotBlank()) {
                    val newItem = Item(
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
                    viewModel.insert(newItem) { id ->
                        // abrir detalle tras creación
                        val fragment = ItemDetailFragment.newInstance(id)
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment)
                            .addToBackStack(null)
                            .commit()
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditItemDialog(item: Item) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_item, null)
        val etTitulo = dialogView.findViewById<EditText>(R.id.etTitulo)
        val etValor = dialogView.findViewById<EditText>(R.id.etValor)
        val etDescripcion = dialogView.findViewById<EditText>(R.id.etDescripcion)
        val spinnerCategoria = dialogView.findViewById<Spinner>(R.id.spinnerCategoria)

        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoriasList.map { it.nombre })
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = spinnerAdapter

        val selectedIndex = categoriasList.indexOfFirst { it.id == item.categoriaId }
        if (selectedIndex >= 0) spinnerCategoria.setSelection(selectedIndex)

        etTitulo.setText(item.titulo)
        etValor.setText(item.valor.toString())
        etDescripcion.setText(item.descripcion)

        AlertDialog.Builder(requireContext())
            .setTitle("Editar item")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val titulo = etTitulo.text.toString()
                val valor = etValor.text.toString().toDoubleOrNull() ?: item.valor
                val descripcion = etDescripcion.text.toString().takeIf { it.isNotBlank() }
                val categoriaId = categoriasList.getOrNull(spinnerCategoria.selectedItemPosition)?.id ?: item.categoriaId

                val updatedItem = item.copy(
                    titulo = titulo,
                    valor = valor,
                    descripcion = descripcion,
                    categoriaId = categoriaId
                )
                viewModel.update(updatedItem)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showCreateCategoriaDialog() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_categoria, null)
        val etNombre = dialogView.findViewById<EditText>(R.id.etCategoriaNombre)

        AlertDialog.Builder(requireContext())
            .setTitle("Crear categoría")
            .setView(dialogView)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = etNombre.text.toString()
                if (nombre.isNotBlank()) {
                    val repo = CategoriaRepository(DatabaseProvider.getDatabase(requireContext()).categoriaDao())
                    lifecycleScope.launch {
                        repo.insert(Categoria(nombre = nombre))
                        categoriasList = repo.allCategoriasOnce()
                        categoriasMap = categoriasList.associate { it.id to it.nombre }
                    }
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