package com.example.phantoms.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.phantoms.R
import com.example.phantoms.data.model.Product
import com.example.phantoms.presentation.loadSmart

class CoverProductAdapter(
    private val ctx: Context,
    private val coverProductList: ArrayList<Product>,
    private val onCheckClick: (product: Product, position: Int) -> Unit
) : RecyclerView.Adapter<CoverProductAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.cover_single, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val p = coverProductList[position]

        holder.productNoteCover.text = p.productNote
        holder.productImageCover.loadSmart(ctx, p.productImage)

        val click: (View) -> Unit = {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                onCheckClick(coverProductList[pos], pos)
            }
        }
        holder.productCheckCover.setOnClickListener(click)
        holder.itemView.setOnClickListener(click)
    }

    override fun getItemCount() = coverProductList.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val productImageCover: ImageView = itemView.findViewById(R.id.productImage_coverPage)
        val productNoteCover: TextView  = itemView.findViewById(R.id.productNoteCover)
        val productCheckCover: Button   = itemView.findViewById(R.id.productCheck_coverPage)
    }
}
