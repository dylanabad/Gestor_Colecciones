package com.example.gestor_colecciones.fragment

import android.content.ContentValues
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.Manifest
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import com.google.android.material.transition.MaterialSharedAxis
import com.bumptech.glide.Glide
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.adapters.ColeccionAdapter
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentColeccionesBinding
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.model.ColeccionColors
import com.example.gestor_colecciones.model.LogroDefinicion
import com.example.gestor_colecciones.model.LogroManager
import com.example.gestor_colecciones.auth.AuthStore
import com.example.gestor_colecciones.repository.ExportRepository
import com.example.gestor_colecciones.repository.LogroRepository
import com.example.gestor_colecciones.repository.RepositoryProvider
import com.example.gestor_colecciones.repository.PrestamoRepository
import com.example.gestor_colecciones.util.ImageUtils
import com.example.gestor_colecciones.network.UploadUtils
import com.example.gestor_colecciones.viewmodel.*
import com.example.gestor_colecciones.network.ApiProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import com.example.gestor_colecciones.activities.MainActivity
import androidx.fragment.app.FragmentManager

class ColeccionesFragment : Fragment() {

    private var _binding: FragmentColeccionesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ColeccionViewModel
    private lateinit var itemViewModel: ItemViewModel
    private lateinit var exportViewModel: ExportViewModel
    private lateinit var logroViewModel: LogroViewModel
    private lateinit var adapter: ColeccionAdapter
    private var listaCompleta: List<Coleccion> = emptyList()
    private var statsMap: MutableMap<Int, String> = mutableMapOf()
    private var statsJob: Job? = null

    private var selectedImageUri: Uri? = null
    private var currentImageView: ImageView? = null
    private var cameraImageUri: Uri? = null

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            currentImageView?.setImageURI(it)
        }
    }

    private val takeImageLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let {
                selectedImageUri = it
                currentImageView?.setImageURI(it)
                finalizePendingImage(it)
            }
        } else {
            takeImagePreviewLauncher.launch(null)
        }
    }

    private val takeImagePreviewLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val uri = saveBitmapToGallery(bitmap, "coleccion")
            if (uri != null) {
                selectedImageUri = uri
                currentImageView?.setImageURI(uri)
            } else {
                showSnackbar("No se pudo guardar la foto")
            }
        } else {
            showSnackbar("No se pudo capturar la foto")
        }
    }

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val cameraGranted = grants[Manifest.permission.CAMERA] == true
        val storageGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            grants[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        } else {
            true
        }
        if (cameraGranted && storageGranted) {
            openCameraForCollection()
        } else {
            showSnackbar("Permiso de camara/almacenamiento denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough().apply { duration = 220 }
        returnTransition = MaterialFadeThrough().apply { duration = 200 }
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply { duration = 240 }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply { duration = 240 }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColeccionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repo = RepositoryProvider.coleccionRepository(requireContext())
        viewModel = ViewModelProvider(this, ColeccionViewModelFactory(repo))[ColeccionViewModel::class.java]

        val itemRepo = RepositoryProvider.itemRepository(requireContext())
        itemViewModel = ViewModelProvider(this, ItemViewModelFactory(itemRepo))[ItemViewModel::class.java]

        val exportRepo = ExportRepository(repo, itemRepo)
        exportViewModel = ViewModelProvider(
            this, ExportViewModelFactory(exportRepo)
        )[ExportViewModel::class.java]

        val logroRepo = LogroRepository(
            DatabaseProvider.getDatabase(requireContext()).logroDao(),
            ApiProvider.getApi(requireContext())
        )
        val prestamoRepo = PrestamoRepository(ApiProvider.getApi(requireContext()))
        val logroManager = LogroManager(logroRepo, repo, itemRepo, prestamoRepo)
        logroViewModel = ViewModelProvider(
            this, LogroViewModelFactory(logroRepo, logroManager)
        )[LogroViewModel::class.java]

        adapter = ColeccionAdapter(
            emptyList(),
            onClick = { coleccion ->
                val fragment = ItemListByCollectionFragment.newInstance(coleccion.id)
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace((view.parent as ViewGroup).id, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onLongClick = { coleccion -> showEditCollectionDialog(coleccion) },
            coleccionStats = statsMap
        )

        binding.rvColecciones.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvColecciones.adapter = adapter
        binding.rvColecciones.itemAnimator = DefaultItemAnimator().apply {
            supportsChangeAnimations = false
            addDuration = 180
            removeDuration = 160
            moveDuration = 180
            changeDuration = 160
        }
        binding.rvColecciones.addItemDecoration(GridSpacingItemDecoration(2, 16, true))

        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val coleccion = adapter.getItem(viewHolder.adapterPosition)
                viewLifecycleOwner.lifecycleScope.launch {
                    val papeleraRepo = RepositoryProvider.papeleraRepository(requireContext())
                    papeleraRepo.moverColeccionAPapelera(coleccion)
                    Snackbar.make(binding.root, "\"${coleccion.nombre}\" movida a la papelera", Snackbar.LENGTH_LONG)
                        .setAction("Ver papelera") {
                            parentFragmentManager.beginTransaction()
                                .setReorderingAllowed(true)
                                .replace((view.parent as ViewGroup).id, PapeleraFragment())
                                .addToBackStack(null)
                                .commit()
                        }
                        .setAnchorView(binding.fabAddColeccion)
                        .show()
                }
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvColecciones)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.colecciones.collectLatest { lista ->
                listaCompleta = lista
                updateStatsAndAdapter()
                logroViewModel.checkLogros()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            logroViewModel.nuevoLogro.collect { key ->
                LogroDefinicion.getInfo(key)?.let { info ->
                    showSnackbar("🏅 Logro desbloqueado: ${info.titulo}")
                    (requireActivity() as? MainActivity)?.lanzarConfeti()
                }
            }
        }

        binding.fabAddColeccion.setOnClickListener { showCreateCollectionDialog() }
        binding.fabExport.setOnClickListener { showExportDialog() }

        binding.btnBusqueda.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace((view.parent as ViewGroup).id, BusquedaFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnDeseos.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace((view.parent as ViewGroup).id, DeseosFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnStats.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace((view.parent as ViewGroup).id, StatsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnLogros.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace((view.parent as ViewGroup).id, LogrosFragment())
                .addToBackStack(null)
                .commit()
        }

        // ← NUEVO
        binding.btnPrestamos.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace((view.parent as ViewGroup).id, PrestamosFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnPapelera.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace((view.parent as ViewGroup).id, PapeleraFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnLogout.setOnClickListener { button ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Quieres cerrar la sesión y borrar los datos locales?")
                .setPositiveButton("Cerrar sesión") { _, _ ->
                    button.isEnabled = false
                    viewLifecycleOwner.lifecycleScope.launch {
                        AuthStore(requireContext()).clear()
                        withContext(Dispatchers.IO) {
                            DatabaseProvider.getDatabase(requireContext()).clearAllTables()
                        }
                        parentFragmentManager.popBackStack(
                            null,
                            FragmentManager.POP_BACK_STACK_INCLUSIVE
                        )
                        parentFragmentManager.beginTransaction()
                            .setReorderingAllowed(true)
                            .replace((view.parent as ViewGroup).id, AuthFragment())
                            .commit()
                        button.isEnabled = true
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            exportViewModel.exportState.collectLatest { state ->
                when (state) {
                    is ExportState.Loading -> {
                        binding.fabExport.isEnabled = false
                    }
                    is ExportState.Success -> {
                        binding.fabExport.isEnabled = true
                        if (state.share) shareFile(state.file)
                        else saveFileToDownloads(state.file)
                        exportViewModel.resetState()
                    }
                    is ExportState.Error -> {
                        binding.fabExport.isEnabled = true
                        showSnackbar("Error al exportar: ${state.message}")
                        exportViewModel.resetState()
                    }
                    else -> {
                        binding.fabExport.isEnabled = true
                    }
                }
            }
        }

        binding.searchColecciones.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val texto = newText ?: ""
                val filtradas = listaCompleta.filter {
                    it.nombre.lowercase().contains(texto.lowercase())
                }
                updateStatsAndAdapter(filtradas)
                return true
            }
        })
    }

    private fun showExportDialog() {
        if (listaCompleta.isEmpty()) {
            showSnackbar("No hay colecciones para exportar")
            return
        }

        val nombres = listaCompleta.map { it.nombre }.toTypedArray()
        val seleccionadas = BooleanArray(listaCompleta.size) { true }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar colecciones")
            .setMultiChoiceItems(nombres, seleccionadas) { _, which, isChecked ->
                seleccionadas[which] = isChecked
            }
            .setPositiveButton("Siguiente") { _, _ ->
                val idsSeleccionados = listaCompleta
                    .filterIndexed { index, _ -> seleccionadas[index] }
                    .map { it.id }
                if (idsSeleccionados.isEmpty()) {
                    showSnackbar("Selecciona al menos una colección")
                    return@setPositiveButton
                }
                showExportFormatDialog(idsSeleccionados)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showExportFormatDialog(ids: List<Int>) {
        val opciones = arrayOf(
            "Guardar Catálogo PDF en Descargas",
            "Compartir Catálogo PDF",
            "Guardar CSV en Descargas",
            "Compartir CSV"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Exportar como")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> exportViewModel.exportCatalogoPdf(requireContext(), share = false, ids = ids)
                    1 -> exportViewModel.exportCatalogoPdf(requireContext(), share = true, ids = ids)
                    2 -> exportViewModel.exportCsv(requireContext(), share = false, ids = ids)
                    3 -> exportViewModel.exportCsv(requireContext(), share = true, ids = ids)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun shareFile(file: File) {
        val mimeType = if (file.extension == "pdf") "application/pdf" else "text/csv"
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Compartir catálogo"))
    }

    private fun saveFileToDownloads(file: File) {
        val mimeType = if (file.extension == "pdf") "application/pdf" else "text/csv"
        val fileName = "catalogo_export_${System.currentTimeMillis()}.${file.extension}"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = requireContext().contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, mimeType)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                uri?.let {
                    resolver.openOutputStream(it)?.use { output ->
                        file.inputStream().copyTo(output)
                    }
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
            } else {
                val downloadsDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                file.copyTo(File(downloadsDir, fileName), overwrite = true)
            }
            showSnackbar("Guardado en Descargas: $fileName")
        } catch (e: Exception) {
            showSnackbar("Error al guardar: ${e.message}")
        }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.fabAddColeccion)
            .show()
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Galeria", "Camara")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar imagen")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
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
                            openCameraForCollection()
                        } else {
                            val perms = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                )
                            } else {
                                arrayOf(Manifest.permission.CAMERA)
                            }
                            cameraPermissionLauncher.launch(perms)
                        }
                    }
                }
            }
            .show()
    }

    private fun openCameraForCollection() {
        val uri = createGalleryImageUri("coleccion")
        cameraImageUri = uri
        takeImageLauncher.launch(uri)
    }

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

    private fun finalizePendingImage(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = requireContext().contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, values, null, null)
        }
    }

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

    private fun copyImageToInternalStorage(uri: Uri, fileName: String): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val file = File(requireContext().filesDir, fileName)
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun updateStatsAndAdapter(filteredList: List<Coleccion>? = null) {
        val lista = filteredList ?: listaCompleta
        adapter.updateList(lista)
        statsJob?.cancel()
        statsMap.clear()
        statsJob = viewLifecycleOwner.lifecycleScope.launch {
            coroutineScope {
                lista.forEach { coleccion ->
                    launch {
                        itemViewModel.getItemsByCollection(coleccion.id).collectLatest { items ->
                            statsMap[coleccion.id] =
                                "${items.size} items · Valor: ${items.sumOf { it.valor }}€"
                            adapter.notifyStatsChangedFor(coleccion.id)
                        }
                    }
                }
            }
        }
    }

    private fun showCreateCollectionDialog() {
        selectedImageUri = null
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_collection, null)

        val etNombre = view.findViewById<EditText>(R.id.etNombre)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        val ivPreview = view.findViewById<ImageView>(R.id.ivPreview)
        val btnSelectImage = view.findViewById<Button>(R.id.btnSelectImage)
        val chipGroupColors = view.findViewById<ChipGroup>(R.id.chipGroupCollectionColors)

        currentImageView = ivPreview
        btnSelectImage.setOnClickListener { showImageSourceDialog() }

        var selectedColor = 0
        setupCollectionColorChips(chipGroupColors, selectedColor) { selectedColor = it }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva colección")
            .setView(view)
            .setPositiveButton("Crear", null)
            .setNegativeButton("Cancelar") { _, _ ->
                selectedImageUri = null
                currentImageView = null
            }
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val nombre = etNombre.text.toString().trim()
                        if (nombre.isBlank()) {
                            Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val descripcion = etDescripcion.text.toString()

                        val posibleDuplicado = listaCompleta.firstOrNull { coleccion ->
                            coleccion.nombre.trim().equals(nombre, ignoreCase = true)
                        }

                        val insertarColeccion = {
                            viewLifecycleOwner.lifecycleScope.launch {
                                val imagePath = selectedImageUri?.let { uri -> uploadImage(uri) }
                                viewModel.insert(
                                    Coleccion(
                                        nombre = nombre,
                                        descripcion = descripcion,
                                        fechaCreacion = Date(),
                                        imagenPath = imagePath,
                                        color = selectedColor
                                    )
                                )
                                showSnackbar("Colección \"$nombre\" creada")
                            }
                            selectedImageUri = null
                            currentImageView = null
                            dialog.dismiss()
                        }

                        if (posibleDuplicado != null) {
                            MaterialAlertDialogBuilder(requireContext())
                                .setTitle("⚠️ Posible duplicado")
                                .setMessage(
                                    "Ya existe una colección con un nombre similar:\n\n" +
                                            "• Nombre: ${posibleDuplicado.nombre}\n" +
                                            "• Descripción: ${posibleDuplicado.descripcion?.ifBlank { "Sin descripción" } ?: "Sin descripción"}\n" +
                                            "• Items: ${listaCompleta.indexOf(posibleDuplicado) + 1}\n\n" +
                                            "¿Quieres crearla igualmente?"
                                )
                                .setPositiveButton("Crear igualmente") { _, _ -> insertarColeccion() }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        } else {
                            insertarColeccion()
                        }
                    }
                }
                dialog.show()
            }
    }

    private fun showEditCollectionDialog(coleccion: Coleccion) {
        selectedImageUri = null
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_collection, null)

        val etNombre = view.findViewById<EditText>(R.id.etNombre)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        val ivPreview = view.findViewById<ImageView>(R.id.ivPreview)
        val btnSelectImage = view.findViewById<Button>(R.id.btnSelectImage)
        val chipGroupColors = view.findViewById<ChipGroup>(R.id.chipGroupCollectionColors)

        etNombre.setText(coleccion.nombre)
        etDescripcion.setText(coleccion.descripcion)
        ImageUtils.toGlideModel(coleccion.imagenPath)?.let { model ->
            Glide.with(this).load(model).into(ivPreview)
        }

        currentImageView = ivPreview
        btnSelectImage.setOnClickListener { showImageSourceDialog() }

        var selectedColor = coleccion.color
        setupCollectionColorChips(chipGroupColors, selectedColor) { selectedColor = it }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar colección")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    val imagePath = selectedImageUri?.let { uri -> uploadImage(uri) } ?: coleccion.imagenPath
                    viewModel.update(
                        coleccion.copy(
                            nombre = etNombre.text.toString().trim(),
                            descripcion = etDescripcion.text.toString(),
                            imagenPath = imagePath,
                            color = selectedColor
                        )
                    )
                    showSnackbar("Colección \"${coleccion.nombre}\" actualizada")
                }
                selectedImageUri = null
                currentImageView = null
            }
            .setNegativeButton("Cancelar") { _, _ ->
                selectedImageUri = null
                currentImageView = null
            }
            .show()
    }

    private fun setupCollectionColorChips(
        chipGroup: ChipGroup,
        initialColor: Int,
        onSelected: (Int) -> Unit
    ) {
        chipGroup.removeAllViews()
        chipGroup.isSingleSelection = true

        fun addChip(label: String, color: Int, isDefault: Boolean = false) {
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                if (isDefault) {
                    chipBackgroundColor = ColorStateList.valueOf(0xFFECEFF1.toInt())
                    chipStrokeWidth = 1f
                    chipStrokeColor = ColorStateList.valueOf(0xFFB0BEC5.toInt())
                    setTextColor(0xFF263238.toInt())
                } else {
                    chipBackgroundColor = ColorStateList.valueOf(color)
                    setTextColor(0xFFFFFFFF.toInt())
                }
                id = View.generateViewId()
                setOnCheckedChangeListener { _, checked -> if (checked) onSelected(color) }
            }
            chipGroup.addView(chip)
            if (color == initialColor) chip.isChecked = true
            if (initialColor == 0 && isDefault) chip.isChecked = true
        }

        addChip("Default", 0, isDefault = true)
        ColeccionColors.PALETTE.forEach { option -> addChip(option.name, option.color) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount
        if (includeEdge) {
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            if (position < spanCount) outRect.top = spacing
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) outRect.top = spacing
        }
    }
}
