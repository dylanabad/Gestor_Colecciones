package com.example.gestor_colecciones.fragment

import android.app.AlertDialog
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.*
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.adapters.ColeccionAdapter
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentColeccionesBinding
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.repository.ColeccionRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.viewmodel.ColeccionViewModel
import com.example.gestor_colecciones.viewmodel.ColeccionViewModelFactory
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File
import java.util.Date

class ColeccionesFragment : Fragment() {

    private var _binding: FragmentColeccionesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ColeccionViewModel
    private lateinit var itemViewModel: ItemViewModel
    private lateinit var adapter: ColeccionAdapter
    private var listaCompleta: List<Coleccion> = emptyList()
    private var statsMap: MutableMap<Int, String> = mutableMapOf()

    // 🔥 NUEVO: imagen
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColeccionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ViewModels
        val repo = ColeccionRepository(DatabaseProvider.getColeccionDao(requireContext()))
        viewModel = ViewModelProvider(this, ColeccionViewModelFactory(repo))[ColeccionViewModel::class.java]

        val itemRepo = ItemRepository(DatabaseProvider.getItemDao(requireContext()))
        itemViewModel = ViewModelProvider(this, ItemViewModelFactory(itemRepo))[ItemViewModel::class.java]

        adapter = ColeccionAdapter(
            emptyList(),
            onClick = { coleccion ->
                val fragment = ItemListByCollectionFragment.newInstance(coleccion.id)
                parentFragmentManager.beginTransaction()
                    .replace((view.parent as ViewGroup).id, fragment)
                    .addToBackStack(null)
                    .commit()
            },
            onLongClick = { coleccion ->
                showEditCollectionDialog(coleccion)
            },
            coleccionStats = statsMap
        )

        binding.rvColecciones.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvColecciones.adapter = adapter
        binding.rvColecciones.addItemDecoration(GridSpacingItemDecoration(2, 32, true))

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
                        lifecycleScope.launch { viewModel.delete(coleccion) }
                    }.start()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvColecciones)

        lifecycleScope.launch {
            viewModel.colecciones.collectLatest { lista ->
                listaCompleta = lista
                updateStatsAndAdapter()
            }
        }

        binding.fabAddColeccion.setOnClickListener { showCreateCollectionDialog() }

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

    // 🔹 Función para copiar imagen al almacenamiento interno
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
        statsMap.clear()
        lifecycleScope.launch {
            lista.forEach { coleccion ->
                itemViewModel.getItemsByCollection(coleccion.id).collect { items ->
                    statsMap[coleccion.id] =
                        "${items.size} items | Valor: ${items.sumOf { it.valor }}€"
                    adapter.updateList(lista)
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

        currentImageView = ivPreview
        btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Nueva colección")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->
                val nombre = etNombre.text.toString()
                val descripcion = etDescripcion.text.toString()
                var imagePath: String? = null

                selectedImageUri?.let { uri ->
                    imagePath = copyImageToInternalStorage(
                        uri,
                        "coleccion_${System.currentTimeMillis()}.jpg"
                    )
                }

                if (nombre.isNotEmpty()) {
                    val coleccion = Coleccion(
                        0,
                        nombre,
                        descripcion,
                        Date(),
                        imagenPath = imagePath
                    )
                    lifecycleScope.launch { viewModel.insert(coleccion) }
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

        etNombre.setText(coleccion.nombre)
        etDescripcion.setText(coleccion.descripcion)

        // Cargar imagen existente si la hay
        coleccion.imagenPath?.let { path ->
            val bitmap = BitmapFactory.decodeFile(path)
            ivPreview.setImageBitmap(bitmap)
        }

        currentImageView = ivPreview
        btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Editar colección")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->
                var imagePath = coleccion.imagenPath
                selectedImageUri?.let { uri ->
                    imagePath = copyImageToInternalStorage(
                        uri,
                        "coleccion_${System.currentTimeMillis()}.jpg"
                    )
                }

                val actualizado = coleccion.copy(
                    nombre = etNombre.text.toString(),
                    descripcion = etDescripcion.text.toString(),
                    imagenPath = imagePath
                )
                lifecycleScope.launch { viewModel.update(actualizado) }

                selectedImageUri = null
                currentImageView = null
            }
            .setNegativeButton("Cancelar") { _, _ ->
                selectedImageUri = null
                currentImageView = null
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Espaciado entre elementos del Grid
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