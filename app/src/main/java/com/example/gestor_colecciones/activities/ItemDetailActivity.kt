package com.example.gestor_colecciones.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.gestor_colecciones.databinding.ActivityItemDetailBinding
import com.example.gestor_colecciones.database.DatabaseProvider
import com.example.gestor_colecciones.entities.Item
import com.example.gestor_colecciones.util.ThemeManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Activity encargada de mostrar el detalle de un ítem
class ItemDetailActivity : AppCompatActivity() {

    // ViewBinding para acceder a las vistas del layout de detalle
    private lateinit var binding: ActivityItemDetailBinding

    // Scope de corrutinas usando el hilo principal (UI)
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.apply(this)
        super.onCreate(savedInstanceState)

        // Inicialización del binding
        binding = ActivityItemDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recuperar ID del ítem pasado en el Intent desde la pantalla anterior
        val itemId = intent.getIntExtra("ITEM_ID", -1)

        // Si el ID es válido, se carga el ítem desde la base de datos
        if (itemId != -1) {
            loadItem(itemId)
        }
    }

    // Función encargada de cargar los datos del ítem desde la BD
    private fun loadItem(itemId: Int) {

        // Se obtiene la instancia de la base de datos
        val db = DatabaseProvider.getDatabase(this)

        // Lanzamos una corrutina en el hilo principal
        scope.launch {

            // Consulta al DAO para obtener el ítem por su ID
            val item: Item? = db.itemDao().getItemById(itemId) // DAO debe tener getItemById

            // Si el ítem existe, se rellenan los campos de la UI
            item?.let {
                binding.tvTitleDetail.text = it.titulo
                
                // Cargar nombre de categoría si es posible, o el ID
                binding.chipCategory.text = "Categoría ${it.categoriaId}"

                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                binding.chipFecha.text = sdf.format(it.fechaAdquisicion)

                binding.tvValorDetail.text = String.format("%.2f€", it.valor)
                binding.tvDescripcionDetail.text = it.descripcion?.takeIf { d -> d.isNotBlank() } ?: "Sin descripción disponible."

                it.imagenPath?.let { path ->
                    com.bumptech.glide.Glide.with(this@ItemDetailActivity)
                        .load(com.example.gestor_colecciones.util.ImageUtils.toGlideModel(path))
                        .placeholder(com.example.gestor_colecciones.R.mipmap.ic_launcher)
                        .into(binding.ivItemDetail)
                }
                
                binding.toolbar.setNavigationOnClickListener { finish() }
            }
        }
    }
}
