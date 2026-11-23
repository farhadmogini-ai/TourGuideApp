package com.example.tourguide

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object ApiClient {

    suspend fun login(
        serverUrl: String,
        username: String,
        password: String
    ): Pair<String, User>? = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$serverUrl/api/auth/login").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000

            val body = JSONObject().apply {
                put("username", username)
                put("password", password)
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            if (conn.responseCode != 200) return@withContext null
            val responseBody = conn.inputStream.bufferedReader().readText()
            val obj = JSONObject(responseBody)
            val token = obj.getString("token")
            val u = obj.getJSONObject("user")
            val user = User(
                id = u.getInt("id"),
                username = u.getString("username"),
                role = u.getString("role"),
                firstName = u.getString("firstName"),
                lastName = u.getString("lastName"),
                city = u.optString("city").takeIf { it.isNotEmpty() && it != "null" },
                busNumber = u.optString("busNumber").takeIf { it.isNotEmpty() && it != "null" },
                originHotel = u.optString("originHotel").takeIf { it.isNotEmpty() && it != "null" },
                phone = u.optString("phone").takeIf { it.isNotEmpty() && it != "null" }
            )
            token to user
        } catch (e: Exception) { null }
    }

    suspend fun getStations(serverUrl: String, token: String): List<Station>? =
        withContext(Dispatchers.IO) {
            try {
                val conn = URL("$serverUrl/api/stations").openConnection() as HttpURLConnection
                conn.setRequestProperty("Authorization", "Bearer $token")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000

                if (conn.responseCode != 200) return@withContext null
                val body = conn.inputStream.bufferedReader().readText()
                val arr = JSONArray(body)
                val list = mutableListOf<Station>()
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(Station(
                        id = o.optInt("id", 0),
                        name = o.getString("name"),
                        latitude = o.getDouble("lat"),
                        longitude = o.getDouble("lon")
                    ))
                }
                list
            } catch (e: Exception) { null }
        }

    suspend fun sendLocation(
        serverUrl: String,
        token: String,
        lat: Double,
        lon: Double
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$serverUrl/api/location").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val body = JSONObject().apply { put("lat", lat); put("lon", lon) }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val ok = conn.responseCode == 200
            conn.disconnect()
            ok
        } catch (e: Exception) { false }
    }

    suspend fun sendEmergency(
        serverUrl: String,
        token: String,
        problemType: String,
        lat: Double?,
        lon: Double?
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$serverUrl/api/emergency").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.doOutput = true
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            val body = JSONObject().apply {
                put("problemType", problemType)
                if (lat != null && lon != null) { put("lat", lat); put("lon", lon) }
            }
            conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val ok = conn.responseCode == 200
            conn.disconnect()
            ok
        } catch (e: Exception) { false }
    }
}
