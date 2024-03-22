package com.example.locationtracker.model

// LocationEntryModel represents a location entry in the database
data class LocationEntryModel(
    val deviceId: String,
    val latitude: Double,
    val longitude: Double
)
