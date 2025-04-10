package com.example.findcourse

import KakaoApiService
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private val addressList = mutableListOf<Address>()
    private lateinit var adapter: AddressAdapter
    private lateinit var kakaoApi: KakaoApiService
    private val authHeader = "KakaoAK ${BuildConfig.KAKAO_API_KEY}"


    private val searchResultList = mutableListOf<String>()
    private lateinit var searchAdapter: SearchResultAdapter

    private var searchJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.point_management)

        val input = findViewById<EditText>(R.id.addressInput)
        val button = findViewById<Button>(R.id.saveButton)
        val recyclerView = findViewById<RecyclerView>(R.id.addressList)

        adapter = AddressAdapter(addressList) { position ->
            addressList.removeAt(position)
            adapter.notifyItemRemoved(position)
        }
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        // Retrofit 초기화
        val retrofit = Retrofit.Builder()
            .baseUrl("https://dapi.kakao.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        kakaoApi = retrofit.create(KakaoApiService::class.java)

        val searchRecyclerView = findViewById<RecyclerView>(R.id.searchResultList)

        searchAdapter = SearchResultAdapter(searchResultList) { selected ->
            addressList.add(Address(selected))
            adapter.notifyItemInserted(addressList.size - 1)
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
                    delay(300) // 300ms 딜레이로 디바운싱
                    searchAddress(text) { results ->
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
                searchAddress(query) { results ->
                    searchResultList.clear()
                    searchResultList.addAll(results)
                    searchAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun searchAddress(query: String, callback: (List<String>) -> Unit) {
        val call = kakaoApi.searchAddress(authHeader, query)

        Log.d("KakaoKey", "Header: $authHeader")  // 👈 여기 추가

        call.enqueue(object : Callback<KakaoAddressResponse> {
            override fun onResponse(
                call: Call<KakaoAddressResponse>,
                response: Response<KakaoAddressResponse>
            ) {
                if (response.isSuccessful) {
                    val body = response.body()
                    val addresses = body?.documents?.map { it.address_name } ?: emptyList()
                    callback(addresses)
                } else {
                    Log.e("주소검색", "API 오류: ${response.code()}")
                    callback(emptyList())
                }
            }

            override fun onFailure(call: Call<KakaoAddressResponse>, t: Throwable) {
                Log.e("주소검색", "요청 실패: ${t.message}")
                callback(emptyList())
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineScope.cancel()
    }
}
