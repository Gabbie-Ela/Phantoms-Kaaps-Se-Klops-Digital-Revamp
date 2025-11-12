package com.example.phantoms.data.local.room.artist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "concerts")
data class ConcertEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startUtc: String, // ISO-8601 UTC 'yyyy-MM-ddTHH:mm:ssZ'
    val endUtc: String? = null,
    val status: String = "SCHEDULED", // SCHEDULED | CANCELLED | POSTPONED
    val isSoldOut: Boolean = false
)
