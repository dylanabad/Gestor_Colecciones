package com.example.gestor_colecciones.model

/*
 * OnboardingData.kt
 *
 * Contiene la definición de las páginas que se muestran en el flujo de
 * onboarding inicial de la aplicación. Cada página se representa con la
 * data class `OnboardingPage` que incluye un emoji, título, descripción,
 * texto de detalle y un color de fondo (hex).
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
            emoji = "✨",
            titulo = "Bienvenido a\nCollectHub",
            descripcion = "Organiza tu pasión de forma profesional y sencilla",
            detalle = "Registra figuras, libros, monedas o cualquier tesoro. Todo lo que amas, ahora en un solo lugar y siempre a mano.",
            colorFondo = "#6C63FF"
        ),
        OnboardingPage(
            emoji = "📂",
            titulo = "Personaliza tus\nColecciones",
            descripcion = "Crea categorías únicas para cada interés",
            detalle = "Asigna imágenes y colores distintivos. Gestiona con gestos: desliza para borrar o mantén pulsado para editar tus colecciones.",
            colorFondo = "#48CAE4"
        ),
        OnboardingPage(
            emoji = "📊",
            titulo = "Control Total de\ntus Ítems",
            descripcion = "Detalles precisos para coleccionistas exigentes",
            detalle = "Añade valor, estado y calificación. Encuentra lo que buscas al instante con filtros avanzados y búsqueda inteligente.",
            colorFondo = "#FF6B9D"
        ),
        OnboardingPage(
            emoji = "🌟",
            titulo = "Lleva tu Pasión\nal Siguiente Nivel",
            descripcion = "Estadísticas, Logros y Exportación",
            detalle = "Visualiza tu progreso, gestiona tu lista de deseos, exporta catálogos en PDF y comparte tarjetas visuales con tus amigos.",
            colorFondo = "#FFD166"
        )
    )
}