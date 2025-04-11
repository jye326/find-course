package com.example.findcourse.api

data class KakaoKeywordResponse(
    val documents: List<KakaoPlace>
)

data class KakaoPlace(
    val place_name: String,
    val address_name: String,
    val road_address_name: String?,
    val x: String,  // 경도
    val y: String   // 위도
)
