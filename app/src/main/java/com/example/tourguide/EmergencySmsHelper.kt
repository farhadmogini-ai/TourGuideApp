package com.example.tourguide

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telephony.SmsManager
import android.widget.Toast
import androidx.core.content.ContextCompat

object EmergencySmsHelper {

    fun sendEmergencySms(
        context: Context,
        problemType: String,
        location: Pair<Double, Double>?
    ) {
        val message = buildMessage(problemType, location)

        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            sendDirectly(context, message)
        } else {
            showPermissionDialog(context, message)
        }
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

    private fun sendDirectly(context: Context, message: String) {
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
                "✅ پیامک اضطراری ارسال شد",
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

    private fun showPermissionDialog(context: Context, message: String) {
        AlertDialog.Builder(context)
            .setTitle("مجوز پیامک داده نشده")
            .setMessage(
                "برای ارسال خودکار پیامک، برنامه به مجوز «ارسال پیامک» نیاز دارد.\n\n" +
                "دو راه دارید:\n\n" +
                "۱) رفتن به تنظیمات و فعال کردن دستی مجوز (سریع‌ترین راه)\n\n" +
                "۲) ارسال از طریق اپ پیامک گوشی (فقط دکمه «ارسال» را بزنید)"
            )
            .setPositiveButton("رفتن به تنظیمات") { _, _ ->
                openAppSettings(context)
            }
            .setNegativeButton("ارسال دستی") { _, _ ->
                sendViaIntent(context, message)
            }
            .setNeutralButton("لغو", null)
            .show()
    }

    private fun openAppSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun sendViaIntent(context: Context, message: String) {
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
