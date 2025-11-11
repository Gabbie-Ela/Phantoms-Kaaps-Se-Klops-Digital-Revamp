package com.example.phantoms.presentation.fragment

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.phantoms.R
import com.example.phantoms.data.local.room.artist.UpcomingRow
import com.example.phantoms.presentation.adapter.UpcomingAdapter
import com.example.phantoms.presentation.viewmodel.ArtistViewModel
import com.google.firebase.auth.FirebaseAuth

class PhantomsFeedFragment : Fragment() {

    private val vm: ArtistViewModel by viewModels()

    private lateinit var rvEvents: RecyclerView
    private lateinit var webView: WebView
    private lateinit var progress: ProgressBar
    private lateinit var upcomingAdapter: UpcomingAdapter

    private val tiktokProfileUrl = "https://www.tiktok.com/@phantomscommunitydevelop"

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_phantoms_feed, container, false)
        rvEvents = v.findViewById(R.id.rvEvents)
        webView = v.findViewById(R.id.tiktokWebView)
        progress = v.findViewById(R.id.tiktokProgress)



        setupEventsList()
        setupTikTokWebView()
        bindData()
        return v
    }

    private fun setupEventsList() {
        rvEvents.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        upcomingAdapter = UpcomingAdapter(
            onGoing = { row -> onRsvpGoing(row) },
            onInterested = { row -> onRsvpInterested(row) }
        )
        rvEvents.adapter = upcomingAdapter
    }

    private fun onRsvpGoing(row: UpcomingRow) {
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid -> vm.setGoing(uid, row) }
    }

    private fun onRsvpInterested(row: UpcomingRow) {
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid -> vm.setInterested(uid, row) }
    }

    private fun bindData() {
        vm.seedIfNeeded()
        vm.upcomingForPhantoms().observe(viewLifecycleOwner) { rows ->
            upcomingAdapter.submitList(rows)
        }
    }

    // --- TikTok embed ---
    @SuppressLint("SetJavaScriptEnabled")
    private fun setupTikTokWebView() {
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            loadWithOverviewMode = true
            useWideViewPort = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            userAgentString = WebSettings.getDefaultUserAgent(requireContext())
                .replace("wv", "") + " TikTok/28.0.0 Mobile Safari"
            setSupportMultipleWindows(true)
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webChromeClient = object : WebChromeClient() {
            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message?
            ): Boolean {
                val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                transport.webView = webView // keep links in same WebView
                resultMsg.sendToTarget()
                return true
            }
        }

        webView.webViewClient = object : WebViewClient() {
            // API 24+
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url ?: return false
                val scheme = url.scheme?.lowercase() ?: return false

                // Allow normal web links inside WebView
                if (scheme == "http" || scheme == "https") return false

                // Handle intent:// deep links
                if (scheme == "intent") {
                    try {
                        val intent = Intent.parseUri(url.toString(), Intent.URI_INTENT_SCHEME)
                        intent.`package`?.let {
                            try { startActivity(intent); return true } catch (_: Exception) {}
                        }
                        intent.getStringExtra("browser_fallback_url")?.let { fb ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fb))); return true
                        }
                        intent.`package`?.let { pkg ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))); return true
                        }
                    } catch (_: Exception) {}
                    return true
                }

                // Handle TikTok custom schemes externally
                if (scheme.startsWith("snssdk") || scheme == "tiktok") {
                    try { startActivity(Intent(Intent.ACTION_VIEW, url)) } catch (_: Exception) {}
                    return true
                }

                // Block any other unknown schemes
                return true
            }

            // Pre-API 24
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                val u = url ?: return false
                val scheme = Uri.parse(u).scheme?.lowercase() ?: return false
                if (scheme == "http" || scheme == "https") return false
                if (scheme == "intent") {
                    try {
                        val intent = Intent.parseUri(u, Intent.URI_INTENT_SCHEME)
                        intent.`package`?.let {
                            try { startActivity(intent); return true } catch (_: Exception) {}
                        }
                        intent.getStringExtra("browser_fallback_url")?.let { fb ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fb))); return true
                        }
                        intent.`package`?.let { pkg ->
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg"))); return true
                        }
                    } catch (_: Exception) {}
                    return true
                }
                if (scheme.startsWith("snssdk") || scheme == "tiktok") {
                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u))) } catch (_: Exception) {}
                    return true
                }
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progress.visibility = View.GONE
            }
        }

        progress.visibility = View.VISIBLE
        webView.loadUrl(tiktokProfileUrl)
    }

    private fun openExternal(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: ActivityNotFoundException) { /* ignore */ }
    }

    // --- Open in TikTok app (best-effort with graceful fallbacks) ---
    private fun isAppInstalled(pkg: String): Boolean =
        try { requireContext().packageManager.getPackageInfo(pkg, 0); true } catch (_: Exception) { false }

    private fun openInTikTokApp(fallbackUrl: String = tiktokProfileUrl) {
        val tiktokPkg = "com.zhiliaoapp.musically"
        val username = "phantomscommunitydevelop"

        if (isAppInstalled(tiktokPkg)) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                    setPackage(tiktokPkg); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent); return
            } catch (_: Exception) {}
        }

        val deepLinks = listOf(
            "tiktok://user/@$username",
            "snssdk1128://user/profile/$username",
            "snssdk1128://user/profile"
        )
        for (dl in deepLinks) {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(dl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); return }
            catch (_: Exception) {}
        }

        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) {}
    }

    override fun onResume() { super.onResume(); webView.onResume() }
    override fun onPause() { webView.onPause(); super.onPause() }
    override fun onDestroyView() {
        (webView.parent as? ViewGroup)?.removeView(webView)
        webView.removeAllViews()
        webView.destroy()
        super.onDestroyView()
    }
}
