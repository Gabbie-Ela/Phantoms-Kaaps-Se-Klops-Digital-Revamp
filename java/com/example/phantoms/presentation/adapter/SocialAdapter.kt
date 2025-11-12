package com.example.phantoms.presentation.adapter

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.RecyclerView
import com.example.phantoms.R
import com.example.phantoms.data.model.SocialPost
import com.example.phantoms.presentation.loadSmart

private const val TAG = "SocialAdapter"

class SocialAdapter(
    private val ctx: Context,
    private val posts: ArrayList<SocialPost>
) : RecyclerView.Adapter<SocialAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.social_cover_single, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val post = posts[position]

        holder.socialTitle.text = post.title
        holder.socialImage.loadSmart(ctx, post.imageUrl)

        val clicker = View.OnClickListener {
            if (post.postUrl.isNullOrBlank()) {
                Toast.makeText(ctx, "No link for this post", Toast.LENGTH_SHORT).show()
                Log.w(TAG, "postUrl is null/blank for item at $position → ${post.title}")
                return@OnClickListener
            }
            openUrl(post.postUrl!!)
        }
        holder.socialOpenBtn.setOnClickListener(clicker)
        holder.itemView.setOnClickListener(clicker)
    }

    override fun getItemCount(): Int = posts.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val socialImage: ImageView = itemView.findViewById(R.id.socialImage)
        val socialTitle: TextView  = itemView.findViewById(R.id.socialTitle)
        val socialOpenBtn: Button  = itemView.findViewById(R.id.socialOpenBtn)
    }

    private fun openUrl(raw: String) {
        // If you’re using custom schemes (e.g., instagram://…), don’t force https
        val uri = if (raw.startsWith("http://") || raw.startsWith("https://") || raw.contains("://"))
            Uri.parse(raw)
        else
            Uri.parse("https://$raw")

        try {
            val cti = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()

            // Prefer Chrome if available
            getCustomTabsPackage()?.let { pkg -> cti.intent.`package` = pkg }

            cti.launchUrl(ctx, uri)
        } catch (e: ActivityNotFoundException) {
            // Fallback to default browser
            Log.w(TAG, "CustomTabs not available, falling back to ACTION_VIEW", e)
            val i = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                ctx.startActivity(i)
            } catch (e2: Exception) {
                Toast.makeText(ctx, "No browser found to open link", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Unable to open uri=$uri", e2)
            }
        }
    }

    private fun getCustomTabsPackage(): String? {
        val pm = ctx.packageManager
        val activityIntent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com"))
        val resolved = pm.queryIntentActivities(activityIntent, 0)
        for (ri in resolved) {
            val serviceIntent = Intent("android.support.customtabs.action.CustomTabsService")
            serviceIntent.`package` = ri.activityInfo.packageName
            if (pm.resolveService(serviceIntent, 0) != null) {
                if (ri.activityInfo.packageName == "com.android.chrome") return "com.android.chrome"
                return ri.activityInfo.packageName
            }
        }
        return null
    }
}
