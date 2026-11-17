package com.example.tourguide

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object LocationRepository {
    private val _location = MutableStateFlow<Pair<Double, Double>?>(null)
    val location: StateFlow<Pair<Double, Double>?> = _location

    fun update(lat: Double, lon: Double) {
        _location.value = lat to lon
    }
}
