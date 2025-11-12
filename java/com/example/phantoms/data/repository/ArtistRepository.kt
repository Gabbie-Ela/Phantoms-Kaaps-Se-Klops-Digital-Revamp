package com.example.phantoms.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.withTransaction
import com.example.phantoms.data.local.room.AppDatabase
import com.example.phantoms.data.local.room.artist.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ArtistRepository(
    private val db: AppDatabase,
    private val dao: ArtistDao,
    private val rsvpDao: RsvpDao,
    private val socialDao: SocialPostDao = db.socialPostDao()
) {

    companion object {
        const val PHANTOMS_ID: Long = 1L
    }

    // --------- Observe ----------
    fun observeArtists(): LiveData<List<ArtistEntity>> = dao.observeArtists()
    fun observeArtist(artistId: Long): LiveData<ArtistEntity> = dao.observeArtist(artistId)
    fun observeUpcomingForArtist(artistId: Long): LiveData<List<UpcomingRow>> =
        dao.observeUpcomingForArtist(artistId)
    fun observeUpcomingForPhantoms(): LiveData<List<UpcomingRow>> =
        dao.observeUpcomingForArtist(PHANTOMS_ID)
    fun observeMyRsvps(userUid: String): LiveData<List<Long>> =
        rsvpDao.observeUserConcertIds(userUid, PHANTOMS_ID)

    // --------- Social feed (Room-only) ----------
    fun observeSocialFeedForPhantoms(): LiveData<List<SocialPostEntity>> =
        socialDao.observeForArtist(PHANTOMS_ID)

    private suspend fun seedSocialIfEmpty() {
        val count = socialDao.countForArtist(PHANTOMS_ID)
        if (count > 0) return

        val now = System.currentTimeMillis()
        val demo = listOf(
            SocialPostEntity(
                artistId = PHANTOMS_ID,
                source = "Instagram",
                text = "Studio sesh tonight 🎶",
                imageUrl = null,
                linkUrl = "https://www.instagram.com/p/Cx123456789/",
                createdAt = now - 2 * 24 * 60 * 60 * 1000L
            ),
            SocialPostEntity(
                artistId = PHANTOMS_ID,
                source = "TikTok",
                text = "New chorus sneak peek",
                imageUrl = null,
                linkUrl = "https://www.tiktok.com/@phantomsband/video/7300000000000000000",
                createdAt = now - 1 * 24 * 60 * 60 * 1000L
            ),
            SocialPostEntity(
                artistId = PHANTOMS_ID,
                source = "Website",
                text = "Merch drop this Friday!",
                imageUrl = null,
                linkUrl = "https://example.com/phantoms/merch",
                createdAt = now - 6 * 60 * 60 * 1000L
            )
        )
        socialDao.insertAll(demo)
    }

    // --------- RSVP actions ----------
    suspend fun setRsvpGoing(uid: String, row: UpcomingRow) = setRsvp(uid, row, "GOING")
    suspend fun setRsvpInterested(uid: String, row: UpcomingRow) = setRsvp(uid, row, "INTERESTED")

    suspend fun clearRsvp(uid: String, row: UpcomingRow) {
        withContext(Dispatchers.IO) {
            rsvpDao.upsert(
                RsvpEntity(
                    id = 0,
                    userUid = uid,
                    aclArtistId = PHANTOMS_ID,
                    aclConcertId = row.concertId,
                    aclLocationId = row.locationId,
                    status = "CANCELLED",
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun setRsvp(uid: String, row: UpcomingRow, status: String) {
        withContext(Dispatchers.IO) {
            rsvpDao.upsert(
                RsvpEntity(
                    id = 0,
                    userUid = uid,
                    aclArtistId = PHANTOMS_ID,
                    aclConcertId = row.concertId,
                    aclLocationId = row.locationId,
                    status = status,
                    createdAt = System.currentTimeMillis()
                )
            )
        }
    }

    // --------- Seeding entrypoint ----------
    suspend fun ensureSeeded(context: Context) {
        withContext(Dispatchers.IO) {
            val artistCount = dao.countArtists()
            if (artistCount == 0) {
                seedSafely(context)
            }
            seedSocialIfEmpty()
        }
    }

    private suspend fun seedSafely(context: Context) {
        db.withTransaction {
            val payload = try {
                val text = context.assets.open("artists_seed.json").use { input ->
                    BufferedReader(InputStreamReader(input)).readText()
                }
                parseJsonSeed(JSONObject(text))
            } catch (_: Throwable) {
                buildFallbackSeed()
            }

            val normalized = normalizeSeed(payload)

            dao.insertArtists(normalized.artists)
            dao.insertMembers(normalized.members)
            dao.insertLocations(normalized.locations)
            dao.insertConcerts(normalized.concerts)
            dao.insertArtistConcertLinks(normalized.links)
        }
    }

    // ---- Seed model (kept INSIDE the class) ----
    private data class RawSeed(
        val artists: MutableList<ArtistEntity>,
        val members: MutableList<ArtistMemberEntity>,
        val locations: MutableList<LocationEntity>,
        val concerts: MutableList<ConcertEntity>,
        val links: MutableList<ArtistConcertEntity>
    )

    private fun parseJsonSeed(json: JSONObject): RawSeed {
        val artists = mutableListOf<ArtistEntity>()
        val members = mutableListOf<ArtistMemberEntity>()
        val locations = mutableListOf<LocationEntity>()
        val concerts = mutableListOf<ConcertEntity>()
        val links = mutableListOf<ArtistConcertEntity>()

        val jArtists = json.optJSONArray("artists") ?: org.json.JSONArray()
        for (i in 0 until jArtists.length()) {
            val a = jArtists.getJSONObject(i)
            artists.add(
                ArtistEntity(
                    id = a.optLong("id", 0),
                    name = a.getString("name"),
                    imageUrl = a.optString("imageUrl", null),
                    startYear = if (a.has("startYear")) a.getInt("startYear") else null,
                    firstAlbumDate = a.optString("firstAlbumDate", null),
                    bio = a.optString("bio", null)
                )
            )
            val jMembers = a.optJSONArray("members")
            if (jMembers != null) {
                for (m in 0 until jMembers.length()) {
                    val mm = jMembers.getJSONObject(m)
                    members.add(
                        ArtistMemberEntity(
                            id = mm.optLong("id", 0),
                            artistId = a.optLong("id", 0),
                            fullName = mm.getString("fullName"),
                            roleTitle = mm.optString("roleTitle", null),
                            joinDate = mm.optString("joinDate", null),
                            leaveDate = mm.optString("leaveDate", null)
                        )
                    )
                }
            }
        }

        val jLocations = json.optJSONArray("locations") ?: org.json.JSONArray()
        for (i in 0 until jLocations.length()) {
            val l = jLocations.getJSONObject(i)
            locations.add(
                LocationEntity(
                    id = l.optLong("id", 0),
                    venueName = l.getString("venueName"),
                    street = l.optString("street", null),
                    city = l.getString("city"),
                    region = l.optString("region", null),
                    country = l.getString("country"),
                    postalCode = l.optString("postalCode", null),
                    latitude = if (l.has("latitude")) l.getDouble("latitude") else null,
                    longitude = if (l.has("longitude")) l.getDouble("longitude") else null,
                    tz = l.getString("tz")
                )
            )
        }

        val jConcerts = json.optJSONArray("concerts") ?: org.json.JSONArray()
        for (i in 0 until jConcerts.length()) {
            val c = jConcerts.getJSONObject(i)
            concerts.add(
                ConcertEntity(
                    id = c.optLong("id", 0),
                    startUtc = c.getString("startUtc"),
                    endUtc = c.optString("endUtc", null),
                    status = c.optString("status", "SCHEDULED"),
                    isSoldOut = c.optBoolean("isSoldOut", false)
                )
            )
        }

        val jLinks = json.optJSONArray("artist_concerts") ?: org.json.JSONArray()
        for (i in 0 until jLinks.length()) {
            val x = jLinks.getJSONObject(i)
            links.add(
                ArtistConcertEntity(
                    artistId = x.getLong("artistId"),
                    concertId = x.getLong("concertId"),
                    locationId = x.getLong("locationId"),
                    tourName = x.optString("tourName", null),
                    notes = x.optString("notes", null)
                )
            )
        }

        return RawSeed(artists, members, locations, concerts, links)
    }

    // --- API21-safe UTC time helpers ---
    private fun utcNowMillis(): Long {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        return cal.timeInMillis
    }

    private fun toIsoUtc(millis: Long): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date(millis))
    }

    private fun isoUtcPlusDays(days: Int): String {
        val oneDay = 24L * 60L * 60L * 1000L
        val future = utcNowMillis() + days * oneDay
        return toIsoUtc(future)
    }

    /**
     * Fallback minimal seed used when assets are missing/malformed.
     */
    private fun buildFallbackSeed(): RawSeed {
        val nowPlus7 = isoUtcPlusDays(7)
        val artists = mutableListOf(
            ArtistEntity(
                id = PHANTOMS_ID,
                name = "Phantoms",
                imageUrl = null,
                startYear = 2013,
                firstAlbumDate = null,
                bio = "Electronic duo."
            )
        )
        val members = mutableListOf<ArtistMemberEntity>()
        val locations = mutableListOf(
            LocationEntity(
                id = 100L,
                venueName = "City Hall",
                street = null,
                city = "Cape Town",
                region = "WC",
                country = "ZA",
                postalCode = null,
                latitude = null,
                longitude = null,
                tz = "Africa/Johannesburg"
            )
        )
        val concerts = mutableListOf(
            ConcertEntity(
                id = 1000L,
                startUtc = nowPlus7,
                endUtc = null,
                status = "SCHEDULED",
                isSoldOut = false
            )
        )
        val links = mutableListOf(
            ArtistConcertEntity(
                artistId = PHANTOMS_ID,
                concertId = 1000L,
                locationId = 100L,
                tourName = "SA Mini Tour",
                notes = null
            )
        )
        return RawSeed(artists, members, locations, concerts, links)
    }

    /**
     * Normalize and guarantee referential integrity.
     */
    private fun normalizeSeed(seed: RawSeed): RawSeed {
        var nextArtistId = (seed.artists.maxOfOrNull { it.id } ?: 0L).coerceAtLeast(PHANTOMS_ID) + 1
        var nextMemberId = (seed.members.maxOfOrNull { it.id } ?: 0L) + 1
        var nextLocId = (seed.locations.maxOfOrNull { it.id } ?: 0L) + 1
        var nextConcertId = (seed.concerts.maxOfOrNull { it.id } ?: 0L) + 1

        if (seed.artists.none { it.id == PHANTOMS_ID }) {
            seed.artists.add(
                ArtistEntity(
                    id = PHANTOMS_ID,
                    name = "Phantoms",
                    imageUrl = null,
                    startYear = 2013,
                    firstAlbumDate = null,
                    bio = "Electronic duo."
                )
            )
        }

        fun <T> fixIds(
            items: MutableList<T>,
            getId: (T) -> Long,
            setId: (T, Long) -> T,
            startId: () -> Long
        ) {
            val seen = hashSetOf<Long>()
            for (i in items.indices) {
                var id = getId(items[i])
                if (id == 0L || id in seen) {
                    id = startId()
                    items[i] = setId(items[i], id)
                }
                seen.add(id)
            }
        }

        fixIds(
            seed.artists,
            getId = { it.id },
            setId = { a, id -> a.copy(id = id) },
            startId = { (nextArtistId++).also { if (it == PHANTOMS_ID) nextArtistId++ } }
        )
        fixIds(
            seed.members,
            getId = { it.id },
            setId = { m, id -> m.copy(id = id) },
            startId = { nextMemberId++ }
        )
        fixIds(
            seed.locations,
            getId = { it.id },
            setId = { l, id -> l.copy(id = id) },
            startId = { nextLocId++ }
        )
        fixIds(
            seed.concerts,
            getId = { it.id },
            setId = { c, id -> c.copy(id = id) },
            startId = { nextConcertId++ }
        )

        val defaultFuture = { isoUtcPlusDays(7) }
        for (i in seed.concerts.indices) {
            val c = seed.concerts[i]
            if (c.startUtc.isBlank()) {
                seed.concerts[i] = c.copy(startUtc = isoUtcPlusDays(7))
            }
        }

        val artistIds = seed.artists.map { it.id }.toHashSet()
        val locIds = seed.locations.map { it.id }.toHashSet()
        val concertIds = seed.concerts.map { it.id }.toHashSet()
        val filteredLinks = seed.links.filter {
            it.artistId in artistIds && it.locationId in locIds && it.concertId in concertIds
        }.toMutableList()

        val phantomsHasShow = filteredLinks.any { it.artistId == PHANTOMS_ID }
        if (!phantomsHasShow) {
            val locId = seed.locations.firstOrNull()?.id
                ?: run {
                    val newId = nextLocId++
                    seed.locations.add(
                        LocationEntity(
                            id = newId,
                            venueName = "City Hall",
                            street = null,
                            city = "Cape Town",
                            region = "WC",
                            country = "ZA",
                            postalCode = null,
                            latitude = null,
                            longitude = null,
                            tz = "Africa/Johannesburg"
                        )
                    )
                    newId
                }
            val concertId = nextConcertId++
            seed.concerts.add(
                ConcertEntity(
                    id = concertId,
                    startUtc = defaultFuture(),
                    endUtc = null,
                    status = "SCHEDULED",
                    isSoldOut = false
                )
            )
            filteredLinks.add(
                ArtistConcertEntity(
                    artistId = PHANTOMS_ID,
                    concertId = concertId,
                    locationId = locId,
                    tourName = "SA Mini Tour",
                    notes = null
                )
            )
        }

        return seed.copy(links = filteredLinks)
    }
}
