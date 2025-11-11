package com.example.phantoms.presentation

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.example.phantoms.R

fun ImageView.loadSmart(
    ctx: Context,
    src: String?,
    placeholderResId: Int = R.drawable.bn // your default/fallback image
) {
    if (src.isNullOrBlank()) {
        Glide.with(ctx)
            .load(placeholderResId)
            .into(this)
        return
    }

    if (src.startsWith("res://")) {
        val name = src.removePrefix("res://")
        val resId = ctx.resources.getIdentifier(name, "drawable", ctx.packageName)
        if (resId != 0) {
            Glide.with(ctx).load(resId).placeholder(placeholderResId).into(this)
        } else {
            // name not found → fallback
            Glide.with(ctx).load(placeholderResId).into(this)
        }
    } else {
        Glide.with(ctx).load(src).placeholder(placeholderResId).into(this)
    }
}
