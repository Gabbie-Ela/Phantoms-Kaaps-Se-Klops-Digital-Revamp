package com.example.phantoms.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.phantoms.data.local.room.AppDatabase
import com.example.phantoms.data.local.room.artist.UpcomingRow
import com.example.phantoms.data.repository.ArtistRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ArtistViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.getInstance(app)
    private val repo = ArtistRepository(db, db.artistDao(), db.rsvpDao()) // socialDao comes from db

    val artists = repo.observeArtists()

    fun seedIfNeeded() = viewModelScope.launch {
        repo.ensureSeeded(getApplication())
        android.util.Log.d("ArtistVM", "post-seed artistCount=${withContext(Dispatchers.IO){ db.artistDao().countArtists() }}")
    }

    fun observeArtist(artistId: Long) = repo.observeArtist(artistId)
    fun observeUpcomingForArtist(artistId: Long) = repo.observeUpcomingForArtist(artistId)
    fun upcomingForPhantoms() = repo.observeUpcomingForPhantoms()
    fun myRsvpConcertIdsOrNull(uid: String?) = uid?.let { repo.observeMyRsvps(it) }

    // NEW
    fun phantomsSocialFeed() = repo.observeSocialFeedForPhantoms()

    fun setGoing(uid: String, row: UpcomingRow) = viewModelScope.launch { repo.setRsvpGoing(uid, row) }
    fun setInterested(uid: String, row: UpcomingRow) = viewModelScope.launch { repo.setRsvpInterested(uid, row) }
}
