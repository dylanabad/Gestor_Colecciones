package com.example.gestor_colecciones.fragment

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.adapters.PrestamoAdapter
import com.example.gestor_colecciones.auth.AuthStore
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.network.ApiProvider
import com.example.gestor_colecciones.network.dto.PrestamoDto
import com.example.gestor_colecciones.network.dto.PrestamoRequest
import com.example.gestor_colecciones.network.dto.UsuarioDto
import com.example.gestor_colecciones.repository.PrestamoRepository
import com.example.gestor_colecciones.repository.RepositoryProvider
import com.example.gestor_colecciones.viewmodel.PrestamoState
import com.example.gestor_colecciones.viewmodel.PrestamoViewModel
import com.example.gestor_colecciones.viewmodel.PrestamoViewModelFactory
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class PrestamosFragment : Fragment() {

    private lateinit var viewModel: PrestamoViewModel
    private lateinit var adapterPrestados: PrestamoAdapter
    private lateinit var adapterRecibidos: PrestamoAdapter

    private lateinit var tabLayout: TabLayout
    private lateinit var rvPrestados: RecyclerView
    private lateinit var rvRecibidos: RecyclerView
    private lateinit var layoutPrestados: View
    private lateinit var layoutRecibidos: View
    private lateinit var fabNuevoPrestamo: View
    private lateinit var emptyPrestados: View
    private lateinit var emptyRecibidos: View

    private var recibidosLoadedOnce = false
    private val prefs by lazy {
        requireContext().getSharedPreferences("prestamos_prefs", Context.MODE_PRIVATE)
    }

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingNotification?.invoke()
        }
        pendingNotification = null
    }
    private var pendingNotification: (() -> Unit)? = null

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
        layoutPrestados = view.findViewById(R.id.layoutPrestados)
        layoutRecibidos = view.findViewById(R.id.layoutRecibidos)
        fabNuevoPrestamo = view.findViewById(R.id.fabNuevoPrestamo)
        emptyPrestados = view.findViewById(R.id.emptyPrestados)
        emptyRecibidos = view.findViewById(R.id.emptyRecibidos)

        val currentUsername = AuthStore(requireContext()).getUsername()
        adapterPrestados = PrestamoAdapter(
            emptyList(),
            PrestamoAdapter.Modo.PRESTADOS,
            onDevolver = { prestamo -> confirmarDevolucion(prestamo) },
            onDelete = { prestamo -> confirmarEliminacion(prestamo) },
            currentUsername = currentUsername
        )
        adapterRecibidos = PrestamoAdapter(
            emptyList(),
            PrestamoAdapter.Modo.RECIBIDOS,
            onDevolver = null,
            onDelete = { prestamo -> confirmarEliminacion(prestamo) },
            currentUsername = currentUsername
        )

        rvPrestados.layoutManager = LinearLayoutManager(requireContext())
        rvPrestados.adapter = adapterPrestados
        rvRecibidos.layoutManager = LinearLayoutManager(requireContext())
        rvRecibidos.adapter = adapterRecibidos

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        layoutPrestados.visibility = View.VISIBLE
                        layoutRecibidos.visibility = View.GONE
                        viewModel.cargarPrestados()
                    }
                    1 -> {
                        layoutPrestados.visibility = View.GONE
                        layoutRecibidos.visibility = View.VISIBLE
                        viewModel.cargarRecibidos()
                    }
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
                handleRecibidosNotifications(lista)
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
                        val msg = state.message
                        if (msg.contains("No autenticado", ignoreCase = true) || msg.contains("autenticad", ignoreCase = true)) {
                            AuthStore(requireContext()).clear()
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
            .setTitle("Registrar devolucion")
            .setMessage("Confirmas que \"${prestamo.itemTitulo}\" ha sido devuelto por ${prestamo.prestatarioUsername}?")
            .setPositiveButton("Confirmar") { _, _ -> viewModel.devolverPrestamo(prestamo.movimientoId) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminacion(prestamo: PrestamoDto) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar prestamo")
            .setMessage("Deseas eliminar el prestamo de \"${prestamo.itemTitulo}\"?")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.eliminarPrestamo(prestamo.movimientoId) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showCrearPrestamoDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_crear_prestamo, null)
        val spinnerItem = dialogView.findViewById<Spinner>(R.id.spinnerItem)
        val spinnerUsuario = dialogView.findViewById<Spinner>(R.id.spinnerUsuario)
        val etFechaDevolucion = dialogView.findViewById<EditText>(R.id.etFechaDevolucion)
        val tilFechaDevolucion = dialogView.findViewById<TextInputLayout>(R.id.tilFechaDevolucion)
        val etNotas = dialogView.findViewById<EditText>(R.id.etNotas)

        var usuariosLista: List<UsuarioDto> = emptyList()
        var itemsLista: List<Item> = emptyList()
        var fechaSeleccionada: String? = null

        fun openDatePicker() {
            val picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Selecciona fecha")
                .build()
            picker.addOnPositiveButtonClickListener { millis ->
                val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                fmt.timeZone = TimeZone.getTimeZone("UTC")
                val formatted = fmt.format(java.util.Date(millis))
                fechaSeleccionada = formatted
                etFechaDevolucion.setText(formatted)
            }
            picker.show(parentFragmentManager, "datePickerPrestamo")
        }

        tilFechaDevolucion.setEndIconOnClickListener { openDatePicker() }
        etFechaDevolucion.setOnClickListener { openDatePicker() }

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

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val itemRepo = RepositoryProvider.itemRepository(requireContext())
                val items = itemRepo.allItems.first()
                itemsLista = items
                val labels = items.map { item ->
                    if (item.prestado) "${item.titulo} (Prestado)" else item.titulo
                }
                spinnerItem.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    labels
                )
            } catch (_: Exception) {
                spinnerItem.adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_dropdown_item,
                    emptyList<String>()
                )
            }
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nuevo prestamo")
            .setView(dialogView)
            .setPositiveButton("Prestar") { _, _ ->
                val usuarioSeleccionado = usuariosLista.getOrNull(spinnerUsuario.selectedItemPosition)
                val itemSeleccionado = itemsLista.getOrNull(spinnerItem.selectedItemPosition)
                val fechaDevolucion = fechaSeleccionada ?: etFechaDevolucion.text.toString().ifBlank { null }
                val notas = etNotas.text.toString().ifBlank { null }
                if (itemSeleccionado == null) {
                    Snackbar.make(requireView(), "Selecciona un item", Snackbar.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                if (itemSeleccionado.prestado) {
                    Snackbar.make(requireView(), "Este item ya esta prestado", Snackbar.LENGTH_SHORT).show()
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

    private fun handleRecibidosNotifications(lista: List<PrestamoDto>) {
        val currentIds = lista.map { it.movimientoId.toString() }.toSet()
        val stored = prefs.getStringSet("recibidos_ids", emptySet()) ?: emptySet()
        if (!recibidosLoadedOnce) {
            prefs.edit().putStringSet("recibidos_ids", currentIds).apply()
            recibidosLoadedOnce = true
            return
        }

        val nuevos = currentIds.minus(stored)
        if (nuevos.isNotEmpty()) {
            val nuevosItems = lista.filter { nuevos.contains(it.movimientoId.toString()) }
            notifyPrestamosRecibidos(nuevosItems)
        }
        prefs.edit().putStringSet("recibidos_ids", currentIds).apply()
    }

    private fun notifyPrestamosRecibidos(nuevos: List<PrestamoDto>) {
        if (nuevos.isEmpty()) return
        val send = {
            ensureNotificationChannel()
            val title = "Tienes prestamos recibidos"
            val content = if (nuevos.size == 1) {
                "Nuevo prestamo: ${nuevos.first().itemTitulo}"
            } else {
                "Tienes ${nuevos.size} nuevos prestamos"
            }
            val notification = NotificationCompat.Builder(requireContext(), "prestamos")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(requireContext()).notify(2001, notification)
        }

        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            pendingNotification = send
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            send()
        }
    }

    private fun ensureNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                "prestamos",
                "Prestamos",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = requireContext().getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
