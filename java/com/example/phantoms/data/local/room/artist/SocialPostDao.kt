package com.example.phantoms.data.local.room.artist

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface SocialPostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<SocialPostEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: SocialPostEntity)

    @Query("SELECT COUNT(*) FROM social_posts WHERE artistId = :artistId")
    suspend fun countForArtist(artistId: Long): Int

    @Query("""
        SELECT * FROM social_posts
        WHERE artistId = :artistId
        ORDER BY createdAt DESC
    """)
    fun observeForArtist(artistId: Long): LiveData<List<SocialPostEntity>>
}
