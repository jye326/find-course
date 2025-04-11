package com.example.findcourse.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoRouteService {
    @GET("v1/directions")
    fun getRoute(
        @Header("Authorization") auth: String,
        @Query("origin") origin: String,
        @Query("destination") destination: String
    ): Call<KakaoRouteResponse>
}
