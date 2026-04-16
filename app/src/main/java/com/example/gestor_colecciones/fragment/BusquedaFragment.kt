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
import com.example.gestor_colecciones.adapters.BusquedaAdapter
import com.example.gestor_colecciones.adapters.BusquedaItem
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentBusquedaBinding
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.repository.BusquedaRepository
import com.example.gestor_colecciones.viewmodel.BusquedaViewModel
import com.example.gestor_colecciones.viewmodel.BusquedaViewModelFactory
import com.example.gestor_colecciones.viewmodel.BusquedaState
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.example.gestor_colecciones.R

class BusquedaFragment : Fragment() {

    private var _binding: FragmentBusquedaBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BusquedaViewModel
    private lateinit var adapter: BusquedaAdapter
    
    private var lastColecciones: List<Coleccion> = emptyList()
    private var lastItems: List<Item> = emptyList()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBusquedaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = DatabaseProvider.getDatabase(requireContext())
        val repo = BusquedaRepository(db.coleccionDao(), db.itemDao())

        viewModel = ViewModelProvider(this, BusquedaViewModelFactory(repo))[BusquedaViewModel::class.java]

        adapter = BusquedaAdapter(emptyList()) { resultado ->
            navegarAResultado(resultado)
        }

        binding.rvResultados.layoutManager = LinearLayoutManager(requireContext())
        binding.rvResultados.adapter = adapter

        // Configuración de Material 3 SearchBar y SearchView
        binding.searchView.setupWithSearchBar(binding.searchBar)

        binding.searchView.editText.setOnEditorActionListener { _, _, _ ->
            viewModel.buscar(binding.searchView.text.toString())
            false
        }

        // Listener para los chips de filtrado
        binding.chipGroupFiltros.setOnCheckedStateChangeListener { _, checkedIds ->
            actualizarResultadosConFiltro()
        }

        // Observar cambios en el texto del SearchView (en tiempo real)
        binding.searchView.editText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.buscar(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                when (state) {
                    is BusquedaState.Idle -> {
                        mostrarEstadoIdle()
                    }
                    is BusquedaState.Loading -> {
                        // Opcional: mostrar un progress sutil
                    }
                    is BusquedaState.Success -> {
                        lastColecciones = state.resultado.colecciones
                        lastItems = state.resultado.items
                        actualizarResultadosConFiltro()
                    }
                    is BusquedaState.Empty -> {
                        mostrarEstadoVacio()
                    }
                }
            }
        }
    }

    private fun actualizarResultadosConFiltro() {
        val filtrarColecciones = binding.chipAll.isChecked || binding.chipColecciones.isChecked
        val filtrarItems = binding.chipAll.isChecked || binding.chipItems.isChecked

        val resultados = buildList(
            if (filtrarColecciones) lastColecciones else emptyList(),
            if (filtrarItems) lastItems else emptyList()
        )

        if (resultados.isEmpty() && (lastColecciones.isNotEmpty() || lastItems.isNotEmpty())) {
            // Caso donde hay resultados pero el filtro los oculta todos
            mostrarEstadoVacio()
        } else if (resultados.isEmpty()) {
            mostrarEstadoVacio()
        } else {
            mostrarResultados(resultados)
        }
    }

    private fun mostrarEstadoIdle() {
        binding.layoutIdle.visibility = View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE
        binding.rvResultados.visibility = View.GONE
        binding.chipGroupFiltros.visibility = View.GONE
    }

    private fun mostrarEstadoVacio() {
        binding.layoutIdle.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
        binding.rvResultados.visibility = View.GONE
        binding.chipGroupFiltros.visibility = if (lastColecciones.isNotEmpty() || lastItems.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun mostrarResultados(resultados: List<BusquedaItem>) {
        binding.layoutIdle.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE
        binding.rvResultados.visibility = View.VISIBLE
        binding.chipGroupFiltros.visibility = View.VISIBLE
        
        adapter.updateList(resultados)
        
        // Animación elegante de entrada
        val animation = android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.item_fade_up)
        binding.rvResultados.startAnimation(animation)
    }

    private fun buildList(colecciones: List<Coleccion>, items: List<Item>): List<BusquedaItem> {
        val list = mutableListOf<BusquedaItem>()
        if (colecciones.isNotEmpty()) {
            list.add(BusquedaItem.Header("Colecciones"))
            colecciones.forEach {
                list.add(BusquedaItem.Resultado(
                    id = it.id,
                    icono = "📁", // Icono por defecto para colecciones
                    titulo = it.nombre,
                    subtitulo = it.descripcion?.let { desc -> if (desc.length > 40) desc.take(40) + "..." else desc } ?: "",
                    tipo = "Colección",
                    esColeccion = true
                ))
            }
        }
        if (items.isNotEmpty()) {
            list.add(BusquedaItem.Header("Items"))
            items.forEach {
                list.add(BusquedaItem.Resultado(
                    id = it.id,
                    icono = "💎", // Icono por defecto para items
                    titulo = it.titulo,
                    subtitulo = it.estado,
                    tipo = it.estado,
                    esColeccion = false
                ))
            }
        }
        return list
    }

    private fun navegarAResultado(resultado: BusquedaItem.Resultado) {
        val fragment = if (resultado.esColeccion) {
            // Pasamos el ID de la colección a ItemListFragment mediante Bundle si no tiene newInstance
            ItemListFragment().apply {
                arguments = Bundle().apply {
                    putInt("coleccionId", resultado.id)
                    putString("coleccionNombre", resultado.titulo)
                }
            }
        } else {
            ItemDetailFragment.newInstance(resultado.id)
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
