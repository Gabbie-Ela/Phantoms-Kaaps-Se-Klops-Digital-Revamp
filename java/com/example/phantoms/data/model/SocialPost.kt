package com.example.phantoms.data.model

data class SocialPost(
    val title: String,
    val imageUrl: String,      // http(s)://... OR "res://your_drawable_name"
    val postUrl: String,       // Instagram post/profile URL to open
    val createdAt: Long = 0L   // epoch millis; default 0 if your JSON omits it
)
