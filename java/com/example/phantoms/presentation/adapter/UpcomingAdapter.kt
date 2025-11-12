package com.example.phantoms.presentation.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.phantoms.R
import com.example.phantoms.data.local.room.artist.UpcomingRow
import java.text.SimpleDateFormat
import java.util.*

class UpcomingAdapter(
    private val onGoing: (UpcomingRow) -> Unit,
    private val onInterested: (UpcomingRow) -> Unit
) : ListAdapter<UpcomingRow, UpcomingAdapter.VH>(Diff()) {

    class Diff : DiffUtil.ItemCallback<UpcomingRow>() {
        override fun areItemsTheSame(oldItem: UpcomingRow, newItem: UpcomingRow): Boolean {
            // Identity = same concert + same location
            return oldItem.concertId == newItem.concertId &&
                    oldItem.locationId == newItem.locationId
        }
        override fun areContentsTheSame(oldItem: UpcomingRow, newItem: UpcomingRow): Boolean {
            // Data class equals is fine here
            return oldItem == newItem
        }
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val ivPoster: ImageView = v.findViewById(R.id.ivPoster)
        val tvTitle: TextView = v.findViewById(R.id.tvTitle)
        val tvWhereWhen: TextView = v.findViewById(R.id.tvWhereWhen)
        val tvMeta: TextView = v.findViewById(R.id.tvMeta)
        val btnGoing: Button = v.findViewById(R.id.btnGoing)
        val btnInterested: Button = v.findViewById(R.id.btnInterested)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.row_upcoming_event, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, pos: Int) {
        val item = getItem(pos)

        // Title
        h.tvTitle.text = item.title

        // Where & When
        val dateStr = formatDate(item.startAtMillis)
        val where = item.venue?.takeIf { it.isNotBlank() } ?: "TBA"
        h.tvWhereWhen.text = "$where • $dateStr"

        // Meta (fee)
        h.tvMeta.text = feeText(item.feeCents)

        // Image (optional)
        if (!item.imageUrl.isNullOrBlank()) {
            h.ivPoster.visibility = View.VISIBLE
            // TODO: load image (Glide/Picasso/your loadSmart)
        } else {
            h.ivPoster.visibility = View.GONE
        }

        h.btnGoing.setOnClickListener { onGoing(item) }
        h.btnInterested.setOnClickListener { onInterested(item) }
    }

    private fun formatDate(epochMillis: Long): String {
        val df = SimpleDateFormat("EEE, dd MMM yyyy • HH:mm", Locale.getDefault())
        return df.format(Date(epochMillis))
    }

    private fun feeText(cents: Int?): String =
        when {
            cents == null -> "Fee: N/A"
            cents == 0 -> "Free"
            cents > 0 -> "R ${"%.2f".format(cents / 100.0)}"
            else -> "Fee: N/A"
        }
}
