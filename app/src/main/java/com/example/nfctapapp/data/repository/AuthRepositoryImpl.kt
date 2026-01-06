package com.example.nfctapapp.data.repository

import com.example.nfctapapp.data.local.UserPreferences
import com.example.nfctapapp.data.remote.SupabaseClient
import com.example.nfctapapp.data.remote.dto.NfcRegistrationDto
import com.example.nfctapapp.data.remote.dto.NfcRegistrationWithUser
import com.example.nfctapapp.data.remote.dto.UserDto
import com.example.nfctapapp.domain.model.User
import com.example.nfctapapp.util.DeviceUtils
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class AuthRepositoryImpl(
    private val supabaseClient: SupabaseClient,
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
        return try {
            val registrations = supabaseClient.client
                .from("nfc_registrations")
                .select(Columns.raw("*, users(*)")) {
                    filter {
                        eq("nfc_uid", cachedNfcUid)
                        eq("device_id", cachedDeviceId)
                        eq("is_active", true)
                    }
                }
                .decodeList<NfcRegistrationWithUser>()

            if (registrations.isEmpty()) {
                // 서버에 없으면 캐시 삭제
                userPreferences.clearCache()
                Result.success(null)
            } else {
                // 서버에 있으면 최신 정보로 업데이트
                val registration = registrations.first()
                val userDto = registration.users
                if (userDto == null) {
                    userPreferences.clearCache()
                    return Result.success(null)
                }

                val user = User(
                    id = userDto.id ?: registration.userId,
                    name = userDto.name,
                    phone = userDto.phone
                )

                // 캐시 갱신
                userPreferences.cacheUser(user, cachedNfcUid, cachedDeviceId)
                Result.success(user)
            }
        } catch (e: Exception) {
            // 네트워크 오류 시 캐시 유지 (오프라인 지원)
            Result.success(cachedUser)
        }
    }

    override suspend fun authenticateWithNfc(nfcUid: String): Result<User> {
        val deviceId = deviceUtils.getDeviceId()

        // 1. 서버에서 먼저 조회 (서버 우선)
        return try {
            val registrations = supabaseClient.client
                .from("nfc_registrations")
                .select(Columns.raw("*, users(*)")) {
                    filter {
                        eq("nfc_uid", nfcUid)
                        eq("device_id", deviceId)
                        eq("is_active", true)
                    }
                }
                .decodeList<NfcRegistrationWithUser>()

            if (registrations.isEmpty()) {
                // 서버에 없으면 캐시도 삭제 (기존 캐시가 있을 수 있음)
                userPreferences.clearCache()
                Result.failure(NfcNotRegisteredException("NFC 태그가 등록되지 않았습니다"))
            } else {
                val registration = registrations.first()
                val userDto = registration.users
                    ?: return Result.failure(Exception("사용자 정보를 찾을 수 없습니다"))

                val user = User(
                    id = userDto.id ?: registration.userId,
                    name = userDto.name,
                    phone = userDto.phone
                )

                // 마지막 사용 시간 업데이트
                supabaseClient.client
                    .from("nfc_registrations")
                    .update(mapOf("last_used_at" to "now()")) {
                        filter {
                            eq("id", registration.id)
                        }
                    }

                // 캐시 저장
                userPreferences.cacheUser(user, nfcUid, deviceId)

                Result.success(user)
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

    override suspend fun registerUser(name: String, phone: String?, nfcUid: String): Result<User> {
        val deviceId = deviceUtils.getDeviceId()
        val deviceName = deviceUtils.getDeviceName()

        return try {
            // 1. 사용자 생성
            val userDto = UserDto(name = name, phone = phone)
            val insertedUsers = supabaseClient.client
                .from("users")
                .insert(userDto) { select() }
                .decodeList<UserDto>()

            if (insertedUsers.isEmpty()) {
                return Result.failure(Exception("사용자 생성에 실패했습니다"))
            }

            val insertedUser = insertedUsers.first()
            val userId = insertedUser.id ?: return Result.failure(Exception("사용자 ID를 찾을 수 없습니다"))

            // 2. NFC 등록 생성
            val registrationDto = NfcRegistrationDto(
                userId = userId,
                nfcUid = nfcUid,
                deviceId = deviceId,
                deviceName = deviceName
            )

            supabaseClient.client
                .from("nfc_registrations")
                .insert(registrationDto)

            val user = User(
                id = userId,
                name = name,
                phone = phone
            )

            // 3. 캐시 저장
            userPreferences.cacheUser(user, nfcUid, deviceId)

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout() {
        userPreferences.clearCache()
    }
}

class NfcNotRegisteredException(message: String) : Exception(message)
