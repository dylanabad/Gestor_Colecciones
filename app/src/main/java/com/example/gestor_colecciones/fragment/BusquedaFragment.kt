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

class BusquedaFragment : Fragment() {

    private var _binding: FragmentBusquedaBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BusquedaViewModel
    private lateinit var adapter: BusquedaAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialFadeThrough().apply { duration = 220 }
        returnTransition = MaterialFadeThrough().apply { duration = 200 }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.buscar(newText.orEmpty())
                return true
            }
        })

        // Enfocar automáticamente el teclado
        binding.searchView.requestFocus()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                when (state) {
                    is BusquedaState.Idle -> {
                        binding.layoutIdle.visibility = View.VISIBLE
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvResultados.visibility = View.GONE
                    }
                    is BusquedaState.Loading -> {
                        binding.layoutIdle.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvResultados.visibility = View.GONE
                    }
                    is BusquedaState.Empty -> {
                        binding.layoutIdle.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.VISIBLE
                        binding.rvResultados.visibility = View.GONE
                    }
                    is BusquedaState.Success -> {
                        binding.layoutIdle.visibility = View.GONE
                        binding.layoutEmpty.visibility = View.GONE
                        binding.rvResultados.visibility = View.VISIBLE
                        adapter.updateList(buildList(state.resultado.colecciones, state.resultado.items))
                    }
                }
            }
        }
    }

    private fun buildList(colecciones: List<Coleccion>, items: List<Item>): List<BusquedaItem> {
        val lista = mutableListOf<BusquedaItem>()

        if (colecciones.isNotEmpty()) {
            lista.add(BusquedaItem.Header("Colecciones (${colecciones.size})"))
            colecciones.forEach { c ->
                lista.add(BusquedaItem.Resultado(
                    id = c.id,
                    icono = "📦",
                    titulo = c.nombre,
                    subtitulo = c.descripcion?.ifBlank { "Sin descripción" } ?: "Sin descripción",
                    tipo = "Colección",
                    esColeccion = true
                ))
            }
        }

        if (items.isNotEmpty()) {
            lista.add(BusquedaItem.Header("Items (${items.size})"))
            items.forEach { item ->
                lista.add(BusquedaItem.Resultado(
                    id = item.id,
                    icono = "🗂",
                    titulo = item.titulo,
                    subtitulo = "${item.estado}  ·  ${"%.2f".format(item.valor)} €",
                    tipo = "Item",
                    esColeccion = false
                ))
            }
        }

        return lista
    }

    private fun navegarAResultado(resultado: BusquedaItem.Resultado) {
        val fragment = if (resultado.esColeccion) {
            ItemListByCollectionFragment.newInstance(resultado.id)
        } else {
            ItemDetailFragment.newInstance(resultado.id)
        }
        parentFragmentManager.beginTransaction()
            .setReorderingAllowed(true)
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}