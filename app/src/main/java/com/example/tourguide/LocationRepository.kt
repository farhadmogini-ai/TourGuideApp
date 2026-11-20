package com.example.tourguide

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LocationRepository {
    private val _location = MutableStateFlow<Pair<Double, Double>?>(null)
    val location: StateFlow<Pair<Double, Double>?> = _location

    private val _serverStatus = MutableStateFlow("آماده")
    val serverStatus: StateFlow<String> = _serverStatus

    fun update(lat: Double, lon: Double) {
        _location.value = lat to lon
    }

    fun updateServerStatus(status: String) {
        _serverStatus.value = status
    }
}
