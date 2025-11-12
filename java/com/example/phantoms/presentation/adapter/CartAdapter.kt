package com.example.phantoms.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.phantoms.R
import com.example.phantoms.data.local.room.ProductEntity
import com.example.phantoms.presentation.loadSmart // <-- same helper you use in ProductAdapter

class CartAdapter(
    private val ctx: Context,
    val listener: CartItemClickAdapter
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val cartList = arrayListOf<ProductEntity>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.cart_item_single, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = cartList[position]

        holder.cartName.text = item.name ?: "Item"
        holder.cartPrice.text = "R${item.price}"
        holder.quantityTvCart.text = (item.qua ?: 1).toString()

        // ✅ Unified image loader (URLs, content/file URIs, drawable names, drawable ids)
        // Uses the same extension you already rely on in ProductAdapter
        holder.cartImage.loadSmart(ctx, item.Image, R.drawable.bn)

        holder.cartMore.setOnClickListener { listener.onItemDeleteClick(item) }
        // If you add +/− actions later, call listener.onItemUpdateClick(item) after changing qty.
    }

    override fun getItemCount(): Int = cartList.size

    fun updateList(newList: List<ProductEntity>) {
        cartList.clear()
        cartList.addAll(newList)
        notifyDataSetChanged()
    }

    class CartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cartImage: ImageView = itemView.findViewById(R.id.cartImage)
        val cartMore: ImageView = itemView.findViewById(R.id.cartMore)
        val cartName: TextView = itemView.findViewById(R.id.cartName)
        val cartPrice: TextView = itemView.findViewById(R.id.cartPrice)
        val quantityTvCart: TextView = itemView.findViewById(R.id.quantityTvCart)
    }
}

interface CartItemClickAdapter {
    fun onItemDeleteClick(product: ProductEntity)
    fun onItemUpdateClick(product: ProductEntity)
}
