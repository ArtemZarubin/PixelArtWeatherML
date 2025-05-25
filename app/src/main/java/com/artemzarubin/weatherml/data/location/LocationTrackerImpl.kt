package com.artemzarubin.weatherml.data.location

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.artemzarubin.weatherml.domain.location.LocationTracker
import com.artemzarubin.weatherml.util.Resource
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationTrackerImpl @Inject constructor(
    private val locationClient: FusedLocationProviderClient,
    @ApplicationContext private val application: Application
) : LocationTracker {

    override fun getCurrentLocation(): Flow<Resource<Location?>> {
        return callbackFlow {
            Log.d("LocationTrackerImpl", "getCurrentLocation Flow started")

            if (ContextCompat.checkSelfPermission(
                    application,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    application,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("LocationTrackerImpl", "Location permission not granted.")
                trySend(Resource.Error("Location permission not granted."))
                channel.close()
                return@callbackFlow
            }

            val locationManager =
                application.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled =
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                Log.w("LocationTrackerImpl", "GPS and Network providers are disabled.")
                trySend(Resource.Error("GPS is disabled. Please enable location services."))
                channel.close()
                return@callbackFlow
            }

            trySend(Resource.Loading(message = "Fetching current location..."))
            Log.d(
                "LocationTrackerImpl",
                "Requesting current location update using FusedLocationProviderClient.getCurrentLocation()."
            )

            val cancellationTokenSource = CancellationTokenSource()
            // Создаем запрос на текущее местоположение
            val currentLocationRequest = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY) // Вы можете выбрать другой приоритет
                // .setDurationMillis(10000) // Опционально: максимальное время ожидания
                .build()

            locationClient.getCurrentLocation(currentLocationRequest, cancellationTokenSource.token)
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        Log.i("LocationTrackerImpl", "Successfully got CURRENT location: $location")
                        trySend(Resource.Success(location))
                    } else {
                        // Эта ситуация маловероятна при успешном вызове, но возможна,
                        // если геолокация отключается в момент запроса.
                        Log.w(
                            "LocationTrackerImpl",
                            "FusedLocationProviderClient.getCurrentLocation() returned null despite success listener."
                        )
                        trySend(Resource.Error("Failed to get current location (null result). Try enabling high accuracy GPS."))
                    }
                    channel.close()
                }
                .addOnFailureListener { exception ->
                    Log.e(
                        "LocationTrackerImpl",
                        "Failed to get current location using FusedLocationProviderClient.getCurrentLocation()",
                        exception
                    )
                    trySend(Resource.Error("Failed to get current location: ${exception.message}"))
                    channel.close()
                }
                .addOnCanceledListener { // Этот слушатель обычно не вызывается, если вы сами не отменяете CancellationToken
                    Log.w(
                        "LocationTrackerImpl",
                        "Current location request was cancelled by FusedLocationProviderClient."
                    )
                    trySend(Resource.Error("Location request cancelled."))
                    channel.close()
                }

            awaitClose {
                Log.d("LocationTrackerImpl", "getCurrentLocation Flow closing. Cancelling token.")
                cancellationTokenSource.cancel() // Важно отменить токен при закрытии Flow
            }
        }
    }
}