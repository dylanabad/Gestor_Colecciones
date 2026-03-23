package com.example.gestor_colecciones.model

data class OnboardingPage(
    val emoji: String,
    val titulo: String,
    val descripcion: String,
    val detalle: String,
    val colorFondo: String
)

object OnboardingData {
    val PAGES = listOf(
        OnboardingPage(
            emoji = "📦",
            titulo = "Bienvenido a\nGestor de Colecciones",
            descripcion = "Tu espacio personal para organizar todo lo que coleccionas",
            detalle = "Desde figuras y libros hasta monedas o cualquier otra cosa. Todo en un solo lugar, siempre ordenado.",
            colorFondo = "#6C63FF"
        ),
        OnboardingPage(
            emoji = "🗂",
            titulo = "Crea tus\ncolecciones",
            descripcion = "Organiza tus items en colecciones personalizadas",
            detalle = "Añade imágenes, colores y descripciones. Desliza a la izquierda para mover a la papelera. Mantén pulsado para editar.",
            colorFondo = "#48CAE4"
        ),
        OnboardingPage(
            emoji = "🗃",
            titulo = "Gestiona\ntus items",
            descripcion = "Añade items con valor, estado, categoría y calificación",
            detalle = "Filtra y ordena por precio, fecha o nombre. Busca cualquier item al instante con la barra de búsqueda.",
            colorFondo = "#FF6B9D"
        ),
        OnboardingPage(
            emoji = "🏆",
            titulo = "Descubre\ntodas las funciones",
            descripcion = "Logros, deseos, estadísticas y tarjetas visuales",
            detalle = "Desbloquea logros coleccionando, gestiona tu lista de deseos, exporta catálogos PDF y genera tarjetas para compartir.",
            colorFondo = "#FFD166"
        )
    )
}