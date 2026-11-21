package com.example.tourguide

data class Station(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

object AppConfig {
    // 📞 شماره کاربر ۳ (مدیر اصلی) — شماره تست
    const val MANAGER_PHONE_NUMBER = "09148687240"

    // 🌐 آدرس سرور محلی — IP کامپیوتر شما
    const val SERVER_URL = "http://192.168.1.11:3000"

    // 🆔 شناسه این راهنما
    const val GUIDE_ID = "guide-1"
    const val GUIDE_NAME = "راهنمای تست"
    const val BUS_NUMBER = "5"

    // 📞 شماره تلفن این راهنما (برای نمایش در پنل مدیر)
    const val GUIDE_PHONE = "09143031986"

    // 🚌 ایستگاه‌های نمونه مسیر تبریز → مشهد
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
