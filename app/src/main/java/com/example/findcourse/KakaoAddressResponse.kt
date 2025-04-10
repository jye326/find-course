package com.example.findcourse

data class KakaoAddressResponse(
    val documents: List<Document>
)

data class Document(
    val address_name: String
)
