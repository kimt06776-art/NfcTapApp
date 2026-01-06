package com.example.nfctapapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String? = null,
    val name: String,
    val phone: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
