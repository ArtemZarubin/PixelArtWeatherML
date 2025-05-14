package com.artemzarubin.weatherml.di

import android.app.Application
import com.artemzarubin.weatherml.data.location.LocationTrackerImpl
import com.artemzarubin.weatherml.data.remote.ApiService
import com.artemzarubin.weatherml.data.repository.WeatherRepositoryImpl
import com.artemzarubin.weatherml.domain.location.LocationTracker
import com.artemzarubin.weatherml.domain.repository.WeatherRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import javax.inject.Singleton

@Module // Marks this as a Hilt Module
@InstallIn(SingletonComponent::class) // Specifies that bindings in this module are available in the ApplicationComponent (singleton scope)
object NetworkModule {

    // Base URL for the weather API (e.g., OpenWeatherMap)
    private const val BASE_URL = "https://api.openweathermap.org/data/2.5/"

    @Provides
    @Singleton // Ensures a single instance of HttpLoggingInterceptor is created and reused
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.level =
            HttpLoggingInterceptor.Level.BODY // Log request and response bodies
        return loggingInterceptor
    }

    @Provides
    @Singleton // Ensures a single instance of OkHttpClient is created and reused
    fun provideOkHttpClient(loggingInterceptor: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor) // Add the logging interceptor for debugging
            // You can add other configurations here, like timeouts, other interceptors, etc.
            // .connectTimeout(30, TimeUnit.SECONDS)
            // .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton // Ensures a single instance of Json (for Kotlinx Serialization) is created
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true // Ignores keys in JSON that are not in our data classes
            isLenient = true         // Allows for some malformed JSON (use with caution)
            coerceInputValues = true
        }
    }

    @Provides
    @Singleton // Ensures a single instance of Retrofit is created and reused
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient) // Use our custom OkHttpClient
            .addConverterFactory(json.asConverterFactory(contentType)) // Use Kotlinx Serialization for JSON parsing
            .build()
    }

    @Provides
    @Singleton // Ensures a single instance of ApiService is created and reused
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideWeatherRepository(apiService: ApiService): WeatherRepository {
        // Hilt will provide ApiService, and we return our implementation
        return WeatherRepositoryImpl(apiService)
    }

    @Provides
    @Singleton
    fun provideFusedLocationProviderClient(application: Application): FusedLocationProviderClient {
        return LocationServices.getFusedLocationProviderClient(application)
    }

    @Provides
    @Singleton
    fun provideLocationTracker(
        application: Application,
        fusedLocationProviderClient: FusedLocationProviderClient
    ): LocationTracker {
        // Hilt will provide Application and FusedLocationProviderClient
        return LocationTrackerImpl(application, fusedLocationProviderClient)
    }
}