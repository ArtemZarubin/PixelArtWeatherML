package com.artemzarubin.weatherml.util

/**
 * A generic class that holds a value with its loading status.
 * @param <T>
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    /**
     * Represents a successful state with data.
     */
    class Success<T>(data: T) : Resource<T>(data)

    /**
     * Represents an error state with an error message.
     * Data can optionally be provided if some partial data was loaded before the error.
     */
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)

    /**
     * Represents a loading state.
     * Data can optionally be provided if showing old data while new data is loading.
     */
    class Loading<T>(data: T? = null) : Resource<T>(data)
}