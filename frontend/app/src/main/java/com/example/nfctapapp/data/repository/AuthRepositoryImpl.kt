package com.example.nfctapapp.data.repository

import com.example.nfctapapp.data.local.UserPreferences
import com.example.nfctapapp.data.remote.api.AuthApiService
import com.example.nfctapapp.data.remote.api.NfcAuthRequest
import com.example.nfctapapp.data.remote.api.UserRegisterRequest
import com.example.nfctapapp.data.remote.api.UserValidateRequest
import com.example.nfctapapp.domain.model.User
import com.example.nfctapapp.util.DeviceUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auth Repository (Backend API 사용)
 *
 * 이전: SupabaseClient 직접 사용
 * 현재: Backend REST API 사용 (보안 강화)
 */
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authApiService: AuthApiService,
    private val userPreferences: UserPreferences,
    private val deviceUtils: DeviceUtils
) : AuthRepository {

    override fun getCachedUser(): Flow<User?> {
        return userPreferences.cachedUser
    }

    override suspend fun validateCachedUser(): Result<User?> {
        // 1. 캐시된 정보 확인
        val cachedUser = userPreferences.cachedUser.first() ?: return Result.success(null)
        val cachedNfcUid = userPreferences.cachedNfcUid.first() ?: return Result.success(null)
        val cachedDeviceId = userPreferences.cachedDeviceId.first() ?: return Result.success(null)

        // 2. 서버에서 사용자 존재 여부 확인
        return withContext(Dispatchers.IO) {
            try {
                val response = authApiService.validateUser(
                    UserValidateRequest(
                        userId = cachedUser.id,
                        nfcUid = cachedNfcUid,
                        deviceId = cachedDeviceId
                    )
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.user != null) {
                        val userDto = body.user
                        val user = User(
                            id = userDto.id ?: cachedUser.id,
                            name = userDto.name,
                            phone = userDto.phone
                        )

                        // 캐시 갱신
                        userPreferences.cacheUser(user, cachedNfcUid, cachedDeviceId)
                        Result.success(user)
                    } else {
                        // 서버에 없으면 캐시 삭제
                        userPreferences.clearCache()
                        Result.success(null)
                    }
                } else {
                    // HTTP 오류 시 캐시 유지 (오프라인 지원)
                    Result.success(cachedUser)
                }
            } catch (e: Exception) {
                // 네트워크 오류 시 캐시 유지 (오프라인 지원)
                Result.success(cachedUser)
            }
        }
    }

    override suspend fun authenticateWithNfc(nfcUid: String): Result<User> {
        val deviceId = deviceUtils.getDeviceId()

        // 1. 서버에서 먼저 조회 (서버 우선)
        return withContext(Dispatchers.IO) {
            try {
                val response = authApiService.authenticateWithNfc(
                    NfcAuthRequest(
                        nfcUid = nfcUid,
                        deviceId = deviceId
                    )
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.user != null) {
                        val userDto = body.user
                        val user = User(
                            id = userDto.id ?: "",
                            name = userDto.name,
                            phone = userDto.phone
                        )

                        // 캐시 저장
                        userPreferences.cacheUser(user, nfcUid, deviceId)

                        Result.success(user)
                    } else {
                        // 서버에 없으면 캐시도 삭제
                        userPreferences.clearCache()
                        Result.failure(NfcNotRegisteredException(
                            body?.error ?: "NFC 태그가 등록되지 않았습니다"
                        ))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                // 2. 네트워크 오류 시에만 캐시 사용 (오프라인 fallback)
                val fallbackUser = userPreferences.verifyOffline(nfcUid, deviceId)
                if (fallbackUser != null) {
                    Result.success(fallbackUser)
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun registerUser(name: String, phone: String?, nfcUid: String): Result<User> {
        val deviceId = deviceUtils.getDeviceId()
        val deviceName = deviceUtils.getDeviceName()

        return withContext(Dispatchers.IO) {
            try {
                val response = authApiService.registerUser(
                    UserRegisterRequest(
                        name = name,
                        phone = phone,
                        nfcUid = nfcUid,
                        deviceId = deviceId,
                        deviceName = deviceName
                    )
                )

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.success == true && body.user != null) {
                        val userDto = body.user
                        val user = User(
                            id = userDto.id ?: "",
                            name = userDto.name,
                            phone = userDto.phone
                        )

                        // 캐시 저장
                        userPreferences.cacheUser(user, nfcUid, deviceId)

                        Result.success(user)
                    } else {
                        Result.failure(Exception(body?.error ?: "사용자 등록 실패"))
                    }
                } else {
                    Result.failure(Exception("HTTP ${response.code()}: ${response.message()}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun logout() {
        userPreferences.clearCache()
    }
}

class NfcNotRegisteredException(message: String) : Exception(message)
