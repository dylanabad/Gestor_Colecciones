package com.example.gestor_colecciones.model

/*
 * ItemEstados.kt
 *
 * Objeto que expone una lista por defecto de estados que puede tener un Item
 * dentro de la aplicación. Se utiliza para inicializar selectores/AutoComplete
 * y para ofrecer valores por defecto al crear o editar items.
 */
object ItemEstados {
    // Lista de estados predefinidos (orden de presentación en la UI)
    val DEFAULT: List<String> = listOf(
        "Nuevo",
        "Usado",
        "En reparación",
        "Vendido"
    )
}

