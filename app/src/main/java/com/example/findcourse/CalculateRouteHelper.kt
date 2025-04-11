package com.example.findcourse

import android.content.Context
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.example.findcourse.api.KakaoApiService
import com.example.findcourse.api.KakaoRouteService
import kotlinx.coroutines.*
import kotlin.math.*

object CalculateRouteHelper {

    private val routeCache: MutableMap<PointPair, RouteSection> = mutableMapOf()

    fun calculateWithCombinations(
        context: Context,
        startKeyword: String,
        placeCount: Int,
        dao: AddressDao,
        kakaoApi: KakaoApiService,
        routeApi: KakaoRouteService,
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

            val startLat = startPlace.y.toDouble()
            val startLng = startPlace.x.toDouble()

            val savedPlaces = withContext(Dispatchers.IO) { dao.getAll() }
            if (savedPlaces.isEmpty()) {
                resultView.text = "📭 저장된 장소가 없습니다."
                return@launch
            }

            var candidates = savedPlaces.map {
                it to calculateDistance(startLat, startLng, it.latitude, it.longitude)
            }.sortedBy { it.second }.map { it.first }

            if (candidates.containsPlace(startLat, startLng)) {
                candidates = candidates.filterNot { it.latitude == startLat && it.longitude == startLng }
            }

            val combinations = candidates.combinations(placeCount)
            val routes = combinations.flatMap { it.permutations() }
            resultView.text = "📦 ${routes.size}개의 경로 계산 중..."

            val results = routes.map { route ->
                async {
                    try {
                        val sectionDistance = mutableListOf<Double>()
                        val sectionTime = mutableListOf<Int>()
                        var prevLat = startLat
                        var prevLng = startLng

                        for (place in route) {
                            val key = PointPair(prevLat, prevLng, place.latitude, place.longitude)

                            val section = routeCache.getOrPut(key) {
                                val response = withContext(Dispatchers.IO) {
                                    routeApi.getRoute(
                                        authHeader,
                                        "$prevLng,$prevLat",
                                        "${place.longitude},${place.latitude}"
                                    ).execute()
                                }

                                val rawSummary = response.body()?.routes?.firstOrNull()?.summary
                                    ?: throw Exception("Invalid response")

                                // ✅ Summary → RouteSection 매핑
                                RouteSection(
                                    distance = rawSummary.distance,
                                    duration = rawSummary.duration
                                )
                            }

                            sectionDistance.add(section.distance.toDouble())
                            sectionTime.add(section.duration / 1000 / 60)
                            prevLat = place.latitude
                            prevLng = place.longitude
                        }

                        RouteResult(route, sectionDistance.sum(), sectionTime.sum(), sectionDistance, sectionTime)
                    } catch (e: Exception) {
                        Log.e("🚨 KakaoRoute", "fallback: ${e.message}")
                        val sectionDistance = mutableListOf<Double>()
                        var total = 0.0
                        var prevLat = startLat
                        var prevLng = startLng
                        for (place in route) {
                            val dist = calculateDistance(prevLat, prevLng, place.latitude, place.longitude)
                            total += dist
                            sectionDistance.add(dist)
                            prevLat = place.latitude
                            prevLng = place.longitude
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "🚨 API 요청 제한! 직선 거리로 계산합니다.", Toast.LENGTH_SHORT).show()
                        }
                        RouteResult(route, total, -1, sectionDistance, List(sectionDistance.size) { -1 })
                    }
                }
            }.awaitAll().filterNotNull()

            val best = results.minByOrNull { it.totalDistance }
            if (best == null) {
                resultView.text = "❌ 경로 계산 실패"
                return@launch
            }

            val display = StringBuilder("🚗 최적 경로\n총 거리: ${best.totalDistance.toInt()}m / 시간: ${best.totalTime}분\n")
            best.places.forEachIndexed { i, place ->
                display.append("${i + 1}. ${place.placeName}\n")
                if (i < best.sectionDistance.size) {
                    val dist = best.sectionDistance[i].toInt()
                    val time = best.sectionTime[i]
                    display.append("   ↳ 거리: ${dist}m, 시간: ${if (time >= 0) "$time 분" else "N/A"}\n")
                }
            }
            resultView.text = display.toString()
        }
    }

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val R = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }

    private fun List<AddressEntity>.containsPlace(lat: Double, lng: Double): Boolean {
        return any { it.latitude == lat && it.longitude == lng }
    }

    private fun <T> List<T>.permutations(): List<List<T>> {
        if (size <= 1) return listOf(this)
        val result = mutableListOf<List<T>>()
        for (i in indices) {
            val rest = toMutableList().also { it.removeAt(i) }
            for (perm in rest.permutations()) {
                result.add(listOf(this[i]) + perm)
            }
        }
        return result
    }

    private fun <T> List<T>.combinations(k: Int): List<List<T>> {
        if (k == 0) return listOf(emptyList())
        if (isEmpty()) return emptyList()
        val head = first()
        val tail = drop(1)
        val withHead = tail.combinations(k - 1).map { listOf(head) + it }
        val withoutHead = tail.combinations(k)
        return withHead + withoutHead
    }
}


data class RouteResult(
    val places: List<AddressEntity>,
    val totalDistance: Double,
    val totalTime: Int,
    val sectionDistance: List<Double>,
    val sectionTime: List<Int>
)


// 카카오 길찾기 응답 단축 구조용
data class RouteSection(val distance: Int, val duration: Int)
