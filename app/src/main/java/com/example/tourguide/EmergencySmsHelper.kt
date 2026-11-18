package com.example.tourguide

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat

object EmergencySmsHelper {

    fun sendEmergencySms(
        context: Context,
        problemType: String,
        location: Pair<Double, Double>?
    ) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.SEND_SMS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(
                context,
                "برای ارسال پیامک اضطراری، مجوز لازم است",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val locText = if (location != null) {
            "موقعیت: %.5f, %.5f".format(location.first, location.second)
        } else {
            "موقعیت در دسترس نیست"
        }

        val message = "🚨 گزارش اضطراری\nمشکل: $problemType\n$locText"

        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                SmsManager.getDefault()
            }
            smsManager.sendTextMessage(
                AppConfig.MANAGER_PHONE_NUMBER,
                null,
                message,
                null,
                null
            )
            Toast.makeText(
                context,
                "پیامک اضطراری ارسال شد",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "خطا در ارسال پیامک: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
