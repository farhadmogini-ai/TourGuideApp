package com.example.tourguide

data class User(
    val id: Int,
    val username: String,
    val role: String,
    val firstName: String,
    val lastName: String,
    val city: String?,
    val busNumber: String?,
    val originHotel: String?,
    val phone: String?
) {
    val fullName: String get() = "$firstName $lastName"
}
