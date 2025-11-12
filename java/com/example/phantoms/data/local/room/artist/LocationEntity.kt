package com.example.phantoms.data.local.room.artist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "locations")
data class LocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val venueName: String,
    val street: String? = null,
    val city: String,
    val region: String? = null,
    val country: String,
    val postalCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val tz: String // IANA TZ e.g., Africa/Johannesburg
)
