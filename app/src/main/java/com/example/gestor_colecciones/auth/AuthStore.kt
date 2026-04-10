package com.example.gestor_colecciones.auth

import android.content.Context

// Clase encargada de almacenar y recuperar datos de autenticación en almacenamiento local
class AuthStore(context: Context) {

    // SharedPreferences donde se guardan los datos de sesión
    private val prefs = context.getSharedPreferences(
        "auth_prefs",
        Context.MODE_PRIVATE
    )

    // Guarda token, username y email en SharedPreferences
    fun save(token: String, username: String, email: String) {
        prefs.edit()
            .putString("token", token)
            .putString("username", username)
            .putString("email", email)
            .apply()
    }

    // Devuelve el token guardado (si existe)
    fun getToken(): String? =
        prefs.getString("token", null)

    // Devuelve el nombre de usuario guardado (si existe)
    fun getUsername(): String? =
        prefs.getString("username", null)

    // Devuelve el email guardado (si existe)
    fun getEmail(): String? =
        prefs.getString("email", null)

    // Borra todos los datos de sesión almacenados
    fun clear() {
        prefs.edit().clear().apply()
    }
}