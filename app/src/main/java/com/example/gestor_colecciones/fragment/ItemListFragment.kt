package com.example.gestor_colecciones.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentItemListBinding
import com.example.gestor_colecciones.adapters.ItemAdapter
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.repository.CategoriaRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import com.example.gestor_colecciones.R
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class ItemListFragment : Fragment() {

    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ItemViewModel

    // Adapter inicializado desde el principio
    private val adapter: ItemAdapter by lazy { ItemAdapter(emptyList(), emptyMap()) }

    private var categoriasMap: Map<Int, String> = emptyMap()
    private var fullItemList: List<Item> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter
        binding.fabAddItem.isEnabled = false // deshabilitamos hasta cargar categorías

        val db = DatabaseProvider.getDatabase(requireContext())
        val itemRepo = ItemRepository(db.itemDao())
        val categoriaRepo = CategoriaRepository(db.categoriaDao())

        viewModel = ViewModelProvider(this, ItemViewModelFactory(itemRepo))[ItemViewModel::class.java]

        lifecycleScope.launch {
            // Cargar categorías
            val categorias: List<Categoria> = categoriaRepo.allCategoriasOnce()
            categoriasMap = categorias.associate { it.id to it.nombre }

            // Asignar categorías y listeners al adapter
            adapter.categoriasMap = categoriasMap
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

            // Habilitar FAB y asignar listener
            binding.fabAddItem.isEnabled = true
            binding.fabAddItem.setOnClickListener {
                showCreateItemDialog(categoriaRepo)
            }

            // Observar items
            viewModel.items.collectLatest { items ->
                fullItemList = items
                adapter.updateList(items)
            }
        }

        // Configurar búsqueda
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
                    viewModel.searchItems(query).collectLatest { filtered ->
                        adapter.updateList(filtered)
                    }
                }
            }
        })
    }

    private fun showCreateItemDialog(categoriaRepo: CategoriaRepository) {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_item, null)

        val etTitulo = view.findViewById<EditText>(R.id.etTitulo)
        val etValor = view.findViewById<EditText>(R.id.etValor)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        val spinnerCategoria = view.findViewById<Spinner>(R.id.spinnerCategoria)

        val categoriasList = categoriasMap.entries.toList()
        val adapterSpinner = android.widget.ArrayAdapter(
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
                    val categoriaId = if (categoriasList.isNotEmpty())
                        categoriasList[spinnerCategoria.selectedItemPosition].key else 0

                    if (titulo.isNotBlank()) {
                        val newItem = Item(
                            titulo = titulo,
                            categoriaId = categoriaId,
                            collectionId = 1,
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
        val adapterSpinner = android.widget.ArrayAdapter(
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
                    val categoriaId = if (categoriasList.isNotEmpty())
                        categoriasList[spinnerCategoria.selectedItemPosition].key else item.categoriaId

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
}