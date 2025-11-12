package com.example.phantoms.data.local.room.artist

import androidx.lifecycle.LiveData
import androidx.room.*

data class ConcertWithLocation(
    @Embedded val concert: ConcertEntity,
    @Relation(parentColumn = "id", entityColumn = "id", entity = LocationEntity::class)
    val location: LocationEntity? = null
)

@Dao
interface ArtistDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtists(artists: List<ArtistEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<ArtistMemberEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocations(locations: List<LocationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConcerts(concerts: List<ConcertEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArtistConcertLinks(links: List<ArtistConcertEntity>)

    @Query("SELECT * FROM artists ORDER BY name ASC")
    fun observeArtists(): LiveData<List<ArtistEntity>>

    @Query("SELECT * FROM artists WHERE id = :artistId LIMIT 1")
    fun observeArtist(artistId: Long): LiveData<ArtistEntity>

    @Query("SELECT COUNT(*) FROM artists")
    suspend fun countArtists(): Int

    // PHANTOMS feed: upcoming (>= now), joined with location
    @Query("""
        SELECT
            c.id                                AS concertId,
            l.id                                AS locationId,
            -- Compose a readable title. Replace with a real column when you add one.
            COALESCE(
              NULLIF(TRIM(
                l.venueName ||
                CASE WHEN l.city IS NOT NULL AND l.city != '' THEN (' (' || l.city || ')') ELSE '' END
              ), ''),
              'Show'
            )                                   AS title,
            l.venueName                         AS venue,
            CAST(strftime('%s', c.startUtc) AS INTEGER) * 1000 AS startAtMillis,
            NULL                                AS feeCents,
            NULL                                AS imageUrl
        FROM concerts c
        INNER JOIN artist_concerts ac ON ac.concertId = c.id
        LEFT JOIN locations l ON l.id = ac.locationId
        WHERE ac.artistId = :artistId
          AND c.status = 'SCHEDULED'
          AND datetime(c.startUtc) >= datetime('now')
        ORDER BY c.startUtc ASC
    """)
    fun observeUpcomingForArtist(artistId: Long): LiveData<List<UpcomingRow>>
}