package com.artemzarubin.weatherml

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp // This annotation triggers Hilt's code generation
class WeatherMLApplication : Application() {
    // Currently, no additional code is needed here for basic Hilt setup.
    // This class can be used later for app-wide initializations if necessary.
}