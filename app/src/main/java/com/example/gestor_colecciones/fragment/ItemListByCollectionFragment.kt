package com.example.gestor_colecciones.fragment

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestor_colecciones.adapters.ItemAdapter
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentItemListBinding
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.repository.CategoriaRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
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
            onItemClick = { /* Detalle del item */ },
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
                    }.start()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvItems)

        // ViewModel
        val repo = ItemRepository(DatabaseProvider.getDatabase(requireContext()).itemDao())
        viewModel = ViewModelProvider(
            this,
            ItemViewModelFactory(repo)
        )[ItemViewModel::class.java]

        // Observamos items de la colección
        lifecycleScope.launch {
            viewModel.getItemsByCollection(collectionId).collect { items ->
                fullItemList = items
                adapter.updateList(items)
            }
        }

        // FAB para añadir item
        binding.fabAddItem.setOnClickListener { showCreateItemDialog() }

        // SearchView
        binding.searchItems.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val texto = newText ?: ""
                val filtered = fullItemList.filter { it.titulo.contains(texto, ignoreCase = true) }
                adapter.updateList(filtered)
                return true
            }
        })
    }

    private fun showCreateItemDialog() {
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_item, null)

        val etTitulo = view.findViewById<EditText>(R.id.etTitulo)
        val etValor = view.findViewById<EditText>(R.id.etValor)

        val spinnerCategoria = Spinner(requireContext())
        val categoriaRepo = CategoriaRepository(DatabaseProvider.getDatabase(requireContext()).categoriaDao())

        val vm = viewModel // capturamos el viewModel para usar dentro del lambda

        // Cargamos las categorías para el spinner
        lifecycleScope.launch {
            val categorias = categoriaRepo.allCategoriasOnce()
            val nombres = if (categorias.isNotEmpty()) categorias.map { it.nombre } else listOf("Sin categoría")
            val adapterSpinner = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, nombres)
            adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategoria.adapter = adapterSpinner
        }

        (view as LinearLayout).addView(spinnerCategoria, 1) // insertamos el spinner en el layout

        AlertDialog.Builder(requireContext())
            .setTitle("Nuevo item")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                val titulo = etTitulo.text.toString()
                val valor = etValor.text.toString().toDoubleOrNull() ?: 0.0

                if (titulo.isNotBlank()) {
                    lifecycleScope.launch {
                        val categorias = categoriaRepo.allCategoriasOnce()
                        val selectedPosition = spinnerCategoria.selectedItemPosition
                        val categoriaId = if (categorias.isNotEmpty()) categorias[selectedPosition].id else 0

                        val item = Item(
                            titulo = titulo,
                            categoriaId = categoriaId,
                            collectionId = collectionId,
                            fechaAdquisicion = Date(),
                            valor = valor,
                            imagenPath = null,
                            estado = "Nuevo",
                            descripcion = null,
                            calificacion = 0f
                        )
                        vm.insert(item)
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

        etTitulo.setText(item.titulo)
        etValor.setText(item.valor.toString())

        AlertDialog.Builder(requireContext())
            .setTitle("Editar item")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                val titulo = etTitulo.text.toString()
                val valor = etValor.text.toString().toDoubleOrNull() ?: item.valor
                val actualizado = item.copy(titulo = titulo, valor = valor)
                lifecycleScope.launch { viewModel.update(actualizado) }
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