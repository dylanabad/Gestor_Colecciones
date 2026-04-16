package com.example.gestor_colecciones.fragment

/*
 * PrestamosFragment.kt
 *
 * Fragmento responsable de gestionar préstamos: muestra listas de préstamos
 * prestados y recibidos, permite crear nuevos préstamos, registrar devoluciones
 * y eliminar préstamos. También maneja notificaciones locales cuando se reciben
 * nuevos préstamos.
 *
 */

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
import android.widget.TextView
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
import com.example.gestor_colecciones.database.DatabaseProvider
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
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PrestamosFragment : Fragment() {

    // ViewModel y adaptadores para las dos vistas (prestados / recibidos)
    private lateinit var viewModel: PrestamoViewModel
    private lateinit var adapterPrestados: PrestamoAdapter
    private lateinit var adapterRecibidos: PrestamoAdapter

    // Referencias a vistas del layout (se inicializan en onViewCreated)
    private lateinit var tabLayout: TabLayout
    private lateinit var rvPrestados: RecyclerView
    private lateinit var rvRecibidos: RecyclerView
    private lateinit var layoutPrestados: View
    private lateinit var layoutRecibidos: View
    private lateinit var fabNuevoPrestamo: View
    private lateinit var emptyPrestados: View
    private lateinit var emptyRecibidos: View
    private lateinit var tvCountPrestados: TextView
    private lateinit var tvCountRecibidos: TextView
    private lateinit var tvCountVencidos: TextView

    // Estado para evitar sobrescribir el set inicial de IDs recibidos
    private var recibidosLoadedOnce = false
    // SharedPreferences local para llevar el control de IDs ya notificados
    private val prefs by lazy {
        requireContext().getSharedPreferences("prestamos_prefs", Context.MODE_PRIVATE)
    }

    // Lanzador para solicitar el permiso de notificaciones en Android 13+
    // Si el permiso se concede, ejecuta la notificación pendiente (si existe)
    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingNotification?.invoke()
        }
        pendingNotification = null
    }
    // Función pendiente que envía la notificación (se guarda mientras se pide permiso)
    private var pendingNotification: (() -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_prestamos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Inicializar repositorio y ViewModel
        val repo = RepositoryProvider.prestamoRepository(requireContext())
        viewModel = ViewModelProvider(
            this, PrestamoViewModelFactory(repo)
        )[PrestamoViewModel::class.java]

        // Referencias a las vistas del layout
        tabLayout = view.findViewById(R.id.tabLayoutPrestamos)
        rvPrestados = view.findViewById(R.id.rvPrestados)
        rvRecibidos = view.findViewById(R.id.rvRecibidos)
        layoutPrestados = view.findViewById(R.id.layoutPrestados)
        layoutRecibidos = view.findViewById(R.id.layoutRecibidos)
        fabNuevoPrestamo = view.findViewById(R.id.fabNuevoPrestamo)
        emptyPrestados = view.findViewById(R.id.emptyPrestados)
        emptyRecibidos = view.findViewById(R.id.emptyRecibidos)
        tvCountPrestados = view.findViewById(R.id.tvCountPrestados)
        tvCountRecibidos = view.findViewById(R.id.tvCountRecibidos)
        tvCountVencidos = view.findViewById(R.id.tvCountVencidos)

        // Crear adaptadores para las dos listas (prestados y recibidos)
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

        // Configurar RecyclerViews
        rvPrestados.layoutManager = LinearLayoutManager(requireContext())
        rvPrestados.adapter = adapterPrestados
        rvRecibidos.layoutManager = LinearLayoutManager(requireContext())
        rvRecibidos.adapter = adapterRecibidos

        // Listener de pestañas: cambia la vista visible y carga los datos según la pestaña
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

        // FAB para crear un nuevo préstamo
        fabNuevoPrestamo.setOnClickListener {
            viewModel.cargarUsuarios()
            showCrearPrestamoDialog()
        }

        var latestPrestados: List<PrestamoDto> = emptyList()
        var latestRecibidos: List<PrestamoDto> = emptyList()

        // Observar flujos del ViewModel y actualizar adaptadores/UI
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.prestados.collectLatest { lista ->
                latestPrestados = lista
                adapterPrestados.updateList(lista)
                emptyPrestados.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                actualizarHeader(latestPrestados, latestRecibidos)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.recibidos.collectLatest { lista ->
                latestRecibidos = lista
                adapterRecibidos.updateList(lista)
                emptyRecibidos.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                handleRecibidosNotifications(lista)
                actualizarHeader(latestPrestados, latestRecibidos)
            }
        }

        // Estado global del ViewModel: errores o mensajes de éxito
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
                        // Si hay problema de autenticación, limpiar credenciales y volver al login
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

        // Cargar inicialmente ambas listas
        viewModel.cargarPrestados()
        viewModel.cargarRecibidos()
    }

    private fun actualizarHeader(prestados: List<PrestamoDto>, recibidos: List<PrestamoDto>) {
        tvCountPrestados.text = prestados.size.toString()
        tvCountRecibidos.text = recibidos.size.toString()
        tvCountVencidos.text = (prestados + recibidos).count { esVencido(it) }.toString()
    }

    private fun esVencido(p: PrestamoDto): Boolean {
        if (p.estado.equals("DEVUELTO", ignoreCase = true)) return false
        val fechaStr = p.fechaDevolucionPrevista?.take(10) ?: return false
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fecha = fmt.parse(fechaStr)
            fecha != null && fecha.before(Date())
        } catch (_: Exception) {
            false
        }
    }

    private fun confirmarDevolucion(prestamo: PrestamoDto) {
        // Mostrar diálogo para confirmar la devolución de un préstamo
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Registrar devolucion")
            .setMessage("Confirmas que \"${prestamo.itemTitulo}\" ha sido devuelto por ${prestamo.prestatarioUsername}?")
            .setPositiveButton("Confirmar") { _, _ -> viewModel.devolverPrestamo(prestamo.movimientoId) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun confirmarEliminacion(prestamo: PrestamoDto) {
        // Mostrar diálogo de confirmación para eliminar un préstamo
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar prestamo")
            .setMessage("Deseas eliminar el prestamo de \"${prestamo.itemTitulo}\"?")
            .setPositiveButton("Eliminar") { _, _ -> viewModel.eliminarPrestamo(prestamo.movimientoId) }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showCrearPrestamoDialog() {
        // Muestra un diálogo para crear un nuevo préstamo: selector de item, usuario,
        // fecha de devolución y notas. Gestiona la lógica de pickers y validaciones
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_crear_prestamo, null)
        val spinnerItem = dialogView.findViewById<Spinner>(R.id.spinnerItem)
        val spinnerUsuario = dialogView.findViewById<Spinner>(R.id.spinnerUsuario)
        val etFechaDevolucion = dialogView.findViewById<EditText>(R.id.etFechaDevolucion)
        val tilFechaDevolucion = dialogView.findViewById<TextInputLayout>(R.id.tilFechaDevolucion)
        val etNotas = dialogView.findViewById<EditText>(R.id.etNotas)

        // Variables locales que se rellenan asíncronamente desde repositorios
        var usuariosLista: List<UsuarioDto> = emptyList()
        var itemsLista: List<Item> = emptyList()
        var fechaSeleccionada: String? = null

        // Abre un DatePicker para seleccionar la fecha de devolución prevista
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

        // Asociar el picker al TextInputLayout y al EditText
        tilFechaDevolucion.setEndIconOnClickListener { openDatePicker() }
        etFechaDevolucion.setOnClickListener { openDatePicker() }

        // Rellenar el spinner de usuarios desde el flujo de usuarios del ViewModel
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

        // Cargar lista de items desde repositorio (operación IO), y preparar labels
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
                // En caso de fallo, dejar el spinner vacío
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
                // Validar selección y crear el préstamo a través del ViewModel
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
        // Gestor de notificaciones para préstamos recibidos. Compara el set actual de IDs
        // con los almacenados en SharedPreferences para detectar nuevos préstamos.
        val currentIds = lista.map { it.movimientoId.toString() }.toSet()
        val stored = prefs.getStringSet("recibidos_ids", emptySet()) ?: emptySet()
        if (!recibidosLoadedOnce) {
            // Primera carga: guardar el estado inicial y no notificar
            prefs.edit().putStringSet("recibidos_ids", currentIds).apply()
            recibidosLoadedOnce = true
            return
        }

        // Calcular IDs nuevos y notificar si hay alguno
        val nuevos = currentIds.minus(stored)
        if (nuevos.isNotEmpty()) {
            val nuevosItems = lista.filter { nuevos.contains(it.movimientoId.toString()) }
            notifyPrestamosRecibidos(nuevosItems)
        }
        // Actualizar el set almacenado
        prefs.edit().putStringSet("recibidos_ids", currentIds).apply()
    }

    private fun notifyPrestamosRecibidos(nuevos: List<PrestamoDto>) {
        // Construye y envía una notificación local informando de nuevos préstamos recibidos.
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

        // Si estamos en Android 13+, solicitar permiso POST_NOTIFICATIONS si no está concedido.
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            // Guardar la acción pendiente y lanzar el request de permiso
            pendingNotification = send
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            send()
        }
    }

    private fun ensureNotificationChannel() {
        // Crear canal de notificación (necesario en Android O+)
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
