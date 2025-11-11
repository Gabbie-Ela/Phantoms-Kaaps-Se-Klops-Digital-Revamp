package com.example.phantoms.presentation.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.phantoms.R
import com.example.phantoms.data.local.room.artist.ArtistEntity
import com.example.phantoms.presentation.viewmodel.ArtistViewModel

class ArtistsFragment : Fragment(), ArtistsAdapter.OnArtistClick {

    private lateinit var artistsRv: RecyclerView
    private lateinit var emptyTv: TextView
    private lateinit var vm: ArtistViewModel
    private lateinit var adapter: ArtistsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_artists, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        vm = ViewModelProvider(this).get(ArtistViewModel::class.java)
        vm.seedIfNeeded()

        artistsRv = view.findViewById(R.id.rvArtists)
        emptyTv = view.findViewById(R.id.tvEmpty)

        artistsRv.layoutManager = LinearLayoutManager(requireContext())
        adapter = ArtistsAdapter(this)
        artistsRv.adapter = adapter

        vm.artists.observe(viewLifecycleOwner) { list: List<ArtistEntity>? ->
            val safe = list ?: emptyList()
            android.util.Log.d("ArtistsFragment", "artists size = ${safe.size}")
            adapter.submit(safe)
            emptyTv.isVisible = safe.isEmpty()
            artistsRv.isGone = safe.isEmpty()
        }
    }

    override fun onArtistClick(artist: ArtistEntity) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.nav_fragment, ArtistDetailFragment.newInstance(artist.id))
            .addToBackStack("artist_detail")
            .commit()
    }
}
