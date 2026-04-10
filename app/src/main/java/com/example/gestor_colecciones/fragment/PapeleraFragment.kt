package com.example.gestor_colecciones.fragment

/*
 * PapeleraFragment.kt
 *
 * Fragmento que muestra los elementos eliminados (colecciones e items) y permite
 * restaurarlos o eliminarlos de forma permanente. Contiene lógica para alternar
 * entre pestañas (colecciones/items), mostrar el conteo y manejar gestos de
 * deslizamiento para acciones rápidas.
 *
 * Nota: Solo se añaden comentarios explicativos en español; no se modifica la lógica.
 */

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.transition.MaterialFadeThrough
import com.example.gestor_colecciones.adapters.PapeleraAdapter
import com.example.gestor_colecciones.adapters.PapeleraItem
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.databinding.FragmentPapeleraBinding
import com.example.gestor_colecciones.entities.Coleccion
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.repository.RepositoryProvider
import com.example.gestor_colecciones.viewmodel.PapeleraViewModel
import com.example.gestor_colecciones.viewmodel.PapeleraViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit

class PapeleraFragment : Fragment() {

    // ViewBinding para acceder a las vistas del layout; se inicializa en onCreateView
    // y se limpia en onDestroyView para evitar fugas de memoria.
    private var _binding: FragmentPapeleraBinding? = null
    private val binding get() = _binding!!

    // ViewModel que expone flujos con los elementos en la papelera
    private lateinit var viewModel: PapeleraViewModel
    // Adaptador del RecyclerView que muestra elementos de la papelera
    private lateinit var adapter: PapeleraAdapter

    // Listas locales con colecciones e items eliminados (sin filtrar)
    private var coleccionesEliminadas: List<Coleccion> = emptyList()
    private var itemsEliminados: List<Item> = emptyList()
    // Índice de la pestaña actualmente visible (0 = colecciones, 1 = items)
    private var tabActual = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configurar transiciones suaves al mostrar/ocultar el fragmento
        enterTransition = MaterialFadeThrough().apply { duration = 220 }
        returnTransition = MaterialFadeThrough().apply { duration = 200 }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPapeleraBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Inicializar repositorios y ViewModel para recuperar datos de la papelera
        val db = DatabaseProvider.getDatabase(requireContext())
        val repo = RepositoryProvider.papeleraRepository(requireContext())
        viewModel = ViewModelProvider(this, PapeleraViewModelFactory(repo))[PapeleraViewModel::class.java]

        // Preparar adaptador del RecyclerView con callbacks para restaurar y eliminar
        adapter = PapeleraAdapter(
            emptyList(),
            onRestaurar = { item -> restaurar(item) },
            onLongClick = { item -> confirmarEliminacionDefinitiva(item) }
        )

        binding.rvPapelera.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPapelera.adapter = adapter

        // Manejo de swipe hacia la izquierda para eliminar definitivamente
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                confirmarEliminacionDefinitiva(adapter.getItem(viewHolder.adapterPosition))
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvPapelera)

        // Botón atrás
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        // Listener de las pestañas para alternar entre colecciones e items eliminados
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) { tabActual = tab.position; actualizarLista() }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        // Observar flujos del ViewModel y actualizar listas locales cuando cambien
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.coleccionesEliminadas.collectLatest { lista ->
                coleccionesEliminadas = lista
                actualizarLista()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.itemsEliminados.collectLatest { lista ->
                itemsEliminados = lista
                actualizarLista()
            }
        }
    }

    private fun actualizarLista() {
        // Actualiza el contador y el adaptador según la pestaña seleccionada
        val total = coleccionesEliminadas.size + itemsEliminados.size
        binding.tvContador.text = "$total elementos"
        val items = if (tabActual == 0) coleccionesEliminadas.map { it.toPapeleraItem() }
        else itemsEliminados.map { it.toPapeleraItem() }
        adapter.updateList(items)
    }

    private fun restaurar(papeleraItem: PapeleraItem) {
        // Restaura la colección o item según la pestaña activa
        if (tabActual == 0) {
            val coleccion = coleccionesEliminadas.find { it.id == papeleraItem.id } ?: return
            viewModel.restaurarColeccion(coleccion)
            showSnackbar("\"${coleccion.nombre}\" restaurada ✅")
        } else {
            val item = itemsEliminados.find { it.id == papeleraItem.id } ?: return
            viewModel.restaurarItem(item)
            showSnackbar("\"${item.titulo}\" restaurado ✅")
        }
    }

    private fun confirmarEliminacionDefinitiva(papeleraItem: PapeleraItem) {
        // Mostrar diálogo de confirmación antes de eliminar de forma permanente
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Eliminar definitivamente")
            .setMessage("\"${papeleraItem.nombre}\" se eliminará de forma permanente. ¿Continuar?")
            .setPositiveButton("Eliminar") { _, _ ->
                if (tabActual == 0) {
                    val coleccion = coleccionesEliminadas.find { it.id == papeleraItem.id } ?: return@setPositiveButton
                    viewModel.eliminarColeccionDefinitivamente(coleccion)
                } else {
                    val item = itemsEliminados.find { it.id == papeleraItem.id } ?: return@setPositiveButton
                    viewModel.eliminarItemDefinitivamente(item)
                }
                showSnackbar("Eliminado permanentemente")
            }
            .setNegativeButton("Cancelar") { _, _ -> actualizarLista() }
            .show()
    }

    private fun diasRestantes(fechaEliminacion: Date?): Int {
        // Calcula los días restantes hasta que el elemento sea eliminado definitivamente
        fechaEliminacion ?: return 0
        val expira = Calendar.getInstance().apply {
            time = fechaEliminacion
            add(Calendar.DAY_OF_YEAR, 30)
        }.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(expira - System.currentTimeMillis()).toInt().coerceAtLeast(0)
    }

    private fun Coleccion.toPapeleraItem() = PapeleraItem(
        id = id,
        icono = "📦",
        nombre = nombre,
        meta = descripcion?.ifBlank { "Sin descripción" } ?: "Sin descripción",
        fechaEliminacion = fechaEliminacion ?: Date(),
        diasRestantes = diasRestantes(fechaEliminacion)
    )

    private fun Item.toPapeleraItem() = PapeleraItem(
        id = id,
        icono = "🗂",
        nombre = titulo,
        meta = "Estado: $estado  ·  Valor: ${"%.2f".format(valor)} €",
        fechaEliminacion = fechaEliminacion ?: Date(),
        diasRestantes = diasRestantes(fechaEliminacion)
    )

    private fun showSnackbar(message: String) {
        // Muestra un Snackbar simple con el mensaje recibido
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
