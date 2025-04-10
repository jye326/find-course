package com.example.findcourse

data class KakaoKeywordResponse(
    val documents: List<KakaoPlace>
)

data class KakaoPlace(
    val place_name: String,
    val address_name: String,
    val road_address_name: String?
)
