package com.example.gestor_colecciones.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import com.example.gestor_colecciones.repository.RepositoryProvider
import com.example.gestor_colecciones.entities.Item
import kotlinx.coroutines.flow.first
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.auth.AuthStore
import com.example.gestor_colecciones.adapters.PrestamoAdapter
import com.example.gestor_colecciones.network.ApiProvider
import com.example.gestor_colecciones.network.dto.PrestamoDto
import com.example.gestor_colecciones.network.dto.PrestamoRequest
import com.example.gestor_colecciones.network.dto.UsuarioDto
import com.example.gestor_colecciones.repository.PrestamoRepository
import com.example.gestor_colecciones.viewmodel.PrestamoState
import com.example.gestor_colecciones.viewmodel.PrestamoViewModel
import com.example.gestor_colecciones.viewmodel.PrestamoViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PrestamosFragment : Fragment() {

    private lateinit var viewModel: PrestamoViewModel
    private lateinit var adapterPrestados: PrestamoAdapter
    private lateinit var adapterRecibidos: PrestamoAdapter

    private lateinit var tabLayout: TabLayout
    private lateinit var rvPrestados: RecyclerView
    private lateinit var rvRecibidos: RecyclerView
    private lateinit var fabNuevoPrestamo: View
    private lateinit var emptyPrestados: View
    private lateinit var emptyRecibidos: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_prestamos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repo = PrestamoRepository(ApiProvider.getApi(requireContext()))
        viewModel = ViewModelProvider(
            this, PrestamoViewModelFactory(repo)
        )[PrestamoViewModel::class.java]

        tabLayout = view.findViewById(R.id.tabLayoutPrestamos)
        rvPrestados = view.findViewById(R.id.rvPrestados)
        rvRecibidos = view.findViewById(R.id.rvRecibidos)
        fabNuevoPrestamo = view.findViewById(R.id.fabNuevoPrestamo)
        emptyPrestados = view.findViewById(R.id.emptyPrestados)
        emptyRecibidos = view.findViewById(R.id.emptyRecibidos)

        adapterPrestados = PrestamoAdapter(
            emptyList(),
            PrestamoAdapter.Modo.PRESTADOS,
            onDevolver = { prestamo -> confirmarDevolucion(prestamo) }
        )
        adapterRecibidos = PrestamoAdapter(emptyList(), PrestamoAdapter.Modo.RECIBIDOS)

        rvPrestados.layoutManager = LinearLayoutManager(requireContext())
        rvPrestados.adapter = adapterPrestados
        rvRecibidos.layoutManager = LinearLayoutManager(requireContext())
        rvRecibidos.adapter = adapterRecibidos

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> { rvPrestados.visibility = View.VISIBLE; rvRecibidos.visibility = View.GONE }
                    1 -> { rvPrestados.visibility = View.GONE; rvRecibidos.visibility = View.VISIBLE }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        fabNuevoPrestamo.setOnClickListener {
            viewModel.cargarUsuarios()
            showCrearPrestamoDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.prestados.collectLatest { lista ->
                adapterPrestados.updateList(lista)
                emptyPrestados.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recibidos.collectLatest { lista ->
                adapterRecibidos.updateList(lista)
                emptyRecibidos.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.state.collectLatest { state ->
                when (state) {
                    is PrestamoState.Success -> {
                        Snackbar.make(view, state.message, Snackbar.LENGTH_SHORT).show()
                        viewModel.resetState()
                    }
                    is PrestamoState.Error -> {
                        Snackbar.make(view, state.message, Snackbar.LENGTH_LONG).show()
                        // Si el error es por no estar autenticado, limpiamos el token y navegamos al login
                        val msg = state.message
                        if (msg.contains("No autenticado", ignoreCase = true) || msg.contains("autenticad", ignoreCase = true)) {
                            AuthStore(requireContext()).clear()
                            // Reemplazamos el fragment actual por AuthFragment (navegación simple)
                            parentFragmentManager.beginTransaction()
                                .setReorderingAllowed(true)
                                .replace((view.parent as ViewGroup).id, AuthFragment())
                                .commit()
                        }
                        viewModel.resetState()
                    }
                    else -> {}
                }
            }
        }

        viewModel.cargarPrestados()
        viewModel.cargarRecibidos()
    }

    private fun confirmarDevolucion(prestamo: PrestamoDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Registrar devolución")
            .setMessage("¿Confirmas que \"${prestamo.itemTitulo}\" ha sido devuelto por ${prestamo.prestatarioUsername}?")
            .setPositiveButton("Confirmar") { _, _ -> viewModel.devolverPrestamo(prestamo.movimientoId) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showCrearPrestamoDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_crear_prestamo, null)
        val spinnerItem = dialogView.findViewById<Spinner>(R.id.spinnerItem)
        val spinnerUsuario = dialogView.findViewById<Spinner>(R.id.spinnerUsuario)
        val etFechaDevolucion = dialogView.findViewById<EditText>(R.id.etFechaDevolucion)
        val etNotas = dialogView.findViewById<EditText>(R.id.etNotas)

        var usuariosLista: List<UsuarioDto> = emptyList()
        var itemsLista: List<Item> = emptyList()

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.usuarios.collectLatest { usuarios ->
                usuariosLista = usuarios
                spinnerUsuario.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    usuarios.map { it.username }
                )
            }
        }

        // Cargamos items locales y rellenamos spinner de items
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val itemRepo = RepositoryProvider.itemRepository(requireContext())
                val items = itemRepo.allItems.first()
                itemsLista = items
                spinnerItem.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    items.map { it.titulo }
                )
            } catch (_: Exception) {
                // Si falla la carga de items, dejamos el spinner vacío
                spinnerItem.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, emptyList<String>())
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nuevo préstamo")
            .setView(dialogView)
            .setPositiveButton("Prestar") { _, _ ->
                val usuarioSeleccionado = usuariosLista.getOrNull(spinnerUsuario.selectedItemPosition)
                val itemSeleccionado = itemsLista.getOrNull(spinnerItem.selectedItemPosition)
                val fechaDevolucion = etFechaDevolucion.text.toString().ifBlank { null }
                val notas = etNotas.text.toString().ifBlank { null }
                if (itemSeleccionado == null) {
                    Snackbar.make(requireView(), "Selecciona un ítem", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (usuarioSeleccionado == null) {
                    Snackbar.make(requireView(), "Selecciona un usuario", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.crearPrestamo(
                    PrestamoRequest(
                        itemId = itemSeleccionado.id.toLong(),
                        prestatarioUsuarioId = usuarioSeleccionado.id,
                        fechaDevolucionPrevista = fechaDevolucion,
                        notas = notas
                    )
                )
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }
}