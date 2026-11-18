package com.example.tourguide

data class Station(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

object AppConfig {
    // 📞 شماره کاربر ۳ (مدیر اصلی) — شماره تست
    const val MANAGER_PHONE_NUMBER = "09148687240"

    // 🚌 ایستگاه‌های نمونه مسیر تبریز → مشهد
    // (بعداً مدیر اصلی می‌تواند این لیست را از سرور دریافت کند)
    val STATIONS = listOf(
        Station("تبریز - مبدا", 38.0800, 46.2919),
        Station("استراحتگاه زنجان", 36.6769, 48.4963),
        Station("ایستگاه قزوین", 36.2688, 50.0041),
        Station("استراحتگاه گرمسار", 35.2183, 52.0635),
        Station("ایستگاه شاهرود", 36.4182, 54.9761),
        Station("استراحتگاه نیشابور", 36.2133, 58.7958),
        Station("مشهد - مقصد", 36.2972, 59.6067)
    )
}
