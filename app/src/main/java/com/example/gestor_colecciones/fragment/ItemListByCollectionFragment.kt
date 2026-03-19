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
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.adapters.ItemAdapter
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.repository.ColeccionRepository
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.model.ItemFilterSortState
import com.example.gestor_colecciones.model.ItemSortField
import com.example.gestor_colecciones.model.ItemEstados
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
    private var searchQuery: String = ""
    private var filterSortState: ItemFilterSortState = ItemFilterSortState()

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
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply { duration = 240 }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply { duration = 220 }
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply { duration = 240 }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply { duration = 220 }
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
        viewLifecycleOwner.lifecycleScope.launch {
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
                        .transition(DrawableTransitionOptions.withCrossFade(180))
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
        binding.rvItems.itemAnimator = DefaultItemAnimator().apply {
            supportsChangeAnimations = false
            addDuration = 180
            removeDuration = 160
            moveDuration = 180
            changeDuration = 160
        }

        adapter.onItemClick = { item ->
            // Aquí puedes abrir un detalle si tienes ItemDetailFragment
            val fragment = ItemDetailFragment.newInstance(item.id)
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace((view.parent as ViewGroup).id, fragment)
                .addToBackStack(null)
                .commit()
        }

        adapter.onItemLongClick = { item ->
            showEditItemDialog(item)
        }

        parentFragmentManager.setFragmentResultListener(
            ItemFilterSortBottomSheet.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val categoriaId = bundle.getInt(ItemFilterSortBottomSheet.BUNDLE_CATEGORY_ID, -1)
            val estado = bundle.getString(ItemFilterSortBottomSheet.BUNDLE_ESTADO, null)
            val minRating = bundle.getFloat(ItemFilterSortBottomSheet.BUNDLE_MIN_RATING, 0f)
            val sortFieldName = bundle.getString(ItemFilterSortBottomSheet.BUNDLE_SORT_FIELD, ItemSortField.DATE.name)
            val ascending = bundle.getBoolean(ItemFilterSortBottomSheet.BUNDLE_ASCENDING, false)

            val sortField = runCatching { ItemSortField.valueOf(sortFieldName ?: ItemSortField.DATE.name) }
                .getOrDefault(ItemSortField.DATE)

            filterSortState = ItemFilterSortState(
                categoriaId = categoriaId.takeIf { it != -1 },
                estado = estado,
                minCalificacion = minRating,
                sortField = sortField,
                ascending = ascending
            )
            applyFiltersAndSort()
        }

        // Cargar categorías y items
        viewLifecycleOwner.lifecycleScope.launch {
            val categorias = categoriaRepo.allCategoriasOnce()
            categoriasMap.putAll(categorias.associate { it.id to it.nombre })
            updateFabState()

            viewModel.getItemsByCollection(collectionId).collect { list ->
                fullItemList = list
                applyFiltersAndSort()
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
                viewModel.delete(item) {
                    showSnackbar("Item \"${item.titulo}\" eliminado")
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvItems)

        // SearchView
        binding.searchItems.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText.orEmpty()
                applyFiltersAndSort()
                return true
            }
        })

        binding.btnFilterSort.setOnClickListener { openFilterSort() }
    }

    private fun openFilterSort() {
        val sortedCategorias = categoriasMap.entries
            .sortedBy { it.value.lowercase(Locale.getDefault()) }

        val categoryIds = intArrayOf(-1) + sortedCategorias.map { it.key }.toIntArray()
        val categoryNames = arrayOf("Todas") + sortedCategorias.map { it.value }.toTypedArray()

        val estados = (ItemEstados.DEFAULT + fullItemList.mapNotNull { it.estado.takeIf { s -> s.isNotBlank() } })
            .distinct()
            .sortedBy { it.lowercase(Locale.getDefault()) }
        val statusList = arrayOf("Cualquiera") + estados.toTypedArray()

        ItemFilterSortBottomSheet
            .newInstance(categoryIds, categoryNames, statusList, filterSortState)
            .show(parentFragmentManager, "ItemFilterSortBottomSheet")
    }

    private fun applyFiltersAndSort() {
        val query = searchQuery.trim().lowercase(Locale.getDefault())
        var list = fullItemList

        if (query.isNotBlank()) {
            list = list.filter { it.titulo.lowercase(Locale.getDefault()).contains(query) }
        }

        filterSortState.categoriaId?.let { categoriaId ->
            list = list.filter { it.categoriaId == categoriaId }
        }

        filterSortState.estado?.let { estado ->
            list = list.filter { it.estado.equals(estado, ignoreCase = true) }
        }

        if (filterSortState.minCalificacion > 0f) {
            val min = filterSortState.minCalificacion
            list = list.filter { it.calificacion >= min }
        }

        val baseComparator = when (filterSortState.sortField) {
            ItemSortField.NAME -> compareBy<Item> { it.titulo.lowercase(Locale.getDefault()) }
            ItemSortField.VALUE -> compareBy<Item> { it.valor }
            ItemSortField.DATE -> compareBy<Item> { it.fechaAdquisicion.time }
        }
        val comparator = (if (filterSortState.ascending) baseComparator else baseComparator.reversed())
            .thenBy { it.id }

        adapter.updateList(list.sortedWith(comparator))
    }

    private fun updateFabState() {
        binding.fabAddItem.isEnabled = categoriasMap.isNotEmpty()
        binding.fabAddItem.alpha = if (categoriasMap.isNotEmpty()) 1f else 0.5f
        adapter.categoriasMap = categoriasMap
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.fabAddItem)
            .show()
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
        val actvEstado = view.findViewById<AutoCompleteTextView>(R.id.actvItemEstado)
        val spinnerCategoria = view.findViewById<Spinner>(R.id.spinnerCategoria)
        val ivPreview = view.findViewById<ImageView>(R.id.ivItemPreview)
        val btnSelectImage = view.findViewById<Button>(R.id.btnSelectImage)
        val rbCalificacion = view.findViewById<RatingBar>(R.id.rbCalificacion)
        val tvCalificacionValue = view.findViewById<TextView>(R.id.tvCalificacionValue)

        currentItemImageView = ivPreview
        btnSelectImage.setOnClickListener { pickItemImageLauncher.launch("image/*") }

        fun updateRatingLabel(rating: Float) {
            tvCalificacionValue.text = String.format(Locale.getDefault(), "%.1f", rating)
        }
        updateRatingLabel(rbCalificacion.rating)
        rbCalificacion.setOnRatingBarChangeListener { _, rating, _ -> updateRatingLabel(rating) }

        val estadosAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, ItemEstados.DEFAULT)
        actvEstado.setAdapter(estadosAdapter)
        actvEstado.setText(ItemEstados.DEFAULT.firstOrNull().orEmpty(), false)
        actvEstado.keyListener = null
        actvEstado.isCursorVisible = false
        actvEstado.setOnClickListener { actvEstado.showDropDown() }
        actvEstado.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) actvEstado.showDropDown() }

        val adapterSpinner = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoriasList.map { it.value }
        )
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapterSpinner

        val dialog = MaterialAlertDialogBuilder(requireContext())
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
                val estado = actvEstado.text?.toString()?.trim().orEmpty().ifBlank { "Nuevo" }

                val newItem = Item(
                    titulo = titulo,
                    categoriaId = categoriaId,
                    collectionId = collectionId,
                    fechaAdquisicion = Date(),
                    valor = valor,
                    imagenPath = selectedItemImageUri?.let { uri ->
                        copyImageToInternalStorage(uri, "item_${System.currentTimeMillis()}.jpg")
                    },
                    estado = estado,
                    descripcion = descripcion,
                    calificacion = rbCalificacion.rating
                )
                viewModel.insert(newItem) {
                    showSnackbar("Item \"$titulo\" creado")
                }
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
        val actvEstado = view.findViewById<AutoCompleteTextView>(R.id.actvItemEstado)
        val spinnerCategoria = view.findViewById<Spinner>(R.id.spinnerCategoria)
        val ivPreview = view.findViewById<ImageView>(R.id.ivItemPreview)
        val btnSelectImage = view.findViewById<Button>(R.id.btnSelectImage)
        val rbCalificacion = view.findViewById<RatingBar>(R.id.rbCalificacion)
        val tvCalificacionValue = view.findViewById<TextView>(R.id.tvCalificacionValue)

        currentItemImageView = ivPreview
        item.imagenPath?.let { Glide.with(requireContext()).load(File(it)).into(ivPreview) }
        btnSelectImage.setOnClickListener { pickItemImageLauncher.launch("image/*") }

        etTitulo.setText(item.titulo)
        etValor.setText(item.valor.toString())
        etDescripcion.setText(item.descripcion)
        rbCalificacion.rating = item.calificacion.coerceIn(0f, 5f)
        tvCalificacionValue.text = String.format(Locale.getDefault(), "%.1f", rbCalificacion.rating)
        rbCalificacion.setOnRatingBarChangeListener { _, rating, _ ->
            tvCalificacionValue.text = String.format(Locale.getDefault(), "%.1f", rating)
        }

        val estadosLista = (ItemEstados.DEFAULT + item.estado).distinct()
        val estadosAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, estadosLista)
        actvEstado.setAdapter(estadosAdapter)
        actvEstado.setText(item.estado, false)
        actvEstado.keyListener = null
        actvEstado.isCursorVisible = false
        actvEstado.setOnClickListener { actvEstado.showDropDown() }
        actvEstado.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) actvEstado.showDropDown() }

        val adapterSpinner = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoriasList.map { it.value }
        )
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapterSpinner

        val selectedIndex = categoriasList.indexOfFirst { it.key == item.categoriaId }
        if (selectedIndex >= 0) spinnerCategoria.setSelection(selectedIndex)

        val dialog = MaterialAlertDialogBuilder(requireContext())
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
                val estado = actvEstado.text?.toString()?.trim().orEmpty().ifBlank { item.estado }

                val actualizado = item.copy(
                    titulo = titulo,
                    valor = valor,
                    descripcion = descripcion,
                    categoriaId = categoriaId,
                    imagenPath = selectedItemImageUri?.let { uri ->
                        copyImageToInternalStorage(uri, "item_${System.currentTimeMillis()}.jpg")
                    } ?: item.imagenPath,
                    estado = estado,
                    calificacion = rbCalificacion.rating
                )
                viewModel.update(actualizado) {
                    showSnackbar("Item \"$titulo\" actualizado")
                }
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
                viewLifecycleOwner.lifecycleScope.launch {
                    val cat = Categoria(nombre = nombre)
                    val id = categoriaRepo.insert(cat).toInt()
                    categoriasMap[id] = nombre
                    adapterList.clear()
                    adapterList.addAll(categoriasMap.values)
                    adapterList.notifyDataSetChanged()
                    etCategoriaNombre.text.clear()
                    updateFabState()
                    showSnackbar("Categoría \"$nombre\" creada")
                }
            }
        }

        lvCategorias.setOnItemClickListener { _, _, position, _ ->
            val categoriaId = categoriasMap.keys.toList()[position]
            val nombreActual = categoriasMap[categoriaId]!!
            val editView = EditText(requireContext())
            editView.setText(nombreActual)
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Editar categoría")
                .setView(editView)
                .setPositiveButton("Guardar") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        val nuevoNombre = editView.text.toString()
                        val cat = Categoria(id = categoriaId, nombre = nuevoNombre)
                        categoriaRepo.update(cat)
                        categoriasMap[categoriaId] = nuevoNombre
                        adapterList.clear()
                        adapterList.addAll(categoriasMap.values)
                        adapterList.notifyDataSetChanged()
                        showSnackbar("Categoría actualizada")
                    }
                }
                .setNegativeButton("Eliminar") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        val cat = Categoria(id = categoriaId, nombre = nombreActual)
                        categoriaRepo.delete(cat)
                        categoriasMap.remove(categoriaId)
                        adapterList.clear()
                        adapterList.addAll(categoriasMap.values)
                        adapterList.notifyDataSetChanged()
                        updateFabState()
                        showSnackbar("Categoría \"$nombreActual\" eliminada")
                    }
                }
                .show()
        }

        MaterialAlertDialogBuilder(requireContext())
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
