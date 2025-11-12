package com.example.phantoms.data.local.room.artist

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "artist_members",
    foreignKeys = [ForeignKey(
        entity = ArtistEntity::class,
        parentColumns = ["id"],
        childColumns = ["artistId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("artistId")]
)
data class ArtistMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val artistId: Long,
    val fullName: String,
    val roleTitle: String? = null,
    val joinDate: String? = null, // yyyy-MM-dd
    val leaveDate: String? = null  // yyyy-MM-dd
)
