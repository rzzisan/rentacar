package com.rzzisan.carrental.util

import android.content.Context
import android.location.Geocoder
import java.util.Locale

fun reverseGeocode(context: Context, lat: Double, lng: Double): String {
    return try {
        val geocoder = Geocoder(context, Locale.getDefault())
        @Suppress("DEPRECATION")
        val addresses = geocoder.getFromLocation(lat, lng, 1)
        if (!addresses.isNullOrEmpty()) {
            val addr = addresses[0]
            listOfNotNull(addr.subLocality, addr.locality, addr.adminArea)
                .take(2).joinToString(", ")
                .ifBlank { "$lat,$lng" }
        } else "$lat,$lng"
    } catch (_: Exception) {
        "$lat,$lng"
    }
}
