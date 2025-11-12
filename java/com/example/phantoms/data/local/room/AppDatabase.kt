package com.example.phantoms.data.local.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.phantoms.data.local.room.Card.CardDao
import com.example.phantoms.data.local.room.Card.CardEntity
import com.example.phantoms.data.local.room.artist.*

@Database(
    entities = [
        ProductEntity::class,
        CardEntity::class,

        // Artist domain
        ArtistEntity::class,
        ArtistMemberEntity::class,
        LocationEntity::class,
        ConcertEntity::class,
        ArtistConcertEntity::class,
        RsvpEntity::class,

        // NEW: offline social feed
        SocialPostEntity::class
    ],
    version = 3, // <-- bumped
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun cardDao(): CardDao

    abstract fun artistDao(): ArtistDao
    abstract fun rsvpDao(): RsvpDao

    // NEW
    abstract fun socialPostDao(): SocialPostDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Existing 1 -> 2 migration (unchanged)
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Artists
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS artists(
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        imageUrl TEXT,
                        startYear INTEGER,
                        firstAlbumDate TEXT,
                        bio TEXT
                    )
                """)
                // Artist Members
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS artist_members(
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        artistId INTEGER NOT NULL,
                        fullName TEXT NOT NULL,
                        roleTitle TEXT,
                        joinDate TEXT,
                        leaveDate TEXT,
                        FOREIGN KEY (artistId) REFERENCES artists(id) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_members_artistId ON artist_members(artistId)")

                // Locations
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS locations(
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        venueName TEXT NOT NULL,
                        street TEXT,
                        city TEXT NOT NULL,
                        region TEXT,
                        country TEXT NOT NULL,
                        postalCode TEXT,
                        latitude REAL,
                        longitude REAL,
                        tz TEXT NOT NULL
                    )
                """)

                // Concerts
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS concerts(
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        startUtc TEXT NOT NULL,
                        endUtc TEXT,
                        status TEXT NOT NULL,
                        isSoldOut INTEGER NOT NULL
                    )
                """)

                // Artist ↔ Concert ↔ Location
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS artist_concerts(
                        artistId INTEGER NOT NULL,
                        concertId INTEGER NOT NULL,
                        locationId INTEGER NOT NULL,
                        tourName TEXT,
                        notes TEXT,
                        PRIMARY KEY (artistId, concertId, locationId),
                        FOREIGN KEY (artistId) REFERENCES artists(id) ON DELETE CASCADE,
                        FOREIGN KEY (concertId) REFERENCES concerts(id) ON DELETE CASCADE,
                        FOREIGN KEY (locationId) REFERENCES locations(id)
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_concerts_artistId ON artist_concerts(artistId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_concerts_concertId ON artist_concerts(concertId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_artist_concerts_locationId ON artist_concerts(locationId)")

                // RSVPs
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS rsvps(
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        userUid TEXT NOT NULL,
                        aclArtistId INTEGER NOT NULL,
                        aclConcertId INTEGER NOT NULL,
                        aclLocationId INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY (aclArtistId, aclConcertId, aclLocationId)
                        REFERENCES artist_concerts(artistId, concertId, locationId) ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS index_rsvps_aclArtistId ON rsvps(aclArtistId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_rsvps_aclConcertId ON rsvps(aclConcertId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_rsvps_aclLocationId ON rsvps(aclLocationId)")
            }
        }

        // NEW 2 -> 3 migration: create social_posts
        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS social_posts(
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        artistId INTEGER NOT NULL,
                        source TEXT NOT NULL,
                        text TEXT,
                        imageUrl TEXT,
                        linkUrl TEXT,
                        createdAt INTEGER NOT NULL,
                        FOREIGN KEY (artistId) REFERENCES artists(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("CREATE INDEX IF NOT EXISTS index_social_posts_artistId ON social_posts(artistId)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_social_posts_createdAt ON social_posts(createdAt)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // For dev only you can use the line below to avoid migration pain:
                    // .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
