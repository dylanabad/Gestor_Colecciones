package com.example.gestor_colecciones.model

/*
 * OnboardingData.kt
 *
 * Contiene la definición de las páginas que se muestran en el flujo de
 * onboarding inicial de la aplicación. Cada página se representa con la
 * data class `OnboardingPage` que incluye un emoji, título, descripción,
 * texto de detalle y un color de fondo (hex).
 *
 * El objeto `OnboardingData` expone la lista `PAGES` que es consumida por el
 * `OnboardingAdapter`/`OnboardingFragment` para renderizar las pantallas.
 *
 * Nota: Solo se añaden comentarios explicativos en español; no se modifica la lógica.
 */

// Representa una página del onboarding con campos utilizados por la UI
data class OnboardingPage(
    val emoji: String,
    val titulo: String,
    val descripcion: String,
    val detalle: String,
    // Color de fondo en formato hex (p. ej. "#6C63FF")
    val colorFondo: String
)

// Contenedor con las páginas de ejemplo mostradas en el onboarding
object OnboardingData {
    // Lista de páginas en el orden en que se mostrarán
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