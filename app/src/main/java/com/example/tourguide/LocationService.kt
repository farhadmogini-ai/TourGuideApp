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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class LocationService : Service() {

    private lateinit var locationManager: LocationManager
    private var locationListener: LocationListener? = null

    companion object {
        const val CHANNEL_ID = "location_channel"
        const val NOTIFICATION_ID = 1001
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
        startUpdates()
        return START_STICKY
    }

    private fun startUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                LocationRepository.update(location.latitude, location.longitude)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        locationListener = listener

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER, 5000L, 0f, listener
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 5000L, 0f, listener
            )
        } catch (e: SecurityException) {
            // ignore
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
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
