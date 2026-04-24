package com.example.gestor_colecciones.fragment

import android.Manifest
import android.content.ContentValues
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.adapters.ItemAdapter
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.model.ItemEstados
import com.example.gestor_colecciones.model.ItemFilterSortState
import com.example.gestor_colecciones.model.ItemSortField
import com.example.gestor_colecciones.repository.CategoriaRepository
import com.example.gestor_colecciones.repository.ItemHistoryRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.repository.RepositoryProvider
import com.example.gestor_colecciones.repository.TagRepository
import com.example.gestor_colecciones.util.ImageUtils
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import com.example.gestor_colecciones.databinding.FragmentItemListBinding
import com.example.gestor_colecciones.network.ApiProvider
import com.example.gestor_colecciones.network.UploadUtils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Presenta el catalogo global de items del usuario fuera del contexto de una sola coleccion.
 */
class ItemListFragment : Fragment() {

    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter

    private var categoriasMap: MutableMap<Int, String> = mutableMapOf()
    private var fullItemList: List<Item> = emptyList()

    private var searchQuery: String = ""
    private var filterSortState: ItemFilterSortState = ItemFilterSortState()

    private var tagIdsByItemId: Map<Int, Set<Int>> = emptyMap()

    private lateinit var itemRepo: ItemRepository
    private lateinit var categoriaRepo: CategoriaRepository
    private lateinit var tagRepo: TagRepository
    private lateinit var historyRepo: ItemHistoryRepository

    // Manejo de imágenes (similar a ItemListByCollectionFragment para consistencia en edición)
    private var selectedItemImageUri: Uri? = null
    private var currentItemImageView: ImageView? = null
    private var cameraItemImageUri: Uri? = null

    private val pickItemImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            selectedItemImageUri = it
            currentItemImageView?.setImageURI(it)
        }
    }

    private val takeItemImageLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            cameraItemImageUri?.let {
                selectedItemImageUri = it
                currentItemImageView?.setImageURI(it)
                finalizePendingImage(it)
            }
        } else {
            takeItemImagePreviewLauncher.launch(null)
        }
    }

    private val takeItemImagePreviewLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            val uri = saveBitmapToGallery(bitmap, "item")
            if (uri != null) {
                selectedItemImageUri = uri
                currentItemImageView?.setImageURI(uri)
            }
        }
    }

    private val cameraItemPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val cameraGranted = grants[Manifest.permission.CAMERA] == true
        val storageGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            grants[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        } else {
            true
        }
        if (cameraGranted && storageGranted) {
            openCameraForItem()
        } else {
            showSnackbar("Permiso de cámara/almacenamiento denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply { duration = 240 }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply { duration = 220 }
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply { duration = 240 }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply { duration = 220 }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = DatabaseProvider.getDatabase(requireContext())
        itemRepo = RepositoryProvider.itemRepository(requireContext())
        categoriaRepo = RepositoryProvider.categoriaRepository(requireContext())
        tagRepo = RepositoryProvider.tagRepository(requireContext())
        historyRepo = ItemHistoryRepository(db.itemHistoryDao())

        viewModel = ViewModelProvider(this, ItemViewModelFactory(itemRepo, categoriaRepo, historyRepo))[ItemViewModel::class.java]

        adapter = ItemAdapter(emptyList(), categoriasMap)
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter
        binding.rvItems.itemAnimator = DefaultItemAnimator().apply {
            supportsChangeAnimations = false
        }

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return

                val item = adapter.getItem(position)
                viewLifecycleOwner.lifecycleScope.launch {
                    RepositoryProvider.papeleraRepository(requireContext()).moverItemAPapelera(item)
                    Snackbar.make(
                        binding.root,
                        "\"${item.titulo}\" movido a la papelera",
                        Snackbar.LENGTH_LONG
                    )
                        .setAction("Ver papelera") {
                            parentFragmentManager.beginTransaction()
                                .setReorderingAllowed(true)
                                .replace((view.parent as ViewGroup).id, PapeleraFragment())
                                .addToBackStack(null)
                                .commit()
                        }
                        .setAnchorView(binding.fabAddItem)
                        .show()
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvItems)

        adapter.onItemClick = { item ->
            val fragment = ItemDetailFragment.newInstance(item.id)
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        adapter.onItemLongClick = { item ->
            showEditItemDialog(item)
        }

        parentFragmentManager.setFragmentResultListener(ItemFilterSortBottomSheet.RESULT_KEY, viewLifecycleOwner) { _, bundle ->
            val categoriaId = bundle.getInt(ItemFilterSortBottomSheet.BUNDLE_CATEGORY_ID, -1)
            val estado = bundle.getString(ItemFilterSortBottomSheet.BUNDLE_ESTADO, null)
            val tagId = bundle.getInt(ItemFilterSortBottomSheet.BUNDLE_TAG_ID, -1)
            val minRating = bundle.getFloat(ItemFilterSortBottomSheet.BUNDLE_MIN_RATING, 0f)
            val sortFieldName = bundle.getString(ItemFilterSortBottomSheet.BUNDLE_SORT_FIELD, ItemSortField.DATE.name)
            val ascending = bundle.getBoolean(ItemFilterSortBottomSheet.BUNDLE_ASCENDING, false)

            val sortField = runCatching { ItemSortField.valueOf(sortFieldName ?: ItemSortField.DATE.name) }.getOrDefault(ItemSortField.DATE)

            filterSortState = ItemFilterSortState(
                categoriaId = categoriaId.takeIf { it != -1 },
                tagId = tagId.takeIf { it != -1 },
                estado = estado,
                minCalificacion = minRating,
                sortField = sortField,
                ascending = ascending
            )
            applyFiltersAndSort()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val categorias = categoriaRepo.allCategoriasOnce()
            categoriasMap.putAll(categorias.associate { it.id to it.nombre })
            updateFabState()

            viewModel.items.collect { items ->
                fullItemList = items
                refreshTagsMaps()
                applyFiltersAndSort()
            }
        }

        binding.fabAddItem.setOnClickListener {
            showSnackbar("Crea ítems desde dentro de una colección")
        }

        binding.fabAddCategory.setOnClickListener {
            showCreateCategoriaDialog()
        }

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

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            refreshTagsMaps()
            applyFiltersAndSort()
        }
    }

    private suspend fun refreshTagsMaps() {
        val ids = fullItemList.map { it.id }
        val infos = tagRepo.getTagInfoForItemsOnce(ids)
        val namesMap = infos.groupBy { it.itemId }.mapValues { (_, v) -> v.map { it.nombre } }
        tagIdsByItemId = infos.groupBy { it.itemId }.mapValues { (_, v) -> v.map { it.tagId }.toSet() }
        adapter.tagsByItemId = namesMap
    }

    private fun openFilterSort() {
        viewLifecycleOwner.lifecycleScope.launch {
            val sortedCategorias = categoriasMap.entries.sortedBy { it.value.lowercase(Locale.getDefault()) }
            val categoryIds = intArrayOf(-1) + sortedCategorias.map { it.key }.toIntArray()
            val categoryNames = arrayOf("Todas") + sortedCategorias.map { it.value }.toTypedArray()
            val estados = (ItemEstados.DEFAULT + fullItemList.mapNotNull { it.estado.takeIf { s -> s.isNotBlank() } }).distinct().sortedBy { it.lowercase(Locale.getDefault()) }
            val statusList = arrayOf("Cualquiera") + estados.toTypedArray()
            val tags = tagRepo.getAllTagsOnce().sortedBy { it.nombre.lowercase(Locale.getDefault()) }
            val tagIds = intArrayOf(-1) + tags.map { it.id }.toIntArray()
            val tagNames = arrayOf("Cualquiera") + tags.map { it.nombre }.toTypedArray()

            ItemFilterSortBottomSheet.newInstance(categoryIds, categoryNames, statusList, tagIds, tagNames, filterSortState).show(parentFragmentManager, "ItemFilterSortBottomSheet")
        }
    }

    private fun applyFiltersAndSort() {
        val query = searchQuery.trim().lowercase(Locale.getDefault())
        var list = fullItemList

        if (query.isNotBlank()) {
            list = list.filter { it.titulo.lowercase(Locale.getDefault()).contains(query) }
        }
        filterSortState.categoriaId?.let { id -> list = list.filter { it.categoriaId == id } }
        filterSortState.tagId?.let { id -> list = list.filter { tagIdsByItemId[it.id]?.contains(id) == true } }
        filterSortState.estado?.let { e -> list = list.filter { it.estado.equals(e, ignoreCase = true) } }
        if (filterSortState.minCalificacion > 0f) {
            list = list.filter { it.calificacion >= filterSortState.minCalificacion }
        }

        val baseComparator = when (filterSortState.sortField) {
            ItemSortField.NAME -> compareBy<Item> { it.titulo.lowercase(Locale.getDefault()) }
            ItemSortField.VALUE -> compareBy<Item> { it.valor }
            ItemSortField.DATE -> compareBy<Item> { it.fechaAdquisicion }
        }
        val primaryComparator = if (filterSortState.ascending) baseComparator else baseComparator.reversed()
        val comparator = compareByDescending<Item> { it.favorito }.then(primaryComparator).thenBy { it.id }

        adapter.updateList(list.sortedWith(comparator))
    }

    private fun updateFabState() {
        binding.fabAddItem.isEnabled = true
        binding.fabAddItem.alpha = 0.6f
        adapter.categoriasMap = categoriasMap
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).setAnchorView(binding.fabAddItem).show()
    }

    // --- Lógica de Imágenes ---

    private fun showItemImageSourceDialog() {
        val options = arrayOf("Galería", "Cámara")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar imagen")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickItemImageLauncher.launch("image/*")
                    1 -> checkCameraPermissions()
                }
            }
            .show()
    }

    private fun checkCameraPermissions() {
        val cameraGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        val storageGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        } else true

        if (cameraGranted && storageGranted) openCameraForItem()
        else {
            val perms = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            else arrayOf(Manifest.permission.CAMERA)
            cameraItemPermissionLauncher.launch(perms)
        }
    }

    private fun openCameraForItem() {
        val uri = createGalleryImageUri("item")
        cameraItemImageUri = uri
        takeItemImageLauncher.launch(uri)
    }

    private fun createGalleryImageUri(prefix: String): Uri {
        val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GestorColecciones")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        return requireContext().contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
    }

    private fun finalizePendingImage(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            requireContext().contentResolver.update(uri, values, null, null)
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap, prefix: String): Uri? {
        val uri = createGalleryImageUri(prefix)
        requireContext().contentResolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        finalizePendingImage(uri)
        return uri
    }

    private suspend fun uploadImage(uri: Uri): String? {
        return try {
            val api = ApiProvider.getApi(requireContext())
            val part = UploadUtils.createImagePart(requireContext(), uri)
            api.uploadImage(part).url
        } catch (e: Exception) {
            showSnackbar("Error subiendo imagen")
            null
        }
    }

    // --- Edición de Item ---

    private fun showEditItemDialog(item: Item) {
        selectedItemImageUri = null
        val categoriasList = categoriasMap.entries.toList()
        if (categoriasList.isEmpty()) return

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_item, null)
        val etTitulo = view.findViewById<EditText>(R.id.etTitulo)
        val etValor = view.findViewById<EditText>(R.id.etValor)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        val etFecha = view.findViewById<EditText>(R.id.etFechaAdquisicion)
        val actvEstado = view.findViewById<AutoCompleteTextView>(R.id.actvItemEstado)
        val actvCategoria = view.findViewById<AutoCompleteTextView>(R.id.actvItemCategoria)
        val ivPreview = view.findViewById<ImageView>(R.id.ivItemPreview)
        val btnSelectImage = view.findViewById<Button>(R.id.btnSelectImage)
        val rbCalificacion = view.findViewById<RatingBar>(R.id.rbCalificacion)
        val tvCalificacionValue = view.findViewById<TextView>(R.id.tvCalificacionValue)

        var selectedDate = item.fechaAdquisicion
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        etFecha.setText(sdf.format(selectedDate))

        etFecha.setOnClickListener {
            val calendar = Calendar.getInstance().apply { time = selectedDate }
            android.app.DatePickerDialog(requireContext(), { _, year, month, day ->
                calendar.set(year, month, day)
                selectedDate = calendar.time
                etFecha.setText(sdf.format(selectedDate))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        currentItemImageView = ivPreview
        ImageUtils.toGlideModel(item.imagenPath)?.let { Glide.with(requireContext()).load(it).into(ivPreview) }
        btnSelectImage.setOnClickListener { showItemImageSourceDialog() }

        etTitulo.setText(item.titulo)
        etValor.setText(item.valor.toString())
        etDescripcion.setText(item.descripcion)
        rbCalificacion.rating = item.calificacion
        tvCalificacionValue.text = String.format(Locale.getDefault(), "%.1f", item.calificacion)
        rbCalificacion.setOnRatingBarChangeListener { _, r, _ -> tvCalificacionValue.text = String.format(Locale.getDefault(), "%.1f", r) }

        val estadosAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, (ItemEstados.DEFAULT + item.estado).distinct())
        actvEstado.setAdapter(estadosAdapter)
        actvEstado.setText(item.estado, false)

        val adapterCategorias = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categoriasList.map { it.value })
        actvCategoria.setAdapter(adapterCategorias)
        categoriasList.find { it.key == item.categoriaId }?.let { actvCategoria.setText(it.value, false) }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar ítem")
            .setView(view)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val titulo = etTitulo.text.toString().trim()
                if (titulo.isBlank()) {
                    Toast.makeText(requireContext(), "El título es obligatorio", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                viewLifecycleOwner.lifecycleScope.launch {
                    val imagePath = selectedItemImageUri?.let { uploadImage(it) } ?: item.imagenPath
                    val categoriaId = categoriasList.find { it.value == actvCategoria.text.toString() }?.key ?: item.categoriaId

                    viewModel.update(item.copy(
                        titulo = titulo,
                        valor = etValor.text.toString().toDoubleOrNull() ?: item.valor,
                        descripcion = etDescripcion.text.toString(),
                        fechaAdquisicion = selectedDate,
                        categoriaId = categoriaId,
                        imagenPath = imagePath,
                        estado = actvEstado.text.toString(),
                        calificacion = rbCalificacion.rating
                    ), onUpdated = {
                        showSnackbar("Ítem actualizado")
                        dialog.dismiss()
                    })
                }
            }
        }
        dialog.show()
    }

    private fun showCreateCategoriaDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_categoria, null)
        val rvCategorias = view.findViewById<RecyclerView>(R.id.rvCategorias)
        val etCategoriaNombre = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCategoriaNombre)
        val btnAddCategoria = view.findViewById<Button>(R.id.btnAddCategoria)

        val categoriesList = categoriasMap.entries.map { it.toPair() }.toMutableList()

        class CategoriaVH(v: View) : RecyclerView.ViewHolder(v) {
            val tvNombre: TextView = v.findViewById(R.id.tvCategoriaNombre)
            val btnEdit: View = v.findViewById(R.id.btnEditCategoria)
            val btnDelete: View = v.findViewById(R.id.btnDeleteCategoria)
        }

        val adapter = object : RecyclerView.Adapter<CategoriaVH>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaVH {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_categoria_manage, parent, false)
                return CategoriaVH(v)
            }

            override fun onBindViewHolder(holder: CategoriaVH, position: Int) {
                val item = categoriesList[position]
                holder.tvNombre.text = item.second

                holder.btnEdit.setOnClickListener {
                    val editView = EditText(requireContext()).apply { setText(item.second) }
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Editar categoria")
                        .setView(editView)
                        .setPositiveButton("Guardar") { _, _ ->
                            val nuevoNombre = editView.text.toString().trim()
                            if (nuevoNombre.isNotBlank()) {
                                viewLifecycleOwner.lifecycleScope.launch {
                                    categoriaRepo.update(Categoria(id = item.first, nombre = nuevoNombre))
                                    categoriasMap[item.first] = nuevoNombre
                                    refresh()
                                    updateFabState()
                                    showSnackbar("Categoria actualizada")
                                }
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }

                holder.btnDelete.setOnClickListener {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Eliminar categoria")
                        .setMessage("¿Seguro que quieres eliminar \"${item.second}\"?")
                        .setPositiveButton("Eliminar") { _, _ ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                categoriaRepo.delete(Categoria(id = item.first, nombre = item.second))
                                categoriasMap.remove(item.first)
                                refresh()
                                updateFabState()
                                showSnackbar("Categoria eliminada")
                            }
                        }
                        .setNegativeButton("Cancelar", null)
                        .show()
                }
            }

            override fun getItemCount() = categoriesList.size

            fun refresh() {
                categoriesList.clear()
                categoriesList.addAll(categoriasMap.entries.map { it.toPair() })
                notifyDataSetChanged()
            }
        }

        rvCategorias.layoutManager = LinearLayoutManager(requireContext())
        rvCategorias.adapter = adapter

        btnAddCategoria.setOnClickListener {
            val nombre = etCategoriaNombre.text?.toString()?.trim().orEmpty()
            if (nombre.isNotBlank()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val id = categoriaRepo.insert(Categoria(nombre = nombre)).toInt()
                    categoriasMap[id] = nombre
                    adapter.refresh()
                    etCategoriaNombre.text?.clear()
                    updateFabState()
                    showSnackbar("Categoría creada")
                }
            }
        }

        /*
            val categoriaId = categoriasMap.keys.toList()[position]
            val nombreActual = categoriasMap[categoriaId]!!
            val editView = EditText(requireContext()).apply { setText(nombreActual) }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Editar categoría")
                .setView(editView)
                .setPositiveButton("Guardar") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        categoriaRepo.update(Categoria(id = categoriaId, nombre = editView.text.toString()))
                        categoriasMap[categoriaId] = editView.text.toString()
                        adapterList.clear(); adapterList.addAll(categoriasMap.values); adapterList.notifyDataSetChanged()
                    }
                }
                .setNegativeButton("Eliminar") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        categoriaRepo.delete(Categoria(id = categoriaId, nombre = nombreActual))
                        categoriasMap.remove(categoriaId)
                        adapterList.clear(); adapterList.addAll(categoriasMap.values); adapterList.notifyDataSetChanged()
                        updateFabState()
                    }
                }.show()
        */

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Gestionar categorías")
            .setView(view)
            .setNegativeButton("Cerrar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
