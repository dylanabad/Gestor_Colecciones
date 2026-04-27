package com.example.gestor_colecciones.model

/*
 * LogroDefinicion.kt
 *
 * Define los logros disponibles en la aplicación. Cada logro tiene una clave
 * única (`key`), un título legible, una descripción y un icono (emoji) que se
 * muestra en la UI. El objeto `LogroDefinicion` expone la lista `TODOS` con
 * todas las definiciones y una función helper `getInfo` para recuperar
 * información de un logro por su clave.
 */

// Información básica de un logro: clave, título, descripción y emoji
data class LogroInfo(
    val key: String,
    val titulo: String,
    val descripcion: String,
    val icono: String   // emoji para mostrar en UI
)

// Contenedor de las definiciones de logros disponibles en la app
object LogroDefinicion {
    // Lista estática con todos los logros; está organizada por categorías comentadas
    val TODOS = listOf(
        // Colecciones
        LogroInfo("PRIMERA_COLECCION",     "Primera colección",        "Crea tu primera colección",                 "🗂️"),
        LogroInfo("COLECCIONES_5",         "Aficionado",               "Crea 5 colecciones",                         "📦"),
        LogroInfo("COLECCIONES_10",        "Coleccionista serio",      "Crea 10 colecciones",                        "🏆"),
        // Items
        LogroInfo("PRIMER_ITEM",           "Primer tesoro",            "Añade tu primer item",                       "⭐"),
        LogroInfo("ITEMS_10",              "Empezando fuerte",         "Añade 10 items en total",                    "🔥"),
        LogroInfo("ITEMS_50",              "Medio centenar",           "Añade 50 items en total",                    "💎"),
        LogroInfo("ITEMS_100",             "Centenario",               "Añade 100 items en total",                   "🎯"),
        // Valor
        LogroInfo("VALOR_1000",            "Inversión notable",        "Alcanza 1.000€ de valor total",              "💰"),
        LogroInfo("VALOR_10000",           "Gran coleccionista",       "Alcanza 10.000€ de valor total",             "💸"),
        // Especiales
        LogroInfo("PRIMERA_IMAGEN",        "Fotógrafo",                "Añade una imagen a una colección o item",    "📷"),
        LogroInfo("COLECCION_20_ITEMS",    "Colección completa",       "Una sola colección con 20 o más items",      "✅"),
        LogroInfo("CALIFICACION_5",        "Perfeccionista",           "Añade un item con calificación máxima (5★)", "🌟"),
        // Préstamos
        LogroInfo("PRIMER_PRESTAMO",       "Primer préstamo",          "Presta tu primer item",                        "🤝"),
        LogroInfo("PRESTAMOS_5",           "Prestamista",              "Presta 5 items",                               "📤"),
        LogroInfo("PRIMER_PRESTAMO_RECIBIDO","Primer préstamo recibido","Recibe tu primer préstamo",                  "📥"),
        LogroInfo("PRESTAMOS_RECIBIDOS_5", "Confiable",                "Recibe 5 préstamos",                           "🤗")
    )

    // Helper para obtener la definición de un logro por su clave
    fun getInfo(key: String): LogroInfo? = TODOS.find { it.key == key }
}
