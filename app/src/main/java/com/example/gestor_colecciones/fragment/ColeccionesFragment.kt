package com.example.gestor_colecciones.fragment

import android.content.ContentValues
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.graphics.Rect
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
import androidx.appcompat.widget.SearchView
import androidx.core.content.FileProvider
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
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.adapters.ColeccionAdapter
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentColeccionesBinding
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.model.ColeccionColors
import com.example.gestor_colecciones.model.LogroDefinicion
import com.example.gestor_colecciones.model.LogroManager
import com.example.gestor_colecciones.repository.ColeccionRepository
import com.example.gestor_colecciones.repository.ExportRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.repository.LogroRepository
import com.example.gestor_colecciones.viewmodel.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

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

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            selectedImageUri = it
            currentImageView?.setImageURI(it)
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

        val repo = ColeccionRepository(DatabaseProvider.getColeccionDao(requireContext()))
        viewModel = ViewModelProvider(this, ColeccionViewModelFactory(repo))[ColeccionViewModel::class.java]

        val itemRepo = ItemRepository(DatabaseProvider.getItemDao(requireContext()))
        itemViewModel = ViewModelProvider(this, ItemViewModelFactory(itemRepo))[ItemViewModel::class.java]

        val exportRepo = ExportRepository(repo, itemRepo)
        exportViewModel = ViewModelProvider(
            this, ExportViewModelFactory(exportRepo)
        )[ExportViewModel::class.java]

        // ── Logros ────────────────────────────────────────────────────────────
        val logroRepo = LogroRepository(DatabaseProvider.getDatabase(requireContext()).logroDao())
        val logroManager = LogroManager(logroRepo, repo, itemRepo)
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
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val coleccion = adapter.getItem(viewHolder.adapterPosition)
                viewHolder.itemView.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        viewLifecycleOwner.lifecycleScope.launch {
                            viewModel.delete(coleccion)
                            showSnackbar("Colección \"${coleccion.nombre}\" eliminada")
                        }
                    }.start()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvColecciones)

        // Observar colecciones y comprobar logros tras cada cambio
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.colecciones.collectLatest { lista ->
                listaCompleta = lista
                updateStatsAndAdapter()
                logroViewModel.checkLogros()
            }
        }

        // Mostrar Snackbar cuando se desbloquea un logro
        viewLifecycleOwner.lifecycleScope.launch {
            logroViewModel.nuevoLogro.collect { key ->
                LogroDefinicion.getInfo(key)?.let { info ->
                    showSnackbar("🏅 Logro desbloqueado: ${info.titulo}")
                }
            }
        }

        binding.fabAddColeccion.setOnClickListener { showCreateCollectionDialog() }
        binding.fabExport.setOnClickListener { showExportDialog() }

        // ── Navegación a lista de deseos ──────────────────────────────────
        binding.btnDeseos.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace((view.parent as ViewGroup).id, DeseosFragment())
                .addToBackStack(null)
                .commit()
        }

        // ── Navegación a estadísticas ─────────────────────────────────────
        binding.btnStats.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace((view.parent as ViewGroup).id, StatsFragment())
                .addToBackStack(null)
                .commit()
        }

        // ── Navegación a logros ───────────────────────────────────────────
        binding.btnLogros.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace((view.parent as ViewGroup).id, LogrosFragment())
                .addToBackStack(null)
                .commit()
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

    // ── Exportación ──────────────────────────────────────────────────────────

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
            "Guardar CSV en Descargas",
            "Guardar PDF en Descargas",
            "Compartir CSV",
            "Compartir PDF"
        )
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Exportar como")
            .setItems(opciones) { _, which ->
                when (which) {
                    0 -> exportViewModel.exportCsv(requireContext(), share = false, ids = ids)
                    1 -> exportViewModel.exportPdf(requireContext(), share = false, ids = ids)
                    2 -> exportViewModel.exportCsv(requireContext(), share = true, ids = ids)
                    3 -> exportViewModel.exportPdf(requireContext(), share = true, ids = ids)
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
        startActivity(Intent.createChooser(intent, "Compartir exportación"))
    }

    private fun saveFileToDownloads(file: File) {
        val mimeType = if (file.extension == "pdf") "application/pdf" else "text/csv"
        val fileName = "colecciones_export_${System.currentTimeMillis()}.${file.extension}"
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

    // ── Resto del fragment ────────────────────────────────────────────────────

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.fabAddColeccion)
            .show()
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
        btnSelectImage.setOnClickListener { pickImageLauncher.launch("image/*") }

        var selectedColor = 0
        setupCollectionColorChips(chipGroupColors, selectedColor) { selectedColor = it }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nueva colección")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = etNombre.text.toString().trim()
                val descripcion = etDescripcion.text.toString()
                var imagePath: String? = null
                selectedImageUri?.let { uri ->
                    imagePath = copyImageToInternalStorage(
                        uri, "coleccion_${System.currentTimeMillis()}.jpg"
                    )
                }
                if (nombre.isNotEmpty()) {
                    viewLifecycleOwner.lifecycleScope.launch {
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
        coleccion.imagenPath?.let { ivPreview.setImageBitmap(BitmapFactory.decodeFile(it)) }

        currentImageView = ivPreview
        btnSelectImage.setOnClickListener { pickImageLauncher.launch("image/*") }

        var selectedColor = coleccion.color
        setupCollectionColorChips(chipGroupColors, selectedColor) { selectedColor = it }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar colección")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                var imagePath = coleccion.imagenPath
                selectedImageUri?.let { uri ->
                    imagePath = copyImageToInternalStorage(
                        uri, "coleccion_${System.currentTimeMillis()}.jpg"
                    )
                }
                viewLifecycleOwner.lifecycleScope.launch {
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