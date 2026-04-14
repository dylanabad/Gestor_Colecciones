package com.example.gestor_colecciones.fragment

/*
 * ItemListByCollectionFragment.kt
 *
 * Fragmento que muestra una lista de items pertenecientes a una colección.
 * Contiene: gestión de vistas, adaptador de RecyclerView, filtrado/ordenación,
 * creación/edición de items, manejo de imágenes (galería/cámara), generación de
 * una "tarjeta" con los items principales y operaciones sobre categorías.
 *
 * Nota: Solo se han añadido comentarios explicativos al código; no se ha modificado
 * ninguna lógica ni comportamiento.
 */

import android.content.ContentValues
import android.content.Intent
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
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
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
import com.example.gestor_colecciones.export.TarjetaColeccionGenerator
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.model.ItemFilterSortState
import com.example.gestor_colecciones.model.ItemSortField
import com.example.gestor_colecciones.model.ItemEstados
import com.example.gestor_colecciones.repository.ColeccionRepository
import com.example.gestor_colecciones.repository.CategoriaRepository
import com.example.gestor_colecciones.repository.ItemHistoryRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.repository.RepositoryProvider
import com.example.gestor_colecciones.network.ApiProvider
import com.example.gestor_colecciones.network.UploadUtils
import com.example.gestor_colecciones.repository.TagRepository
import com.example.gestor_colecciones.util.ImageUtils
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import com.example.gestor_colecciones.databinding.FragmentItemListBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

// Fragment principal que muestra items filtrados por colección.
// Gestiona UI, eventos de usuario y operaciones de negocio delegadas al ViewModel y repositorios.
class ItemListByCollectionFragment : Fragment() {

    // ID de la colección mostrada en este fragmento
    private var collectionId: Int = 0

    // Binding de vistas (ViewBinding) — se gestiona en onCreateView/onDestroyView
    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    // ViewModel y adaptador para la lista de items
    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter

    // Mapa de categorías (id -> nombre) cargado desde la BBDD
    private var categoriasMap: MutableMap<Int, String> = mutableMapOf()

    // Lista completa de items (sin filtrar) y estado de búsqueda/filtrado
    private var fullItemList: List<Item> = emptyList()
    private var searchQuery: String = ""
    private var filterSortState: ItemFilterSortState = ItemFilterSortState()

    // Mapa auxiliar que contiene para cada item los ids de sus tags (usado en filtros)
    private var tagIdsByItemId: Map<Int, Set<Int>> = emptyMap()

    // Repositorios usados para acceder a datos (Room / red / etc.)
    private lateinit var categoriaRepo: CategoriaRepository
    private lateinit var itemRepo: ItemRepository
    private lateinit var coleccionRepo: ColeccionRepository
    private lateinit var tagRepo: TagRepository
    private lateinit var historyRepo: ItemHistoryRepository

    // --- Manejo de imágenes para items ---------------------------------
    // Uri seleccionada actualmente (galería o cámara)
    private var selectedItemImageUri: Uri? = null
    // Referencia a la ImageView que muestra la previsualización temporal
    private var currentItemImageView: ImageView? = null
    // Uri temporal creada cuando se toma foto con cámara y se guarda en galería
    private var cameraItemImageUri: Uri? = null

    // Lanzador para seleccionar imagen desde la galería
    private val pickItemImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedItemImageUri = it
            currentItemImageView?.setImageURI(it)
        }
    }

    // Lanzador para tomar una foto y guardar directamente en la Uri proporcionada
    private val takeItemImageLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraItemImageUri?.let {
                selectedItemImageUri = it
                currentItemImageView?.setImageURI(it)
                finalizePendingImage(it)
            }
        } else {
            // Si falla, mostramos un preview como alternativa
            takeItemImagePreviewLauncher.launch(null)
        }
    }

    // Lanzador que devuelve un Bitmap de preview si no se pudo guardar la foto
    private val takeItemImagePreviewLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val uri = saveBitmapToGallery(bitmap, "item")
            if (uri != null) {
                selectedItemImageUri = uri
                currentItemImageView?.setImageURI(uri)
            } else {
                showSnackbar("No se pudo guardar la foto")
            }
        } else {
            showSnackbar("No se pudo capturar la foto")
        }
    }

    // Lanzador para solicitar permisos (cámara y, si aplica, escritura en almacenamiento)
    private val cameraItemPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val cameraGranted = grants[Manifest.permission.CAMERA] == true
        val storageGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            grants[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        } else {
            true
        }
        if (cameraGranted && storageGranted) {
            openCameraForItem()
        } else {
            showSnackbar("Permiso de camara/almacenamiento denegado")
        }
    }

    // Ciclo de vida: inicialización del fragmento
    // Se recupera el ID de la colección y se configuran transiciones compartidas
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        collectionId = arguments?.getInt(ARG_COLLECTION_ID) ?: 0
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply { duration = 240 }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply { duration = 220 }
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply { duration = 240 }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply { duration = 220 }
    }

    // Inflar la vista y preparar el binding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Configuración de la UI una vez creada la vista: inicializa repositorios, ViewModel,
    // adaptador, listeners y recoge datos iniciales (categorías, items, etc.).
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = DatabaseProvider.getDatabase(requireContext())
        itemRepo = RepositoryProvider.itemRepository(requireContext())
        categoriaRepo = RepositoryProvider.categoriaRepository(requireContext())
        coleccionRepo = RepositoryProvider.coleccionRepository(requireContext())
        tagRepo = RepositoryProvider.tagRepository(requireContext())
        historyRepo = ItemHistoryRepository(db.itemHistoryDao())
        viewModel = ViewModelProvider(
            this,
            ItemViewModelFactory(itemRepo, categoriaRepo, historyRepo)
        )[ItemViewModel::class.java]

        // Header: nombre + imagen de la colección
        viewLifecycleOwner.lifecycleScope.launch {
            val coleccion = coleccionRepo.getById(collectionId)
            if (coleccion != null) {
                binding.tvCollectionName.text = coleccion.nombre
                binding.tvCollectionName.visibility = View.VISIBLE
                val model = ImageUtils.toGlideModel(coleccion.imagenPath)
                if (model != null) {
                    binding.ivCollectionImage.visibility = View.VISIBLE
                    Glide.with(this@ItemListByCollectionFragment)
                        .load(model)
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
            val fragment = ItemDetailFragment.newInstance(item.id)
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace((view.parent as ViewGroup).id, fragment)
                .addToBackStack(null)
                .commit()
        }

        adapter.onItemLongClick = { item -> showEditItemDialog(item) }

        parentFragmentManager.setFragmentResultListener(
            ItemFilterSortBottomSheet.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val categoriaId = bundle.getInt(ItemFilterSortBottomSheet.BUNDLE_CATEGORY_ID, -1)
            val estado = bundle.getString(ItemFilterSortBottomSheet.BUNDLE_ESTADO, null)
            val tagId = bundle.getInt(ItemFilterSortBottomSheet.BUNDLE_TAG_ID, -1)
            val minRating = bundle.getFloat(ItemFilterSortBottomSheet.BUNDLE_MIN_RATING, 0f)
            val sortFieldName = bundle.getString(ItemFilterSortBottomSheet.BUNDLE_SORT_FIELD, ItemSortField.DATE.name)
            val ascending = bundle.getBoolean(ItemFilterSortBottomSheet.BUNDLE_ASCENDING, false)
            val sortField = runCatching { ItemSortField.valueOf(sortFieldName ?: ItemSortField.DATE.name) }
                .getOrDefault(ItemSortField.DATE)
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
            viewModel.getItemsByCollection(collectionId).collect { list ->
                fullItemList = list
                refreshTagsMaps()
                applyFiltersAndSort()
            }
        }

        binding.fabAddItem.setOnClickListener {
            if (categoriasMap.isEmpty()) {
                Toast.makeText(requireContext(), "No puedes crear un item sin categorías", Toast.LENGTH_SHORT).show()
            } else {
                showCreateItemDialog()
            }
        }

        binding.fabAddCategory.setOnClickListener { showCreateCategoriaDialog() }

        // ── FAB tarjeta ───────────────────────────────────────────────────────
        binding.fabTarjeta.setOnClickListener {
            if (fullItemList.isEmpty()) {
                showSnackbar("No hay items para generar la tarjeta")
                return@setOnClickListener
            }
            generarTarjeta()
        }

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: androidx.recyclerview.widget.RecyclerView, vh: androidx.recyclerview.widget.RecyclerView.ViewHolder, t: androidx.recyclerview.widget.RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val item = adapter.getItem(viewHolder.adapterPosition)
                viewLifecycleOwner.lifecycleScope.launch {
                    val papeleraRepo = RepositoryProvider.papeleraRepository(requireContext())
                    papeleraRepo.moverItemAPapelera(item)
                    Snackbar.make(binding.root, "\"${item.titulo}\" movido a la papelera", Snackbar.LENGTH_LONG)
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
        // Refrescar información dependiente del lifecycle cada vez que el fragmento vuelve a primer plano
        viewLifecycleOwner.lifecycleScope.launch {
            refreshTagsMaps()
            applyFiltersAndSort()
        }
    }

    // Actualiza los mapas de tags por item y los nombres usados por el adaptador.
    // Se consulta el repositorio de tags de forma puntual (once) y se transforma
    // la información para uso en filtros y en la UI.
    private suspend fun refreshTagsMaps() {
        val ids = fullItemList.map { it.id }
        val infos = tagRepo.getTagInfoForItemsOnce(ids)
        val namesMap = infos.groupBy { it.itemId }.mapValues { (_, v) -> v.map { it.nombre } }
        tagIdsByItemId = infos.groupBy { it.itemId }.mapValues { (_, v) -> v.map { it.tagId }.toSet() }
        adapter.tagsByItemId = namesMap
    }

    // Abre el BottomSheet para seleccionar filtros y ordenación. Construye las listas
    // (categorías, estados, tags) y pasa el estado actual para inicializar la UI.
    private fun openFilterSort() {
        viewLifecycleOwner.lifecycleScope.launch {
            val sortedCategorias = categoriasMap.entries.sortedBy { it.value.lowercase(Locale.getDefault()) }
            val categoryIds = intArrayOf(-1) + sortedCategorias.map { it.key }.toIntArray()
            val categoryNames = arrayOf("Todas") + sortedCategorias.map { it.value }.toTypedArray()
            val estados = (ItemEstados.DEFAULT + fullItemList.mapNotNull { it.estado.takeIf { s -> s.isNotBlank() } })
                .distinct().sortedBy { it.lowercase(Locale.getDefault()) }
            val statusList = arrayOf("Cualquiera") + estados.toTypedArray()
            val tags = tagRepo.getAllTagsOnce().sortedBy { it.nombre.lowercase(Locale.getDefault()) }
            val tagIds = intArrayOf(-1) + tags.map { it.id }.toIntArray()
            val tagNames = arrayOf("Cualquiera") + tags.map { it.nombre }.toTypedArray()
            ItemFilterSortBottomSheet
                .newInstance(categoryIds, categoryNames, statusList, tagIds, tagNames, filterSortState)
                .show(parentFragmentManager, "ItemFilterSortBottomSheet")
        }
    }

    // Aplica la búsqueda, filtros y ordenación al listado completo y actualiza el adaptador.
    private fun applyFiltersAndSort() {
        val query = searchQuery.trim().lowercase(Locale.getDefault())
        var list = fullItemList
        if (query.isNotBlank()) list = list.filter { it.titulo.lowercase(Locale.getDefault()).contains(query) }
        filterSortState.categoriaId?.let { cat -> list = list.filter { it.categoriaId == cat } }
        filterSortState.tagId?.let { tagId -> list = list.filter { tagIdsByItemId[it.id]?.contains(tagId) == true } }
        filterSortState.estado?.let { estado -> list = list.filter { it.estado.equals(estado, ignoreCase = true) } }
        if (filterSortState.minCalificacion > 0f) list = list.filter { it.calificacion >= filterSortState.minCalificacion }
        val baseComparator = when (filterSortState.sortField) {
            ItemSortField.NAME -> compareBy<Item> { it.titulo.lowercase(Locale.getDefault()) }
            ItemSortField.VALUE -> compareBy<Item> { it.valor }
            ItemSortField.DATE -> compareBy<Item> { it.fechaAdquisicion.time }
        }
        val primaryComparator = if (filterSortState.ascending) baseComparator else baseComparator.reversed()
        val comparator = compareByDescending<Item> { it.favorito }
            .then(primaryComparator)
            .thenBy { it.id }
        adapter.updateList(list.sortedWith(comparator))
    }

    // Actualiza el estado (habilitado/visibilidad) del FAB según existan categorías
    private fun updateFabState() {
        binding.fabAddItem.isEnabled = categoriasMap.isNotEmpty()
        binding.fabAddItem.alpha = if (categoriasMap.isNotEmpty()) 1f else 0.5f
        adapter.categoriasMap = categoriasMap
    }

    private fun showSnackbar(message: String) {
        // Helper corto para mostrar mensajes tipo Snackbar con anclaje al FAB
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.fabAddItem)
            .show()
    }

    private fun showItemImageSourceDialog() {
        // Muestra un diálogo para seleccionar la fuente de la imagen (galería o cámara)
        val options = arrayOf("Galeria", "Camara")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar imagen")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickItemImageLauncher.launch("image/*")
                    1 -> {
                        val cameraGranted = ContextCompat.checkSelfPermission(
                            requireContext(), Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                        val storageGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            ContextCompat.checkSelfPermission(
                                requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) == PackageManager.PERMISSION_GRANTED
                        } else {
                            true
                        }
                        if (cameraGranted && storageGranted) {
                            openCameraForItem()
                        } else {
                            val perms = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                            } else {
                                arrayOf(Manifest.permission.CAMERA)
                            }
                            cameraItemPermissionLauncher.launch(perms)
                        }
                    }
                }
            }
            .show()
    }

    private fun openCameraForItem() {
        // Abre la cámara creando una uri en la galería donde se guardará la foto
        val uri = createGalleryImageUri("item")
        cameraItemImageUri = uri
        takeItemImageLauncher.launch(uri)
    }

    // Crea una Uri en la galería / MediaStore para guardar una imagen nueva
    private fun createGalleryImageUri(prefix: String): Uri {
        val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
        val resolver = requireContext().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GestorColecciones")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val destDir = File(picturesDir, "GestorColecciones").also { it.mkdirs() }
                val file = File(destDir, fileName)
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
        }
        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("No se pudo crear la imagen en la galeria")
    }

    // Marca como no pendiente una imagen creada en MediaStore (API Q+)
    private fun finalizePendingImage(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = requireContext().contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, values, null, null)
        }
    }

    // Guarda un Bitmap en la galería y devuelve la Uri resultante
    private fun saveBitmapToGallery(bitmap: Bitmap, prefix: String): Uri? {
        val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
        val resolver = requireContext().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GestorColecciones")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val destDir = File(picturesDir, "GestorColecciones").also { it.mkdirs() }
                val file = File(destDir, fileName)
                put(MediaStore.Images.Media.DATA, file.absolutePath)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        finalizePendingImage(uri)
        return uri
    }

    // ── Tarjeta ───────────────────────────────────────────────────────────────

    private fun generarTarjeta() {
        // Genera una imagen (tarjeta) con los items más relevantes de la colección
        viewLifecycleOwner.lifecycleScope.launch {
            val coleccion = coleccionRepo.getById(collectionId) ?: return@launch
            val topItems = fullItemList.sortedByDescending { it.valor }.take(4)
            val file = TarjetaColeccionGenerator(requireContext()).generate(coleccion, topItems)

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Tarjeta generada ✅")
                .setMessage("¿Qué quieres hacer con la tarjeta de \"${coleccion.nombre}\"?")
                .setPositiveButton("Compartir") { _, _ -> compartirTarjeta(file) }
                .setNegativeButton("Guardar en galería") { _, _ -> guardarTarjetaEnGaleria(file) }
                .setNeutralButton("Cancelar", null)
                .show()
        }
    }

    private fun compartirTarjeta(file: File) {
        // Comparte la imagen usando un FileProvider para exponer la Uri temporalmente
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir tarjeta"))
    }

    private fun guardarTarjetaEnGaleria(file: File) {
        // Guarda la tarjeta generada en la galería del dispositivo (MediaStore o FS según API)
        try {
            val fileName = "tarjeta_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = requireContext().contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GestorColecciones")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { output -> file.inputStream().copyTo(output) }
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val destDir = File(picturesDir, "GestorColecciones").also { it.mkdirs() }
                file.copyTo(File(destDir, fileName), overwrite = true)
            }
            showSnackbar("✅ Tarjeta guardada en la galería")
        } catch (e: Exception) {
            showSnackbar("Error al guardar: ${e.message}")
        }
    }

    // ── Crear item ────────────────────────────────────────────────────────────

    private fun showCreateItemDialog() {
        // Preparar diálogo para creación de un nuevo item: campos, preview de imagen y validaciones
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
        btnSelectImage.setOnClickListener { showItemImageSourceDialog() }

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
            requireContext(), android.R.layout.simple_spinner_item, categoriasList.map { it.value }
        )
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapterSpinner


        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nuevo item").setView(view)
            .setPositiveButton("Crear", null).setNegativeButton("Cancelar", null).create()

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
                val posibleDuplicado = fullItemList.firstOrNull { it.titulo.trim().equals(titulo, ignoreCase = true) }
                val insertarItem = {
                    viewLifecycleOwner.lifecycleScope.launch {
                        val imagePath = selectedItemImageUri?.let { uploadImage(it) }
                        viewModel.insert(
                            Item(
                                titulo = titulo, categoriaId = categoriaId, collectionId = collectionId,
                                fechaAdquisicion = Date(), valor = valor,
                                imagenPath = imagePath,
                                estado = estado, descripcion = descripcion, calificacion = rbCalificacion.rating
                            ),
                            onInserted = { showSnackbar("Item \"$titulo\" creado") },
                            onError = { msg -> showSnackbar(msg) }
                        )
                        dialog.dismiss()
                    }
                }
                if (posibleDuplicado != null) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("⚠️ Posible duplicado")
                        .setMessage("Ya existe un item con un nombre similar:\n\n• Título: ${posibleDuplicado.titulo}\n• Estado: ${posibleDuplicado.estado}\n• Valor: ${"%.2f".format(posibleDuplicado.valor)} €\n\n¿Quieres añadirlo igualmente?")
                        .setPositiveButton("Añadir igualmente") { _, _ -> insertarItem() }
                        .setNegativeButton("Cancelar", null).show()
                } else insertarItem()
            }
        }
        dialog.show()
    }

    // ── Editar item ───────────────────────────────────────────────────────────
    private fun showEditItemDialog(item: Item) {
        // Preparar diálogo para editar un item existente, cargar valores y permitir cambiar imagen
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
        ImageUtils.toGlideModel(item.imagenPath)?.let { model ->
            Glide.with(requireContext()).load(model).into(ivPreview)
        }
        btnSelectImage.setOnClickListener { showItemImageSourceDialog() }

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
            requireContext(), android.R.layout.simple_spinner_item, categoriasList.map { it.value }
        )
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategoria.adapter = adapterSpinner
        val selectedIndex = categoriasList.indexOfFirst { it.key == item.categoriaId }
        if (selectedIndex >= 0) spinnerCategoria.setSelection(selectedIndex)


        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar item").setView(view)
            .setPositiveButton("Guardar", null).setNegativeButton("Cancelar", null).create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val titulo = etTitulo.text.toString().trim()
                if (titulo.isBlank()) {
                    Toast.makeText(requireContext(), "El título no puede estar vacío", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    val imagePath = selectedItemImageUri?.let { uploadImage(it) } ?: item.imagenPath
                    viewModel.update(
                        item.copy(
                            titulo = titulo,
                            valor = etValor.text.toString().toDoubleOrNull() ?: item.valor,
                            descripcion = etDescripcion.text.toString().takeIf { it.isNotBlank() },
                            categoriaId = categoriasList[spinnerCategoria.selectedItemPosition].key,
                            imagenPath = imagePath,
                            estado = actvEstado.text?.toString()?.trim().orEmpty().ifBlank { item.estado },
                            calificacion = rbCalificacion.rating
                        ),
                        onUpdated = { showSnackbar("Item \"$titulo\" actualizado") },
                        onError = { msg -> showSnackbar(msg) }
                    )
                    dialog.dismiss()
                }
            }
        }
        dialog.show()
    }

    // ── Categorías ────────────────────────────────────────────────────────────

    private fun showCreateCategoriaDialog() {
        // Muestra un diálogo para gestionar (crear/editar/eliminar) categorías
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_categoria, null)
        val lvCategorias = view.findViewById<ListView>(R.id.lvCategorias)
        val etCategoriaNombre = view.findViewById<EditText>(R.id.etCategoriaNombre)
        val btnAddCategoria = view.findViewById<Button>(R.id.btnAddCategoria)

        val adapterList = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categoriasMap.values.toMutableList())
        lvCategorias.adapter = adapterList

        btnAddCategoria.setOnClickListener {
            val nombre = etCategoriaNombre.text.toString().trim()
            if (nombre.isNotBlank()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val id = categoriaRepo.insert(Categoria(nombre = nombre)).toInt()
                    categoriasMap[id] = nombre
                    adapterList.clear(); adapterList.addAll(categoriasMap.values); adapterList.notifyDataSetChanged()
                    etCategoriaNombre.text.clear()
                    updateFabState()
                    showSnackbar("Categoría \"$nombre\" creada")
                }
            }
        }

        lvCategorias.setOnItemClickListener { _, _, position, _ ->
            val categoriaId = categoriasMap.keys.toList()[position]
            val nombreActual = categoriasMap[categoriaId]!!
            val editView = EditText(requireContext()).also { it.setText(nombreActual) }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Editar categoría").setView(editView)
                .setPositiveButton("Guardar") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        categoriaRepo.update(Categoria(id = categoriaId, nombre = editView.text.toString()))
                        categoriasMap[categoriaId] = editView.text.toString()
                        adapterList.clear(); adapterList.addAll(categoriasMap.values); adapterList.notifyDataSetChanged()
                        showSnackbar("Categoría actualizada")
                    }
                }
                .setNegativeButton("Eliminar") { _, _ ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        categoriaRepo.delete(Categoria(id = categoriaId, nombre = nombreActual))
                        categoriasMap.remove(categoriaId)
                        adapterList.clear(); adapterList.addAll(categoriasMap.values); adapterList.notifyDataSetChanged()
                        updateFabState()
                        showSnackbar("Categoría \"$nombreActual\" eliminada")
                    }
                }.show()
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Gestionar categorías").setView(view).setNegativeButton("Cerrar", null).show()
    }

    private suspend fun uploadImage(uri: Uri): String? {
        // Sube una imagen a la API remota y devuelve la URL resultante (o null en fallo)
        return try {
            val api = ApiProvider.getApi(requireContext())
            val part = UploadUtils.createImagePart(requireContext(), uri)
            api.uploadImage(part).url
        } catch (e: Exception) {
            showSnackbar("Error subiendo imagen")
            null
        }
    }

    private fun copyImageToInternalStorage(uri: Uri, filename: String): String {
        // Copia el contenido referenciado por la Uri al almacenamiento interno de la app
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val file = File(requireContext().filesDir, filename)
        inputStream.use { input -> FileOutputStream(file).use { output -> input?.copyTo(output) } }
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
        // Limpiar referencia al binding para evitar fugas de memoria
        _binding = null
    }
}

