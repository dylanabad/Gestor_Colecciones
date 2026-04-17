package com.example.gestor_colecciones.fragment

// Imports de Android, Material, Glide, Room, Coroutines, etc.
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
import androidx.appcompat.app.AppCompatActivity
import com.example.gestor_colecciones.util.ThemeManager
import com.example.gestor_colecciones.util.ThemePalette
import com.example.gestor_colecciones.util.ThemeMode
import com.example.gestor_colecciones.util.ThemePrefs
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

/**
 * Fragment principal de la app.
 * Muestra la lista de colecciones del usuario y actúa como hub
 * de navegación hacia el resto de secciones (búsqueda, logros,
 * préstamos, papelera, estadísticas, etc.).
 */
class ColeccionesFragment : Fragment() {

    // View Binding: referencia nula fuera del ciclo de vida de la vista
    private var _binding: FragmentColeccionesBinding? = null
    private val binding get() = _binding!!

    // ViewModels que gestionan la lógica de negocio de cada área
    private lateinit var viewModel: ColeccionViewModel       // CRUD de colecciones
    private lateinit var itemViewModel: ItemViewModel        // Items dentro de cada colección
    private lateinit var exportViewModel: ExportViewModel    // Exportación a PDF/CSV
    private lateinit var logroViewModel: LogroViewModel      // Sistema de logros/achievements

    private lateinit var adapter: ColeccionAdapter

    // Copia completa de la lista para poder filtrar sin perder datos
    private var listaCompleta: List<Coleccion> = emptyList()

    // Mapa que guarda el texto de estadísticas por ID de colección (ej: "5 items · Valor: 120€")
    private var statsMap: MutableMap<Int, String> = mutableMapOf()

    // Job para cancelar el cálculo de estadísticas si la lista cambia antes de terminar
    private var statsJob: Job? = null

    // URI de la imagen seleccionada pendiente de subir al crear/editar colección
    private var selectedImageUri: Uri? = null

    // ImageView del diálogo activo donde se mostrará la preview de la imagen
    private var currentImageView: ImageView? = null

    // URI temporal usada al tomar foto con la cámara (antes de confirmar)
    private var cameraImageUri: Uri? = null

    // ── Launchers de Activity Result ──────────────────────────────────────────

    /** Abre la galería del dispositivo para seleccionar una imagen. */
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            currentImageView?.setImageURI(it) // Muestra preview inmediata en el diálogo
        }
    }

    /**
     * Lanza la cámara para tomar una foto de alta resolución.
     * Si falla (el dispositivo no soporta ACTION_IMAGE_CAPTURE con URI),
     * cae al launcher de preview como fallback.
     */
    private val takeImageLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraImageUri?.let {
                selectedImageUri = it
                currentImageView?.setImageURI(it)
                finalizePendingImage(it) // Marca IS_PENDING=0 en MediaStore (Android 10+)
            }
        } else {
            // Fallback: captura de baja resolución como Bitmap
            takeImagePreviewLauncher.launch(null)
        }
    }

    /**
     * Fallback de cámara: recibe un Bitmap de baja resolución
     * y lo guarda manualmente en la galería.
     */
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

    /**
     * Solicita los permisos necesarios para usar la cámara.
     * En Android < Q también necesita WRITE_EXTERNAL_STORAGE.
     */
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val cameraGranted = grants[Manifest.permission.CAMERA] == true
        val storageGranted = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            grants[Manifest.permission.WRITE_EXTERNAL_STORAGE] == true
        } else {
            true // Android 10+ no necesita este permiso para MediaStore
        }
        if (cameraGranted && storageGranted) {
            openCameraForCollection()
        } else {
            showSnackbar("Permiso de camara/almacenamiento denegado")
        }
    }

    // ── Ciclo de vida ─────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transiciones de Material Design al entrar/salir del fragment
        enterTransition = MaterialFadeThrough().apply { duration = 220 }
        returnTransition = MaterialFadeThrough().apply { duration = 200 }
        // Desplazamiento horizontal al navegar hacia un hijo (ej: lista de items)
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply { duration = 240 }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply { duration = 240 }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColeccionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Inicialización de ViewModels ──────────────────────────────────────

        val repo = RepositoryProvider.coleccionRepository(requireContext())
        viewModel = ViewModelProvider(this, ColeccionViewModelFactory(repo))[ColeccionViewModel::class.java]

        val itemRepo = RepositoryProvider.itemRepository(requireContext())
        itemViewModel = ViewModelProvider(this, ItemViewModelFactory(itemRepo))[ItemViewModel::class.java]

        // ExportRepository combina colecciones e items para generar el archivo
        val exportRepo = ExportRepository(repo, itemRepo)
        exportViewModel = ViewModelProvider(
            this, ExportViewModelFactory(exportRepo)
        )[ExportViewModel::class.java]

        // LogroManager necesita varios repositorios para evaluar las condiciones de cada logro
        val logroRepo = LogroRepository(
            DatabaseProvider.getDatabase(requireContext()).logroDao(),
            ApiProvider.getApi(requireContext())
        )
        val prestamoRepo = RepositoryProvider.prestamoRepository(requireContext())
        val logroManager = LogroManager(logroRepo, repo, itemRepo, prestamoRepo)
        logroViewModel = ViewModelProvider(
            this, LogroViewModelFactory(logroRepo, logroManager)
        )[LogroViewModel::class.java]

        // ── Configuración del RecyclerView ────────────────────────────────────

        adapter = ColeccionAdapter(
            emptyList(),
            onClick = { coleccion ->
                // Al pulsar una colección, navega a su lista de items
                val fragment = ItemListByCollectionFragment.newInstance(coleccion.id)
                parentFragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .replace((view.parent as ViewGroup).id, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onLongClick = { coleccion -> showEditCollectionDialog(coleccion) }, // Editar con long press
            coleccionStats = statsMap
        )

        // Cuadrícula de 2 columnas para que las celdas sean más grandes y legibles
        binding.rvColecciones.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvColecciones.adapter = adapter

        // Animaciones suaves; se desactivan las de cambio para evitar parpadeos al actualizar stats
        binding.rvColecciones.itemAnimator = DefaultItemAnimator().apply {
            supportsChangeAnimations = false
            addDuration = 180
            removeDuration = 160
            moveDuration = 180
            changeDuration = 160
        }

        // Espaciado uniforme entre celdas del grid (2 columnas, 16dp de espacio para un look más aireado)
        binding.rvColecciones.addItemDecoration(GridSpacingItemDecoration(2, 16, true))

        // Swipe a la izquierda → mover colección a la papelera
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val coleccion = adapter.getItem(viewHolder.adapterPosition)
                viewLifecycleOwner.lifecycleScope.launch {
                    val papeleraRepo = RepositoryProvider.papeleraRepository(requireContext())
                    papeleraRepo.moverColeccionAPapelera(coleccion)
                    // Snackbar con acceso directo a la papelera
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

        // ── Observadores de Flow ──────────────────────────────────────────────

        // Observa cambios en la lista de colecciones y actualiza el adapter + comprueba logros
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.colecciones.collectLatest { lista ->
                listaCompleta = lista
                updateStatsAndAdapter()
                logroViewModel.checkLogros() // Evalúa si se ha desbloqueado algún logro nuevo
            }
        }

        // Muestra un Snackbar y lanza confeti cuando se desbloquea un logro
        viewLifecycleOwner.lifecycleScope.launch {
            logroViewModel.nuevoLogro.collect { key ->
                LogroDefinicion.getInfo(key)?.let { info ->
                    showSnackbar("🏅 Logro desbloqueado: ${info.titulo}")
                    (requireActivity() as? MainActivity)?.lanzarConfeti()
                }
            }
        }

        // ── Listeners de botones de navegación ───────────────────────────────

        binding.fabAddColeccion.setOnClickListener { showCreateCollectionDialog() }
        binding.fabExport.setOnClickListener { showExportDialog() }

        // Cada botón reemplaza el fragment actual por la sección correspondiente
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

        binding.btnProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace((view.parent as ViewGroup).id, PerfilFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnSettings.setOnClickListener {
            showThemeMenu()
        }

        // Cierre de sesión: limpia AuthStore y borra toda la base de datos local
        binding.btnLogout.setOnClickListener { button ->
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Cerrar sesión")
                .setMessage("¿Quieres cerrar la sesión y borrar los datos locales?")
                .setPositiveButton("Cerrar sesión") { _, _ ->
                    button.isEnabled = false // Evita doble pulsación durante la operación
                    viewLifecycleOwner.lifecycleScope.launch {
                        AuthStore(requireContext()).clear()
                        withContext(Dispatchers.IO) {
                            // clearAllTables() es una operación de Room, se ejecuta en IO
                            DatabaseProvider.getDatabase(requireContext()).clearAllTables()
                        }
                        // Limpia el back stack completo y navega al login
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

        // Observa el estado de exportación para habilitar/deshabilitar el FAB y mostrar errores
        viewLifecycleOwner.lifecycleScope.launch {
            exportViewModel.exportState.collectLatest { state ->
                when (state) {
                    is ExportState.Loading -> {
                        binding.fabExport.isEnabled = false // Bloquea mientras se genera el archivo
                    }
                    is ExportState.Success -> {
                        binding.fabExport.isEnabled = true
                        if (state.share) shareFile(state.file)   // Comparte con otras apps
                        else saveFileToDownloads(state.file)      // Guarda en Descargas
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

        // Filtro de búsqueda en tiempo real sobre listaCompleta
        binding.searchColecciones.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val texto = newText ?: ""
                val filtradas = listaCompleta.filter {
                    it.nombre.lowercase().contains(texto.lowercase())
                }
                updateStatsAndAdapter(filtradas) // Actualiza el adapter solo con las coincidencias
                return true
            }
        })
    }

    private fun showThemeMenu() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_theme_selector, null)
        val paletteContainer = dialogView.findViewById<LinearLayout>(R.id.paletteContainer)
        val toggleMode = dialogView.findViewById<com.google.android.material.button.MaterialButtonToggleGroup>(R.id.toggleMode)
        
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .create()

        // 1. Configurar Selector de Paleta (Círculos)
        val currentPalette = ThemePrefs.getPalette(requireContext())
        ThemePalette.entries.forEach { palette ->
            val colorView = View(requireContext()).apply {
                val size = (48 * resources.displayMetrics.density).toInt()
                layoutParams = LinearLayout.LayoutParams(size, size).apply {
                    setMargins(16, 8, 16, 8)
                }
                
                // Fondo circular con borde si está seleccionado
                val shape = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(palette.primaryColor)
                    if (palette == currentPalette) {
                        setStroke(8, ContextCompat.getColor(context, android.R.color.white))
                    } else {
                        setStroke(2, 0x44000000)
                    }
                }
                background = shape
                elevation = 4f
                
                setOnClickListener {
                    if (palette != ThemePrefs.getPalette(context)) {
                        ThemeManager.updatePalette(requireActivity() as AppCompatActivity, palette)
                        dialog.dismiss()
                    }
                }
            }
            paletteContainer.addView(colorView)
        }

        // 2. Configurar Selector de Modo
        val currentMode = ThemePrefs.getMode(requireContext())
        when (currentMode) {
            ThemeMode.LIGHT -> toggleMode.check(R.id.btnModeLight)
            ThemeMode.DARK -> toggleMode.check(R.id.btnModeDark)
            ThemeMode.SYSTEM -> toggleMode.check(R.id.btnModeSystem)
        }

        toggleMode.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val newMode = when (checkedId) {
                    R.id.btnModeLight -> ThemeMode.LIGHT
                    R.id.btnModeDark -> ThemeMode.DARK
                    else -> ThemeMode.SYSTEM
                }
                if (newMode != ThemePrefs.getMode(requireContext())) {
                    ThemeManager.updateMode(requireActivity() as AppCompatActivity, newMode)
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    // ── Exportación ───────────────────────────────────────────────────────────

    /** Muestra un diálogo para seleccionar qué colecciones exportar (multi-selección). */
    private fun showExportDialog() {
        if (listaCompleta.isEmpty()) {
            showSnackbar("No hay colecciones para exportar")
            return
        }

        val nombres = listaCompleta.map { it.nombre }.toTypedArray()
        val seleccionadas = BooleanArray(listaCompleta.size) { true } // Todas marcadas por defecto

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
                showExportFormatDialog(idsSeleccionados) // Pasa al siguiente paso: elegir formato
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    /** Segundo paso de exportación: elige entre PDF o CSV, y guardar o compartir. */
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

    /** Comparte un archivo generado usando un Intent estándar con FileProvider. */
    private fun shareFile(file: File) {
        val mimeType = if (file.extension == "pdf") "application/pdf" else "text/csv"
        // FileProvider es necesario para exponer rutas internas a otras apps de forma segura
        val uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Permiso de lectura temporal
        }
        startActivity(Intent.createChooser(intent, "Compartir catálogo"))
    }

    /**
     * Guarda el archivo exportado en la carpeta Descargas del dispositivo.
     * Usa MediaStore en Android 10+ para no necesitar permisos de escritura.
     */
    private fun saveFileToDownloads(file: File) {
        val mimeType = if (file.extension == "pdf") "application/pdf" else "text/csv"
        val fileName = "catalogo_export_${System.currentTimeMillis()}.${file.extension}"
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+: inserta en MediaStore con IS_PENDING para escritura atómica
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
                    // Marca como listo para que sea visible en el explorador de archivos
                    contentValues.clear()
                    contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                    resolver.update(it, contentValues, null, null)
                }
            } else {
                // Android < 10: copia directamente al directorio público de Descargas
                val downloadsDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                file.copyTo(File(downloadsDir, fileName), overwrite = true)
            }
            showSnackbar("Guardado en Descargas: $fileName")
        } catch (e: Exception) {
            showSnackbar("Error al guardar: ${e.message}")
        }
    }

    // ── Utilidades de UI ──────────────────────────────────────────────────────

    /** Muestra un Snackbar anclado al FAB principal para no tapar contenido importante. */
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.fabAddColeccion)
            .show()
    }

    /** Diálogo para elegir entre galería o cámara al seleccionar imagen de colección. */
    private fun showImageSourceDialog() {
        val options = arrayOf("Galeria", "Camara")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Seleccionar imagen")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageLauncher.launch("image/*")
                    1 -> {
                        // Comprueba permisos antes de abrir la cámara
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
                            // Solicita los permisos que falten
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

    /**
     * Crea una URI en MediaStore y lanza la cámara apuntando a ella.
     * Esto permite capturar fotos de alta resolución sin pasar por el clipboard de bitmaps.
     */
    private fun openCameraForCollection() {
        val uri = createGalleryImageUri("coleccion")
        cameraImageUri = uri // Guardamos la URI para recuperarla en el callback
        takeImageLauncher.launch(uri)
    }

    /**
     * Crea una entrada vacía en MediaStore para que la cámara escriba la imagen directamente.
     * En Android < Q usa la ruta de fichero absoluta como fallback.
     */
    private fun createGalleryImageUri(prefix: String): Uri {
        val fileName = "${prefix}_${System.currentTimeMillis()}.jpg"
        val resolver = requireContext().contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GestorColecciones")
                put(MediaStore.Images.Media.IS_PENDING, 1) // Reserva el slot hasta que la cámara termine
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

    /**
     * Marca la imagen como IS_PENDING=0 en MediaStore para que sea visible
     * en la galería y otras apps. Solo necesario en Android 10+.
     */
    private fun finalizePendingImage(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = requireContext().contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            resolver.update(uri, values, null, null)
        }
    }

    /**
     * Guarda un Bitmap en la galería del dispositivo como JPEG con calidad 92.
     * Se usa como fallback cuando la cámara devuelve un thumbnail en lugar de la foto completa.
     */
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
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out) // 92% de calidad: buen equilibrio tamaño/calidad
        }
        finalizePendingImage(uri)
        return uri
    }

    /**
     * Sube la imagen seleccionada al servidor y devuelve la URL remota.
     * Retorna null si falla, mostrando un Snackbar de error.
     */
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

    /**
     * Copia una imagen al almacenamiento interno privado de la app.
     * Útil si se quiere trabajar con la imagen sin depender de permisos externos.
     * (Actualmente no se usa en el flujo principal; se prefiere uploadImage.)
     */
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

    /**
     * Actualiza el adapter con la lista recibida (o la completa si no se pasa ninguna)
     * y recalcula las estadísticas de cada colección en paralelo con coroutines.
     * Cancela el Job anterior para evitar acumulación de collectors activos.
     */
    private fun updateStatsAndAdapter(filteredList: List<Coleccion>? = null) {
        val lista = filteredList ?: listaCompleta
        adapter.updateList(lista)
        statsJob?.cancel() // Cancela cálculos previos que ya no son relevantes
        statsMap.clear()
        statsJob = viewLifecycleOwner.lifecycleScope.launch {
            coroutineScope {
                // Lanza un collector por colección en paralelo para mayor rendimiento
                lista.forEach { coleccion ->
                    launch {
                        itemViewModel.getItemsByCollection(coleccion.id).collectLatest { items ->
                            statsMap[coleccion.id] =
                                "${items.size} items · Valor: ${items.sumOf { it.valor }}€"
                            adapter.notifyStatsChangedFor(coleccion.id) // Refresca solo esa celda
                        }
                    }
                }
            }
        }
    }

    // ── Diálogos de creación/edición ──────────────────────────────────────────

    /** Muestra el diálogo para crear una nueva colección con nombre, descripción, imagen y color. */
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

        var selectedColor = 0 // 0 = color por defecto (sin color personalizado)
        setupCollectionColorChips(chipGroupColors, selectedColor) { selectedColor = it }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva colección")
            .setView(view)
            .setPositiveButton("Crear", null) // null para poder sobreescribir el comportamiento
            .setNegativeButton("Cancelar") { _, _ ->
                selectedImageUri = null
                currentImageView = null
            }
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    // Sobreescribimos el botón positivo para evitar que cierre el diálogo
                    // automáticamente si hay errores de validación
                    dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val nombre = etNombre.text.toString().trim()
                        if (nombre.isBlank()) {
                            Toast.makeText(requireContext(), "El nombre no puede estar vacío", Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val descripcion = etDescripcion.text.toString()

                        // Busca si ya existe una colección con nombre similar (case-insensitive)
                        val posibleDuplicado = listaCompleta.firstOrNull { coleccion ->
                            coleccion.nombre.trim().equals(nombre, ignoreCase = true)
                        }

                        // Lambda que ejecuta la inserción real en la base de datos
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
                            // Avisa al usuario del posible duplicado antes de crear
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

    /** Muestra el diálogo de edición precargando los datos actuales de la colección. */
    private fun showEditCollectionDialog(coleccion: Coleccion) {
        selectedImageUri = null
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_collection, null) // Reutiliza el mismo layout

        val etNombre = view.findViewById<EditText>(R.id.etNombre)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        val ivPreview = view.findViewById<ImageView>(R.id.ivPreview)
        val btnSelectImage = view.findViewById<Button>(R.id.btnSelectImage)
        val chipGroupColors = view.findViewById<ChipGroup>(R.id.chipGroupCollectionColors)

        // Precarga los valores actuales en los campos
        etNombre.setText(coleccion.nombre)
        etDescripcion.setText(coleccion.descripcion)
        // Carga la imagen actual con Glide (soporta URLs remotas y rutas locales)
        ImageUtils.toGlideModel(coleccion.imagenPath)?.let { model ->
            Glide.with(this).load(model).into(ivPreview)
        }

        currentImageView = ivPreview
        btnSelectImage.setOnClickListener { showImageSourceDialog() }

        var selectedColor = coleccion.color // Restaura el color actual
        setupCollectionColorChips(chipGroupColors, selectedColor) { selectedColor = it }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar colección")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    // Si hay nueva imagen la sube; si no, mantiene la anterior
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

    /**
     * Crea y añade chips de colores al ChipGroup del diálogo.
     * El chip "Default" representa sin color (valor 0).
     * Cuando el usuario selecciona uno, invoca onSelected con el color elegido.
     */
    private fun setupCollectionColorChips(
        chipGroup: ChipGroup,
        initialColor: Int,
        onSelected: (Int) -> Unit
    ) {
        chipGroup.removeAllViews()
        chipGroup.isSingleSelection = true // Solo puede estar uno seleccionado a la vez

        fun addChip(label: String, color: Int, isDefault: Boolean = false) {
            val chip = Chip(requireContext()).apply {
                text = label
                isCheckable = true
                if (isDefault) {
                    // El chip Default tiene estilo neutro (gris claro) para indicar "sin color"
                    chipBackgroundColor = ColorStateList.valueOf(0xFFECEFF1.toInt())
                    chipStrokeWidth = 1f
                    chipStrokeColor = ColorStateList.valueOf(0xFFB0BEC5.toInt())
                    setTextColor(0xFF263238.toInt())
                } else {
                    // Chips de color: fondo del color elegido, texto blanco para contraste
                    chipBackgroundColor = ColorStateList.valueOf(color)
                    setTextColor(0xFFFFFFFF.toInt())
                }
                id = View.generateViewId()
                setOnCheckedChangeListener { _, checked -> if (checked) onSelected(color) }
            }
            chipGroup.addView(chip)
            // Marca el chip correspondiente al color inicial como seleccionado
            if (color == initialColor) chip.isChecked = true
            if (initialColor == 0 && isDefault) chip.isChecked = true
        }

        addChip("Default", 0, isDefault = true)
        ColeccionColors.PALETTE.forEach { option -> addChip(option.name, option.color) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Evita memory leaks al limpiar la referencia al binding
    }
}

/**
 * ItemDecoration que aplica espaciado uniforme entre celdas de un GridLayoutManager.
 *
 * @param spanCount   Número de columnas del grid.
 * @param spacing     Espacio en píxeles entre celdas.
 * @param includeEdge Si true, también añade margen en los bordes exteriores del grid.
 */
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount // Columna 0-based en la que se encuentra el item

        if (includeEdge) {
            // Distribuye el espaciado equitativamente teniendo en cuenta la columna
            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount
            if (position < spanCount) outRect.top = spacing // Solo primera fila lleva margen superior
            outRect.bottom = spacing
        } else {
            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount
            if (position >= spanCount) outRect.top = spacing // Segunda fila en adelante lleva margen
        }
    }
}
