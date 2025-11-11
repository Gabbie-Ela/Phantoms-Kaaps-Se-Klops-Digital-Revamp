package com.example.phantoms.presentation.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.phantoms.R
import com.example.phantoms.data.local.room.artist.ArtistEntity

class ArtistsAdapter(private val listener: OnArtistClick) :
    RecyclerView.Adapter<ArtistsAdapter.VH>() {

    interface OnArtistClick { fun onArtistClick(a: ArtistEntity) }

    private val data = ArrayList<ArtistEntity>()

    fun submit(list: List<ArtistEntity>) {
        data.clear()
        data.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_artist, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val a = data[position]
        holder.name.text = a.name
        holder.extra.text = buildString {
            a.startYear?.let { append("Started: $it  ") }
            a.firstAlbumDate?.let { append("First album: $it") }
        }
        holder.itemView.setOnClickListener { listener.onArtistClick(a) }

        Log.d("ArtistsAdapter", "bind position=$position name=${a.name}")
    }


    override fun getItemCount() = data.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val name: TextView  = v.findViewById(R.id.tvArtistName)
        val extra: TextView = v.findViewById(R.id.tvArtistExtra)
    }
}
