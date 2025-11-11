package com.example.phantoms.data.local.room.artist

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface RsvpDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rsvp: RsvpEntity)

    @Query("""
       SELECT aclConcertId FROM rsvps
       WHERE userUid = :userUid AND aclArtistId = :artistId
    """)
    fun observeUserConcertIds(userUid: String, artistId: Long): LiveData<List<Long>>
}