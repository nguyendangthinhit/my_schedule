package com.example.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class OpenMeteoDaily(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "weathercode") val weathercode: List<Int> = emptyList(),
    @Json(name = "temperature_2m_max") val tempMax: List<Double> = emptyList(),
    @Json(name = "temperature_2m_min") val tempMin: List<Double> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OpenMeteoHourly(
    @Json(name = "time") val time: List<String> = emptyList(),
    @Json(name = "temperature_2m") val temperature: List<Double> = emptyList(),
    @Json(name = "weathercode") val weathercode: List<Int> = emptyList()
)

@JsonClass(generateAdapter = true)
data class OpenMeteoResponse(
    @Json(name = "daily") val daily: OpenMeteoDaily? = null,
    @Json(name = "hourly") val hourly: OpenMeteoHourly? = null
)

interface WeatherApiService {
    @GET("v1/forecast")
    suspend fun get7DayForecast(
        @Query("latitude") latitude: Double = 21.0285, // Hà Nội / Việt Nam default
        @Query("longitude") longitude: Double = 105.8542,
        @Query("daily") daily: String = "weathercode,temperature_2m_max,temperature_2m_min",
        @Query("hourly") hourly: String = "temperature_2m,weathercode",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): OpenMeteoResponse

    companion object {
        private const val BASE_URL = "https://api.open-meteo.com/"

        fun create(): WeatherApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(WeatherApiService::class.java)
        }
    }
}
