package com.example.gestor_colecciones.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.entities.Logro
import com.example.gestor_colecciones.model.LogroDefinicion
import java.text.SimpleDateFormat
import java.util.*

class LogroAdapter(private var logros: List<Logro>) :
    RecyclerView.Adapter<LogroAdapter.LogroViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    inner class LogroViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvIcono: TextView = itemView.findViewById(R.id.tvIcono)
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvFecha: TextView = itemView.findViewById(R.id.tvFecha)
        val tvEstado: TextView = itemView.findViewById(R.id.tvEstado)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogroViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_logro, parent, false)
        return LogroViewHolder(view)
    }

    override fun onBindViewHolder(holder: LogroViewHolder, position: Int) {
        val logro = logros[position]
        val info = LogroDefinicion.getInfo(logro.key)

        holder.tvIcono.text = info?.icono ?: "🏅"
        holder.tvTitulo.text = info?.titulo ?: logro.key
        holder.tvDescripcion.text = info?.descripcion ?: ""

        if (logro.desbloqueado) {
            holder.tvEstado.text = "✅"
            holder.itemView.alpha = 1f
            logro.fechaDesbloqueo?.let {
                holder.tvFecha.visibility = View.VISIBLE
                holder.tvFecha.text = "Desbloqueado el ${dateFormat.format(it)}"
            }
        } else {
            holder.tvEstado.text = "🔒"
            holder.itemView.alpha = 0.45f
            holder.tvFecha.visibility = View.GONE
        }
    }

    override fun getItemCount() = logros.size

    fun updateList(nuevos: List<Logro>) {
        // Ordenar: desbloqueados primero, luego bloqueados
        logros = nuevos.sortedWith(compareByDescending<Logro> { it.desbloqueado }
            .thenByDescending { it.fechaDesbloqueo })
        notifyDataSetChanged()
    }
}