package com.example.nfctapapp.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NfcRegistrationDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("nfc_uid")
    val nfcUid: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("device_name")
    val deviceName: String? = null,
    @SerialName("is_active")
    val isActive: Boolean = true,
    @SerialName("registered_at")
    val registeredAt: String? = null,
    @SerialName("last_used_at")
    val lastUsedAt: String? = null
)

@Serializable
data class NfcRegistrationWithUser(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("nfc_uid")
    val nfcUid: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("is_active")
    val isActive: Boolean,
    val users: UserDto? = null
)
