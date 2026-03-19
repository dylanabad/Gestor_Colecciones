package com.example.gestor_colecciones.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.model.ItemFilterSortState
import com.example.gestor_colecciones.model.ItemSortField
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import java.util.Locale

class ItemFilterSortBottomSheet : BottomSheetDialogFragment() {

    override fun onStart() {
        super.onStart()
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottomsheet_item_filter_sort, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val actvCategoria = view.findViewById<MaterialAutoCompleteTextView>(R.id.actvCategoria)
        val actvEstado = view.findViewById<MaterialAutoCompleteTextView>(R.id.actvEstado)
        val tvMinRating = view.findViewById<TextView>(R.id.tvMinRating)
        val sliderMinRating = view.findViewById<Slider>(R.id.sliderMinRating)
        val tgSortField = view.findViewById<MaterialButtonToggleGroup>(R.id.tgSortField)
        val switchAscending = view.findViewById<SwitchMaterial>(R.id.switchAscending)
        val btnClear = view.findViewById<Button>(R.id.btnClear)
        val btnApply = view.findViewById<Button>(R.id.btnApply)

        val categoryIds = requireArguments().getIntArray(ARG_CATEGORY_IDS) ?: intArrayOf(-1)
        val categoryNames = requireArguments().getStringArray(ARG_CATEGORY_NAMES) ?: arrayOf("Todas")
        val statusList = requireArguments().getStringArray(ARG_STATUS_LIST) ?: arrayOf("Cualquiera")

        val currentCategoriaId = requireArguments().getInt(ARG_CURRENT_CATEGORY_ID, -1)
        val currentEstado = requireArguments().getString(ARG_CURRENT_ESTADO, null)
        val currentMinRating = requireArguments().getFloat(ARG_CURRENT_MIN_RATING, 0f)
        val currentSortFieldName = requireArguments().getString(ARG_CURRENT_SORT_FIELD, ItemSortField.DATE.name)
        val currentAscending = requireArguments().getBoolean(ARG_CURRENT_ASCENDING, false)

        var selectedCategoryIndex = categoryIds.indexOf(currentCategoriaId).takeIf { it >= 0 } ?: 0
        var selectedStatusIndex = statusList.indexOf(currentEstado).takeIf { it >= 0 } ?: 0

        val categoriaAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, categoryNames)
        actvCategoria.setAdapter(categoriaAdapter)
        actvCategoria.setText(categoryNames.getOrNull(selectedCategoryIndex) ?: categoryNames.firstOrNull().orEmpty(), false)
        actvCategoria.keyListener = null
        actvCategoria.isCursorVisible = false
        actvCategoria.setOnClickListener { actvCategoria.showDropDown() }
        actvCategoria.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) actvCategoria.showDropDown() }
        actvCategoria.setOnItemClickListener { _, _, position, _ -> selectedCategoryIndex = position }

        val estadoAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, statusList)
        actvEstado.setAdapter(estadoAdapter)
        actvEstado.setText(statusList.getOrNull(selectedStatusIndex) ?: statusList.firstOrNull().orEmpty(), false)
        actvEstado.keyListener = null
        actvEstado.isCursorVisible = false
        actvEstado.setOnClickListener { actvEstado.showDropDown() }
        actvEstado.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) actvEstado.showDropDown() }
        actvEstado.setOnItemClickListener { _, _, position, _ -> selectedStatusIndex = position }

        fun updateRatingLabel(value: Float) {
            val formatted = String.format(Locale.getDefault(), "%.1f", value)
            tvMinRating.text = "Calificación mínima: $formatted"
        }
        sliderMinRating.value = currentMinRating.coerceIn(0f, 5f)
        updateRatingLabel(sliderMinRating.value)
        sliderMinRating.addOnChangeListener { _, value, _ -> updateRatingLabel(value) }

        val currentSortField = runCatching { ItemSortField.valueOf(currentSortFieldName ?: ItemSortField.DATE.name) }
            .getOrDefault(ItemSortField.DATE)
        tgSortField.check(
            when (currentSortField) {
                ItemSortField.NAME -> R.id.btnSortName
                ItemSortField.VALUE -> R.id.btnSortValue
                ItemSortField.DATE -> R.id.btnSortDate
            }
        )
        switchAscending.isChecked = currentAscending

        fun resetToDefault() {
            selectedCategoryIndex = 0
            selectedStatusIndex = 0
            actvCategoria.setText(categoryNames.firstOrNull().orEmpty(), false)
            actvEstado.setText(statusList.firstOrNull().orEmpty(), false)
            sliderMinRating.value = 0f
            tgSortField.check(R.id.btnSortDate)
            switchAscending.isChecked = false
        }

        btnClear.setOnClickListener { resetToDefault() }

        btnApply.setOnClickListener {
            val selectedCategoryId = categoryIds.getOrNull(selectedCategoryIndex) ?: -1

            val estadoSelected = statusList.getOrNull(selectedStatusIndex)
                ?.takeIf { it.isNotBlank() && it != statusList.firstOrNull() }

            val sortField = when (tgSortField.checkedButtonId) {
                R.id.btnSortName -> ItemSortField.NAME
                R.id.btnSortValue -> ItemSortField.VALUE
                else -> ItemSortField.DATE
            }

            parentFragmentManager.setFragmentResult(
                RESULT_KEY,
                Bundle().apply {
                    putInt(BUNDLE_CATEGORY_ID, selectedCategoryId)
                    putString(BUNDLE_ESTADO, estadoSelected)
                    putFloat(BUNDLE_MIN_RATING, sliderMinRating.value)
                    putString(BUNDLE_SORT_FIELD, sortField.name)
                    putBoolean(BUNDLE_ASCENDING, switchAscending.isChecked)
                }
            )
            dismiss()
        }
    }

    companion object {
        const val RESULT_KEY = "item_filter_sort_result"

        const val BUNDLE_CATEGORY_ID = "category_id"
        const val BUNDLE_ESTADO = "estado"
        const val BUNDLE_MIN_RATING = "min_rating"
        const val BUNDLE_SORT_FIELD = "sort_field"
        const val BUNDLE_ASCENDING = "ascending"

        private const val ARG_CATEGORY_IDS = "arg_category_ids"
        private const val ARG_CATEGORY_NAMES = "arg_category_names"
        private const val ARG_STATUS_LIST = "arg_status_list"
        private const val ARG_CURRENT_CATEGORY_ID = "arg_current_category_id"
        private const val ARG_CURRENT_ESTADO = "arg_current_estado"
        private const val ARG_CURRENT_MIN_RATING = "arg_current_min_rating"
        private const val ARG_CURRENT_SORT_FIELD = "arg_current_sort_field"
        private const val ARG_CURRENT_ASCENDING = "arg_current_ascending"

        fun newInstance(
            categoryIds: IntArray,
            categoryNames: Array<String>,
            statusList: Array<String>,
            current: ItemFilterSortState
        ): ItemFilterSortBottomSheet {
            val currentCategoryId = current.categoriaId ?: -1
            return ItemFilterSortBottomSheet().apply {
                arguments = Bundle().apply {
                    putIntArray(ARG_CATEGORY_IDS, categoryIds)
                    putStringArray(ARG_CATEGORY_NAMES, categoryNames)
                    putStringArray(ARG_STATUS_LIST, statusList)
                    putInt(ARG_CURRENT_CATEGORY_ID, currentCategoryId)
                    putString(ARG_CURRENT_ESTADO, current.estado)
                    putFloat(ARG_CURRENT_MIN_RATING, current.minCalificacion)
                    putString(ARG_CURRENT_SORT_FIELD, current.sortField.name)
                    putBoolean(ARG_CURRENT_ASCENDING, current.ascending)
                }
            }
        }
    }
}
