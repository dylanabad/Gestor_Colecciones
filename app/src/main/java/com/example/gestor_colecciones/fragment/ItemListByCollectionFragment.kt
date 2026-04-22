package com.example.gestor_colecciones.fragment

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.adapters.ItemAdapter
import com.example.gestor_colecciones.databinding.FragmentItemListBinding
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.model.ItemEstados
import com.example.gestor_colecciones.model.ItemFilterSortState
import com.example.gestor_colecciones.model.ItemSortField
import com.example.gestor_colecciones.network.ApiProvider
import com.example.gestor_colecciones.network.UploadUtils
import com.example.gestor_colecciones.repository.ColeccionRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.repository.RepositoryProvider
import com.example.gestor_colecciones.repository.TagRepository
import com.example.gestor_colecciones.util.ImageUtils
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Muestra los ítems de una colección concreta.
 * Este fragment es el punto natural para crear/editar ítems dentro de una colección.
 */
class ItemListByCollectionFragment : Fragment() {

    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    private lateinit var itemRepo: ItemRepository
    private lateinit var coleccionRepo: ColeccionRepository
    private lateinit var tagRepo: TagRepository
    private val categoriaRepo by lazy { RepositoryProvider.categoriaRepository(requireContext()) }

    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter

    private val categoriasMap = mutableMapOf<Int, String>()
    private var fullItemList: List<Item> = emptyList()
    private var searchQuery: String = ""
    private var filterSortState: ItemFilterSortState = ItemFilterSortState()
    private var tagIdsByItemId: Map<Int, Set<Int>> = emptyMap()

    // --- Imágenes (crear item) ---
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val collectionId = requireArguments().getInt(ARG_COLLECTION_ID)

        itemRepo = RepositoryProvider.itemRepository(requireContext())
        coleccionRepo = RepositoryProvider.coleccionRepository(requireContext())
        tagRepo = RepositoryProvider.tagRepository(requireContext())

        viewModel = ViewModelProvider(
            this,
            ItemViewModelFactory(itemRepo, categoriaRepo, null)
        )[ItemViewModel::class.java]

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

        // Header (nombre + imagen de la colección)
        viewLifecycleOwner.lifecycleScope.launch {
            val coleccion = coleccionRepo.getById(collectionId)
            if (coleccion != null) {
                binding.tvCollectionName.visibility = View.VISIBLE
                binding.tvCollectionName.text = coleccion.nombre

                val model = ImageUtils.toGlideModel(coleccion.imagenPath)
                if (model != null) {
                    binding.ivCollectionImage.visibility = View.VISIBLE
                    Glide.with(this@ItemListByCollectionFragment)
                        .load(model)
                        .centerCrop()
                        .into(binding.ivCollectionImage)
                } else {
                    binding.ivCollectionImage.visibility = View.GONE
                }
            }
        }

        // Categorías -> adapter
        viewLifecycleOwner.lifecycleScope.launch {
            categoriaRepo.allCategorias.collectLatest { lista ->
                categoriasMap.clear()
                lista.forEach { categoriasMap[it.id] = it.nombre }
                adapter.categoriasMap = categoriasMap
            }
        }

        // Items de esta colección
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getItemsByCollectionFlow(collectionId).collectLatest { items ->
                fullItemList = items
                refreshTagsMaps()
                applyFiltersAndSort()
            }
        }

        // Buscar
        binding.searchItems.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                searchQuery = newText.orEmpty()
                applyFiltersAndSort()
                return true
            }
        })

        // Crear item
        binding.fabAddItem.setOnClickListener {
            showCreateItemDialog(collectionId)
        }

        // Gestionar categorías
        binding.fabAddCategory.setOnClickListener {
            showCreateCategoriaDialog()
        }

        // Filtros avanzados: mismo BottomSheet que en ItemListFragment
        parentFragmentManager.setFragmentResultListener(
            ItemFilterSortBottomSheet.RESULT_KEY,
            viewLifecycleOwner
        ) { _, bundle ->
            val categoriaId = bundle.getInt(ItemFilterSortBottomSheet.BUNDLE_CATEGORY_ID, -1)
            val estado = bundle.getString(ItemFilterSortBottomSheet.BUNDLE_ESTADO, null)
            val tagId = bundle.getInt(ItemFilterSortBottomSheet.BUNDLE_TAG_ID, -1)
            val minRating = bundle.getFloat(ItemFilterSortBottomSheet.BUNDLE_MIN_RATING, 0f)
            val sortFieldName = bundle.getString(
                ItemFilterSortBottomSheet.BUNDLE_SORT_FIELD,
                ItemSortField.DATE.name
            )
            val ascending = bundle.getBoolean(ItemFilterSortBottomSheet.BUNDLE_ASCENDING, false)

            val sortField = runCatching {
                ItemSortField.valueOf(sortFieldName ?: ItemSortField.DATE.name)
            }.getOrDefault(ItemSortField.DATE)

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

        binding.btnFilterSort.setOnClickListener { openFilterSort() }
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            refreshTagsMaps()
            applyFiltersAndSort()
        }
    }

    private fun openFilterSort() {
        viewLifecycleOwner.lifecycleScope.launch {
            val sortedCategorias = categoriasMap.entries.sortedBy { it.value.lowercase(Locale.getDefault()) }
            val categoryIds = intArrayOf(-1) + sortedCategorias.map { it.key }.toIntArray()
            val categoryNames = arrayOf("Todas") + sortedCategorias.map { it.value }.toTypedArray()

            val estados = (ItemEstados.DEFAULT + fullItemList.mapNotNull { it.estado.takeIf { s -> s.isNotBlank() } })
                .distinct()
                .sortedBy { it.lowercase(Locale.getDefault()) }
            val statusList = arrayOf("Cualquiera") + estados.toTypedArray()

            val tags = tagRepo.getAllTagsOnce().sortedBy { it.nombre.lowercase(Locale.getDefault()) }
            val tagIds = intArrayOf(-1) + tags.map { it.id }.toIntArray()
            val tagNames = arrayOf("Cualquiera") + tags.map { it.nombre }.toTypedArray()

            ItemFilterSortBottomSheet
                .newInstance(categoryIds, categoryNames, statusList, tagIds, tagNames, filterSortState)
                .show(parentFragmentManager, "ItemFilterSortBottomSheet")
        }
    }

    private suspend fun refreshTagsMaps() {
        val ids = fullItemList.map { it.id }
        val infos = tagRepo.getTagInfoForItemsOnce(ids)
        val namesMap = infos.groupBy { it.itemId }.mapValues { (_, v) -> v.map { it.nombre } }
        tagIdsByItemId = infos.groupBy { it.itemId }.mapValues { (_, v) -> v.map { it.tagId }.toSet() }
        adapter.tagsByItemId = namesMap
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

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.fabAddItem)
            .show()
    }

    // -------------------------------------------------------------------------
    // Crear Item (básico)
    // -------------------------------------------------------------------------

    private fun showCreateItemDialog(collectionId: Int) {
        selectedItemImageUri = null

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_item, null)

        val etTitulo = dialogView.findViewById<TextInputEditText>(R.id.etTitulo)
        val etValor = dialogView.findViewById<TextInputEditText>(R.id.etValor)
        val actvEstado = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.actvItemEstado)
        val actvCategoria = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.actvItemCategoria)
        val etFecha = dialogView.findViewById<TextInputEditText>(R.id.etFechaAdquisicion)
        val etDescripcion = dialogView.findViewById<TextInputEditText>(R.id.etDescripcion)
        val rbCalificacion = dialogView.findViewById<RatingBar>(R.id.rbCalificacion)
        val tvCalificacionValue = dialogView.findViewById<MaterialTextView>(R.id.tvCalificacionValue)
        val ivPreview = dialogView.findViewById<ImageView>(R.id.ivItemPreview)
        val btnSelectImage = dialogView.findViewById<Button>(R.id.btnSelectImage)

        currentItemImageView = ivPreview

        // Estado
        val estados = ItemEstados.DEFAULT
        actvEstado.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, estados))
        actvEstado.setText(estados.firstOrNull() ?: "Nuevo", false)

        // Categoría
        val categoriasOrdenadas = categoriasMap.entries.sortedBy { it.value.lowercase(Locale.getDefault()) }
        val categoriaNames = listOf("Sin categoría") + categoriasOrdenadas.map { it.value }
        actvCategoria.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categoriaNames))
        actvCategoria.setText("Sin categoría", false)

        // Fecha (hoy por defecto)
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var selectedDate: Date = Date()
        etFecha.setText(sdf.format(selectedDate))
        etFecha.setOnClickListener {
            val cal = Calendar.getInstance().apply { time = selectedDate }
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val c = Calendar.getInstance()
                    c.set(Calendar.YEAR, year)
                    c.set(Calendar.MONTH, month)
                    c.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    c.set(Calendar.HOUR_OF_DAY, 0)
                    c.set(Calendar.MINUTE, 0)
                    c.set(Calendar.SECOND, 0)
                    c.set(Calendar.MILLISECOND, 0)
                    selectedDate = c.time
                    etFecha.setText(sdf.format(selectedDate))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Rating label
        fun updateRatingLabel(r: Float) {
            tvCalificacionValue.text = String.format(Locale.getDefault(), "%.1f", r.coerceIn(0f, 5f))
        }
        updateRatingLabel(rbCalificacion.rating)
        rbCalificacion.setOnRatingBarChangeListener { _, rating, _ ->
            updateRatingLabel(rating)
        }

        // Imagen
        btnSelectImage.setOnClickListener {
            pickItemImageLauncher.launch("image/*")
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Crear ítem")
            .setView(dialogView)
            .setPositiveButton("Crear", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val btn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            btn.setOnClickListener {
                val titulo = etTitulo.text?.toString()?.trim().orEmpty()
                if (titulo.isBlank()) {
                    showSnackbar("Introduce un título")
                    return@setOnClickListener
                }

                val valor = etValor.text?.toString()?.toDoubleOrNull() ?: 0.0
                val estado = actvEstado.text?.toString()?.trim().orEmpty().ifBlank { "Nuevo" }

                val categoriaNombre = actvCategoria.text?.toString()?.trim().orEmpty()
                val categoriaId = if (categoriaNombre == "Sin categoría") 0 else {
                    categoriasOrdenadas.firstOrNull { it.value == categoriaNombre }?.key ?: 0
                }

                val descripcion = etDescripcion.text?.toString()

                viewLifecycleOwner.lifecycleScope.launch {
                    val imagePath = selectedItemImageUri?.let { uploadImage(it) }
                    val nuevo = Item(
                        id = 0,
                        titulo = titulo,
                        categoriaId = categoriaId,
                        collectionId = collectionId,
                        fechaAdquisicion = selectedDate,
                        valor = valor,
                        imagenPath = imagePath,
                        estado = estado,
                        descripcion = descripcion,
                        calificacion = rbCalificacion.rating
                    )

                    viewModel.insert(
                        nuevo,
                        onInserted = {
                            showSnackbar("Ítem creado")
                            dialog.dismiss()
                        },
                        onError = { msg ->
                            showSnackbar(msg)
                        }
                    )
                }
            }
        }

        dialog.show()
    }

    // -------------------------------------------------------------------------
    // Editar Item (diálogo)
    // -------------------------------------------------------------------------

    private fun showEditItemDialog(item: Item) {
        // Reset del estado de imagen de esta edición
        selectedItemImageUri = null

        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_item, null)

        val etTitulo = dialogView.findViewById<TextInputEditText>(R.id.etTitulo)
        val etValor = dialogView.findViewById<TextInputEditText>(R.id.etValor)
        val actvEstado = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.actvItemEstado)
        val actvCategoria = dialogView.findViewById<MaterialAutoCompleteTextView>(R.id.actvItemCategoria)
        val etFecha = dialogView.findViewById<TextInputEditText>(R.id.etFechaAdquisicion)
        val etDescripcion = dialogView.findViewById<TextInputEditText>(R.id.etDescripcion)
        val rbCalificacion = dialogView.findViewById<RatingBar>(R.id.rbCalificacion)
        val tvCalificacionValue = dialogView.findViewById<MaterialTextView>(R.id.tvCalificacionValue)
        val ivPreview = dialogView.findViewById<ImageView>(R.id.ivItemPreview)
        val btnSelectImage = dialogView.findViewById<Button>(R.id.btnSelectImage)

        currentItemImageView = ivPreview

        // Prefill
        etTitulo.setText(item.titulo)
        etValor.setText(if (item.valor == 0.0) "" else item.valor.toString())
        etDescripcion.setText(item.descripcion.orEmpty())
        rbCalificacion.rating = item.calificacion.coerceIn(0f, 5f)

        // Imagen actual
        val model = ImageUtils.toGlideModel(item.imagenPath)
        if (model != null) {
            Glide.with(this)
                .load(model)
                .centerCrop()
                .into(ivPreview)
        } else {
            ivPreview.setImageResource(R.drawable.ic_no_image)
        }

        // Estado
        val estados = ItemEstados.DEFAULT
        actvEstado.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, estados))
        val estadoActual = item.estado.ifBlank { estados.firstOrNull() ?: "Nuevo" }
        actvEstado.setText(estadoActual, false)

        // Categoría
        val categoriasOrdenadas = categoriasMap.entries.sortedBy { it.value.lowercase(Locale.getDefault()) }
        val categoriaNames = listOf("Sin categoría") + categoriasOrdenadas.map { it.value }
        actvCategoria.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categoriaNames))
        val categoriaActual = categoriasMap[item.categoriaId]
        actvCategoria.setText(categoriaActual ?: "Sin categoría", false)

        // Fecha
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        var selectedDate: Date = item.fechaAdquisicion
        etFecha.setText(sdf.format(selectedDate))
        etFecha.setOnClickListener {
            val cal = Calendar.getInstance().apply { time = selectedDate }
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val c = Calendar.getInstance()
                    c.set(Calendar.YEAR, year)
                    c.set(Calendar.MONTH, month)
                    c.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                    c.set(Calendar.HOUR_OF_DAY, 0)
                    c.set(Calendar.MINUTE, 0)
                    c.set(Calendar.SECOND, 0)
                    c.set(Calendar.MILLISECOND, 0)
                    selectedDate = c.time
                    etFecha.setText(sdf.format(selectedDate))
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Rating label
        fun updateRatingLabel(r: Float) {
            tvCalificacionValue.text = String.format(Locale.getDefault(), "%.1f", r.coerceIn(0f, 5f))
        }
        updateRatingLabel(rbCalificacion.rating)
        rbCalificacion.setOnRatingBarChangeListener { _, rating, _ -> updateRatingLabel(rating) }

        // Cambiar imagen (galería)
        btnSelectImage.setOnClickListener {
            pickItemImageLauncher.launch("image/*")
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar ítem")
            .setView(dialogView)
            .setPositiveButton("Guardar", null)
            .setNegativeButton("Cancelar", null)
            .create()

        dialog.setOnShowListener {
            val btn = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
            btn.setOnClickListener {
                val titulo = etTitulo.text?.toString()?.trim().orEmpty()
                if (titulo.isBlank()) {
                    showSnackbar("Introduce un título")
                    return@setOnClickListener
                }

                val valor = etValor.text?.toString()?.toDoubleOrNull() ?: item.valor
                val estado = actvEstado.text?.toString()?.trim().orEmpty().ifBlank { item.estado }

                val categoriaNombre = actvCategoria.text?.toString()?.trim().orEmpty()
                val categoriaId = if (categoriaNombre == "Sin categoría") 0 else {
                    categoriasOrdenadas.firstOrNull { it.value == categoriaNombre }?.key ?: item.categoriaId
                }

                val descripcion = etDescripcion.text?.toString()

                viewLifecycleOwner.lifecycleScope.launch {
                    val imagePath = selectedItemImageUri?.let { uploadImage(it) } ?: item.imagenPath
                    val updated = item.copy(
                        titulo = titulo,
                        valor = valor,
                        estado = estado,
                        categoriaId = categoriaId,
                        fechaAdquisicion = selectedDate,
                        descripcion = descripcion,
                        calificacion = rbCalificacion.rating,
                        imagenPath = imagePath
                    )

                    viewModel.update(
                        updated,
                        onUpdated = {
                            showSnackbar("Ítem actualizado")
                            dialog.dismiss()
                        },
                        onError = { msg ->
                            showSnackbar(msg)
                        }
                    )
                }
            }
        }

        dialog.show()
    }

    // -------------------------------------------------------------------------
    // Categorías (reutiliza la UI existente)
    // -------------------------------------------------------------------------

    fun showCreateCategoriaDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_categoria, null)
        val rvCategorias = view.findViewById<RecyclerView>(R.id.rvCategorias)
        val etCategoriaNombre = view.findViewById<TextInputEditText>(R.id.etCategoriaNombre)
        val btnAddCategoria = view.findViewById<Button>(R.id.btnAddCategoria)

        val categoriesList = categoriasMap.entries.map { it.toPair() }.toMutableList()

        val adapter = object : RecyclerView.Adapter<CategoriaViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoriaViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_categoria_manage, parent, false)
                return CategoriaViewHolder(v)
            }

            override fun onBindViewHolder(holder: CategoriaViewHolder, position: Int) {
                val item = categoriesList[position]
                holder.tvNombre.text = item.second

                holder.btnEdit.setOnClickListener {
                    showEditSingleCategoriaDialog(item.first, item.second) { refresh() }
                }

                holder.btnDelete.setOnClickListener {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Eliminar categoría")
                        .setMessage("¿Estás seguro de eliminar \"${item.second}\"?")
                        .setPositiveButton("Eliminar") { _, _ ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                categoriaRepo.delete(Categoria(id = item.first, nombre = item.second))
                                categoriasMap.remove(item.first)
                                refresh()
                                showSnackbar("Categoría eliminada")
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
                    showSnackbar("Categoría \"$nombre\" creada")
                }
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setNegativeButton("Cerrar", null)
            .show()
    }

    private fun showEditSingleCategoriaDialog(id: Int, nombreActual: String, onUpdate: () -> Unit) {
        val context = requireContext()
        val et = TextInputEditText(context).apply {
            setText(nombreActual)
            setPadding(48, 48, 48, 48)
        }
        val layout = com.google.android.material.textfield.TextInputLayout(
            context,
            null,
            com.google.android.material.R.attr.textInputOutlinedStyle
        ).apply {
            setPadding(64, 32, 64, 0)
            hint = "Nombre"
            setBoxCornerRadii(16f, 16f, 16f, 16f)
            addView(et)
        }

        MaterialAlertDialogBuilder(context)
            .setTitle("Editar categoría")
            .setView(layout)
            .setPositiveButton("Guardar") { _, _ ->
                val nuevoNombre = et.text?.toString()?.trim().orEmpty()
                if (nuevoNombre.isNotBlank()) {
                    viewLifecycleOwner.lifecycleScope.launch {
                        categoriaRepo.update(Categoria(id = id, nombre = nuevoNombre))
                        categoriasMap[id] = nuevoNombre
                        onUpdate()
                        showSnackbar("Categoría actualizada")
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    class CategoriaViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvNombre: TextView = v.findViewById(R.id.tvCategoriaNombre)
        val btnEdit: View = v.findViewById(R.id.btnEditCategoria)
        val btnDelete: View = v.findViewById(R.id.btnDeleteCategoria)
    }

    // -------------------------------------------------------------------------
    // Upload imagen (backend)
    // -------------------------------------------------------------------------

    private suspend fun uploadImage(uri: Uri): String? {
        return try {
            val api = ApiProvider.getApi(requireContext())
            val part = UploadUtils.createImagePart(requireContext(), uri)
            api.uploadImage(part).url
        } catch (_: Exception) {
            showSnackbar("Error subiendo imagen")
            null
        }
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
