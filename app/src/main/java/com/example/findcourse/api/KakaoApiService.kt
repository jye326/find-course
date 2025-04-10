package com.example.findcourse.api

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface KakaoApiService {
    @GET("v2/local/search/keyword.json")
    fun searchKeyword(
        @Header("Authorization") authHeader: String,
        @Query("query") query: String
    ): Call<KakaoKeywordResponse>
}
