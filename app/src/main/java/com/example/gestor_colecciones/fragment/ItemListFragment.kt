package com.example.gestor_colecciones.fragment

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentItemListBinding
import com.example.gestor_colecciones.adapters.ItemAdapter
import com.example.gestor_colecciones.entities.Categoria
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Date

class ItemListFragment : Fragment() {

    private var _binding: FragmentItemListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ItemViewModel
    private lateinit var adapter: ItemAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar RecyclerView
        adapter = ItemAdapter(emptyList())
        binding.rvItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvItems.adapter = adapter

        // Configurar ViewModel
        val db = DatabaseProvider.getDatabase(requireContext())
        val repo = ItemRepository(db.itemDao())
        viewModel = ViewModelProvider(
            this,
            ItemViewModelFactory(repo)
        )[ItemViewModel::class.java]

        // Insertar datos de prueba solo si la base de datos está vacía
        lifecycleScope.launch {
            // Comprobar si ya hay ítems en la base de datos
            val itemCount = db.itemDao().getTotalItems()
            if (itemCount == 0) {
                // 1. Categorías
                val cat1Id = db.categoriaDao().insert(Categoria(nombre = "Monedas")).toInt()
                val cat2Id = db.categoriaDao().insert(Categoria(nombre = "Sellos")).toInt()
                val cat3Id = db.categoriaDao().insert(Categoria(nombre = "Figuras")).toInt()

                // 2. Colecciones
                val col1Id = db.coleccionDao().insert(
                    Coleccion(nombre = "Colección principal", descripcion = "Mi colección favorita", fechaCreacion = Date())
                ).toInt()
                val col2Id = db.coleccionDao().insert(
                    Coleccion(nombre = "Colección secundaria", descripcion = null, fechaCreacion = Date())
                ).toInt()

                // 3. Items
                val sampleItems = listOf(
                    Item(
                        titulo = "Moneda antigua",
                        categoriaId = cat1Id,
                        collectionId = col1Id,
                        fechaAdquisicion = Date(),
                        valor = 12.5,
                        estado = "Bueno",
                        calificacion = 4.5f,
                        imagenPath = null,
                        descripcion = "Moneda de colección"
                    ),
                    Item(
                        titulo = "Sello raro",
                        categoriaId = cat2Id,
                        collectionId = col1Id,
                        fechaAdquisicion = Date(),
                        valor = 5.0,
                        estado = "Excelente",
                        calificacion = 5.0f,
                        imagenPath = null,
                        descripcion = "Sello histórico"
                    ),
                    Item(
                        titulo = "Figura de acción",
                        categoriaId = cat3Id,
                        collectionId = col2Id,
                        fechaAdquisicion = Date(),
                        valor = 25.0,
                        estado = "Nuevo",
                        calificacion = 4.0f,
                        imagenPath = null,
                        descripcion = "Figura de edición limitada"
                    )
                )

                sampleItems.forEach { db.itemDao().insert(it) }
            }
        }

        // Observar lista de ítems
        lifecycleScope.launch {
            viewModel.items.collectLatest { items ->
                adapter.updateList(items)
            }
        }

        // Configurar búsqueda por título
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString()
                lifecycleScope.launch {
                    viewModel.searchItems(query).collectLatest { items ->
                        adapter.updateList(items)
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}