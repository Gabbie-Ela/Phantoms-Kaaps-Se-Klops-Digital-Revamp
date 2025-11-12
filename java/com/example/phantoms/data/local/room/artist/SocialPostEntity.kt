// data/local/room/artist/SocialPostEntity.kt
package com.example.phantoms.data.local.room.artist

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "social_posts",
    foreignKeys = [
        ForeignKey(
            entity = ArtistEntity::class,
            parentColumns = ["id"],
            childColumns = ["artistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("artistId"),
        Index("createdAt")
    ]
)
data class SocialPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val artistId: Long,
    val source: String,     // NOT NULL
    val text: String?,      // nullable
    val imageUrl: String?,  // nullable
    val linkUrl: String?,   // nullable
    val createdAt: Long     // NOT NULL
)
