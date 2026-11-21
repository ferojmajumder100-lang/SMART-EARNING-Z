package com.my.apk

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import okhttp3.OkHttpClient
import java.net.Proxy
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

internal object SecureVault {

    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS = "smart_earning_z_master_v1"
    private const val ENC_PREFIX = "ENC_V1:"

    private val cache = ConcurrentHashMap<String, String>()

    @Volatile
    private var strictApiClient: OkHttpClient? = null

    private val mA = intArrayOf(
        0x49, 0x02, 0xAC, 0x7F, 0x21, 0xD6, 0xBB, 0x68,
        0xE0, 0x3A, 0x5D, 0x92, 0x17, 0x44, 0xFB, 0x60
    )
    private val mB = intArrayOf(
        0xCB, 0x1E, 0xE6, 0x75, 0x3D, 0xDA, 0x80, 0x43,
        0x04, 0xA8, 0x69, 0x56, 0xFD, 0x3F, 0x42, 0x84
    )

    private val bApiBase = intArrayOf(
        171, 154, 155, 229, 82, 64, 250, 71, 6, 39, 95, 191, 139, 98, 16, 143,
        50, 194, 83, 219, 120, 33, 107, 101, 97, 97, 32, 170, 89, 78, 119, 247,
        176, 220, 194, 254, 237, 94, 74, 135, 91, 110, 209, 197, 34, 210, 240, 130,
        28, 104, 18, 39, 24, 245, 160, 23, 29, 230, 3, 142, 9, 132, 169, 72,
        62, 118, 133, 246, 2, 24, 135, 18, 133, 117, 116, 246, 248, 39, 160, 89
    )
    private val bApiKey = intArrayOf(
        35, 240, 40, 38, 177, 202, 253, 148, 215, 64, 228, 110, 27, 177, 212, 156,
        171, 171, 197, 84, 153, 80, 9, 218, 123, 28, 54, 242, 95, 175, 149, 185,
        163, 89, 83, 107, 92, 75, 192
    )
    private val bAuthHeader = intArrayOf(
        157, 117, 187, 138, 188, 11, 20, 48, 152, 6, 68, 74, 32, 197, 2, 254,
        184, 247, 67, 183, 168, 12, 128, 162, 181, 135, 101, 42, 145, 169, 187, 161,
        135, 221, 255, 142
    )
    private val bEpConsole = intArrayOf(
        54, 108, 10, 174, 35, 99, 57, 31, 39, 133, 70, 119, 25, 225, 210, 41,
        212, 31, 4, 229, 175, 122, 118, 12, 172, 80, 224, 176, 82, 0, 106, 209,
        129, 103, 162, 234
    )
    private val bEpLiveAccess = intArrayOf(
        91, 0, 51, 105, 11, 129, 147, 86, 242, 248, 135, 238, 246, 105, 197, 43,
        97, 59, 161, 242, 217, 113, 21, 146, 16, 122, 164, 134, 125, 40, 26, 254,
        138, 77, 163, 10, 26, 241, 44
    )
    private val bEpGetNum = intArrayOf(
        103, 113, 178, 124, 125, 16, 0, 222, 167, 164, 214, 177, 119, 37, 227, 74,
        86, 8, 222, 43, 187, 2, 180, 161, 248, 53, 44, 145, 240, 156, 211, 3,
        208, 93, 77
    )
    private val bEpOtp = intArrayOf(
        153, 59, 13, 41, 61, 18, 151, 180, 213, 8, 208, 102, 213, 76, 68, 220,
        187, 207, 245, 92, 211, 229, 5, 88, 239, 110, 84, 103, 155, 236, 102, 143,
        250, 70, 61, 104, 72, 14, 91, 30
    )
    private val bGhOwner = intArrayOf(
        107, 20, 114, 221, 113, 173, 176, 91, 149, 212, 115, 28, 125, 98, 133, 1,
        190, 220, 13, 162, 174, 199, 232, 8, 90, 29, 203, 76, 205, 10, 72, 36,
        43, 89, 56, 255, 134, 113, 220, 87, 10, 239, 175, 224
    )
    private val bGhRepo = intArrayOf(
        150, 193, 115, 96, 22, 78, 214, 199, 176, 219, 95, 60, 243, 42, 20, 65,
        128, 35, 233, 105, 160, 12, 114, 220, 11, 143, 43, 190, 152, 48, 9, 248,
        100, 202, 15, 164, 11, 178, 164, 154, 146
    )
    private val bGhPrefix = intArrayOf(
        6, 54, 183, 121, 127, 4, 49, 198, 216, 140, 206, 192, 145, 130, 181, 5,
        40, 91, 1, 55, 162, 214, 219, 120, 240, 28, 169, 82, 103, 210, 195, 224,
        251, 107, 111, 173, 215, 57, 165, 236, 98, 206, 163, 19, 47, 94, 229, 41,
        77, 165, 31, 114, 127, 198, 187, 205, 62
    )
    private val bGhSuffix = intArrayOf(
        229, 172, 128, 117, 155, 42, 40, 58, 235, 19, 217, 131, 132, 162, 124, 84,
        211, 57, 207, 4, 136, 15, 230, 250, 68, 88, 244, 56, 207, 188, 148, 221,
        126, 72, 9, 255, 89, 244, 49, 19, 124, 174, 125, 77
    )
    private val bFbProject = intArrayOf(
        46, 224, 32, 84, 49, 135, 183, 53, 226, 174, 22, 163, 151, 18, 194, 94,
        103, 139, 230, 172, 70, 142, 150, 32, 114, 134, 104, 197, 69, 5, 25, 80,
        34, 127, 174, 165, 158, 143, 79, 228, 193, 26, 62
    )
    private val bFbApiKey = intArrayOf(
        232, 9, 4, 245, 3, 84, 49, 79, 0, 240, 245, 36, 230, 202, 12, 102,
        139, 253, 191, 253, 127, 73, 104, 232, 143, 184, 195, 213, 222, 224, 61, 145,
        133, 83, 196, 255, 83, 197, 144, 192, 16, 142, 46, 141, 81, 252, 26, 135,
        207, 33, 127, 7, 94, 75, 224, 237, 84, 133, 208, 228, 75, 34, 2, 100,
        40, 216, 235
    )
    private val bFsBase = intArrayOf(
        45, 19, 127, 126, 172, 157, 175, 117, 249, 225, 230, 33, 240, 228, 106, 135,
        185, 77, 186, 30, 77, 20, 190, 151, 165, 86, 98, 63, 83, 210, 231, 0,
        158, 115, 33, 225, 17, 167, 135, 83, 125, 247, 172, 224, 192, 40, 19, 201,
        200, 80, 101, 169, 202, 208, 85, 108, 18, 88, 215, 253, 137, 111, 139, 114,
        254, 188, 137, 2, 142, 115, 155, 22, 58
    )
    private val bFsMid = intArrayOf(
        203, 199, 227, 224, 59, 156, 188, 254, 218, 50, 31, 45, 216, 231, 139, 81,
        128, 76, 143, 26, 55, 114, 12, 203, 146, 15, 162, 115, 241, 124, 197, 183,
        185, 144, 167, 185, 217, 135, 127, 210, 253, 46, 121, 180, 165, 48, 120, 118,
        3, 191, 218, 122, 100, 171, 95, 201, 96, 228, 196
    )
    private val bRtdbBase = intArrayOf(
        102, 147, 57, 111, 93, 18, 29, 213, 171, 244, 131, 173, 0, 34, 208, 17,
        146, 233, 143, 61, 97, 143, 25, 84, 32, 22, 196, 129, 202, 100, 59, 62,
        62, 157, 85, 164, 78, 101, 148, 127, 242, 153, 126, 91, 50, 62, 147, 31,
        74, 169, 35, 137, 233, 126, 69, 116, 159, 17, 239, 78, 80, 149, 255, 54,
        92, 223, 186, 124, 120, 219, 120, 140, 99, 102, 24, 144, 226, 216, 216, 215
    )

    private fun deriveVaultKey(context: Context): SecretKeySpec {
        val sA = ByteArray(16) { i -> (mA[i] xor 0x33).toByte() }
        val sB = ByteArray(16) { i -> (mB[i] xor 0x5A).toByte() }
        val md = MessageDigest.getInstance("SHA-256")
        md.update(sA)
        md.update(context.packageName.toByteArray(StandardCharsets.UTF_8))
        md.update(sB)
        return SecretKeySpec(md.digest(), "AES")
    }

    private fun unseal(context: Context, tag: String, blob: IntArray): String {
        cache[tag]?.let { return it }
        val all = ByteArray(blob.size) { i -> blob[i].toByte() }
        val iv = all.copyOfRange(0, 12)
        val ct = all.copyOfRange(12, all.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, deriveVaultKey(context), GCMParameterSpec(128, iv))
        cipher.updateAAD(tag.toByteArray(StandardCharsets.UTF_8))
        val plain = String(cipher.doFinal(ct), StandardCharsets.UTF_8)
        cache[tag] = plain
        return plain
    }

    fun apiBase(context: Context): String = unseal(context, "API_BASE", bApiBase)
    fun apiKey(context: Context): String = unseal(context, "API_KEY", bApiKey)
    fun authHeader(context: Context): String = unseal(context, "AUTH_HEADER", bAuthHeader)
    fun endpointConsole(context: Context): String = apiBase(context) + unseal(context, "EP_CONSOLE", bEpConsole)
    fun endpointLiveAccess(context: Context): String = apiBase(context) + unseal(context, "EP_LIVEACCESS", bEpLiveAccess)
    fun endpointGetNum(context: Context): String = apiBase(context) + unseal(context, "EP_GETNUM", bEpGetNum)
    fun endpointOtp(context: Context): String = apiBase(context) + unseal(context, "EP_OTP", bEpOtp)

    fun githubLatestReleaseUrl(context: Context): String {
        val p = unseal(context, "GH_PREFIX", bGhPrefix)
        val o = unseal(context, "GH_OWNER", bGhOwner)
        val r = unseal(context, "GH_REPO", bGhRepo)
        val s = unseal(context, "GH_SUFFIX", bGhSuffix)
        return "$p$o/$r$s"
    }

    fun firestoreDocumentUrl(context: Context, docPath: String): String {
        val base = unseal(context, "FS_BASE", bFsBase)
        val project = unseal(context, "FB_PROJECT", bFbProject)
        val mid = unseal(context, "FS_MID", bFsMid)
        val key = unseal(context, "FB_API_KEY", bFbApiKey)
        return "$base$project$mid$docPath?key=$key"
    }

    fun rtdbUrl(context: Context, nodePath: String): String {
        val base = unseal(context, "RTDB_BASE", bRtdbBase)
        val cleanPath = nodePath.trimStart('/')
        return "$base$cleanPath.json"
    }

    fun getStrictApiClient(): OkHttpClient {
        strictApiClient?.let { return it }
        synchronized(this) {
            strictApiClient?.let { return it }
            val builder = OkHttpClient.Builder()
                .connectTimeout(25, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
            AntiCaptureShield.applyStrictSslPinning(builder, allowExplicitUserProxy = false)
            val client = builder.build()
            strictApiClient = client
            return client
        }
    }

    private fun getOrCreateLocalKey(context: Context): SecretKey {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val ks = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
                val existing = ks.getKey(KEY_ALIAS, null) as? SecretKey
                if (existing != null) return existing
                val keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
                val spec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGen.init(spec)
                return keyGen.generateKey()
            } catch (_: Exception) {
            }
        }
        val md = MessageDigest.getInstance("SHA-256")
        md.update(deriveVaultKey(context).encoded)
        md.update(Build.FINGERPRINT.toByteArray(StandardCharsets.UTF_8))
        return SecretKeySpec(md.digest(), "AES")
    }

    fun encryptLocal(context: Context, plaintext: String): String {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val key = getOrCreateLocalKey(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && key !is SecretKeySpec) {
                cipher.init(Cipher.ENCRYPT_MODE, key)
            } else {
                val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
                cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
            }
            val iv = cipher.iv
            val ct = cipher.doFinal(plaintext.toByteArray(StandardCharsets.UTF_8))
            val combined = ByteArray(1 + iv.size + ct.size)
            combined[0] = iv.size.toByte()
            System.arraycopy(iv, 0, combined, 1, iv.size)
            System.arraycopy(ct, 0, combined, 1 + iv.size, ct.size)
            ENC_PREFIX + Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            plaintext
        }
    }

    fun decryptLocal(context: Context, stored: String, defaultFallback: String = ""): String {
        if (!stored.startsWith(ENC_PREFIX)) return stored
        return try {
            val payload = Base64.decode(stored.removePrefix(ENC_PREFIX), Base64.NO_WRAP)
            val ivLen = payload[0].toInt() and 0xFF
            val iv = payload.copyOfRange(1, 1 + ivLen)
            val ct = payload.copyOfRange(1 + ivLen, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateLocalKey(context), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ct), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            defaultFallback
        }
    }
}
