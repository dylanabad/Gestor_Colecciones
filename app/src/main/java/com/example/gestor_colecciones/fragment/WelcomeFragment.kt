package com.example.gestor_colecciones.fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import android.widget.Button
import com.example.gestor_colecciones.R

class WelcomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)

        view.findViewById<Button>(R.id.btnEnter).setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ColeccionesFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}