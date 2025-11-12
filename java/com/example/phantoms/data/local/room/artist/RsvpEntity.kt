package com.example.phantoms.data.local.room.artist

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "rsvps",
    foreignKeys = [ForeignKey(
        entity = ArtistConcertEntity::class,
        parentColumns = ["artistId","concertId","locationId"],
        childColumns = ["aclArtistId","aclConcertId","aclLocationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("aclArtistId"), Index("aclConcertId"), Index("aclLocationId")]
)
data class RsvpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userUid: String, // Firebase uid or local user id string
    val aclArtistId: Long,
    val aclConcertId: Long,
    val aclLocationId: Long,
    val status: String = "INTERESTED", // GOING | INTERESTED | CANCELLED
    val createdAt: Long = System.currentTimeMillis()
)
