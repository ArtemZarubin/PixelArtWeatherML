package com.artemzarubin.weatherml.data.location

import android.Manifest
import android.app.Application // Needs Application context
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager // To check if GPS is enabled
import android.util.Log
import androidx.core.content.ContextCompat
import com.artemzarubin.weatherml.domain.location.LocationTracker
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

// import android.util.Log // For debugging

class LocationTrackerImpl @Inject constructor(
    private val application: Application, // Hilt can provide Application context
    private val fusedLocationClient: FusedLocationProviderClient // Hilt will provide this
) : LocationTracker {

    override suspend fun getCurrentLocation(): Location? {
        // Check if location permissions are granted
        val hasAccessFineLocationPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasAccessCoarseLocationPermission = ContextCompat.checkSelfPermission(
            application,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        // Check if GPS is enabled
        val locationManager =
            application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!hasAccessFineLocationPermission && !hasAccessCoarseLocationPermission) {
            Log.d("LocationTracker", "Location permission not granted.")
            return null // Permissions not granted
        }

        if (!isGpsEnabled) {
            Log.d("LocationTracker", "GPS or Network location is not enabled.")
            // TODO: Optionally, prompt user to enable location services
            return null // GPS not enabled
        }

        // Permissions are granted and GPS is enabled, try to get location
        // Using suspendCancellableCoroutine to bridge GMS Task API with coroutines
        return suspendCancellableCoroutine { continuation ->
            val cancellationTokenSource = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY, // Or PRIORITY_BALANCED_POWER_ACCURACY for coarse
                cancellationTokenSource.token
            ).addOnSuccessListener { location: Location? ->
                // Got last known location. In some rare situations this can be null.
                // Log.d("LocationTracker", "Location success: $location")
                continuation.resume(location)
            }.addOnFailureListener { exception ->
                Log.e("LocationTracker", "Failed to get location: ${exception.message}")
                continuation.resume(null) // Resume with null on failure
            }.addOnCanceledListener {
                Log.d("LocationTracker", "Location request canceled.")
                continuation.cancel() // Cancel the coroutine if the task is canceled
            }

            // When the coroutine is cancelled, cancel the GMS Task
            continuation.invokeOnCancellation {
                cancellationTokenSource.cancel()
            }
        }
    }
}