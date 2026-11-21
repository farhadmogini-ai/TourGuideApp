package com.example.tourguide

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

class LocationService : Service() {

    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null
    private val executor = Executors.newSingleThreadExecutor()
    private var lastSentTime = 0L

    companion object {
        const val CHANNEL_ID = "location_channel"
        const val NOTIFICATION_ID = 1001
        const val TAG = "LocationService"
        const val SEND_INTERVAL_MS = 30_000L
    }

    override fun onCreate() {
        super.onCreate()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        LocationRepository.updateServerStatus("سرویس شروع شد، منتظر موقعیت...")
        startUpdates()
        return START_STICKY
    }

    private fun startUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            LocationRepository.updateServerStatus("❌ مجوز موقعیت داده نشده")
            return
        }

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                LocationRepository.update(location.latitude, location.longitude)
                maybeSendToServer(location.latitude, location.longitude)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        locationListener = listener

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 3000L, 0f, listener
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 3000L, 0f, listener
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "خطا در دسترسی به موقعیت", e)
            LocationRepository.updateServerStatus("❌ خطای دسترسی: ${e.message}")
        }
    }

    private fun maybeSendToServer(lat: Double, lon: Double) {
        val now = System.currentTimeMillis()
        if (now - lastSentTime < SEND_INTERVAL_MS) return
        lastSentTime = now
        executor.execute { sendToServer(lat, lon) }
    }

    private fun sendToServer(lat: Double, lon: Double) {
        val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        try {
            val url = URL("${AppConfig.SERVER_URL}/api/location")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val json = JSONObject().apply {
                put("id", AppConfig.GUIDE_ID)
                put("name", AppConfig.GUIDE_NAME)
                put("busNumber", AppConfig.BUS_NUMBER)
                put("phone", AppConfig.GUIDE_PHONE)  // 🆕 شماره تلفن
                put("lat", lat)
                put("lon", lon)
            }

            conn.outputStream.use { os ->
                os.write(json.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code == 200) {
                Log.d(TAG, "📍 موقعیت ارسال شد: $lat, $lon")
                LocationRepository.updateServerStatus("✅ ارسال موفق - $timeStr")
            } else {
                Log.w(TAG, "پاسخ سرور: $code")
                LocationRepository.updateServerStatus("⚠️ پاسخ سرور: $code - $timeStr")
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "خطا در ارسال موقعیت: ${e.message}")
            LocationRepository.updateServerStatus("❌ خطا: ${e.message?.take(40)} - $timeStr")
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "ردیابی موقعیت",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("راهنمای کاروان")
            .setContentText("در حال ارسال موقعیت مکانی...")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        locationListener?.let { locationManager.removeUpdates(it) }
        executor.shutdown()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
