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
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.util.*

class ItemListByCollectionFragment : Fragment() {

    private var collectionId: Int = 0
    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter
    private var fullItemList: List<Item> = emptyList()
    private var categoriasMap: MutableMap<Int, String> = mutableMapOf()

    private lateinit var categoriaRepo: CategoriaRepository
    private lateinit var itemRepo: ItemRepository

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
        itemRepo = ItemRepository(db.itemDao())
        categoriaRepo = CategoriaRepository(db.categoriaDao())

        viewModel = ViewModelProvider(this, ItemViewModelFactory(itemRepo))[ItemViewModel::class.java]

        adapter = ItemAdapter(fullItemList, categoriasMap)
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        // Cargar categorías y items
        lifecycleScope.launch {
            val categorias = categoriaRepo.allCategoriasOnce()
            categoriasMap.putAll(categorias.associate { it.id to it.nombre })

            val items = viewModel.getItemsByCollection(collectionId)
            items.collect { list ->
                fullItemList = list
                adapter.updateList(list)
            }
        }

        // FAB para crear item
        binding.fabAddItem.setOnClickListener { showCreateItemDialog() }

        // FAB para crear/editar categorías
        val fabAddCategory: FloatingActionButton = view.findViewById(R.id.fabAddCategory)
        fabAddCategory.setOnClickListener { showManageCategoriesDialog() }

        // Swipe para eliminar items
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                target: androidx.recyclerview.widget.RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val item = adapter.getItem(viewHolder.adapterPosition)
                lifecycleScope.launch { viewModel.delete(item) }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvItems)

        // Búsqueda
        binding.searchItems.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val texto = newText ?: ""
                adapter.updateList(fullItemList.filter { it.titulo.contains(texto, ignoreCase = true) })
                return true
            }
        })

        // Click y long click en items
        adapter.onItemClick = { item ->
            val fragment = ItemDetailFragment.newInstance(item.id)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }
        adapter.onItemLongClick = { item ->
            showEditItemDialog(item)
        }
    }

    // --- CREAR ITEM ---
    private fun showCreateItemDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_item, null)
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
            .setTitle("Nuevo Item")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                lifecycleScope.launch {
                    val titulo = etTitulo.text.toString()
                    val valor = etValor.text.toString().toDoubleOrNull() ?: 0.0
                    val descripcion = etDescripcion.text.toString().takeIf { it.isNotBlank() }
                    val categoriaId = if (categoriasList.isNotEmpty()) categoriasList[spinnerCategoria.selectedItemPosition].key else 0

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
                        viewModel.insert(newItem) { /* No necesitas acción extra */ }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- EDITAR ITEM ---
    private fun showEditItemDialog(item: Item) {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_item, null)
        val etTitulo = view.findViewById<EditText>(R.id.etTitulo)
        val etValor = view.findViewById<EditText>(R.id.etValor)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        val spinnerCategoria = view.findViewById<Spinner>(R.id.spinnerCategoria)

        etTitulo.setText(item.titulo)
        etValor.setText(item.valor.toString())
        etDescripcion.setText(item.descripcion)

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

        AlertDialog.Builder(requireContext())
            .setTitle("Editar Item")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                lifecycleScope.launch {
                    val actualizado = item.copy(
                        titulo = etTitulo.text.toString(),
                        valor = etValor.text.toString().toDoubleOrNull() ?: item.valor,
                        descripcion = etDescripcion.text.toString(),
                        categoriaId = categoriasList[spinnerCategoria.selectedItemPosition].key
                    )
                    viewModel.update(actualizado)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // --- GESTIÓN DE CATEGORÍAS ---
    private fun showManageCategoriesDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_categoria, null)
        val lvCategorias = view.findViewById<ListView>(R.id.lvCategorias)
        val etCategoriaNombre = view.findViewById<EditText>(R.id.etCategoriaNombre)
        val btnAddCategoria = view.findViewById<Button>(R.id.btnAddCategoria)

        val adapterList = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            categoriasMap.values.toMutableList()
        )
        lvCategorias.adapter = adapterList

        btnAddCategoria.setOnClickListener {
            val nombre = etCategoriaNombre.text.toString()
            if (nombre.isNotBlank()) {
                lifecycleScope.launch {
                    val categoria = Categoria(nombre = nombre)
                    val id = categoriaRepo.insert(categoria).toInt()
                    categoriasMap[id] = nombre
                    adapterList.add(nombre)
                    adapterList.notifyDataSetChanged()
                    etCategoriaNombre.text.clear()
                }
            }
        }

        lvCategorias.setOnItemClickListener { _, _, position, _ ->
            val categoriaId = categoriasMap.keys.toList()[position]
            val nombreActual = categoriasMap[categoriaId]!!
            val editView = EditText(requireContext())
            editView.setText(nombreActual)
            AlertDialog.Builder(requireContext())
                .setTitle("Editar categoría")
                .setView(editView)
                .setPositiveButton("Guardar") { _, _ ->
                    lifecycleScope.launch {
                        val cat = Categoria(id = categoriaId, nombre = editView.text.toString())
                        categoriaRepo.update(cat)
                        categoriasMap[categoriaId] = editView.text.toString()
                        adapterList.clear()
                        adapterList.addAll(categoriasMap.values)
                        adapterList.notifyDataSetChanged()
                    }
                }
                .setNegativeButton("Eliminar") { _, _ ->
                    lifecycleScope.launch {
                        val cat = Categoria(id = categoriaId, nombre = nombreActual)
                        categoriaRepo.delete(cat)
                        categoriasMap.remove(categoriaId)
                        adapterList.clear()
                        adapterList.addAll(categoriasMap.values)
                        adapterList.notifyDataSetChanged()
                    }
                }
                .show()
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Gestionar categorías")
            .setView(view)
            .setNegativeButton("Cerrar", null)
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