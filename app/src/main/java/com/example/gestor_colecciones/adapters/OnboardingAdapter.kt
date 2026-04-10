package com.example.gestor_colecciones.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.model.OnboardingPage

// Adapter encargado de mostrar las páginas del onboarding en un RecyclerView
class OnboardingAdapter(private val pages: List<OnboardingPage>) :
    RecyclerView.Adapter<OnboardingAdapter.OnboardingViewHolder>() {

    // ViewHolder que contiene las vistas de cada página del onboarding
    class OnboardingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val tvEmoji: TextView = itemView.findViewById(R.id.tvEmoji)
        val tvTitulo: TextView = itemView.findViewById(R.id.tvTitulo)
        val tvDescripcion: TextView = itemView.findViewById(R.id.tvDescripcion)
        val tvDetalle: TextView = itemView.findViewById(R.id.tvDetalle)
    }

    // Inflado del layout de cada página del onboarding
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OnboardingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_onboarding_page, parent, false)
        return OnboardingViewHolder(view)
    }

    // Vinculación de datos de la página con la vista
    override fun onBindViewHolder(holder: OnboardingViewHolder, position: Int) {

        val page = pages[position]

        holder.tvEmoji.text = page.emoji
        holder.tvTitulo.text = page.titulo
        holder.tvDescripcion.text = page.descripcion
        holder.tvDetalle.text = page.detalle
    }

    // Número total de páginas del onboarding
    override fun getItemCount() = pages.size
}