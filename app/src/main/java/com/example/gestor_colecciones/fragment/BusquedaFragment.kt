package com.example.gestor_colecciones.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialFadeThrough
import com.example.gestor_colecciones.adapters.BusquedaAdapter
import com.example.gestor_colecciones.adapters.BusquedaItem
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentBusquedaBinding
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.repository.BusquedaRepository
import com.example.gestor_colecciones.viewmodel.BusquedaState
import com.example.gestor_colecciones.viewmodel.BusquedaViewModel
import com.example.gestor_colecciones.viewmodel.BusquedaViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.gestor_colecciones.R

/**
 * Fragment que implementa la búsqueda global de la app.
 * Busca en tiempo real entre colecciones e items, mostrando
 * los resultados agrupados por tipo con cabeceras separadoras.
 */
class BusquedaFragment : Fragment() {

    // View Binding: se anula en onDestroyView para evitar memory leaks
    private var _binding: FragmentBusquedaBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BusquedaViewModel
    private lateinit var adapter: BusquedaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transición de entrada y salida con fade
        enterTransition = MaterialFadeThrough().apply {
            duration = 220
        }

        returnTransition = MaterialFadeThrough().apply {
            duration = 200
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBusquedaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = DatabaseProvider.getDatabase(requireContext())

        // El repositorio necesita ambos DAOs para buscar en colecciones e items a la vez
        val repo = BusquedaRepository(
            db.coleccionDao(),
            db.itemDao()
        )

        viewModel = ViewModelProvider(
            this,
            BusquedaViewModelFactory(repo)
        )[BusquedaViewModel::class.java]

        // Al pulsar un resultado navega a su pantalla correspondiente
        adapter = BusquedaAdapter(emptyList()) { resultado ->
            navegarAResultado(resultado)
        }

        binding.rvResultados.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResultados.adapter = adapter

        // Vuelve al fragment anterior en el back stack
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

            // No se usa: la búsqueda es en tiempo real con onQueryTextChange
            override fun onQueryTextSubmit(query: String?) = false

            // Lanza la búsqueda en el ViewModel cada vez que el texto cambia
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.buscar(newText.orEmpty())
                return true
            }
        })

        // Abre el teclado automáticamente al entrar en la pantalla
        binding.searchView.requestFocus()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->

                when (state) {

                    // El usuario aún no ha escrito nada
                    is BusquedaState.Idle -> {
                        binding.layoutIdle.visibility = View.VISIBLE
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvResultados.visibility = View.GONE
                    }

                    // Búsqueda en curso: oculta todo mientras se procesan los resultados
                    is BusquedaState.Loading -> {
                        binding.layoutIdle.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvResultados.visibility = View.GONE
                    }

                    // La búsqueda terminó sin encontrar resultados
                    is BusquedaState.Empty -> {
                        binding.layoutIdle.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.rvResultados.visibility = View.GONE
                    }

                    // Hay resultados: construye la lista agrupada y actualiza el adapter
                    is BusquedaState.Success -> {
                        binding.layoutIdle.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvResultados.visibility = View.VISIBLE

                        adapter.updateList(
                            buildList(
                                state.resultado.colecciones,
                                state.resultado.items
                            )
                        )
                    }
                }
            }
        }
    }

    /**
     * Construye una lista plana combinando colecciones e items con cabeceras separadoras.
     * Solo añade un grupo si tiene al menos un elemento.
     */
    private fun buildList(
        colecciones: List<Coleccion>,
        items: List<Item>
    ): List<BusquedaItem> {

        val lista = mutableListOf<BusquedaItem>()

        if (colecciones.isNotEmpty()) {

            // Cabecera del grupo con el total de colecciones encontradas
            lista.add(
                BusquedaItem.Header("Colecciones (${colecciones.size})")
            )

            colecciones.forEach { c ->
                lista.add(
                    BusquedaItem.Resultado(
                        id = c.id,
                        icono = "📦",
                        titulo = c.nombre,
                        // Muestra "Sin descripción" si está vacía o es null
                        subtitulo = c.descripcion?.ifBlank { "Sin descripción" }
                            ?: "Sin descripción",
                        tipo = "Colección",
                        esColeccion = true // Usado en navegarAResultado para decidir el destino
                    )
                )
            }
        }

        if (items.isNotEmpty()) {

            // Cabecera del grupo con el total de items encontrados
            lista.add(
                BusquedaItem.Header("Items (${items.size})")
            )

            items.forEach { item ->
                lista.add(
                    BusquedaItem.Resultado(
                        id = item.id,
                        icono = "🗂",
                        titulo = item.titulo,
                        // Muestra estado y valor con 2 decimales separados por un punto medio
                        subtitulo = "${item.estado}  ·  ${"%.2f".format(item.valor)} €",
                        tipo = "Item",
                        esColeccion = false
                    )
                )
            }
        }

        return lista
    }

    /**
     * Navega al detalle del resultado pulsado.
     * Si es colección abre su lista de items; si es item abre su detalle.
     */
    private fun navegarAResultado(resultado: BusquedaItem.Resultado) {

        val fragment = if (resultado.esColeccion) {
            ItemListByCollectionFragment.newInstance(resultado.id)
        } else {
            ItemDetailFragment.newInstance(resultado.id)
        }

        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null) // Permite volver a la búsqueda pulsando atrás
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Limpia la referencia al binding para evitar memory leaks
    }
}