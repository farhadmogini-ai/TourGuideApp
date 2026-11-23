package com.example.tourguide

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object EmergencySmsHelper {

    private const val TAG = "EmergencySms"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun sendEmergencySms(
        context: Context,
        problemType: String,
        location: Pair<Double, Double>?
    ) {
        val appContext = context.applicationContext

        // ۱. ارسال به سرور (اگر لاگین هست)
        val token = SessionManager.getToken(appContext)
        if (token != null) {
            scope.launch {
                val ok = ApiClient.sendEmergency(
                    AppConfig.SERVER_URL, token, problemType,
                    location?.first, location?.second
                )
                mainHandler.post {
                    if (ok) {
                        Toast.makeText(appContext, "✅ به پنل ارسال شد", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(appContext, "⚠️ ارسال به پنل ناموفق", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // ۲. باز کردن اپ پیامک (همیشه)
        val user = SessionManager.getUser(appContext)
        val message = buildMessage(user, problemType, location)
        openSmsApp(context, message)
    }

    private fun buildMessage(
        user: User?,
        problemType: String,
        location: Pair<Double, Double>?
    ): String {
        val locText = if (location != null) {
            "موقعیت: %.5f, %.5f".format(location.first, location.second)
        } else {
            "موقعیت در دسترس نیست"
        }
        val userInfo = if (user != null) {
            "راهنما: ${user.fullName}\nاتوبوس: ${user.busNumber ?: "-"}"
        } else {
            "راهنما: نامشخص"
        }
        return "🚨 گزارش اضطراری\nمشکل: $problemType\n$userInfo\n$locText"
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
            Log.e(TAG, "خطا: ${e.message}")
        }
    }
}
