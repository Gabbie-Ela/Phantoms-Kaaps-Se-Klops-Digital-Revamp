// ShopFragment.kt
package com.example.phantoms.presentation.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.phantoms.R
import com.example.phantoms.data.model.Product
import com.example.phantoms.presentation.activity.ProductDetailsActivity
import com.example.phantoms.presentation.adapter.AllProductsAdapter
import com.example.phantoms.presentation.adapter.CoverProductAdapter
import com.example.phantoms.presentation.adapter.UiProduct
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException

class ShopFragment : Fragment() {

    private lateinit var coverRv: RecyclerView
    private lateinit var allRv: RecyclerView

    private lateinit var coverAdapter: CoverProductAdapter
    private lateinit var allAdapter: AllProductsAdapter

    private val coverProducts = arrayListOf<Product>()   // CoverProducts.json
    private val newProducts = arrayListOf<Product>()     // NewProducts.json
    private val combined = mutableListOf<UiProduct>()    // grid data

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_shop, container, false)
        requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        coverRv = v.findViewById(R.id.coverRecView_shopFrag)
        allRv = v.findViewById(R.id.allProductsRecView)

        // Load data
        loadProducts()

        // Cover (horizontal) — clicking “Check” goes to details (source = "Cover")
        coverRv.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        coverRv.setHasFixedSize(true)
        coverRv.isNestedScrollingEnabled = false

        coverAdapter = CoverProductAdapter(
            requireContext(),
            coverProducts
        ) { _, position ->
            // open ProductDetailsActivity with the Cover index
            startActivity(Intent(requireContext(), ProductDetailsActivity::class.java).apply {
                putExtra("ProductIndex", position)
                putExtra("ProductFrom", "Cover")
            })
        }
        coverRv.adapter = coverAdapter

        // Grid (2 columns) of ALL products (New + Cover)
        allRv.layoutManager = GridLayoutManager(requireContext(), 2)
        allRv.setHasFixedSize(true)
        allRv.isNestedScrollingEnabled = false

        // Build combined list (order: New first, then Cover — change if you prefer)
        combined.clear()
        newProducts.forEachIndexed { idx, p ->
            combined.add(UiProduct(p, source = "New", indexInSource = idx))
        }
        coverProducts.forEachIndexed { idx, p ->
            combined.add(UiProduct(p, source = "Cover", indexInSource = idx))
        }

        allAdapter = AllProductsAdapter(requireContext(), combined)
        allRv.adapter = allAdapter

        return v
    }

    private fun loadProducts() {
        coverProducts.clear()
        newProducts.clear()

        val gson = Gson()
        val type = object : TypeToken<List<Product>>() {}.type

        getJsonData(requireContext(), "CoverProducts.json")?.let {
            coverProducts.addAll(gson.fromJson(it, type))
        }
        getJsonData(requireContext(), "NewProducts.json")?.let {
            newProducts.addAll(gson.fromJson(it, type))
        }
    }

    private fun getJsonData(context: Context, fileName: String): String? = try {
        context.assets.open(fileName).bufferedReader().use { it.readText() }
    } catch (e: IOException) {
        e.printStackTrace(); null
    }
}
