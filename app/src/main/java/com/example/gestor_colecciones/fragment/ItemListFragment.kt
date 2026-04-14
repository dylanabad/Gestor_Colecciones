package com.example.gestor_colecciones.fragment

/*
 * ItemListFragment.kt
 *
 * Fragmento que muestra la lista global de items (todas las colecciones).
 * Provee búsqueda, filtrado/ordenación, edición de items y gestión de categorías.
 *
 * Nota: Solo se añaden comentarios explicativos en español, sin cambiar la lógica.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.DefaultItemAnimator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.adapters.ItemAdapter
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentItemListBinding
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.model.ItemFilterSortState
import com.example.gestor_colecciones.model.ItemSortField
import com.example.gestor_colecciones.model.ItemEstados
import com.example.gestor_colecciones.repository.CategoriaRepository
import com.example.gestor_colecciones.repository.ItemHistoryRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.repository.RepositoryProvider
import com.example.gestor_colecciones.repository.TagRepository
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.launch
import java.util.*

// Fragment que muestra la lista de items (vista general).
// Maneja UI, interacción del usuario y delega operaciones a ViewModel/repositorios.
class ItemListFragment : Fragment() {

    // ViewBinding: se inicializa en onCreateView y se limpia en onDestroyView
    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    // ViewModel y adaptador para la lista de items
    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter

    // Map de categorías (id -> nombre) y lista completa de items (sin filtrar)
    private var categoriasMap: MutableMap<Int, String> = mutableMapOf()
    private var fullItemList: List<Item> = emptyList()

    // Estado de búsqueda/filtrado/orden
    private var searchQuery: String = ""
    private var filterSortState: ItemFilterSortState = ItemFilterSortState()

    // Mapa auxiliar: para cada item, IDs de tags asociados (usado en filtros)
    private var tagIdsByItemId: Map<Int, Set<Int>> = emptyMap()

    // Repositorios para acceso a datos (Room / providers)
    private lateinit var itemRepo: ItemRepository
    private lateinit var categoriaRepo: CategoriaRepository
    private lateinit var tagRepo: TagRepository
    private lateinit var historyRepo: ItemHistoryRepository

    // Helper para mostrar mensajes tipo Snackbar con anclaje al FAB
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.fabAddItem)
            .show()
    }

    // Ciclo de vida: inicialización del fragmento y configuración de transiciones compartidas
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply { duration = 240 }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply { duration = 220 }
        exitTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply { duration = 240 }
        reenterTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply { duration = 220 }
    }

    // Infla la vista y prepara el binding
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

        // --- Inicialización repositorios ---
        val db = DatabaseProvider.getDatabase(requireContext())
        itemRepo = RepositoryProvider.itemRepository(requireContext())
        categoriaRepo = RepositoryProvider.categoriaRepository(requireContext())
        tagRepo = RepositoryProvider.tagRepository(requireContext())
        historyRepo = ItemHistoryRepository(db.itemHistoryDao())

        // --- ViewModel ---
        viewModel = ViewModelProvider(this, ItemViewModelFactory(itemRepo, null, historyRepo))[ItemViewModel::class.java]

        // --- RecyclerView ---
        adapter = ItemAdapter(emptyList(), categoriasMap)
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter
        binding.rvItems.itemAnimator = DefaultItemAnimator().apply {
            supportsChangeAnimations = false
            addDuration = 180
            removeDuration = 160
            moveDuration = 180
            changeDuration = 160
        }

        // Click corto: abrir detalle del item
        adapter.onItemClick = { item ->
            val fragment = ItemDetailFragment.newInstance(item.id)
            parentFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commit()
        }

        // Click largo: editar item
        adapter.onItemLongClick = { item ->
            showEditItemDialog(item)
        }

        // Escuchar resultados del BottomSheet de filtros/ordenación
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

        // --- Cargar categorías y items ---
        viewLifecycleOwner.lifecycleScope.launch {
            val categorias = categoriaRepo.allCategoriasOnce()
            categoriasMap.putAll(categorias.associate { it.id to it.nombre })
            updateFabState()

            // Colección de items desde el ViewModel (Flow)
            viewModel.items.collect { items ->
                fullItemList = items
                refreshTagsMaps()
                applyFiltersAndSort()
            }
        }

        // --- FAB para crear item (en la vista global muestra un mensaje)
        binding.fabAddItem.setOnClickListener {
            showSnackbar("Crea items desde dentro de una coleccion")
        }

        // --- FAB para crear categoría (siempre activo) ---
        binding.fabAddCategory.setOnClickListener {
            showCreateCategoriaDialog()
        }

        // --- Buscador ---
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
        // Refrescar información dependiente del lifecycle al volver a primer plano
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
            val sortedCategorias = categoriasMap.entries
                .sortedBy { it.value.lowercase(Locale.getDefault()) }

            val categoryIds = intArrayOf(-1) + sortedCategorias.map { it.key }.toIntArray()
            val categoryNames = arrayOf("Todas") + sortedCategorias.map { it.value }.toTypedArray()

            val estados = (ItemEstados.DEFAULT + fullItemList.mapNotNull { it.estado.takeIf { s -> s.isNotBlank() } })
                .distinct()
                .sortedBy { it.lowercase(Locale.getDefault()) }
            val statusList = arrayOf("Cualquiera") + estados.toTypedArray()

            val tags = tagRepo.getAllTagsOnce()
                .sortedBy { it.nombre.lowercase(Locale.getDefault()) }
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

        if (query.isNotBlank()) {
            list = list.filter { it.titulo.lowercase(Locale.getDefault()).contains(query) }
        }

        filterSortState.categoriaId?.let { categoriaId ->
            list = list.filter { it.categoriaId == categoriaId }
        }

        filterSortState.tagId?.let { tagId ->
            list = list.filter { tagIdsByItemId[it.id]?.contains(tagId) == true }
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
        val primaryComparator = if (filterSortState.ascending) baseComparator else baseComparator.reversed()
        val comparator = compareByDescending<Item> { it.favorito }
            .then(primaryComparator)
            .thenBy { it.id }

        adapter.updateList(list.sortedWith(comparator))
    }

    // --- Actualiza estado del FAB de items ---
    // Ajusta habilitación/alpha y pasa el mapa de categorías al adaptador
    private fun updateFabState() {
        binding.fabAddItem.isEnabled = true
        binding.fabAddItem.alpha = 0.7f
        adapter.categoriasMap = categoriasMap
    }

    // --- EDITAR ITEM ---
    // --- EDITAR ITEM ---
    // Muestra un diálogo para editar los campos de un item existente
    private fun showEditItemDialog(item: Item) {
        val categoriasList = categoriasMap.entries.toList()
        if (categoriasList.isEmpty()) return

        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_item, null)
        val etTitulo = view.findViewById<EditText>(R.id.etTitulo)
        val etValor = view.findViewById<EditText>(R.id.etValor)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)
        val actvEstado = view.findViewById<AutoCompleteTextView>(R.id.actvItemEstado)
        val spinnerCategoria = view.findViewById<Spinner>(R.id.spinnerCategoria)
        val rbCalificacion = view.findViewById<RatingBar>(R.id.rbCalificacion)
        val tvCalificacionValue = view.findViewById<TextView>(R.id.tvCalificacionValue)

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
                    estado = estado,
                    calificacion = rbCalificacion.rating
                )

                viewModel.update(
                    actualizado,
                    onUpdated = { showSnackbar("Item \"$titulo\" actualizado") },
                    onError = { msg -> showSnackbar(msg) }
                )
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // --- CREAR CATEGORIA ---
    // --- CREAR CATEGORIA ---
    // Muestra un diálogo para crear/editar/eliminar categorías
    private fun showCreateCategoriaDialog() {
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_categoria, null)
        val lvCategorias = view.findViewById<ListView>(R.id.lvCategorias)
        val etCategoriaNombre = view.findViewById<EditText>(R.id.etCategoriaNombre)
        val btnAddCategoria = view.findViewById<Button>(R.id.btnAddCategoria)

        val adapterList = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            categoriasMap.values.toMutableList()
        )
        lvCategorias.adapter = adapterList

        btnAddCategoria.setOnClickListener {
            val nombre = etCategoriaNombre.text.toString().trim()
            if (nombre.isNotBlank()) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val categoria = Categoria(nombre = nombre)
                    val id = categoriaRepo.insert(categoria).toInt()
                    categoriasMap[id] = nombre
                    adapterList.clear()
                    adapterList.addAll(categoriasMap.values)
                    adapterList.notifyDataSetChanged()
                    etCategoriaNombre.text.clear()
                    showSnackbar("Categoría \"$nombre\" creada")
                    updateFabState() // habilita el FAB de items si había cero
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

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpiar referencia al binding para evitar fugas de memoria
        _binding = null
    }
}






