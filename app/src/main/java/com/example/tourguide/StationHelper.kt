package com.example.tourguide

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import kotlin.math.*

object StationHelper {

    /**
     * محاسبه فاصله بین دو مختصات با فرمول Haversine (بر حسب کیلومتر)
     */
    fun distanceKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val R = 6371.0 // شعاع زمین به کیلومتر
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    /**
     * تخمین زمان بر حسب دقیقه — با فرض میانگین سرعت ۶۰ کیلومتر در ساعت
     */
    fun estimateMinutes(distanceKm: Double): Int {
        return (distanceKm * 60.0 / 60.0).toInt()
    }

    /**
     * باز کردن نقشه پیش‌فرض گوشی با مسیریابی به ایستگاه
     */
    fun openNavigation(context: Context, station: Station) {
        // تلاش اول: Google Maps با مسیریابی نوبت‌به‌نوبت
        val navIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("google.navigation:q=${station.latitude},${station.longitude}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        // تلاش دوم: هر اپ نقشه دیگری (نشان، بلد، ...)
        val geoIntent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(
                "geo:${station.latitude},${station.longitude}" +
                "?q=${station.latitude},${station.longitude}" +
                "(${Uri.encode(station.name)})"
            )
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(navIntent)
        } catch (e: Exception) {
            try {
                context.startActivity(geoIntent)
            } catch (e2: Exception) {
                Toast.makeText(
                    context,
                    "اپلیکیشن نقشه روی گوشی پیدا نشد",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
