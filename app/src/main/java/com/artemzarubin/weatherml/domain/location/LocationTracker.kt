package com.artemzarubin.weatherml.domain.location

import android.location.Location

interface LocationTracker {
    /**
     * Retrieves the current device location.
     * This function should handle cases where location services are off or permission is denied,
     * though permission checks should ideally happen before calling this.
     *
     * @return The current [Location] object if successful, or null if location cannot be obtained.
     *         Consider returning a Resource<Location> for better error handling.
     */
    suspend fun getCurrentLocation(): Location? // Or Resource<Location>
}