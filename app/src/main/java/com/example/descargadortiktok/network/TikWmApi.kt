package com.example.descargadortiktok.network

import com.example.descargadortiktok.network.models.TikWmResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface TikWmApi {
    @GET("api/")
    suspend fun getVideoInfo(
        @Query("url") url: String,
        @Query("hd") hd: Int = 1
    ): Response<TikWmResponse>

    // Generic GET for fallback APIs
    @GET
    suspend fun getFallbackInfo(@Url url: String): Response<Map<String, Any>>
}