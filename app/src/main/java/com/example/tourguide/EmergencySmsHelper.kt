package com.example.tourguide

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object EmergencySmsHelper {

    fun sendEmergencySms(
        context: Context,
        problemType: String,
        location: Pair<Double, Double>?
    ) {
        val message = buildMessage(problemType, location)
        openSmsApp(context, message)
    }

    private fun buildMessage(
        problemType: String,
        location: Pair<Double, Double>?
    ): String {
        val locText = if (location != null) {
            "موقعیت: %.5f, %.5f".format(location.first, location.second)
        } else {
            "موقعیت در دسترس نیست"
        }
        return "🚨 گزارش اضطراری\nمشکل: $problemType\n$locText"
    }

    private fun openSmsApp(context: Context, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("smsto:${AppConfig.MANAGER_PHONE_NUMBER}")
            putExtra("sms_body", message)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "اپلیکیشن پیامک روی گوشی پیدا نشد",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
