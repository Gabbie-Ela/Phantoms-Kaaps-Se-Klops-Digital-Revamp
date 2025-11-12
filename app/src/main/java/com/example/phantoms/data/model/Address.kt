// com/example/phantoms/data/model/Address.kt
package com.example.phantoms.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Address(
    var id: String = "",
    var userId: String = "",
    var fullName: String = "",
    var phone: String = "",
    var line1: String = "",
    var line2: String = "",
    var city: String = "",
    var region: String = "",          // <-- you used "region" earlier; keep this exact name
    var postalCode: String = "",
    var country: String = "South Africa",
    var isDefault: Boolean = false    // <-- must exist so Firestore can map it
) : Parcelable
