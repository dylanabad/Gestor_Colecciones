package com.example.gestor_colecciones.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.database.DatabaseProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Provider del widget de colecciones (home screen widget)
class ColeccionesWidgetProvider : AppWidgetProvider() {

    // Se llama cada vez que el widget necesita actualizarse
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        // Permite ejecutar trabajo asíncrono sin bloquear el hilo principal del widget
        val pendingResult = goAsync()

        // Se lanza una corrutina en segundo plano para actualizar los widgets
        CoroutineScope(Dispatchers.IO).launch {
            updateWidgets(context, appWidgetManager, appWidgetIds)

            // Finaliza la ejecución diferida del widget
            pendingResult.finish()
        }
    }

    // Funciones compartidas para actualizar todos los widgets activos
    companion object {

        // Función suspendida porque accede a base de datos
        suspend fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            // Obtiene instancia de la base de datos
            val db = DatabaseProvider.getDatabase(context)

            // Obtiene el número total de colecciones (con fallback a 0 si falla)
            val colecciones = runCatching { db.coleccionDao().countColecciones() }.getOrDefault(0)

            // Obtiene el número total de items (con fallback a 0 si falla)
            val items = runCatching { db.itemDao().getTotalItems() }.getOrDefault(0)

            // Recorre cada widget activo para actualizar su UI
            appWidgetIds.forEach { widgetId ->

                // Crea la vista remota del widget usando el layout definido
                val views = RemoteViews(context.packageName, R.layout.widget_resumen).apply {

                    // Asigna el número de colecciones al TextView correspondiente
                    setTextViewText(R.id.tvColeccionesCount, colecciones.toString())

                    // Asigna el número total de items al TextView correspondiente
                    setTextViewText(R.id.tvItemsCount, items.toString())
                }

                // Actualiza el widget en pantalla con los nuevos datos
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}