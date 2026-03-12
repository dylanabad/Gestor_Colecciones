package com.example.gestor_colecciones.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.fragment.ItemListFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Comprobar que el fragment no está ya agregado
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ItemListFragment())
                .commit()
        }
    }
}