package com.example.findcourse

import android.content.Context
import android.widget.TextView
import com.example.findcourse.api.KakaoApiService
import kotlinx.coroutines.*
import kotlin.math.*

object CalculateRouteHelper {

    fun calculateByTSP(
        context: Context,
        startKeyword: String,
        placeCount: Int,
        dao: AddressDao,
        kakaoApi: KakaoApiService,
        resultView: TextView
    ) {
        val scope = CoroutineScope(Dispatchers.Main)

        scope.launch {
            resultView.text = "🚀 출발지 좌표를 가져오는 중..."

            val authHeader = "KakaoAK ${BuildConfig.KAKAO_API_KEY}"
            val startResponse = withContext(Dispatchers.IO) {
                kakaoApi.searchKeyword(authHeader, startKeyword).execute()
            }

            val startPlace = startResponse.body()?.documents?.firstOrNull()
            if (startPlace == null) {
                resultView.text = "❌ 출발지를 찾을 수 없습니다."
                return@launch
            }

            val start = AddressEntity(
                address = startPlace.address_name ?: startPlace.road_address_name ?: startPlace.place_name,
                placeName = startPlace.place_name,
                latitude = startPlace.y.toDouble(),
                longitude = startPlace.x.toDouble()
            )

            val savedPlaces = withContext(Dispatchers.IO) { dao.getAll() }
            if (savedPlaces.isEmpty()) {
                resultView.text = "📭 저장된 장소가 없습니다."
                return@launch
            }

            val candidates = savedPlaces
                .filterNot { it.latitude == start.latitude && it.longitude == start.longitude }
                .sortedBy { calculateDistance(start.latitude, start.longitude, it.latitude, it.longitude) }
                .take(placeCount) // ✅ 개수 제한


            val route = solveTSP(start, candidates)

            val display = StringBuilder("🚗 최적 경로 (직선 거리 기준)\n")
            var totalDistance = 0.0
            var prev: AddressEntity = start

            resultView.textSize = 20f  // ✅ 여기 추가

            for (i in route.indices) {
                val place = route[i]
                val dist = calculateDistance(prev.latitude, prev.longitude, place.latitude, place.longitude)
                totalDistance += dist
                display.append("${i + 1}. ${place.placeName} (거리: ${dist.toInt()}m)\n")
                prev = place
            }

            display.insert(0, "총 거리: ${totalDistance.toInt()}m\n")
            resultView.text = display.toString()
        }
    }

    private fun solveTSP(start: AddressEntity, places: List<AddressEntity>): List<AddressEntity> {
        val unvisited = places.toMutableList()
        val route = mutableListOf<AddressEntity>()
        var current = start

        while (unvisited.isNotEmpty()) {
            val next = unvisited.minByOrNull {
                calculateDistance(current.latitude, current.longitude, it.latitude, it.longitude)
            }!!
            route.add(next)
            unvisited.remove(next)
            current = next
        }
        return route
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
