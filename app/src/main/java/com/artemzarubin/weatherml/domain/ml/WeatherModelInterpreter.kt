package com.artemzarubin.weatherml.domain.ml

import com.artemzarubin.weatherml.domain.model.CurrentWeather

// Input for the model will be a preprocessed float array
// The order of features in this array MUST match feature_names.txt
data class ModelInput(
    val features: FloatArray // Array of features (now 15 for "Feels Like" model)
) {
    // Optional: Add equals and hashCode for data class best practices if needed elsewhere
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as ModelInput
        return features.contentEquals(other.features)
    }

    override fun hashCode(): Int {
        return features.contentHashCode()
    }
}

data class ModelOutput(
    val predictedFeelsLikeTemp: Float // Output: predicted "Feels Like" Temperature
)

interface WeatherModelInterpreter {
    fun initialize(): Boolean
    fun getPrediction(input: ModelInput): ModelOutput?
    fun prepareAndScaleFeatures(currentWeather: CurrentWeather): FloatArray?
    fun close()
}