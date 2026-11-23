package com.example.tourguide

data class Station(
    val id: Int = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double
)

object AppConfig {
    // 📞 شماره کاربر ۳ (مدیر اصلی) — برای SMS آفلاین
    const val MANAGER_PHONE_NUMBER = "09148687240"

    // 🌐 آدرس سرور — IP کامپیوتر شما
    const val SERVER_URL = "http://192.168.1.13:3000"

    // ⚠️ اطلاعات راهنما دیگر اینجا نیست؛ از سرور در زمان لاگین دریافت می‌شود
}
