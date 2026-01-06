package com.example.nfctapapp.util

object NfcUtils {

    fun ByteArray.toHexString(): String {
        return joinToString("") { "%02X".format(it) }
    }

    fun String.hexToByteArray(): ByteArray {
        check(length % 2 == 0) { "Hex string must have even length" }
        return chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}
