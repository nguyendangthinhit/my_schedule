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
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class HolidayDto(
    @Json(name = "date") val date: String, // ISO "yyyy-MM-dd"
    @Json(name = "localName") val localName: String? = null,
    @Json(name = "name") val name: String,
    @Json(name = "countryCode") val countryCode: String? = "VN",
    @Json(name = "fixed") val fixed: Boolean? = null,
    @Json(name = "global") val global: Boolean? = null,
    @Json(name = "types") val types: List<String>? = null
)

interface CalendarApiService {
    @GET("api/v3/PublicHolidays/{year}/{countryCode}")
    suspend fun getPublicHolidays(
        @Path("year") year: Int,
        @Path("countryCode") countryCode: String = "VN"
    ): List<HolidayDto>

    @GET("api/v3/NextPublicHolidays/{countryCode}")
    suspend fun getNextPublicHolidays(
        @Path("countryCode") countryCode: String = "VN"
    ): List<HolidayDto>

    companion object {
        private const val BASE_URL = "https://date.nager.at/"

        fun create(): CalendarApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()

            val moshi = Moshi.Builder()
                .add(KotlinJsonAdapterFactory())
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(CalendarApiService::class.java)
        }
    }
}
