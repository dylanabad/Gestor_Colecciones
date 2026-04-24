package com.example.gestor_colecciones.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DefaultItemAnimator
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialFadeThrough
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.adapters.DeseoAdapter
import com.example.gestor_colecciones.databinding.FragmentDeseosBinding
import com.example.gestor_colecciones.entities.ItemDeseo
import com.example.gestor_colecciones.repository.RepositoryProvider
import com.example.gestor_colecciones.viewmodel.DeseoViewModel
import com.example.gestor_colecciones.viewmodel.DeseoViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

// Fragmento que muestra y gestiona la lista de deseos del usuario
/**
 * Gestiona la lista de deseos del usuario.
 * 
 * Permite crear, editar, marcar como conseguido y enviar a papelera los deseos
 * persistidos tanto en local como en backend.
 */
class DeseosFragment : Fragment() {

    private var _binding: FragmentDeseosBinding? = null // Binding de la vista del fragmento
    private val binding get() = _binding!! // Acceso seguro al binding

    private lateinit var viewModel: DeseoViewModel // ViewModel para la lógica de deseos
    private lateinit var adapter: DeseoAdapter // Adapter del RecyclerView

    private var fullList: List<ItemDeseo> = emptyList() // Lista completa para filtrado

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Transición de entrada y salida del fragmento
        enterTransition = MaterialFadeThrough().apply { duration = 220 }
        returnTransition = MaterialFadeThrough().apply { duration = 200 }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDeseosBinding.inflate(inflater, container, false) // Inflado del layout
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicialización del repositorio y ViewModel
        val repo = RepositoryProvider.itemDeseoRepository(requireContext())
        viewModel = ViewModelProvider(this, DeseoViewModelFactory(repo))[DeseoViewModel::class.java]

        // Inicialización del adapter con acciones de usuario
        adapter = DeseoAdapter(
            emptyList(),
            onConseguido = { item ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("¡Conseguido!")
                    .setMessage("¿Marcar \"${item.titulo}\" como conseguido?")
                    .setPositiveButton("Sí") { _, _ ->
                        viewModel.marcarConseguido(item)
                        showSnackbar("🎉 \"${item.titulo}\" conseguido!")
                    }
                    .setNegativeButton("Cerrar", null)
                    .show()
            },
            onLongClick = { item -> showEditDialog(item) }
        )

        // Configuración del RecyclerView
        binding.rvDeseos.layoutManager = LinearLayoutManager(requireContext())
        binding.rvDeseos.adapter = adapter
        binding.rvDeseos.itemAnimator = DefaultItemAnimator().apply {
            addDuration = 300
            removeDuration = 300
            moveDuration = 300
            changeDuration = 300
        }

        // Swipe para eliminar elementos
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val item = adapter.getItem(viewHolder.adapterPosition)
                viewModel.delete(item)
                Snackbar.make(binding.root, "\"${item.titulo}\" movido a la papelera", Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.fabAddDeseo)
                    .setAction("Ver papelera") {
                        parentFragmentManager.beginTransaction()
                            .setReorderingAllowed(true)
                            .replace(R.id.fragment_container, PapeleraFragment.newInstance(2))
                            .addToBackStack(null)
                            .commit()
                    }
                    .show()
            }
        }
        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.rvDeseos)

        // Botones de navegación y acción
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }
        binding.fabAddDeseo.setOnClickListener { showAddDialog() }

        // Configuración de la búsqueda
        binding.searchDeseos.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                filterList(newText.orEmpty())
                return true
            }
        })

        // Observación de la lista de deseos
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.all.collectLatest { lista ->
                fullList = lista
                filterList(binding.searchDeseos.query.toString())

                // Contador de elementos pendientes
                val pendientes = lista.count { !it.conseguido }
                // binding.tvContador ahora es interno al buscador en el layout XML o se puede ocultar si se prefiere
                // Pero como lo hemos quitado del XML para poner el SearchView, ya no actualizamos binding.tvContador directamente aquí si no existe

                // Alerta de elementos sin precio objetivo
                val sinPrecio = lista.count { !it.conseguido && it.precioObjetivo == 0.0 }
                if (sinPrecio > 0) {
                    binding.cardAlerta.visibility = View.VISIBLE
                    binding.tvAlerta.text =
                        "⚠️ Tienes $sinPrecio item${if (sinPrecio > 1) "s" else ""} sin precio objetivo definido"
                } else {
                    binding.cardAlerta.visibility = View.GONE
                }
            }
        }
    }

    // Filtra la lista según el texto de búsqueda
    private fun filterList(query: String) {
        val filtered = if (query.isEmpty()) {
            fullList
        } else {
            fullList.filter {
                it.titulo.contains(query, ignoreCase = true) ||
                (it.descripcion?.contains(query, ignoreCase = true) ?: false)
            }
        }
        adapter.updateList(filtered)
    }

    // Diálogo para añadir un nuevo deseo
    private fun showAddDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_deseo, null)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nuevo deseo")
            .setView(dialogView)
            .setPositiveButton("Añadir") { _, _ ->
                val titulo = dialogView.findViewById<EditText>(R.id.etTituloDeseo)
                    .text.toString().trim()
                val descripcion = dialogView.findViewById<EditText>(R.id.etDescripcionDeseo)
                    .text.toString()
                val precioStr = dialogView.findViewById<EditText>(R.id.etPrecioDeseo)
                    .text.toString()
                val enlace = dialogView.findViewById<EditText>(R.id.etEnlaceDeseo)
                    .text.toString()
                val rgPrioridad = dialogView.findViewById<RadioGroup>(R.id.rgPrioridad)

                // Animación fluida al cambiar de prioridad
                rgPrioridad.setOnCheckedChangeListener { group, checkedId ->
                    android.transition.TransitionManager.beginDelayedTransition(
                        group,
                        android.transition.AutoTransition().setDuration(200)
                    )
                    
                    // Efecto de escala para que se note el movimiento
                    for (i in 0 until group.childCount) {
                        val child = group.getChildAt(i)
                        if (child.id == checkedId) {
                            child.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start()
                        } else {
                            child.animate().scaleX(0.95f).scaleY(0.95f).setDuration(200).start()
                        }
                    }
                }

                val prioridad = when (rgPrioridad.checkedRadioButtonId) {
                    R.id.rbAlta -> 1
                    R.id.rbMedia -> 2
                    else -> 3
                }

                if (titulo.isNotEmpty()) {
                    viewModel.insert(
                        ItemDeseo(
                            titulo = titulo,
                            descripcion = descripcion.ifBlank { null },
                            precioObjetivo = precioStr.toDoubleOrNull() ?: 0.0,
                            enlace = enlace.ifBlank { null },
                            prioridad = prioridad
                        )
                    )
                    showSnackbar("\"$titulo\" añadido a la lista")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Diálogo para editar un deseo existente
    private fun showEditDialog(item: ItemDeseo) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_deseo, null)

        // Relleno de datos actuales
        dialogView.findViewById<EditText>(R.id.etTituloDeseo).setText(item.titulo)
        dialogView.findViewById<EditText>(R.id.etDescripcionDeseo).setText(item.descripcion ?: "")
        dialogView.findViewById<EditText>(R.id.etPrecioDeseo)
            .setText(if (item.precioObjetivo > 0) item.precioObjetivo.toString() else "")
        dialogView.findViewById<EditText>(R.id.etEnlaceDeseo).setText(item.enlace ?: "")

        val rgPrioridad = dialogView.findViewById<RadioGroup>(R.id.rgPrioridad)
        
        // Animación fluida al cambiar de prioridad
        rgPrioridad.setOnCheckedChangeListener { group, checkedId ->
            android.transition.TransitionManager.beginDelayedTransition(
                group,
                android.transition.AutoTransition().setDuration(200)
            )
            
            // Efecto de escala para que se note el movimiento
            for (i in 0 until group.childCount) {
                val child = group.getChildAt(i)
                if (child.id == checkedId) {
                    child.animate().scaleX(1.05f).scaleY(1.05f).setDuration(200).start()
                } else {
                    child.animate().scaleX(0.95f).scaleY(0.95f).setDuration(200).start()
                }
            }
        }

        when (item.prioridad) {
            1 -> rgPrioridad.check(R.id.rbAlta)
            2 -> rgPrioridad.check(R.id.rbMedia)
            else -> rgPrioridad.check(R.id.rbBaja)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Editar deseo")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val titulo = dialogView.findViewById<EditText>(R.id.etTituloDeseo)
                    .text.toString().trim()
                val descripcion = dialogView.findViewById<EditText>(R.id.etDescripcionDeseo)
                    .text.toString()
                val precioStr = dialogView.findViewById<EditText>(R.id.etPrecioDeseo)
                    .text.toString()
                val enlace = dialogView.findViewById<EditText>(R.id.etEnlaceDeseo)
                    .text.toString()
                val prioridad = when (rgPrioridad.checkedRadioButtonId) {
                    R.id.rbAlta -> 1
                    R.id.rbMedia -> 2
                    else -> 3
                }

                if (titulo.isNotEmpty()) {
                    viewModel.update(
                        item.copy(
                            titulo = titulo,
                            descripcion = descripcion.ifBlank { null },
                            precioObjetivo = precioStr.toDoubleOrNull() ?: 0.0,
                            enlace = enlace.ifBlank { null },
                            prioridad = prioridad
                        )
                    )
                    showSnackbar("\"$titulo\" actualizado")
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // Muestra un Snackbar con un mensaje
    private fun showSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
            .setAnchorView(binding.fabAddDeseo)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Evita fugas de memoria
    }
}
