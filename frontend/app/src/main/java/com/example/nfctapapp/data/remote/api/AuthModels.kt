package com.example.nfctapapp.data.remote.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NfcAuthRequest(
    @Json(name = "nfcUid") val nfcUid: String,
    @Json(name = "deviceId") val deviceId: String
)

@JsonClass(generateAdapter = true)
data class UserRegisterRequest(
    @Json(name = "name") val name: String,
    @Json(name = "phone") val phone: String?,
    @Json(name = "nfcUid") val nfcUid: String,
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "deviceName") val deviceName: String?
)

@JsonClass(generateAdapter = true)
data class UserValidateRequest(
    @Json(name = "userId") val userId: String,
    @Json(name = "nfcUid") val nfcUid: String,
    @Json(name = "deviceId") val deviceId: String
)

@JsonClass(generateAdapter = true)
data class AuthResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "user") val user: UserDto?,
    @Json(name = "error") val error: String?
)

@JsonClass(generateAdapter = true)
data class UserDto(
    @Json(name = "id") val id: String?,
    @Json(name = "name") val name: String,
    @Json(name = "phone") val phone: String?,
    @Json(name = "created_at") val createdAt: String?
)
