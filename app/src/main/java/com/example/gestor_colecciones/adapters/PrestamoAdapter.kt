package com.example.gestor_colecciones.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.network.dto.PrestamoDto

class PrestamoAdapter(
    private var lista: List<PrestamoDto>,
    private val modo: Modo,
    private val onDevolver: ((PrestamoDto) -> Unit)? = null,
    private val onDelete: ((PrestamoDto) -> Unit)? = null,
    private val currentUsername: String? = null
) : RecyclerView.Adapter<PrestamoAdapter.ViewHolder>() {

    enum class Modo { PRESTADOS, RECIBIDOS }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTituloItem: TextView = view.findViewById(R.id.tvTituloItem)
        val tvUsuario: TextView = view.findViewById(R.id.tvUsuario)
        val tvFechaPrestamo: TextView = view.findViewById(R.id.tvFechaPrestamo)
        val tvFechaDevolucion: TextView = view.findViewById(R.id.tvFechaDevolucion)
        val tvEstado: TextView = view.findViewById(R.id.tvEstado)
        val tvNotas: TextView = view.findViewById(R.id.tvNotas)
        val btnDevolver: View = view.findViewById(R.id.btnDevolver)
        val btnEliminar: View = view.findViewById(R.id.btnEliminar)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prestamo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val prestamo = lista[position]

        holder.tvTituloItem.text = prestamo.itemTitulo
        holder.tvUsuario.text = when (modo) {
            Modo.PRESTADOS -> "Prestado a: ${prestamo.prestatarioUsername}"
            Modo.RECIBIDOS -> "Prestado por: ${prestamo.propietarioUsername}"
        }
        holder.tvFechaPrestamo.text = "Fecha: ${prestamo.fechaPrestamo.take(10)}"
        holder.tvFechaDevolucion.text = prestamo.fechaDevolucionPrevista
            ?.let { "Devolución prevista: ${it.take(10)}" }
            ?: "Sin fecha de devolución"
        holder.tvEstado.text = prestamo.estado
        holder.tvEstado.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                if (prestamo.estado == "ACTIVO") R.color.colorEstadoActivo
                else R.color.colorEstadoDevuelto
            )
        )
        holder.tvNotas.text = prestamo.notas ?: ""
        holder.tvNotas.visibility =
            if (prestamo.notas.isNullOrBlank()) View.GONE else View.VISIBLE
        holder.btnDevolver.visibility =
            if (modo == Modo.PRESTADOS && prestamo.estado == "ACTIVO") View.VISIBLE
            else View.GONE
        holder.btnDevolver.setOnClickListener { onDevolver?.invoke(prestamo) }
        // Mostrar el botón eliminar sólo si el usuario actual es el propietario (o si no se conoce, ocultarlo)
        holder.btnEliminar.visibility = if (currentUsername != null && prestamo.propietarioUsername == currentUsername) View.VISIBLE else View.GONE
        holder.btnEliminar.setOnClickListener { onDelete?.invoke(prestamo) }
    }

    override fun getItemCount() = lista.size

    fun updateList(nuevaLista: List<PrestamoDto>) {
        lista = nuevaLista
        notifyDataSetChanged()
    }
}