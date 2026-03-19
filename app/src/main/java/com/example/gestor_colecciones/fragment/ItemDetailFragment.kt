package com.example.gestor_colecciones.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.entities.Tag
import com.example.gestor_colecciones.repository.ItemTagRepository
import com.example.gestor_colecciones.repository.ItemRepository
import com.example.gestor_colecciones.repository.TagRepository
import com.example.gestor_colecciones.viewmodel.ItemViewModel
import com.example.gestor_colecciones.viewmodel.ItemViewModelFactory
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialSharedAxis
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class ItemDetailFragment : Fragment() {

    private var itemId: Int = 0
    private lateinit var viewModel: ItemViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        itemId = arguments?.getInt(ARG_ITEM_ID) ?: 0
        enterTransition = MaterialSharedAxis(MaterialSharedAxis.X, true).apply { duration = 240 }
        returnTransition = MaterialSharedAxis(MaterialSharedAxis.X, false).apply { duration = 220 }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_item_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = DatabaseProvider.getDatabase(requireContext())
        val repo = ItemRepository(db.itemDao())
        val tagRepository = TagRepository(db.tagDao())
        val itemTagRepository = ItemTagRepository(db.itemTagDao())
        viewModel = ItemViewModelFactory(repo).create(ItemViewModel::class.java)

        val tvTitulo = view.findViewById<TextView>(R.id.tvTitulo)
        val tvValor = view.findViewById<TextView>(R.id.tvValor)
        val tvEstado = view.findViewById<TextView>(R.id.tvEstado)
        val tvDescripcion = view.findViewById<TextView>(R.id.tvDescripcion)
        val tvCategoria = view.findViewById<TextView>(R.id.tvCategoria)
        val tvCalificacion = view.findViewById<TextView>(R.id.tvCalificacion)
        val ivImagen = view.findViewById<ImageView>(R.id.ivImagen)
        val rbRating = view.findViewById<RatingBar>(R.id.rbItemRating)
        val chipGroupTags = view.findViewById<ChipGroup>(R.id.chipGroupTags)
        val btnEditTags = view.findViewById<View>(R.id.btnEditTags)

        var currentItem: Item? = null
        rbRating.setOnRatingBarChangeListener { _, rating, fromUser ->
            if (!fromUser) return@setOnRatingBarChangeListener
            val item = currentItem ?: return@setOnRatingBarChangeListener
            val clamped = rating.coerceIn(0f, 5f)
            tvCalificacion.text = String.format(Locale.getDefault(), "%.1f", clamped)
            val updated = item.copy(calificacion = clamped)
            currentItem = updated
            viewModel.update(updated) {
                Snackbar.make(view, "Calificación actualizada", Snackbar.LENGTH_SHORT).show()
            }
        }

        fun renderTags(tags: List<Tag>) {
            chipGroupTags.removeAllViews()
            if (tags.isEmpty()) {
                chipGroupTags.addView(
                    Chip(requireContext()).apply {
                        text = "Sin etiquetas"
                        isClickable = false
                        isCheckable = false
                        isEnabled = false
                    }
                )
                return
            }
            tags.forEach { tag ->
                chipGroupTags.addView(
                    Chip(requireContext()).apply {
                        text = tag.nombre
                        isClickable = false
                        isCheckable = false
                    }
                )
            }
        }

        fun showCreateTagDialog(onCreated: () -> Unit) {
            val input = EditText(requireContext()).apply {
                hint = "Nombre de etiqueta"
            }
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Nueva etiqueta")
                .setView(input)
                .setPositiveButton("Crear") { _, _ ->
                    val nombre = input.text?.toString()?.trim().orEmpty()
                    if (nombre.isBlank()) return@setPositiveButton
                    viewLifecycleOwner.lifecycleScope.launch {
                        tagRepository.insert(Tag(nombre = nombre))
                        Snackbar.make(view, "Etiqueta \"$nombre\" creada", Snackbar.LENGTH_SHORT).show()
                        onCreated()
                    }
                }
                .setNegativeButton("Cancelar", null)
                .show()
        }

        fun showTagPickerDialog() {
            viewLifecycleOwner.lifecycleScope.launch {
                val allTags = tagRepository.getAllTagsOnce()
                if (allTags.isEmpty()) {
                    showCreateTagDialog { showTagPickerDialog() }
                    return@launch
                }

                val selectedIds = tagRepository.getTagsForItemOnce(itemId).map { it.id }.toSet()
                val names = allTags.map { it.nombre }.toTypedArray()
                val checked = BooleanArray(allTags.size) { idx -> allTags[idx].id in selectedIds }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Etiquetas")
                    .setMultiChoiceItems(names, checked) { _, which, isChecked ->
                        checked[which] = isChecked
                    }
                    .setNeutralButton("Nueva") { _, _ ->
                        showCreateTagDialog { showTagPickerDialog() }
                    }
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Guardar") { _, _ ->
                        viewLifecycleOwner.lifecycleScope.launch {
                            val tagIds = allTags.mapIndexedNotNull { index, tag -> tag.id.takeIf { checked[index] } }
                            itemTagRepository.replaceTagsForItem(itemId, tagIds)
                            Snackbar.make(view, "Etiquetas actualizadas", Snackbar.LENGTH_SHORT).show()
                        }
                    }
                    .show()
            }
        }

        btnEditTags.setOnClickListener { showTagPickerDialog() }

        viewLifecycleOwner.lifecycleScope.launch {
            tagRepository.getTagsForItem(itemId).collect { tags ->
                renderTags(tags)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val item: Item? = viewModel.getItemById(itemId)
            item?.let {
                currentItem = it
                tvTitulo.text = it.titulo
                tvValor.text = "Valor: ${it.valor}"
                tvEstado.text = "Estado: ${it.estado}"
                tvDescripcion.text = "Descripción: ${it.descripcion ?: "N/A"}"
                val rating = it.calificacion.coerceIn(0f, 5f)
                rbRating.rating = rating
                tvCalificacion.text = String.format(Locale.getDefault(), "%.1f", rating)

                val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                view.findViewById<TextView>(R.id.tvFecha).text = "Fecha: ${sdf.format(it.fechaAdquisicion)}"

                // Cargar imagen si existe
                val imagePath = it.imagenPath
                val file = imagePath?.let { path -> File(path) }
                if (file != null && file.exists()) {
                    Glide.with(this@ItemDetailFragment)
                        .load(file)
                        .centerCrop()
                        .transition(DrawableTransitionOptions.withCrossFade(180))
                        .placeholder(R.drawable.ic_no_image)
                        .error(R.drawable.ic_no_image)
                        .into(ivImagen)
                } else {
                    ivImagen.setImageResource(R.drawable.ic_no_image) // icono por defecto
                }

                // Cargar categoría (si quieres mostrar nombre de categoría)
                val categoriaDao = db.categoriaDao()
                val categoria = categoriaDao.getAllCategoriasOnce().find { cat -> cat.id == it.categoriaId }
                tvCategoria.text = "Categoría: ${categoria?.nombre ?: "Sin categoría"}"
            }
        }
    }

    companion object {
        private const val ARG_ITEM_ID = "item_id"

        fun newInstance(itemId: Int) = ItemDetailFragment().apply {
            arguments = Bundle().apply { putInt(ARG_ITEM_ID, itemId) }
        }
    }
}
