package com.example.findcourse.api

data class KakaoRouteResponse(
    val routes: List<Route>
)

data class Route(
    val summary: Summary
)

data class Summary(
    val distance: Int,   // meter
    val duration: Int    // millisecond
)
