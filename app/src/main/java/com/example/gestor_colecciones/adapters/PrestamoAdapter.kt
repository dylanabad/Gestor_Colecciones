package com.example.gestor_colecciones.adapters

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.color.MaterialColors
import com.google.android.material.imageview.ShapeableImageView
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.network.dto.PrestamoDto
import com.example.gestor_colecciones.util.ImageUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PrestamoAdapter(
    private var lista: List<PrestamoDto>,
    private val modo: Modo,
    private val onDevolver: ((PrestamoDto) -> Unit)? = null,
    private val onDelete: ((PrestamoDto) -> Unit)? = null,
    private val currentUsername: String? = null
) : RecyclerView.Adapter<PrestamoAdapter.ViewHolder>() {

    enum class Modo { PRESTADOS, RECIBIDOS }

    private enum class EstadoVisual { ACTIVO, VENCIDO, DEVUELTO }

    private data class EstiloEstado(
        val accentColor: Int,
        val badgeBgColor: Int,
        val badgeTextColor: Int,
        val label: String
    )

    companion object {
        private val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }

    // ── ViewHolder ────────────────────────────────────────────────────────────

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val viewAccent: View             = view.findViewById(R.id.viewAccent)
        val tvTituloItem: TextView       = view.findViewById(R.id.tvTituloItem)
        val tvEstado: TextView           = view.findViewById(R.id.tvEstado)
        val ivAvatar: ShapeableImageView = view.findViewById(R.id.ivAvatar)
        val tvAvatar: TextView           = view.findViewById(R.id.tvAvatar)
        val tvUsuario: TextView          = view.findViewById(R.id.tvUsuario)
        val tvFechaPrestamo: TextView    = view.findViewById(R.id.tvFechaPrestamo)
        val tvFechaDevolucion: TextView  = view.findViewById(R.id.tvFechaDevolucion)
        val tvNotas: TextView            = view.findViewById(R.id.tvNotas)
        val btnDevolver: View            = view.findViewById(R.id.btnDevolver)
        val btnEliminar: View            = view.findViewById(R.id.btnEliminar)

        fun bind(p: PrestamoDto) {
            bindTitulo(p)
            bindUsuario(p)
            bindFechas(p)
            bindEstado(p)
            bindNotas(p)
            bindBotones(p)
        }

        private fun bindTitulo(p: PrestamoDto) {
            tvTituloItem.text = p.itemTitulo
        }

        private fun bindUsuario(p: PrestamoDto) {
            val nombre = if (modo == Modo.PRESTADOS) p.prestatarioUsername else p.propietarioUsername
            val avatarPath = if (modo == Modo.PRESTADOS) p.prestatarioAvatarPath else p.propietarioAvatarPath

            tvUsuario.text = if (modo == Modo.PRESTADOS) "Prestado a: $nombre"
            else "Prestado por: $nombre"

            val model = ImageUtils.toGlideModel(avatarPath)
            if (model != null) {
                ivAvatar.visibility = View.VISIBLE
                tvAvatar.visibility = View.GONE
                Glide.with(itemView)
                    .load(model)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person_24)
                    .error(R.drawable.ic_person_24)
                    .into(ivAvatar)
            } else {
                ivAvatar.visibility = View.GONE
                tvAvatar.visibility = View.VISIBLE
                val initials = nombre
                    .split(" ", "_")
                    .filter { it.isNotBlank() }
                    .take(2)
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .joinToString("")
                tvAvatar.text = if (initials.isNotBlank()) initials else "?"
            }
        }

        private fun bindFechas(p: PrestamoDto) {
            tvFechaPrestamo.text = p.fechaPrestamo.take(10)
            tvFechaDevolucion.text = p.fechaDevolucionPrevista?.take(10) ?: "Sin fecha"
        }

        private fun bindEstado(p: PrestamoDto) {
            val estilo = estiloPara(itemView, resolverEstado(p))
            viewAccent.setBackgroundColor(estilo.accentColor)
            tvEstado.text = estilo.label
            tvEstado.setTextColor(estilo.badgeTextColor)
            tvEstado.backgroundTintList =
                ColorStateList.valueOf(estilo.badgeBgColor)
        }

        private fun bindNotas(p: PrestamoDto) {
            val hayNotas = !p.notas.isNullOrBlank()
            tvNotas.visibility = if (hayNotas) View.VISIBLE else View.GONE
            if (hayNotas) tvNotas.text = p.notas
        }

        private fun bindBotones(p: PrestamoDto) {
            val mostrarDevolver = modo == Modo.PRESTADOS && p.estado == "ACTIVO"
            btnDevolver.visibility = if (mostrarDevolver) View.VISIBLE else View.GONE
            btnDevolver.setOnClickListener { onDevolver?.invoke(p) }

            val puedeEliminar = currentUsername != null && when (modo) {
                Modo.PRESTADOS -> currentUsername == p.propietarioUsername
                Modo.RECIBIDOS -> currentUsername == p.prestatarioUsername
            }
            btnEliminar.visibility = if (puedeEliminar) View.VISIBLE else View.GONE
            btnEliminar.setOnClickListener { onDelete?.invoke(p) }
        }
    }

    // ── Ciclo de vida del Adapter ─────────────────────────────────────────────

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_prestamo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(lista[position])
    }

    override fun getItemCount() = lista.size

    // Actualiza la lista usando DiffUtil para animar solo los cambios reales
    fun updateList(nuevaLista: List<PrestamoDto>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = lista.size
            override fun getNewListSize() = nuevaLista.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                lista[oldPos].movimientoId == nuevaLista[newPos].movimientoId
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                lista[oldPos] == nuevaLista[newPos]
        })
        lista = nuevaLista
        diff.dispatchUpdatesTo(this)
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private fun resolverEstado(p: PrestamoDto): EstadoVisual {
        if (p.estado == "DEVUELTO") return EstadoVisual.DEVUELTO
        val fechaStr = p.fechaDevolucionPrevista?.take(10) ?: return EstadoVisual.ACTIVO
        return try {
            val fechaDev = DATE_FORMAT.parse(fechaStr)
            if (fechaDev != null && fechaDev.before(Date())) EstadoVisual.VENCIDO
            else EstadoVisual.ACTIVO
        } catch (_: Exception) {
            EstadoVisual.ACTIVO
        }
    }

    private fun estiloPara(view: View, estado: EstadoVisual): EstiloEstado {
        // Colores basados en tema para que los distintos temas no queden "lavados"
        fun c(attr: Int, fallback: Int): Int = MaterialColors.getColor(view, attr, fallback)

        val accentActivo = c(androidx.appcompat.R.attr.colorPrimary, 0xFF1D9E75.toInt())
        val badgeActivo = c(com.google.android.material.R.attr.colorPrimaryContainer, accentActivo)
        val badgeActivoText = c(com.google.android.material.R.attr.colorOnPrimaryContainer,
            c(com.google.android.material.R.attr.colorOnPrimary, 0xFFFFFFFF.toInt())
        )

        val accentVencido = c(androidx.appcompat.R.attr.colorError, 0xFFEF9F27.toInt())
        val badgeVencido = c(com.google.android.material.R.attr.colorErrorContainer, accentVencido)
        val badgeVencidoText = c(com.google.android.material.R.attr.colorOnErrorContainer,
            c(com.google.android.material.R.attr.colorOnError, 0xFFFFFFFF.toInt())
        )

        val accentDevuelto = c(com.google.android.material.R.attr.colorOutline,
            c(com.google.android.material.R.attr.colorOnSurfaceVariant, 0xFF888780.toInt())
        )
        val badgeDevuelto = c(com.google.android.material.R.attr.colorSurfaceVariant,
            c(com.google.android.material.R.attr.colorSurface, 0xFFF1EFE8.toInt())
        )
        val badgeDevueltoText = c(com.google.android.material.R.attr.colorOnSurfaceVariant,
            c(com.google.android.material.R.attr.colorOnSurface, 0xFF5F5E5A.toInt())
        )

        return when (estado) {
            EstadoVisual.ACTIVO -> EstiloEstado(accentActivo, badgeActivo, badgeActivoText, "Activo")
            EstadoVisual.VENCIDO -> EstiloEstado(accentVencido, badgeVencido, badgeVencidoText, "Vencido")
            EstadoVisual.DEVUELTO -> EstiloEstado(accentDevuelto, badgeDevuelto, badgeDevueltoText, "Devuelto")
        }
    }
}
