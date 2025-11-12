// data/local/room/artist/UpcomingRow.kt
package com.example.phantoms.data.local.room.artist

data class UpcomingRow(
    val concertId: Long,       // for RSVP
    val locationId: Long,      // for RSVP
    val title: String,         // UI
    val venue: String?,        // UI
    val startAtMillis: Long,   // UI (epoch millis)
    val feeCents: Int?,        // UI (optional, null for now)
    val imageUrl: String?      // UI (optional, null for now)
)
