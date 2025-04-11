package com.example.findcourse.api

data class KakaoAddressResponse(
    val documents: List<Document>
)

data class Document(
    val address_name: String
)
