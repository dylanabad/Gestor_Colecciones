package com.example.gestor_colecciones.auth

import android.content.Context

class AuthStore(context: Context) {
    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)

    fun save(token: String, username: String, email: String) {
        prefs.edit()
            .putString("token", token)
            .putString("username", username)
            .putString("email", email)
            .apply()
    }

    fun getToken(): String? = prefs.getString("token", null)

    fun getUsername(): String? = prefs.getString("username", null)

    fun getEmail(): String? = prefs.getString("email", null)

    fun clear() {
        prefs.edit().clear().apply()
    }
}
