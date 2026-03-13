package com.example.gestor_colecciones.fragment

import android.app.AlertDialog
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.SearchView
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
import com.example.gestor_colecciones.viewmodel.ColeccionViewModel
import com.example.gestor_colecciones.viewmodel.ColeccionViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class ColeccionesFragment : Fragment() {

    private var _binding: FragmentColeccionesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ColeccionViewModel
    private lateinit var adapter: ColeccionAdapter
    private var listaCompleta: List<Coleccion> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentColeccionesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Adapter
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
            }
        )

        // Grid layout (2 columnas)
        binding.rvColecciones.layoutManager = GridLayoutManager(requireContext(), 2)
        binding.rvColecciones.adapter = adapter

        // Espaciado entre cards
        binding.rvColecciones.addItemDecoration(
            GridSpacingItemDecoration(2, 32, true)
        )

        // Swipe para eliminar
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {

                val position = viewHolder.adapterPosition
                val coleccion = adapter.getItem(position)

                viewHolder.itemView.animate()
                    .alpha(0f)
                    .setDuration(300)
                    .withEndAction {
                        lifecycleScope.launch {
                            viewModel.delete(coleccion)
                        }
                    }
                    .start()
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvColecciones)

        // ViewModel
        val repo = ColeccionRepository(DatabaseProvider.getColeccionDao(requireContext()))
        viewModel = ViewModelProvider(
            this,
            ColeccionViewModelFactory(repo)
        )[ColeccionViewModel::class.java]

        // Observar colecciones
        lifecycleScope.launch {
            viewModel.colecciones.collectLatest { lista ->
                listaCompleta = lista
                adapter.updateList(lista)
            }
        }

        // Crear colección
        binding.fabAddColeccion.setOnClickListener {
            showCreateCollectionDialog()
        }

        // Buscador
        binding.searchColecciones.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {

                val texto = newText ?: ""

                val filtradas = listaCompleta.filter {
                    it.nombre.lowercase().contains(texto.lowercase())
                }

                adapter.updateList(filtradas)

                return true
            }
        })
    }

    private fun showCreateCollectionDialog() {

        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_collection, null)

        val etNombre = view.findViewById<EditText>(R.id.etNombre)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)

        AlertDialog.Builder(requireContext())
            .setTitle("Nueva colección")
            .setView(view)
            .setPositiveButton("Crear") { _, _ ->

                val nombre = etNombre.text.toString()
                val descripcion = etDescripcion.text.toString()

                if (nombre.isNotEmpty()) {

                    val coleccion = Coleccion(
                        id = 0,
                        nombre = nombre,
                        descripcion = descripcion,
                        fechaCreacion = Date()
                    )

                    lifecycleScope.launch {
                        viewModel.insert(coleccion)
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showEditCollectionDialog(coleccion: Coleccion) {

        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_create_collection, null)

        val etNombre = view.findViewById<EditText>(R.id.etNombre)
        val etDescripcion = view.findViewById<EditText>(R.id.etDescripcion)

        etNombre.setText(coleccion.nombre)
        etDescripcion.setText(coleccion.descripcion)

        AlertDialog.Builder(requireContext())
            .setTitle("Editar colección")
            .setView(view)
            .setPositiveButton("Guardar") { _, _ ->

                val actualizado = coleccion.copy(
                    nombre = etNombre.text.toString(),
                    descripcion = etDescripcion.text.toString()
                )

                lifecycleScope.launch {
                    viewModel.update(actualizado)
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/*
   Clase para añadir espacio entre elementos del Grid
*/
class GridSpacingItemDecoration(
    private val spanCount: Int,
    private val spacing: Int,
    private val includeEdge: Boolean
) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {

        val position = parent.getChildAdapterPosition(view)
        val column = position % spanCount

        if (includeEdge) {

            outRect.left = spacing - column * spacing / spanCount
            outRect.right = (column + 1) * spacing / spanCount

            if (position < spanCount) {
                outRect.top = spacing
            }

            outRect.bottom = spacing

        } else {

            outRect.left = column * spacing / spanCount
            outRect.right = spacing - (column + 1) * spacing / spanCount

            if (position >= spanCount) {
                outRect.top = spacing
            }
        }
    }
}