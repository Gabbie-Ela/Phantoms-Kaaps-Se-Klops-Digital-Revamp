package com.example.phantoms.presentation.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.example.phantoms.presentation.adapter.ProductAdapter
import com.example.phantoms.presentation.adapter.SaleProductAdapter
import com.example.phantoms.data.model.Product
import com.example.phantoms.R
import com.example.phantoms.presentation.activity.VisualSearchActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.IOException
import com.example.phantoms.data.model.SocialPost
import com.example.phantoms.presentation.adapter.SocialAdapter

class HomeFragment : Fragment() {

    // Views
    lateinit var coverRecView: RecyclerView
    lateinit var newRecView: RecyclerView
    lateinit var saleRecView: RecyclerView
    lateinit var animationView: LottieAnimationView
    lateinit var newLayout: LinearLayout
    lateinit var saleLayout: LinearLayout

    // Data (products)
    lateinit var newProduct: ArrayList<Product>
    lateinit var saleProduct: ArrayList<Product>

    // Adapters (products)
    lateinit var newProductAdapter: ProductAdapter
    lateinit var saleProductAdapter: SaleProductAdapter

    // Data + adapter (social cover carousel)
    lateinit var socialPosts: ArrayList<SocialPost>
    lateinit var socialAdapter: SocialAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        // init lists
        socialPosts = arrayListOf()
        newProduct = arrayListOf()
        saleProduct = arrayListOf()

        // bind views
        coverRecView = view.findViewById(R.id.coverRecView)
        newRecView = view.findViewById(R.id.newRecView)
        saleRecView = view.findViewById(R.id.saleRecView)
        newLayout = view.findViewById(R.id.newLayout)
        saleLayout = view.findViewById(R.id.saleLayout)
        animationView = view.findViewById(R.id.animationView)
        val visualSearchBtn_homePage: ImageView = view.findViewById(R.id.visualSearchBtn_homePage)

        hideLayout()

        // Load data
        setCoverSocialData()   // <-- now uses SocialPosts.json for the top carousel
        setNewProductData()
        setSaleProductData()   // <-- fills the Sale row from CoverProducts.json (as before)

        // Cover carousel -> SocialAdapter
        coverRecView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        coverRecView.setHasFixedSize(true)
        socialAdapter = SocialAdapter(requireContext(), socialPosts)
        coverRecView.adapter = socialAdapter

        // New products row
        newRecView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        newRecView.setHasFixedSize(true)
        newProductAdapter = ProductAdapter(newProduct, requireContext())
        newRecView.adapter = newProductAdapter

        // Sale products row
        saleRecView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        saleRecView.setHasFixedSize(true)
        saleProductAdapter = SaleProductAdapter(saleProduct, requireContext())
        saleRecView.adapter = saleProductAdapter

        visualSearchBtn_homePage.setOnClickListener {
            startActivity(Intent(context, VisualSearchActivity::class.java))
        }

        showLayout()
        return view
    }

    private fun hideLayout() {
        animationView.playAnimation()
        animationView.loop(true)
        coverRecView.visibility = View.GONE
        newLayout.visibility = View.GONE
        saleLayout.visibility = View.GONE
        animationView.visibility = View.VISIBLE
    }

    private fun showLayout() {
        animationView.pauseAnimation()
        animationView.visibility = View.GONE
        coverRecView.visibility = View.VISIBLE
        newLayout.visibility = View.VISIBLE
        saleLayout.visibility = View.VISIBLE
    }

    private fun getJsonData(context: Context, fileName: String): String? {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (ioException: IOException) {
            ioException.printStackTrace()
            null
        }
    }

    // --- NEW: cover now uses SocialPosts.json ---
    private fun setCoverSocialData() {
        val jsonFileString = context?.let { getJsonData(it, "SocialPosts.json") } ?: return
        val gson = Gson()
        val listType = object : TypeToken<List<SocialPost>>() {}.type
        val items: List<SocialPost> = gson.fromJson(jsonFileString, listType)

        socialPosts.clear()
        socialPosts.addAll(items) // keep file order
    }


    // kept the same as your original logic for "New"
    private fun setNewProductData() {
        val jsonFileString = context?.let { getJsonData(it, "NewProducts.json") } ?: return
        val gson = Gson()
        val listType = object : TypeToken<List<Product>>() {}.type
        val data: List<Product> = gson.fromJson(jsonFileString, listType)
        newProduct.clear()
        newProduct.addAll(data)
    }

    // NEW helper so "Sale" still loads from CoverProducts.json like before
    private fun setSaleProductData() {
        val jsonFileString = context?.let { getJsonData(it, "CoverProducts.json") } ?: return
        val gson = Gson()
        val listType = object : TypeToken<List<Product>>() {}.type
        val data: List<Product> = gson.fromJson(jsonFileString, listType)
        saleProduct.clear()
        saleProduct.addAll(data)
    }
}
