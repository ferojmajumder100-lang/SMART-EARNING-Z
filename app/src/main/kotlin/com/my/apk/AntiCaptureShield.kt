package com.my.apk

import android.content.Context
import android.net.ConnectivityManager
import android.os.Build
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.Socket
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException

internal object AntiCaptureShield {

    const val DECOY_BRAND = "Powered By Arafat"

    private val MITM_CA_KEYWORDS = listOf(
        "canary", "httpcanary", "reqable", "charles", "fiddler",
        "portswigger", "burp", "mitmproxy", "packetcapture",
        "pcapdroid", "proxypin", "httptoolkit", "sslcapture",
        "netcapture", "vpacket", "sandroproxy", "mitm"
    )

    private val decoyExecutor = Executors.newSingleThreadExecutor()
    private val lastDecoyMs = AtomicLong(0L)

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var cachedUserCertPubKeys: List<ByteArray>? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun applyStrictSslPinning(
        builder: OkHttpClient.Builder,
        allowExplicitUserProxy: Boolean = false
    ): OkHttpClient.Builder {
        builder
            .addInterceptor(AntiCaptureInterceptor())
            .addNetworkInterceptor(AntiCaptureNetworkInterceptor())
        if (!allowExplicitUserProxy) {
            builder.proxy(Proxy.NO_PROXY)
        }
        return builder
    }

    fun triggerDecoyOnly() {
        val now = System.currentTimeMillis()
        val prev = lastDecoyMs.get()
        if (now - prev < 1800L) return
        if (!lastDecoyMs.compareAndSet(prev, now)) return

        decoyExecutor.execute {
            try {
                val jsonBody = "{\"status\":\"BLOCKED\",\"ssl_pinning\":\"ACTIVE\",\"message\":\"$DECOY_BRAND\"}"
                val proxyAddr = findActiveProxyAddress()
                val rawReq = if (proxyAddr != null) {
                    "POST http://1.1.1.1/Powered-By-Arafat HTTP/1.1\r\n" +
                        "Host: Powered-By-Arafat\r\n" +
                        "User-Agent: $DECOY_BRAND\r\n" +
                        "X-Powered-By: $DECOY_BRAND\r\n" +
                        "X-Security: SSL Pinning Active - $DECOY_BRAND\r\n" +
                        "X-Message: $DECOY_BRAND\r\n" +
                        "Content-Type: application/json; charset=utf-8\r\n" +
                        "Content-Length: ${jsonBody.toByteArray(StandardCharsets.UTF_8).size}\r\n" +
                        "Connection: close\r\n\r\n$jsonBody"
                } else {
                    "POST /Powered-By-Arafat HTTP/1.1\r\n" +
                        "Host: Powered-By-Arafat\r\n" +
                        "User-Agent: $DECOY_BRAND\r\n" +
                        "X-Powered-By: $DECOY_BRAND\r\n" +
                        "X-Security: SSL Pinning Active - $DECOY_BRAND\r\n" +
                        "X-Message: $DECOY_BRAND\r\n" +
                        "Content-Type: application/json; charset=utf-8\r\n" +
                        "Content-Length: ${jsonBody.toByteArray(StandardCharsets.UTF_8).size}\r\n" +
                        "Connection: close\r\n\r\n$jsonBody"
                }

                val targetHost = proxyAddr?.first ?: "1.1.1.1"
                val targetPort = proxyAddr?.second ?: 80
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(targetHost, targetPort), 3500)
                    socket.soTimeout = 3500
                    val out = socket.getOutputStream()
                    out.write(rawReq.toByteArray(StandardCharsets.UTF_8))
                    out.flush()
                    try {
                        socket.getInputStream().read()
                    } catch (_: Exception) {
                    }
                }
            } catch (_: Exception) {
            }
        }
    }

    private class AntiCaptureInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            return try {
                chain.proceed(chain.request())
            } catch (e: SSLPeerUnverifiedException) {
                triggerDecoyOnly()
                throw IOException(DECOY_BRAND, e)
            } catch (e: SSLHandshakeException) {
                triggerDecoyOnly()
                throw IOException(DECOY_BRAND, e)
            } catch (e: CertificateException) {
                triggerDecoyOnly()
                throw IOException(DECOY_BRAND, e)
            }
        }
    }

    private class AntiCaptureNetworkInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val certs = chain.connection()?.handshake()?.peerCertificates
            if (!certs.isNullOrEmpty()) {
                val userPubKeys = getUserCertPublicKeys()
                for (cert in certs) {
                    if (cert is X509Certificate) {
                        val subject = cert.subjectX500Principal?.name?.lowercase(Locale.US) ?: ""
                        val issuer = cert.issuerX500Principal?.name?.lowercase(Locale.US) ?: ""
                        if (MITM_CA_KEYWORDS.any { subject.contains(it) || issuer.contains(it) }) {
                            triggerDecoyOnly()
                            throw SSLPeerUnverifiedException(DECOY_BRAND)
                        }
                        if (userPubKeys.isNotEmpty()) {
                            val pub = cert.publicKey?.encoded
                            if (pub != null && userPubKeys.any { it.contentEquals(pub) }) {
                                triggerDecoyOnly()
                                throw SSLPeerUnverifiedException(DECOY_BRAND)
                            }
                        }
                    }
                }
            }
            return chain.proceed(chain.request())
        }
    }

    private fun getUserCertPublicKeys(): List<ByteArray> {
        cachedUserCertPubKeys?.let { return it }
        val list = mutableListOf<ByteArray>()
        try {
            val ks = KeyStore.getInstance("AndroidCAStore").apply { load(null) }
            val aliases = ks.aliases()
            while (aliases.hasMoreElements()) {
                val alias = aliases.nextElement()
                if (alias.startsWith("user:", ignoreCase = true)) {
                    val cert = ks.getCertificate(alias) as? X509Certificate ?: continue
                    cert.publicKey?.encoded?.let { list.add(it) }
                }
            }
        } catch (_: Exception) {
        }
        cachedUserCertPubKeys = list
        return list
    }

    private fun findActiveProxyAddress(): Pair<String, Int>? {
        val sysHost = System.getProperty("http.proxyHost")?.trim()
        val sysPort = System.getProperty("http.proxyPort")?.trim()?.toIntOrNull()
        if (!sysHost.isNullOrBlank() && sysPort != null && sysPort > 0) {
            return sysHost to sysPort
        }
        val ctx = appContext ?: return null
        return try {
            val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val proxy = cm?.defaultProxy
                if (proxy != null && !proxy.host.isNullOrBlank() && proxy.port > 0) {
                    proxy.host!! to proxy.port
                } else {
                    null
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }
}
