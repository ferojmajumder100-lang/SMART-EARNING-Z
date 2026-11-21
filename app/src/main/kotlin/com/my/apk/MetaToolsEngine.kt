package com.my.apk

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.Random
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

internal class MetaHttpUtil {
    var cookieHeader: String = ""

    data class Response(
        val code: Int,
        val body: String,
        val headers: Map<String, List<String>>
    )

    companion object {
        private val client: OkHttpClient by lazy {
            val builder = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
            AntiCaptureShield.applyStrictSslPinning(builder, allowExplicitUserProxy = false)
            builder.build()
        }

        fun encodeForm(data: Map<String, String>): String {
            val sb = StringBuilder()
            for ((k, v) in data) {
                if (sb.isNotEmpty()) sb.append("&")
                sb.append(URLEncoder.encode(k, "UTF-8"))
                    .append("=")
                    .append(URLEncoder.encode(v, "UTF-8"))
            }
            return sb.toString()
        }

        fun collectCookies(headers: Map<String, List<String>>?): Map<String, String> {
            val out = HashMap<String, String>()
            if (headers == null) return out
            for ((key, values) in headers) {
                if (key.equals("Set-Cookie", ignoreCase = true)) {
                    for (hdr in values) {
                        val first = hdr.split(";")[0]
                        val eq = first.indexOf('=')
                        if (eq > 0) {
                            out[first.substring(0, eq).trim()] = first.substring(eq + 1).trim()
                        }
                    }
                }
            }
            return out
        }

        fun mergeCookies(base: String?, newCk: Map<String, String>?): String {
            if (newCk == null || newCk.isEmpty()) return base ?: ""
            val cur = LinkedHashMap<String, String>()
            if (!base.isNullOrEmpty()) {
                for (rawPart in base.split(";")) {
                    val p = rawPart.trim()
                    val eq = p.indexOf('=')
                    if (eq > 0) cur[p.substring(0, eq)] = p.substring(eq + 1)
                }
            }
            cur.putAll(newCk)
            val sb = StringBuilder()
            for ((k, v) in cur) {
                if (sb.isNotEmpty()) sb.append("; ")
                sb.append(k).append("=").append(v)
            }
            return sb.toString()
        }
    }

    @Throws(IOException::class)
    fun get(urlStr: String, headers: Map<String, String>?): Response {
        return request("GET", urlStr, headers, null)
    }

    @Throws(IOException::class)
    fun post(urlStr: String, headers: Map<String, String>?, body: String?): Response {
        return request("POST", urlStr, headers, body)
    }

    @Throws(IOException::class)
    private fun request(
        method: String,
        urlStr: String,
        headers: Map<String, String>?,
        body: String?
    ): Response {
        val builder = Request.Builder().url(urlStr)
        var contentType = "application/x-www-form-urlencoded; charset=utf-8"

        if (headers != null) {
            for ((k, v) in headers) {
                if (k.equals("Content-Type", ignoreCase = true)) {
                    contentType = v
                }
                builder.header(k, v)
            }
        }
        if (cookieHeader.isNotEmpty()) {
            builder.header("Cookie", cookieHeader)
        }

        if (method == "POST") {
            val reqBody = (body ?: "").toRequestBody(contentType.toMediaTypeOrNull())
            builder.post(reqBody)
        } else {
            builder.get()
        }

        client.newCall(builder.build()).execute().use { resp ->
            val rawBytes = resp.body?.bytes() ?: ByteArray(0)
            val enc = resp.header("Content-Encoding")?.lowercase(Locale.US) ?: ""
            val decodedBytes = when {
                enc.contains("gzip") && rawBytes.isNotEmpty() -> {
                    try {
                        GZIPInputStream(ByteArrayInputStream(rawBytes)).use { gis ->
                            val out = ByteArrayOutputStream()
                            val buf = ByteArray(4096)
                            var n: Int
                            while (gis.read(buf).also { n = it } != -1) out.write(buf, 0, n)
                            out.toByteArray()
                        }
                    } catch (_: Exception) {
                        rawBytes
                    }
                }
                enc.contains("deflate") && rawBytes.isNotEmpty() -> {
                    try {
                        InflaterInputStream(ByteArrayInputStream(rawBytes)).use { iis ->
                            val out = ByteArrayOutputStream()
                            val buf = ByteArray(4096)
                            var n: Int
                            while (iis.read(buf).also { n = it } != -1) out.write(buf, 0, n)
                            out.toByteArray()
                        }
                    } catch (_: Exception) {
                        rawBytes
                    }
                }
                else -> rawBytes
            }
            val respBody = String(decodedBytes, StandardCharsets.UTF_8)
            return Response(resp.code, respBody, resp.headers.toMultimap())
        }
    }
}

internal object MetaCreator {
    data class MetaResult(
        var success: Boolean = false,
        var uid: String = "",
        var error: String = ""
    )

    private fun randHex(n: Int): String {
        val hex = "0123456789abcdef"
        val sb = StringBuilder()
        val r = Random()
        for (i in 0 until n) sb.append(hex[r.nextInt(16)])
        return sb.toString()
    }

    private fun newUuid(): String = UUID.randomUUID().toString()

    fun createMeta(email: String, password: String, regionCode: String): MetaResult {
        val res = MetaResult()
        try {
            val http = MetaHttpUtil()
            val h = LinkedHashMap<String, String>()
            h["User-Agent"] = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"
            h["Accept-Encoding"] = "gzip, deflate"
            h["Content-Type"] = "application/x-www-form-urlencoded"
            h["sec-ch-ua-full-version-list"] = "\"Not_A Brand\";v=\"8.0.0.0\", \"Chromium\";v=\"153.0.8010.39\""
            h["sec-ch-ua-platform"] = "\"iOS\""
            h["sec-ch-ua"] = "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"153\""
            h["sec-ch-ua-model"] = "\"itel S665L\""
            h["sec-ch-ua-mobile"] = "?1"
            h["x-asbd-id"] = "359341"
            h["x-fb-lsd"] = "AdQQSft0Lj16s3kt5YP7gVojTpU"
            h["sec-ch-prefers-color-scheme"] = "light"
            h["sec-ch-ua-platform-version"] = "\"12.0.0\""
            h["origin"] = "https://auth.meta.com"
            h["x-requested-with"] = "mark.via.gp"
            h["sec-fetch-site"] = "same-origin"
            h["sec-fetch-mode"] = "cors"
            h["sec-fetch-dest"] = "empty"
            h["referer"] = "https://auth.meta.com/"
            h["accept-language"] = "en-US,en;q=0.9,my-MM;q=0.8,my;q=0.7,fr-FR;q=0.6,fr;q=0.5"
            h["priority"] = "u=1, i"

            val data = LinkedHashMap<String, String>()
            data["consent_version"] = ""
            data["contact_point"] = email
            data["contact_point_type"] = "EMAIL_ADDRESS"
            data["csi"] = "xQj9j44BMbk0h0n9-aT3tMLY"
            data["date_of_birth"] = "2004-10-12"
            data["device_id"] = "device_id_fetch_ig_did"
            data["fb_encrypted_access_token"] = ""
            data["fb_oidc_access_token"] = ""
            data["first_name"] = "Smart"
            data["github_prefill"] = ""
            data["google_id_token"] = ""
            data["has_youth_consent"] = "false"
            data["ig_encrypted_access_token"] = ""
            data["ig_encrypted_auth_header"] = ""
            data["ig_oidc_access_token"] = ""
            data["last_name"] = "EarningZ"
            data["opt_into_marketing"] = "true"
            data["password"] = password
            data["passwordless_registration"] = "false"
            data["redirect_uri"] = ""
            data["reg_integrity"] = "Q8W2BT9mckEJMXpZ6nMJyD_i2hMYyeDc_qiOMjsvaHUNyL0zpyEBrYyufwby9U5MCT3BDjRwqknEfoQY2IqiA8iXOFfgMUHPA5RGmj1QFHAQ1vOzznYwtKwzEKBGHkfERgXkEKWsB85mQPcOSWGep6XY9P79cIcPYxM2DXezzOyDzJV_-ga-Rb5sqKcZixMhJjzOt501MJUSyiTP9Gc74MkvMJsNcKMmLA4G0BgthFswSG6gPQGmhHmHnggg-v6Ivg8RT5EFxReU0nsC6i9VbilUJjCkcvZUrf6-uM3i2KktEAH0SMkmgdWT3w21o9K5gw0aTr-Pw9oNpHeCGPpxLEY0zcd0338W9kcFiaIMpQQ9i9KcW4BsV6TWoRXXBqYuK6AgD3H4BqH1ieJhtg|kregenc"
            data["should_save_credentials"] = "true"
            data["third_party_age_verification_id"] = ""
            data["waterfall_id"] = newUuid()
            data["caa_event_flow"] = "ntf"
            data["entry_point"] = "login_home"
            data["event_client_time"] = String.format(Locale.US, "%.3f", System.currentTimeMillis() / 1000.0)
            data["is_kadabra_zero"] = "false"
            data["regulation_jurisdiction"] = "[\"$regionCode\"]"
            data["qpl_join_id"] = "f" + randHex(16)
            data["__user"] = "0"
            data["__a"] = "1"
            data["__req"] = "1r"
            data["__hs"] = "20738.HYP:frl_comet_auth_pkg.2.1...0"
            data["dpr"] = "2"
            data["__ccg"] = "EXCELLENT"
            data["__rev"] = "1050031415"
            data["__s"] = "iofdj3:gh7l2x:5ddfgi"
            data["__hsi"] = "7695736164154778546"
            data["__dyn"] = "7xe6E5q5U5ObwKBAg5S1Dxu13w8CewSwMwNw9G2S0lW4o0B-q1ew2io2awpUO0n24o5-0Bo7O2l0Fwqo31w9O0H8-U2zxe2GewbS361qw8Xwn82Lw6OyES1Tw8W0Lo6-1FwCwe-1Iwqo5u1qwUw8S1Tw8q0JU0Vy3mew"
            data["__csr"] = "gittQAvGiuXtGXq9WveNkCylOk9gyaULjCG8Gi4biyEWi9x2eybghBCw1my04SE09QnsoQEGN03Ly7g01pFU0H60POG4U5GiE7u02my0b3w0wsB9g7XwGBoS3si0nS097w1a604Io-14w"
            data["__hsdp"] = "gd4AJh4v7xG2Na8G880ty9xa0bVBw0ELG01wpw1XG"
            data["__hblp"] = "09Gi7ob8Cewe65U21wn8Cmcw2X8OGw49waC08cw9G05RE0hqw0zSw0C-wd23u07KE7OexK3K0fIw1iK0aryE1rU"
            data["__sjsp"] = "gd4AJh5Tf7xG"
            data["__comet_req"] = "33"
            data["lsd"] = "AdQQSft0Lj16s3kt5YP7gVojTpU"
            data["jazoest"] = "22314"
            data["__spin_r"] = "1050031415"
            data["__spin_b"] = "trunk"
            data["__spin_t"] = (System.currentTimeMillis() / 1000).toString()
            data["__jssesw"] = "1"

            val body = MetaHttpUtil.encodeForm(data)
            val r = http.post("https://auth.meta.com/login/device-based/register-save-credentials/", h, body)

            var resp = r.body
            if (resp.trim().startsWith("for (;;);")) resp = resp.trim().substring(9)

            if (resp.trim().isEmpty()) {
                res.error = "Empty Response (FB Blocked)"
                return res
            }

            val j = JSONObject(resp)
            val payload = j.optJSONObject("payload")
            if (payload != null) {
                val uid = payload.optString("uid", "")
                if (uid.isNotEmpty() && uid != "0") {
                    res.success = true
                    res.uid = uid
                    return res
                }
            }

            res.error = when {
                j.has("errorDescription") -> j.optString("errorDescription")
                j.has("error") -> j.optString("error")
                else -> "Unknown JSON Error"
            }
        } catch (e: Exception) {
            res.error = "Exception: ${e.message}"
        }
        return res
    }

    private fun getMuseHeaders(csrf: String, cookie: String): Map<String, String> {
        val h = LinkedHashMap<String, String>()
        h["User-Agent"] = "Mozilla/5.0 (iPhone; CPU iPhone OS 18_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.5 Mobile/15E148 Safari/604.1"
        h["Accept-Encoding"] = "gzip, deflate"
        h["Content-Type"] = "application/json"
        h["x-hatch-waterfall-id"] = newUuid()
        h["x-hatch-csrf-token"] = csrf
        h["sec-ch-ua-platform"] = "\"iOS\""
        h["sec-ch-ua"] = "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"153\""
        h["x-hatch-caa-reg-entry-point"] = "login_home"
        h["sec-ch-ua-mobile"] = "?1"
        h["sec-ch-prefers-color-scheme"] = "light"
        h["origin"] = "https://muse.ai"
        h["x-requested-with"] = "mark.via.gp"
        h["sec-fetch-site"] = "same-origin"
        h["sec-fetch-mode"] = "cors"
        h["sec-fetch-dest"] = "empty"
        h["referer"] = "https://muse.ai/"
        h["accept-language"] = "en-US,en;q=0.9,my-MM;q=0.8,my;q=0.7,fr-FR;q=0.6,fr;q=0.5"
        h["priority"] = "u=1, i"
        if (cookie.isNotEmpty()) {
            h["Cookie"] = cookie
        }
        return h
    }

    fun museSendOtp(
        email: String,
        region: String,
        cookie: String,
        csrf: String,
        outSessionCookie: Array<String>
    ): String {
        return try {
            val http = MetaHttpUtil()
            val h = getMuseHeaders(csrf, cookie)
            val json = JSONObject().apply {
                put("contact_point", email)
                put("region", region)
            }.toString()
            val r = http.post("https://muse.ai/api/auth/native/send-otp", h, json)

            val newCk = MetaHttpUtil.collectCookies(r.headers)
            outSessionCookie[0] = MetaHttpUtil.mergeCookies(cookie, newCk)

            if (r.code in 200..299) {
                "OK"
            } else {
                val errBody = if (r.body.length > 100) r.body.substring(0, 100) else r.body
                "HTTP ${r.code} | $errBody"
            }
        } catch (e: Exception) {
            e.message ?: "Error"
        }
    }

    fun museConfirmOtp(otp: String, email: String, cookie: String, csrf: String): String {
        return try {
            val http = MetaHttpUtil()
            val h = getMuseHeaders(csrf, cookie)
            val json = JSONObject().apply {
                put("otp_code", otp)
                put("contact_point", email)
            }.toString()
            val r = http.post("https://muse.ai/api/auth/native/confirm-otp", h, json)

            if (r.code in 200..299) {
                "OK"
            } else {
                val errBody = if (r.body.length > 100) r.body.substring(0, 100) else r.body
                "HTTP ${r.code} | $errBody"
            }
        } catch (e: Exception) {
            e.message ?: "Error"
        }
    }

    data class HotmailCredentials(
        val email: String,
        val pass: String,
        val refreshToken: String,
        val clientId: String
    )

    data class DongvanOtpResult(
        val ok: Boolean,
        val loginOtp: String = "",
        val allLoginOtps: List<String> = emptyList(),
        val ignoredConfirmCodes: List<String> = emptyList(),
        val totalMessages: Int = 0,
        val error: String = ""
    )

    fun parseHotmailLine(raw: String): HotmailCredentials? {
        val cleaned = raw.trim()
        if (!cleaned.contains("|")) return null
        val parts = cleaned.split("|").map { it.trim() }
        if (parts.size < 4) return null
        val email = parts[0]
        val pass = parts[1]
        val refreshToken = parts[2]
        val clientId = parts[3]
        if (!email.contains("@") || !email.contains(".") || refreshToken.isEmpty() || clientId.isEmpty()) {
            return null
        }
        return HotmailCredentials(email, pass, refreshToken, clientId)
    }

    private fun stripHtmlToText(html: String): String {
        if (html.isEmpty()) return ""
        return html
            .replace(Regex("(?is)<style[^>]*>.*?</style>"), " ")
            .replace(Regex("(?is)<script[^>]*>.*?</script>"), " ")
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ", ignoreCase = true)
            .replace("&amp;", "&", ignoreCase = true)
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun extractDigitsFromLoginEmail(codeField: String, subject: String, plainText: String): String {
        val cleanCode = codeField.trim()
        if (cleanCode.matches(Regex("^\\d{4,8}$"))) return cleanCode

        Regex("(?i)use\\s+code\\s+(\\d{4,8})\\s+to\\s+log\\s+in").find(subject)?.let {
            return it.groupValues[1]
        }
        Regex("(?i)(?:login\\s+code|code)\\s*[:\\s]*(\\d{4,8})").find(subject)?.let {
            return it.groupValues[1]
        }
        Regex("\\b(\\d{6})\\b").find(subject)?.let {
            return it.groupValues[1]
        }

        Regex("(?i)login\\s+code\\s*[:\\s]*(\\d{4,8})").find(plainText)?.let {
            return it.groupValues[1]
        }
        Regex("(?i)log\\s+in\\s+to\\s+your\\s+meta\\s+account\\s+by\\s+entering\\s+the\\s+following\\s+code[^\\d]{0,80}(\\d{4,8})").find(plainText)?.let {
            return it.groupValues[1]
        }
        Regex("\\b(\\d{6})\\b").find(plainText)?.let {
            return it.groupValues[1]
        }
        return ""
    }

    fun fetchMetaLoginOtpFromHotmail(
        creds: HotmailCredentials,
        excludeCodes: Set<String> = emptySet()
    ): DongvanOtpResult {
        return try {
            val http = MetaHttpUtil()
            val h = LinkedHashMap<String, String>()
            h["User-Agent"] = "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Mobile Safari/537.36"
            h["Accept-Encoding"] = "gzip, deflate"
            h["Content-Type"] = "application/json"
            h["sec-ch-ua-platform"] = "\"Android\""
            h["sec-ch-ua"] = "\"Not-A.Brand\";v=\"99\", \"Android WebView\";v=\"157\", \"Chromium\";v=\"157\""
            h["sec-ch-ua-mobile"] = "?1"
            h["origin"] = "https://dongvanfb.net"
            h["x-requested-with"] = "mark.via.gp"
            h["sec-fetch-site"] = "same-site"
            h["sec-fetch-mode"] = "cors"
            h["sec-fetch-dest"] = "empty"
            h["referer"] = "https://dongvanfb.net/"
            h["accept-language"] = "en-US,en;q=0.9"
            h["priority"] = "u=1, i"

            val body = JSONObject().apply {
                put("email", creds.email)
                put("pass", creds.pass)
                put("refresh_token", creds.refreshToken)
                put("client_id", creds.clientId)
            }.toString()

            val r = http.post("https://tools.dongvanfb.net/api/get_messages_oauth2", h, body)
            if (r.code !in 200..299 || r.body.isBlank()) {
                return DongvanOtpResult(ok = false, error = "HTTP ${r.code}")
            }

            val root = JSONObject(r.body)
            val statusOk = root.optBoolean("status", false)
            if (!statusOk) {
                val msg = root.optString("content", root.optString("message", "API status false"))
                return DongvanOtpResult(ok = false, error = msg)
            }

            val messages = root.optJSONArray("messages")
            val total = messages?.length() ?: 0
            val loginOtps = mutableListOf<String>()
            val ignoredConfirm = mutableListOf<String>()

            if (messages != null) {
                for (i in 0 until messages.length()) {
                    val item = messages.optJSONObject(i) ?: continue
                    val from = item.optString("from", "").lowercase(Locale.US)
                    val subject = item.optString("subject", "")
                    val codeField = item.optString("code", "")
                    val rawMsg = item.optString("message", "")
                    val plain = stripHtmlToText(rawMsg)
                    val subjLow = subject.lowercase(Locale.US)
                    val plainLow = plain.lowercase(Locale.US)

                    val isFromMeta = from.contains("meta.com") || from.contains("facebook") ||
                        subjLow.contains("meta") || plainLow.contains("meta account")
                    if (!isFromMeta) continue

                    // Skip "Confirm your email address for your new Meta account" (Confirmation code)
                    val isConfirmEmail = subjLow.contains("confirm your email") ||
                        (plainLow.contains("confirm your email address") && !plainLow.contains("login code"))
                    if (isConfirmEmail) {
                        val confCode = Regex("\\b(\\d{6})\\b").find(plain)?.groupValues?.get(1) ?: ""
                        if (confCode.isNotEmpty()) ignoredConfirm.add(confCode)
                        continue
                    }

                    // Match specifically the Meta Login OTP email ("Use code XXXXXX to log in" / "Login code")
                    val isLoginOtpEmail =
                        (subjLow.contains("use code") && subjLow.contains("log in")) ||
                            subjLow.contains("to log in") ||
                            subjLow.contains("login code") ||
                            plainLow.contains("login code") ||
                            plainLow.contains("log in to your meta account")

                    if (!isLoginOtpEmail) continue

                    val extracted = extractDigitsFromLoginEmail(codeField, subject, plain)
                    if (extracted.isNotEmpty() && !loginOtps.contains(extracted)) {
                        loginOtps.add(extracted)
                    }
                }
            }

            val chosen = loginOtps.firstOrNull { !excludeCodes.contains(it) } ?: ""
            DongvanOtpResult(
                ok = true,
                loginOtp = chosen,
                allLoginOtps = loginOtps,
                ignoredConfirmCodes = ignoredConfirm,
                totalMessages = total
            )
        } catch (e: Exception) {
            DongvanOtpResult(ok = false, error = e.message ?: "Fetch error")
        }
    }
}
