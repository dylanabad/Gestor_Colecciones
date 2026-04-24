package com.example.gestor_colecciones.auth

import android.content.Context

/**
 * Encapsula el acceso a las preferencias donde se guarda la sesion del usuario.
 *
 * Se limita a persistir token, nombre de usuario y email para que el resto de
 * la aplicacion no dependa directamente de SharedPreferences.
 */
class AuthStore(context: Context) {

    /** Preferencias privadas utilizadas para conservar la sesion entre arranques. */
    private val prefs = context.getSharedPreferences(
        "auth_prefs",
        Context.MODE_PRIVATE
    )

    /** Guarda los datos minimos necesarios para restaurar la sesion. */
    fun save(token: String, username: String, email: String) {
        prefs.edit()
            .putString("token", token)
            .putString("username", username)
            .putString("email", email)
            .apply()
    }

    /** Recupera el token JWT almacenado, si existe. */
    fun getToken(): String? =
        prefs.getString("token", null)

    /** Recupera el nombre de usuario persistido. */
    fun getUsername(): String? =
        prefs.getString("username", null)

    /** Recupera el email persistido para la sesion actual. */
    fun getEmail(): String? =
        prefs.getString("email", null)

    /** Borra todos los datos asociados a la sesion actual. */
    fun clear() {
        prefs.edit().clear().apply()
    }
}
