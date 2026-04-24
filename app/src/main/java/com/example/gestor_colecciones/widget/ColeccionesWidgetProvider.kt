package com.example.gestor_colecciones.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.example.gestor_colecciones.R
import com.example.gestor_colecciones.auth.AuthStore
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
        private const val PREFS = "widget_prefs"
        private const val KEY_COLECCIONES = "colecciones_count"
        private const val KEY_ITEMS = "items_count"

        fun refreshAllWidgets(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, ColeccionesWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)

            if (ids.isEmpty()) return

            CoroutineScope(Dispatchers.IO).launch {
                refreshSnapshot(context)
                updateWidgets(context, appWidgetManager, ids)
            }
        }

        suspend fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray
        ) {
            val (colecciones, items) = readSnapshot(context)

            appWidgetIds.forEach { widgetId ->
                val views = RemoteViews(context.packageName, R.layout.widget_resumen).apply {
                    setTextViewText(R.id.tvColeccionesCount, colecciones.toString())
                    setTextViewText(R.id.tvItemsCount, items.toString())
                }

                appWidgetManager.updateAppWidget(widgetId, views)
            }
        }

        private suspend fun refreshSnapshot(context: Context) {
            val hasSession = !AuthStore(context).getToken().isNullOrBlank()

            if (!hasSession) {
                saveSnapshot(context, 0, 0)
                return
            }

            val db = DatabaseProvider.getDatabase(context)
            val colecciones = runCatching { db.coleccionDao().countColecciones() }.getOrNull()
            val items = runCatching { db.itemDao().getTotalItems() }.getOrNull()

            if (colecciones != null && items != null) {
                saveSnapshot(context, colecciones, items)
            }
        }

        private fun saveSnapshot(context: Context, colecciones: Int, items: Int) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_COLECCIONES, colecciones)
                .putInt(KEY_ITEMS, items)
                .apply()
        }

        private fun readSnapshot(context: Context): Pair<Int, Int> {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return prefs.getInt(KEY_COLECCIONES, 0) to prefs.getInt(KEY_ITEMS, 0)
        }
    }
}
