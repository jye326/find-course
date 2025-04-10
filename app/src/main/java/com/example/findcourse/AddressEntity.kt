package com.example.findcourse

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "addresses")
data class AddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val address: String,    // 주소
    val placeName:String    // 키워드 ex: 스타벅스 강남점
)