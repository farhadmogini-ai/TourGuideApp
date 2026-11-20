package com.example.tourguide

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object EmergencySmsHelper {

    private const val TAG = "EmergencySms"
    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun sendEmergencySms(
        context: Context,
        problemType: String,
        location: Pair<Double, Double>?
    ) {
        // ۱. ارسال به سرور (برای پنل مدیر)
        val appContext = context.applicationContext
        executor.execute { sendToServer(appContext, problemType, location) }

        // ۲. باز کردن اپ پیامک (روش فعلی)
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

    private fun sendToServer(
        context: Context,
        problemType: String,
        location: Pair<Double, Double>?
    ) {
        try {
            val url = URL("${AppConfig.SERVER_URL}/api/emergency")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.doOutput = true
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            val json = JSONObject().apply {
                put("guideId", AppConfig.GUIDE_ID)
                put("guideName", AppConfig.GUIDE_NAME)
                put("busNumber", AppConfig.BUS_NUMBER)
                put("phone", AppConfig.GUIDE_PHONE)
                put("problemType", problemType)
                if (location != null) {
                    put("lat", location.first)
                    put("lon", location.second)
                } else {
                    put("lat", JSONObject.NULL)
                    put("lon", JSONObject.NULL)
                }
            }

            conn.outputStream.use { os ->
                os.write(json.toString().toByteArray(Charsets.UTF_8))
            }

            val code = conn.responseCode
            if (code == 200) {
                Log.d(TAG, "✅ پیام اضطراری به سرور ارسال شد")
                mainHandler.post {
                    Toast.makeText(
                        context,
                        "✅ پیام اضطراری به پنل ارسال شد",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Log.w(TAG, "⚠️ پاسخ سرور: $code")
            }
            conn.disconnect()
        } catch (e: Exception) {
            Log.e(TAG, "❌ خطا در ارسال به سرور: ${e.message}")
            mainHandler.post {
                Toast.makeText(
                    context,
                    "⚠️ ارسال به پنل ناموفق (پیامک همچنان ارسال می‌شود)",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
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
