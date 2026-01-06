package com.example.nfctapapp.domain.model

data class User(
    val id: String,
    val name: String,
    val phone: String? = null
)
