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

                // Título del ítem
                binding.tvTitleDetail.text = it.titulo

                // Categoría del ítem (actualmente se muestra el ID, no el nombre)
                binding.tvCategoryDetail.text = it.categoriaId.toString()

                // Valor del ítem formateado
                binding.tvValorDetail.text = "Valor: \$${it.valor}"

                // Descripción del ítem (si es null, se muestra vacío)
                binding.tvDescripcionDetail.text = it.descripcion ?: ""

                // Imagen del ítem (comentado, depende de si tienes ruta de imagen)
                // binding.ivItemDetail.setImageURI(Uri.parse(it.imagenPath))
            }
        }
    }
}
