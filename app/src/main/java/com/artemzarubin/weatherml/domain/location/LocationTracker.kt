package com.artemzarubin.weatherml.domain.location

import android.location.Location
import com.artemzarubin.weatherml.util.Resource
import kotlinx.coroutines.flow.Flow

interface LocationTracker {
    fun getCurrentLocation(): Flow<Resource<Location?>>
}