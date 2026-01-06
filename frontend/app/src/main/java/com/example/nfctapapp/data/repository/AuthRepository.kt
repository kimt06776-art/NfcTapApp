package com.example.nfctapapp.data.repository

import com.example.nfctapapp.domain.model.User
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun getCachedUser(): Flow<User?>
    suspend fun validateCachedUser(): Result<User?>
    suspend fun authenticateWithNfc(nfcUid: String): Result<User>
    suspend fun registerUser(name: String, phone: String?, nfcUid: String): Result<User>
    suspend fun logout()
}
