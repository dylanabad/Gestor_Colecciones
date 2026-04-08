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

class ColeccionesWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            updateWidgets(context, appWidgetManager, appWidgetIds)
            pendingResult.finish()
        }
    }

    companion object {
        suspend fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val db = DatabaseProvider.getDatabase(context)
            val colecciones = runCatching { db.coleccionDao().countColecciones() }.getOrDefault(0)
            val items = runCatching { db.itemDao().getTotalItems() }.getOrDefault(0)

            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_resumen).apply {
                    setTextViewText(R.id.tvColeccionesCount, colecciones.toString())
                    setTextViewText(R.id.tvItemsCount, items.toString())
                }
                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }
    }
}
