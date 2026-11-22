package com.twobrain.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class Place(val short: String, val full: String, val lat: Double, val lng: Double)

class LocationRepository(private val context: Context) {
    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission") // checked by caller
    suspend fun current(): Place? = runCatching {
        val loc = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token).await()
            ?: client.lastLocation.await()
            ?: return null
        val address = reverse(loc.latitude, loc.longitude)
        val short = address?.let { it.subLocality ?: it.locality ?: it.subAdminArea ?: it.adminArea } ?: "Unknown place"
        val full = address?.let { a ->
            listOfNotNull(a.thoroughfare?.let { t -> listOfNotNull(a.subThoroughfare, t).joinToString(" ") }, a.locality ?: a.subAdminArea, a.adminArea)
                .distinct().joinToString(", ")
        }?.takeIf { it.isNotBlank() } ?: short
        Place(short, full, loc.latitude, loc.longitude)
    }.getOrNull()

    private suspend fun reverse(lat: Double, lng: Double): Address? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.getDefault())
        return if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine { cont ->
                geocoder.getFromLocation(lat, lng, 1, object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) { cont.resume(addresses.firstOrNull()) }
                    override fun onError(errorMessage: String?) { cont.resume(null) }
                })
            }
        } else withContext(Dispatchers.IO) {
            @Suppress("DEPRECATION")
            runCatching { geocoder.getFromLocation(lat, lng, 1)?.firstOrNull() }.getOrNull()
        }
    }
}
