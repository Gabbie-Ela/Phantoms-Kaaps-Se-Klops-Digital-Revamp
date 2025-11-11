package com.example.phantoms.presentation.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.phantoms.data.model.Product
import com.example.phantoms.presentation.activity.ProductDetailsActivity
import com.example.phantoms.R
import com.example.phantoms.presentation.loadSmart

class ProductAdapter(
    private val productList: ArrayList<Product>,
    context: Context
) : RecyclerView.Adapter<ProductAdapter.ViewHolder>()  {

    private val ctx: Context = context

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val productView = LayoutInflater.from(parent.context)
            .inflate(R.layout.single_product, parent, false)
        return ViewHolder(productView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product: Product = productList[position]

        // show brand, name, id, price, rating
        holder.productBrandName_singleProduct.text = product.productBrand
        // If you don’t have a dedicated TextView for productId, append it to name:
        // “Phantoms Basketball Set (#1)”
        val idSuffix = product.productId?.takeIf { it.isNotBlank() }?.let { " (#$it)" } ?: ""
        holder.productName_singleProduct.text = "${product.productName.orEmpty()}$idSuffix"

        holder.productPrice_singleProduct.text = "R${product.productPrice}"
        holder.productRating_singleProduct.rating = product.productRating

        // ✅ load res:// drawables and remote URLs with a safe fallback
        holder.productImage_singleProduct.loadSmart(ctx, product.productImage, R.drawable.bn)

        // discount / “New” ribbon logic (kept as-is, but null-safe)
        if (product.productHave == true) {
            holder.discount_singleProduct.visibility = View.VISIBLE
            holder.discountTv_singleProduct.text = product.productDisCount.orEmpty()
        } else if (product.productHave == false) {
            holder.discount_singleProduct.visibility = View.VISIBLE
            holder.discountTv_singleProduct.text = "New"
        } else {
            holder.discount_singleProduct.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { goDetailsPage(position) }
    }

    override fun getItemCount(): Int = productList.size

    class ViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val productImage_singleProduct: ImageView = itemView.findViewById(R.id.productImage_singleProduct)
        val productAddToFav_singleProduct: ImageView = itemView.findViewById(R.id.productAddToFav_singleProduct)
        val productRating_singleProduct: RatingBar = itemView.findViewById(R.id.productRating_singleProduct)
        val productBrandName_singleProduct: TextView = itemView.findViewById(R.id.productBrandName_singleProduct)
        val discountTv_singleProduct: TextView = itemView.findViewById(R.id.discountTv_singleProduct)
        val productName_singleProduct: TextView = itemView.findViewById(R.id.productName_singleProduct)
        val productPrice_singleProduct: TextView = itemView.findViewById(R.id.productPrice_singleProduct)
        val discount_singleProduct: LinearLayout = itemView.findViewById(R.id.discount_singleProduct)
    }

    private fun goDetailsPage(position: Int) {
        val intent = Intent(ctx, ProductDetailsActivity::class.java).apply {
            putExtra("ProductIndex", position)
            putExtra("ProductFrom", "New")
        }
        ctx.startActivity(intent)
    }
}
