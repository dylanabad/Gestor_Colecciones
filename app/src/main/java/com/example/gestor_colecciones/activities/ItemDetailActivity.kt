package com.example.gestor_colecciones.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.databinding.ActivityItemDetailBinding
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.entities.Item
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ItemDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityItemDetailBinding
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recuperar ID del ítem pasado en el Intent
        val itemId = intent.getIntExtra("ITEM_ID", -1)
        if (itemId != -1) {
            loadItem(itemId)
        }
    }

    private fun loadItem(itemId: Int) {
        val db = DatabaseProvider.getDatabase(this)
        scope.launch {
            val item: Item? = db.itemDao().getItemById(itemId) // DAO debe tener getItemById
            item?.let {
                binding.tvTitleDetail.text = it.titulo
                binding.tvCategoryDetail.text = it.categoriaId.toString() // Podrías mapear a nombre de categoría
                binding.tvValorDetail.text = "Valor: \$${it.valor}"
                binding.tvDescripcionDetail.text = it.descripcion ?: ""
                // Si tienes imagen en path
                // binding.ivItemDetail.setImageURI(Uri.parse(it.imagenPath))
            }
        }
    }
}