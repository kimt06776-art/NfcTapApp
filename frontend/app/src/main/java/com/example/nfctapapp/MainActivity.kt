package com.example.nfctapapp

import android.app.PendingIntent
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.nfctapapp.ui.navigation.AppNavigation
import com.example.nfctapapp.ui.theme.NfcTapAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var nfcAdapter: NfcAdapter? = null
    private lateinit var pendingIntent: PendingIntent

    // NFC 이벤트 전달용
    private val nfcTagUid = mutableStateOf<String?>(null)
    private var initialNfcUid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setupNfc()
        handleIntent(intent)
        initialNfcUid = nfcTagUid.value  // handleIntent 후 초기값 캡처

        setContent {
            NfcTapAppTheme {
                val nfcUid by nfcTagUid
                val capturedInitialUid = remember { initialNfcUid }

                AppNavigation(
                    nfcTagUid = nfcUid,
                    initialNfcUid = capturedInitialUid,
                    onNfcConsumed = {
                        nfcTagUid.value = null
                        initialNfcUid = null
                    }
                )
            }
        }
    }

    private fun setupNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)

        if (nfcAdapter == null) {
            Toast.makeText(this, "이 기기는 NFC를 지원하지 않습니다", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(this, javaClass).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        }

        pendingIntent = PendingIntent.getActivity(this, 0, intent, flags)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter?.enableForegroundDispatch(this, pendingIntent, null, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter?.disableForegroundDispatch(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            NfcAdapter.ACTION_NDEF_DISCOVERED -> {
                // NDEF 메시지에서 URL 읽기
                val ndefUrl = readNdefUrl(intent)
                Log.d("NFC", "NDEF URL: $ndefUrl")

                // 우리 앱 전용 URL인지 확인 (nfctap://auth 또는 파라미터 포함)
                if (ndefUrl?.startsWith("nfctap://auth") == true) {
                    // 우리 앱 NFC 태그 확인됨 → UID 읽기
                    val tag: Tag? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(NfcAdapter.EXTRA_TAG, Tag::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
                    }

                    tag?.let { nfcTag ->
                        val uid = nfcTag.id.toHexString()
                        Log.d("NFC", "Tag UID: $uid")
                        nfcTagUid.value = uid
                        Toast.makeText(this, "NFC 인증 중...", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 다른 앱의 NFC 태그 → 무시
                    Log.d("NFC", "다른 앱의 NFC 태그: $ndefUrl")
                    Toast.makeText(this, "이 NFC 태그는 지원하지 않습니다", Toast.LENGTH_SHORT).show()
                }
            }
            Intent.ACTION_VIEW -> {
                // 딥링크로 실행된 경우 (nfctap://auth)
                val data = intent.data
                if (data?.scheme == "nfctap" && data.host == "auth") {
                    Log.d("NFC", "딥링크로 실행됨: $data")
                    // 딥링크만으로는 UID를 알 수 없으므로 NFC 태그 터치 안내
                    Toast.makeText(this, "NFC 태그를 터치해주세요", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * NDEF 메시지에서 URL 추출
     */
    private fun readNdefUrl(intent: Intent): String? {
        val rawMessages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES, NdefMessage::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
        }

        rawMessages?.let { messages ->
            for (message in messages) {
                if (message is NdefMessage) {
                    for (record in message.records) {
                        if (record.tnf == NdefRecord.TNF_WELL_KNOWN &&
                            record.type.contentEquals(NdefRecord.RTD_URI)) {
                            // URI 레코드 파싱
                            return parseUriRecord(record)
                        } else if (record.tnf == NdefRecord.TNF_ABSOLUTE_URI) {
                            // 절대 URI
                            return String(record.type, Charsets.UTF_8)
                        }
                    }
                }
            }
        }

        // Intent의 data URI도 확인 (NDEF_DISCOVERED 인텐트의 경우)
        return intent.data?.toString()
    }

    /**
     * NdefRecord에서 URI 파싱
     * RTD_URI는 첫 바이트가 URI 프리픽스 코드
     */
    private fun parseUriRecord(record: NdefRecord): String {
        val payload = record.payload
        if (payload.isEmpty()) return ""

        val prefixCode = payload[0].toInt() and 0xFF
        val prefix = when (prefixCode) {
            0x00 -> ""
            0x01 -> "http://www."
            0x02 -> "https://www."
            0x03 -> "http://"
            0x04 -> "https://"
            else -> ""
        }

        val uriPart = String(payload, 1, payload.size - 1, Charsets.UTF_8)
        return prefix + uriPart
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02X".format(it) }
}
