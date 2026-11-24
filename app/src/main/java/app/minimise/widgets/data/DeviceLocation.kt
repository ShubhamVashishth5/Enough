package app.minimise.widgets.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import android.os.SystemClock
import app.minimise.core.Defaults
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * The phone's rough location, for the weather line, from Android's own LocationManager (no Play
 * Services). Only coarse location is asked for: a couple of kilometres is plenty for the weather.
 */
class DeviceLocation @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val locationManager: LocationManager? = context.getSystemService(LocationManager::class.java)

    fun hasPermission(): Boolean =
        context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** A recent last known location if there is one, otherwise a new reading. Null when neither comes. */
    @SuppressLint("MissingPermission")
    suspend fun current(): Location? {
        val manager = locationManager ?: return null
        if (!hasPermission()) return null
        val last = runCatching {
            manager.getProviders(true).mapNotNull { manager.getLastKnownLocation(it) }.maxByOrNull { it.elapsedRealtimeNanos }
        }.onFailure { Timber.w(it, "No last known location") }.getOrNull()
        if (last != null && age(last) < Defaults.LOCATION_FRESH_FOR) return last
        val provider = provider(manager) ?: return last
        return withTimeoutOrNull(Defaults.LOCATION_TIMEOUT) { fresh(manager, provider) } ?: last
    }

    private fun age(location: Location) = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos).nanoseconds

    /** With coarse location only, GPS is usable from Android 12, which blurs it like the network fix. */
    private fun provider(manager: LocationManager): String? {
        val enabled = manager.getProviders(true)
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && LocationManager.FUSED_PROVIDER in enabled -> LocationManager.FUSED_PROVIDER
            LocationManager.NETWORK_PROVIDER in enabled -> LocationManager.NETWORK_PROVIDER
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && LocationManager.GPS_PROVIDER in enabled -> LocationManager.GPS_PROVIDER
            else -> null
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun fresh(manager: LocationManager, provider: String): Location? = suspendCancellableCoroutine { cont ->
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                cont.invokeOnCancellation { signal.cancel() }
                manager.getCurrentLocation(provider, signal, context.mainExecutor) { cont.resume(it) }
            } else {
                // Android 10's LocationListener has no default methods, so every one is written out.
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        if (cont.isActive) cont.resume(location)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

                    override fun onProviderEnabled(provider: String) = Unit

                    override fun onProviderDisabled(provider: String) {
                        if (cont.isActive) cont.resume(null)
                    }
                }
                cont.invokeOnCancellation { manager.removeUpdates(listener) }
                @Suppress("DEPRECATION")
                manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
            }
        } catch (e: RuntimeException) {
            Timber.w(e, "No location from %s", provider)
            if (cont.isActive) cont.resume(null)
        }
    }
}
