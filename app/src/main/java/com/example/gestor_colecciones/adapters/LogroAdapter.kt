package com.example.gestor_colecciones.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.entities.Logro
import com.example.gestor_colecciones.model.LogroDefinicion
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adaptador para mostrar la lista de logros del usuario en un [RecyclerView].
 *
 * Visualiza cada logro con su icono, título, descripción y estado (bloqueado/desbloqueado),
 * aplicando efectos visuales como escala de grises para los no conseguidos.
 *
 * @property logros Lista de objetos [Logro] a mostrar.
 */
class LogroAdapter(private var logros: List<Logro>) :
    RecyclerView.Adapter<LogroAdapter.LogroViewHolder>() {

    /**
     * ViewHolder que contiene las referencias a las vistas de un item de logro.
     */
    class LogroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIcono: TextView = itemView.findViewById(R.id.tvIcono)
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFechaLogro)
        val ivStatusIcon: ImageView = itemView.findViewById(R.id.ivStatusIcon)
        val cardIconContainer: MaterialCardView = itemView.findViewById(R.id.cardIconContainer)
        val cardLogro: MaterialCardView = itemView.findViewById(R.id.cardLogro)
    }

    /**
     * Infla el diseño del item de logro y crea el [LogroViewHolder].
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogroViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_logro, parent, false)
        return LogroViewHolder(view)
    }

    /**
     * Vincula los datos de un [Logro] con la vista, configurando el aspecto visual
     * según si el logro está desbloqueado o bloqueado.
     */
    override fun onBindViewHolder(holder: LogroViewHolder, position: Int) {
        val logro = logros[position]
        val info = LogroDefinicion.getInfo(logro.key)
        val context = holder.itemView.context

        holder.tvIcono.text = info?.icono ?: "🏅"
        holder.tvTitulo.text = info?.titulo ?: logro.key
        holder.tvDescripcion.text = info?.descripcion ?: ""

        if (logro.desbloqueado) {
            // ESTADO: DESBLOQUEADO (Limpio y resaltado)
            holder.cardLogro.alpha = 1.0f
            holder.cardIconContainer.alpha = 1.0f
            
            // Icono de Trofeo (Victoria)
            holder.ivStatusIcon.setImageResource(R.drawable.ic_trophy_24)
            holder.ivStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                MaterialColors.getColor(context, android.R.attr.colorPrimary, android.graphics.Color.BLACK)
            )

            // Fondo del icono con color primario suave
            holder.cardIconContainer.setCardBackgroundColor(
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorPrimaryContainer, android.graphics.Color.LTGRAY)
            )
            
            // Borde sutil resaltado
            holder.cardLogro.strokeColor = MaterialColors.getColor(context, android.R.attr.colorPrimary, android.graphics.Color.BLACK)
            holder.cardLogro.strokeWidth = (2 * context.resources.displayMetrics.density).toInt()

            // Fecha
            logro.fechaDesbloqueo?.let {
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                holder.tvFecha.text = "DESBLOQUEADO EL ${sdf.format(it).uppercase()}"
                holder.tvFecha.visibility = View.VISIBLE
            }
        } else {
            // ESTADO: BLOQUEADO (Efecto desactivado/gris)
            holder.cardLogro.alpha = 0.6f
            holder.cardIconContainer.alpha = 0.5f
            
            // Icono de Candado (Bloqueado) - Usando ic_auth_lock que ya tienes o similar
            holder.ivStatusIcon.setImageResource(R.drawable.ic_auth_lock)
            holder.ivStatusIcon.imageTintList = android.content.res.ColorStateList.valueOf(
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutline, android.graphics.Color.GRAY)
            )

            // Fondo neutro
            holder.cardIconContainer.setCardBackgroundColor(
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceVariant, android.graphics.Color.LTGRAY)
            )
            
            // Borde neutro
            holder.cardLogro.strokeColor = MaterialColors.getColor(context, com.google.android.material.R.attr.colorOutlineVariant, android.graphics.Color.LTGRAY)
            holder.cardLogro.strokeWidth = (1 * context.resources.displayMetrics.density).toInt()
            
            holder.tvFecha.visibility = View.GONE
        }
    }

    /**
     * Devuelve la cantidad total de logros en la lista.
     */
    override fun getItemCount() = logros.size

    /**
     * Actualiza la lista de logros, ordenándolos para que aparezcan primero los desbloqueados
     * y, dentro de estos, los más recientes.
     *
     * @param nuevos Nueva lista de logros a mostrar.
     */
    fun updateList(nuevos: List<Logro>) {
        logros = nuevos.sortedWith(
            compareByDescending<Logro> { it.desbloqueado }
                .thenByDescending { it.fechaDesbloqueo }
        )
        notifyDataSetChanged()
    }
}
