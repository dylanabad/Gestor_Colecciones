package com.example.gestor_colecciones.fragment

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.adapters.ItemAdapter
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.repository.ColeccionRepository
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.repository.CategoriaRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import com.example.gestor_colecciones.databinding.FragmentItemListBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

class ItemListByCollectionFragment : Fragment() {

    private var collectionId: Int = 0
    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter
    private var categoriasMap: MutableMap<Int, String> = mutableMapOf()
    private var fullItemList: List<Item> = emptyList()

    private lateinit var categoriaRepo: CategoriaRepository
    private lateinit var itemRepo: ItemRepository
    private lateinit var coleccionRepo: ColeccionRepository

    private var selectedItemImageUri: Uri? = null
    private var currentItemImageView: ImageView? = null
    private val pickItemImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedItemImageUri = it
            currentItemImageView?.setImageURI(it)
        }
    }

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
        coleccionRepo = ColeccionRepository(db.coleccionDao())
        viewModel = ViewModelProvider(
            this,
            ItemViewModelFactory(itemRepo, categoriaRepo)
        )[ItemViewModel::class.java]

        // Header: nombre + imagen de la colección
        lifecycleScope.launch {
            val coleccion = coleccionRepo.getById(collectionId)
            if (coleccion != null) {
                binding.tvCollectionName.text = coleccion.nombre
                binding.tvCollectionName.visibility = View.VISIBLE

                val imagePath = coleccion.imagenPath
                val file = imagePath?.let { File(it) }
                if (file != null && file.exists()) {
                    binding.ivCollectionImage.visibility = View.VISIBLE
                    Glide.with(this@ItemListByCollectionFragment)
                        .load(file)
                        .into(binding.ivCollectionImage)
                } else {
                    binding.ivCollectionImage.visibility = View.GONE
                }
            } else {
                binding.tvCollectionName.visibility = View.GONE
                binding.ivCollectionImage.visibility = View.GONE
            }
        }

        adapter = ItemAdapter(fullItemList, categoriasMap)
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        adapter.onItemClick = { item ->
            // Aquí puedes abrir un detalle si tienes ItemDetailFragment
            Toast.makeText(requireContext(), "Click en ${item.titulo}", Toast.LENGTH_SHORT).show()
        }

        adapter.onItemLongClick = { item ->
            showEditItemDialog(item)
        }

        // Cargar categorías y items
        lifecycleScope.launch {
            val categorias = categoriaRepo.allCategoriasOnce()
            categoriasMap.putAll(categorias.associate { it.id to it.nombre })
            updateFabState()

            viewModel.getItemsByCollection(collectionId).collect { list ->
                fullItemList = list
                adapter.updateList(list)
            }
        }

        // FAB para crear item
        binding.fabAddItem.setOnClickListener {
            if (categoriasMap.isEmpty()) {
                Toast.makeText(requireContext(), "No puedes crear un item sin categorías", Toast.LENGTH_SHORT).show()
            } else {
                showCreateItemDialog()
            }
        }

        // FAB para crear/editar categorías
        binding.fabAddCategory.setOnClickListener {
            showCreateCategoriaDialog()
        }

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

        // SearchView
        binding.searchItems.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val texto = newText.orEmpty().lowercase(Locale.getDefault())
                val filtradas = fullItemList.filter {
                    it.titulo.lowercase(Locale.getDefault()).contains(texto)
                }
                adapter.updateList(filtradas)
                return true
            }
        })
    }

    private fun updateFabState() {
        binding.fabAddItem.isEnabled = categoriasMap.isNotEmpty()
        binding.fabAddItem.alpha = if (categoriasMap.isNotEmpty()) 1f else 0.5f
        adapter.categoriasMap = categoriasMap
    }

    // --- CREAR ITEM ---
    private fun showCreateItemDialog() {
        selectedItemImageUri = null
        val categoriasList = categoriasMap.entries.toList()
        if (categoriasList.isEmpty()) return

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_item, null)
        val etTitulo = view.findViewById<EditText>(R.id.etTitulo)
        val etValor = view.findViewById<EditText>(R.id.etValor)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        val spinnerCategoria = view.findViewById<Spinner>(R.id.spinnerCategoria)
        val ivPreview = view.findViewById<ImageView>(R.id.ivItemPreview)
        val btnSelectImage = view.findViewById<Button>(R.id.btnSelectImage)

        currentItemImageView = ivPreview
        btnSelectImage.setOnClickListener { pickItemImageLauncher.launch("image/*") }

        val adapterSpinner = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoriasList.map { it.value }
        )
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapterSpinner

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Nuevo item")
            .setView(view)
            .setPositiveButton("Crear", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val btnCrear = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnCrear.isEnabled = categoriasList.isNotEmpty()
            btnCrear.setOnClickListener {
                val titulo = etTitulo.text.toString().trim()
                if (titulo.isBlank()) {
                    Toast.makeText(requireContext(), "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val valor = etValor.text.toString().toDoubleOrNull() ?: 0.0
                val descripcion = etDescripcion.text.toString().takeIf { it.isNotBlank() }
                val categoriaId = categoriasList[spinnerCategoria.selectedItemPosition].key

                val newItem = Item(
                    titulo = titulo,
                    categoriaId = categoriaId,
                    collectionId = collectionId,
                    fechaAdquisicion = Date(),
                    valor = valor,
                    imagenPath = selectedItemImageUri?.let { uri ->
                        copyImageToInternalStorage(uri, "item_${System.currentTimeMillis()}.jpg")
                    },
                    estado = "Nuevo",
                    descripcion = descripcion,
                    calificacion = 0f
                )
                viewModel.insert(newItem)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // --- EDITAR ITEM ---
    private fun showEditItemDialog(item: Item) {
        selectedItemImageUri = null
        val categoriasList = categoriasMap.entries.toList()
        if (categoriasList.isEmpty()) return

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_item, null)
        val etTitulo = view.findViewById<EditText>(R.id.etTitulo)
        val etValor = view.findViewById<EditText>(R.id.etValor)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        val spinnerCategoria = view.findViewById<Spinner>(R.id.spinnerCategoria)
        val ivPreview = view.findViewById<ImageView>(R.id.ivItemPreview)
        val btnSelectImage = view.findViewById<Button>(R.id.btnSelectImage)

        currentItemImageView = ivPreview
        item.imagenPath?.let { Glide.with(requireContext()).load(File(it)).into(ivPreview) }
        btnSelectImage.setOnClickListener { pickItemImageLauncher.launch("image/*") }

        etTitulo.setText(item.titulo)
        etValor.setText(item.valor.toString())
        etDescripcion.setText(item.descripcion)

        val adapterSpinner = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoriasList.map { it.value }
        )
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapterSpinner

        val selectedIndex = categoriasList.indexOfFirst { it.key == item.categoriaId }
        if (selectedIndex >= 0) spinnerCategoria.setSelection(selectedIndex)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Editar item")
            .setView(view)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val btnGuardar = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            btnGuardar.setOnClickListener {
                val titulo = etTitulo.text.toString().trim()
                if (titulo.isBlank()) {
                    Toast.makeText(requireContext(), "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val valor = etValor.text.toString().toDoubleOrNull() ?: item.valor
                val descripcion = etDescripcion.text.toString().takeIf { it.isNotBlank() }
                val categoriaId = categoriasList[spinnerCategoria.selectedItemPosition].key

                val actualizado = item.copy(
                    titulo = titulo,
                    valor = valor,
                    descripcion = descripcion,
                    categoriaId = categoriaId,
                    imagenPath = selectedItemImageUri?.let { uri ->
                        copyImageToInternalStorage(uri, "item_${System.currentTimeMillis()}.jpg")
                    } ?: item.imagenPath
                )
                viewModel.update(actualizado)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    // --- CREAR / EDITAR CATEGORÍAS ---
    private fun showCreateCategoriaDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_categoria, null)
        val lvCategorias = view.findViewById<ListView>(R.id.lvCategorias)
        val etCategoriaNombre = view.findViewById<EditText>(R.id.etCategoriaNombre)
        val btnAddCategoria = view.findViewById<Button>(R.id.btnAddCategoria)

        val adapterList = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            categoriasMap.values.toMutableList()
        )
        lvCategorias.adapter = adapterList

        btnAddCategoria.setOnClickListener {
            val nombre = etCategoriaNombre.text.toString().trim()
            if (nombre.isNotBlank()) {
                lifecycleScope.launch {
                    val cat = Categoria(nombre = nombre)
                    val id = categoriaRepo.insert(cat).toInt()
                    categoriasMap[id] = nombre
                    adapterList.clear()
                    adapterList.addAll(categoriasMap.values)
                    adapterList.notifyDataSetChanged()
                    etCategoriaNombre.text.clear()
                    updateFabState()
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
                        updateFabState()
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

    private fun copyImageToInternalStorage(uri: Uri, filename: String): String {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val file = File(requireContext().filesDir, filename)
        inputStream.use { input ->
            FileOutputStream(file).use { output ->
                input?.copyTo(output)
            }
        }
        return file.absolutePath
    }


    companion object {
        private const val ARG_COLLECTION_ID = "collection_id"
        fun newInstance(collectionId: Int) = ItemListByCollectionFragment().apply {
            arguments = Bundle().apply { putInt(ARG_COLLECTION_ID, collectionId) }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
