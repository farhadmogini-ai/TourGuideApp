package com.example.tourguide

import android.content.Context
import org.json.JSONObject

object SessionManager {
    private const val PREFS = "tourguide_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER = "user"

    fun save(context: Context, token: String, user: User) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val json = JSONObject().apply {
            put("id", user.id)
            put("username", user.username)
            put("role", user.role)
            put("firstName", user.firstName)
            put("lastName", user.lastName)
            put("city", user.city ?: "")
            put("busNumber", user.busNumber ?: "")
            put("originHotel", user.originHotel ?: "")
            put("phone", user.phone ?: "")
        }
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, json.toString())
            .apply()
    }

    fun getToken(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TOKEN, null)

    fun getUser(context: Context): User? {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_USER, null) ?: return null
        return try {
            val obj = JSONObject(json)
            User(
                id = obj.getInt("id"),
                username = obj.getString("username"),
                role = obj.getString("role"),
                firstName = obj.getString("firstName"),
                lastName = obj.getString("lastName"),
                city = obj.optString("city").takeIf { it.isNotEmpty() },
                busNumber = obj.optString("busNumber").takeIf { it.isNotEmpty() },
                originHotel = obj.optString("originHotel").takeIf { it.isNotEmpty() },
                phone = obj.optString("phone").takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) { null }
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
