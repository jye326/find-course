package com.example.findcourse

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.findcourse.api.KakaoApiService
import com.example.findcourse.api.KakaoKeywordResponse
import com.example.findcourse.api.KakaoPlace
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlin.math.*

class FindCourseActivity : AppCompatActivity() {

    private lateinit var startInput: EditText
    private lateinit var countInput: EditText
    private lateinit var resultText: TextView
    private lateinit var dao: AddressDao

    private lateinit var searchAdapter: SearchResultAdapter
    private val searchResultList = mutableListOf<KakaoPlace>()
    private var searchJob: Job? = null

    private lateinit var kakaoApi: KakaoApiService
    private lateinit var searchRecyclerView: RecyclerView
    private var ignoreNextChange = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_find_course)

        startInput = findViewById(R.id.startInput)
        countInput = findViewById(R.id.visitCountInput)
        resultText = findViewById(R.id.resultText)
        searchRecyclerView = findViewById(R.id.searchStartInputResultList)

        dao = Room.databaseBuilder(
            applicationContext,
            AddressDatabase::class.java, "address-db"
        ).fallbackToDestructiveMigration().build().addressDao()

        val retrofitKeyword = Retrofit.Builder()
            .baseUrl("https://dapi.kakao.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        kakaoApi = retrofitKeyword.create(KakaoApiService::class.java)

        searchAdapter = SearchResultAdapter(searchResultList) { selected ->
            ignoreNextChange = true
            val selectedKeyword = selected.place_name
            startInput.setText(selectedKeyword)
            startInput.setSelection(selectedKeyword.length)
            startInput.clearFocus()
            hideKeyboard()

            searchResultList.clear()
            searchAdapter.notifyDataSetChanged()
            searchRecyclerView.visibility = View.GONE
        }

        searchRecyclerView.layoutManager = LinearLayoutManager(this)
        searchRecyclerView.adapter = searchAdapter

        startInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                searchRecyclerView.visibility = View.GONE
            }
        }

        startInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (ignoreNextChange) {
                    ignoreNextChange = false
                    return
                }

                val text = s?.toString() ?: ""
                searchJob?.cancel()
                if (text.length >= 2 && startInput.hasFocus()) {
                    searchJob = lifecycleScope.launch {
                        delay(300)
                        searchPlace(text) { results ->
                            searchResultList.clear()
                            searchResultList.addAll(results)
                            searchAdapter.notifyDataSetChanged()
                            searchRecyclerView.visibility = if (results.isNotEmpty()) View.VISIBLE else View.GONE
                        }
                    }
                } else {
                    searchResultList.clear()
                    searchAdapter.notifyDataSetChanged()
                    searchRecyclerView.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        findViewById<Button>(R.id.calculateRouteButton).setOnClickListener {
            hideKeyboard() // ✅ 키보드 먼저 내림

            val startKeyword = startInput.text.toString().trim()

            if (startKeyword.isBlank()) {
                Toast.makeText(this, "출발지를 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            CalculateRouteHelper.calculateByTSP(
                context = this,
                startKeyword = startKeyword,
                dao = dao,
                kakaoApi = kakaoApi, // ✅ 이거 추가!
                resultView = resultText,
                placeCount = countInput.text.toString().toInt()
            )
        }

        findViewById<FloatingActionButton>(R.id.toPointManagementButton).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(startInput.windowToken, 0)
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
}

// ✅ 캐시용 데이터 클래스는 더 이상 필요하지 않음
