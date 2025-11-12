package com.example.phantoms.data.local.room.artist

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "artist_concerts",
    primaryKeys = ["artistId", "concertId", "locationId"],
    foreignKeys = [
        ForeignKey(entity = ArtistEntity::class, parentColumns = ["id"], childColumns = ["artistId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = ConcertEntity::class, parentColumns = ["id"], childColumns = ["concertId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = LocationEntity::class, parentColumns = ["id"], childColumns = ["locationId"])
    ],
    indices = [Index("artistId"), Index("concertId"), Index("locationId")]
)
data class ArtistConcertEntity(
    val artistId: Long,
    val concertId: Long,
    val locationId: Long,
    val tourName: String? = null,
    val notes: String? = null
)
