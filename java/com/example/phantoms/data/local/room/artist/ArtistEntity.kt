package com.example.phantoms.data.local.room.artist

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imageUrl: String? = null,
    val startYear: Int? = null,
    val firstAlbumDate: String? = null,
    val bio: String? = null
)