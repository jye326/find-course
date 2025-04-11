package com.example.findcourse

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.findcourse.api.KakaoApiService
import com.example.findcourse.api.KakaoKeywordResponse
import com.example.findcourse.api.KakaoPlace
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


class MainActivity : AppCompatActivity() {

    private val addressList = mutableListOf<AddressEntity>()
    private lateinit var adapter: AddressAdapter
    private lateinit var kakaoApi: KakaoApiService

    private val searchResultList = mutableListOf<KakaoPlace>()
    private lateinit var searchAdapter: SearchResultAdapter

    private var searchJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var dao: AddressDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.point_management)

        val db = Room.databaseBuilder(
            applicationContext,
            AddressDatabase::class.java,
            "address-db"
        ).fallbackToDestructiveMigration()
            .build()
        dao = db.addressDao()

        val input = findViewById<EditText>(R.id.addressInput)
        val button = findViewById<Button>(R.id.saveButton)
        val recyclerView = findViewById<RecyclerView>(R.id.addressList)

        adapter = AddressAdapter(addressList) { position ->
            val address = addressList[position]
            coroutineScope.launch {
                dao.delete(address)
            }
            addressList.removeAt(position)
            adapter.notifyItemRemoved(position)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Room DB에서 저장된 주소 불러오기
        coroutineScope.launch {
            val savedAddresses = dao.getAll()
            addressList.addAll(savedAddresses)
            adapter.notifyDataSetChanged()
        }

        val retrofit = Retrofit.Builder()
            .baseUrl("https://dapi.kakao.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        kakaoApi = retrofit.create(KakaoApiService::class.java)

        val searchRecyclerView = findViewById<RecyclerView>(R.id.searchResultList)

        searchAdapter = SearchResultAdapter(searchResultList) { selected ->
            val selectedAddress = selected.road_address_name ?: selected.address_name
            val placeName = selected.place_name ?: "이름 없음"

            val lat = selected.y?.toDoubleOrNull()
            val lng = selected.x?.toDoubleOrNull()

            if (lat != null && lng != null) {
                val entity = AddressEntity(
                    address = selectedAddress,
                    placeName = placeName,
                    latitude = lat,
                    longitude = lng
                )

                coroutineScope.launch {
                    try {
                        dao.insert(entity)
                        addressList.add(entity)
                        adapter.notifyItemInserted(addressList.lastIndex)
                        Log.d("MainActivity", "✅ 저장 성공: $entity")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "❌ 저장 실패", e)
                    }
                }
            } else {
                Log.e("MainActivity", "❌ 좌표 정보가 없습니다.")
            }

            input.text.clear()
            searchResultList.clear()
            searchAdapter.notifyDataSetChanged()
        }

        searchRecyclerView.layoutManager = LinearLayoutManager(this)
        searchRecyclerView.adapter = searchAdapter

        input.addTextChangedListener { editable ->
            val text = editable.toString()
            searchJob?.cancel()
            if (text.length >= 2) {
                searchJob = coroutineScope.launch {
                    delay(300)
                    searchPlace(text) { results: List<KakaoPlace> ->
                        searchResultList.clear()
                        searchResultList.addAll(results)
                        searchAdapter.notifyDataSetChanged()
                    }
                }
            } else {
                searchResultList.clear()
                searchAdapter.notifyDataSetChanged()
            }
        }

        button.setOnClickListener {
            val query = input.text.toString()
            if (query.isNotBlank()) {
                searchPlace(query) { results ->
                    searchResultList.clear()
                    searchResultList.addAll(results)
                    searchAdapter.notifyDataSetChanged()
                }
            }
        }
    }


    fun searchPlace(query: String, callback: (List<KakaoPlace>) -> Unit) {
        val authHeader = "KakaoAK ${BuildConfig.KAKAO_API_KEY}"
        val call = kakaoApi.searchKeyword(authHeader, query)

        call.enqueue(object : Callback<KakaoKeywordResponse> {
            override fun onResponse(call: Call<KakaoKeywordResponse>, response: Response<KakaoKeywordResponse>) {
                if (response.isSuccessful) {
                    callback(response.body()?.documents ?: emptyList())
                } else {
                    Log.e("KakaoAPI", "❌ ${response.code()}: ${response.errorBody()?.string()}")
                    callback(emptyList())
                }
            }

            override fun onFailure(call: Call<KakaoKeywordResponse>, t: Throwable) {
                Log.e("KakaoAPI", "❌ 요청 실패: ${t.message}")
                callback(emptyList())
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
