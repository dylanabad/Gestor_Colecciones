package com.example.gestor_colecciones.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.entities.Logro
import com.example.gestor_colecciones.model.LogroDefinicion
import com.google.android.material.card.MaterialCardView
import java.util.*

class LogroAdapter(private var logros: List<Logro>) :
    RecyclerView.Adapter<LogroAdapter.LogroViewHolder>() {

    class LogroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIcono: TextView = itemView.findViewById(R.id.tvIcono)
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
        val cardLogro: MaterialCardView = itemView as MaterialCardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogroViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_logro, parent, false)
        return LogroViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogroViewHolder, position: Int) {
        val logro = logros[position]
        val info = LogroDefinicion.getInfo(logro.key)
        val context = holder.itemView.context

        holder.tvIcono.text = info?.icono ?: "🏅"
        holder.tvTitulo.text = info?.titulo ?: logro.key
        holder.tvDescripcion.text = info?.descripcion ?: ""

        if (logro.desbloqueado) {
            holder.itemView.alpha = 1f
            holder.tvEstado.text = "✅"
            
            // Estética Premium: Fondo de icono con color de contenedor
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorPrimaryContainer, typedValue, true)
            val colorContainer = typedValue.data
            holder.itemView.findViewById<MaterialCardView>(R.id.cardIcon).setCardBackgroundColor(colorContainer)
            
            // Borde primario sutil
            context.theme.resolveAttribute(android.R.attr.colorPrimary, typedValue, true)
            holder.cardLogro.strokeColor = typedValue.data
            holder.cardLogro.strokeWidth = (1.5 * context.resources.displayMetrics.density).toInt()

            logro.fechaDesbloqueo?.let {
                holder.tvFecha.visibility = View.VISIBLE
                val cal = Calendar.getInstance().apply { time = it }
                val mes = cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault())?.uppercase() ?: ""
                holder.tvFecha.text = String.format(Locale.getDefault(), "%02d %s %d", cal.get(Calendar.DAY_OF_MONTH), mes, cal.get(Calendar.YEAR))
            }
        } else {
            holder.itemView.alpha = 0.6f
            holder.tvEstado.text = "🔒"
            holder.tvFecha.visibility = View.GONE
            
            // Fondo neutro para bloqueados
            val typedValue = android.util.TypedValue()
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceVariant, typedValue, true)
            holder.itemView.findViewById<MaterialCardView>(R.id.cardIcon).setCardBackgroundColor(typedValue.data)
            
            // Borde sutil para bloqueados
            context.theme.resolveAttribute(com.google.android.material.R.attr.colorOutlineVariant, typedValue, true)
            holder.cardLogro.strokeColor = typedValue.data
            holder.cardLogro.strokeWidth = (1 * context.resources.displayMetrics.density).toInt()
        }
    }

    override fun getItemCount() = logros.size

    fun updateList(nuevos: List<Logro>) {
        logros = nuevos.sortedWith(
            compareByDescending<Logro> { it.desbloqueado }
                .thenByDescending { it.fechaDesbloqueo }
        )
        notifyDataSetChanged()
    }
}
