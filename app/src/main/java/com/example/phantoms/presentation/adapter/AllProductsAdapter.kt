// AllProductsAdapter.kt
package com.example.phantoms.presentation.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.phantoms.R
import com.example.phantoms.data.model.Product
import com.example.phantoms.presentation.activity.ProductDetailsActivity
import com.example.phantoms.presentation.loadSmart

data class UiProduct(
    val product: Product,
    val source: String,       // "New" or "Cover"
    val indexInSource: Int
)

class AllProductsAdapter(
    private val ctx: Context,
    private val items: List<UiProduct>
) : RecyclerView.Adapter<AllProductsAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.single_product, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val ui = items[position]
        val p = ui.product

        h.brand.text = p.productBrand
        h.name.text = p.productName
        h.price.text = "R${p.productPrice}"
        h.rating.rating = p.productRating
        h.image.loadSmart(ctx, p.productImage)

        // discount/new pill
        when {
            p.productHave == true && !p.productDisCount.isNullOrBlank() -> {
                h.discountWrap.visibility = View.VISIBLE
                h.discount.text = p.productDisCount
            }
            p.productHave == false -> {
                h.discountWrap.visibility = View.VISIBLE
                h.discount.text = "New"
            }
            else -> h.discountWrap.visibility = View.GONE
        }

        h.itemView.setOnClickListener {
            val i = Intent(ctx, ProductDetailsActivity::class.java).apply {
                putExtra("ProductIndex", ui.indexInSource)
                putExtra("ProductFrom", ui.source)
            }
            ctx.startActivity(i)
        }
    }

    override fun getItemCount() = items.size

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val image: ImageView = v.findViewById(R.id.productImage_singleProduct)
        val rating: RatingBar = v.findViewById(R.id.productRating_singleProduct)
        val brand: TextView = v.findViewById(R.id.productBrandName_singleProduct)
        val discount: TextView = v.findViewById(R.id.discountTv_singleProduct)
        val name: TextView = v.findViewById(R.id.productName_singleProduct)
        val price: TextView = v.findViewById(R.id.productPrice_singleProduct)
        val discountWrap: LinearLayout = v.findViewById(R.id.discount_singleProduct)
    }
}
