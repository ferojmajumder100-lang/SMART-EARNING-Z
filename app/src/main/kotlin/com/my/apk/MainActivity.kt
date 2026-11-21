package com.my.apk

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.InputType
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class MainActivity : AppCompatActivity() {

    private var adminGuard: AdminControlManager.GuardHandle? = null

    private lateinit var layoutCreate: View
    private lateinit var layoutMyNumbers: View
    private lateinit var layoutLiveRange: View
    private lateinit var layoutLiveConsole: View
    private lateinit var layoutBgCreate: View
    private lateinit var layoutBgPermLock: View
    private lateinit var scrollBgCreateContent: View
    private lateinit var btnBgCreate: Button
    private lateinit var tvBgOverlayBadge: TextView
    private lateinit var tvBgPermOverlay: TextView
    private lateinit var tvBgPermNotif: TextView
    private lateinit var btnBgGrantPermissions: Button
    private lateinit var tvBgActiveConfig: TextView
    private lateinit var btnBgOverlayStart: Button
    private lateinit var btnBgOverlayStop: Button
    private lateinit var tvBgSizeValue: TextView
    private lateinit var seekBgOverlaySize: SeekBar
    private lateinit var btnBgSizeSmall: Button
    private lateinit var btnBgSizeNormal: Button
    private lateinit var btnBgSizeLarge: Button
    private lateinit var btnBgSizeXl: Button
    private lateinit var tvBgOverlayStatus: TextView

    private lateinit var btnNetAuto: Button
    private lateinit var btnNetProxy: Button
    private lateinit var btnMainProxySetup: Button
    private var networkMode = "AUTO"

    private lateinit var btnSelectLanguage: Button
    private lateinit var btnSelectNamePool: Button
    private lateinit var btnSelectBrowser: Button
    private var selectedLanguage = "Chinese"
    private var selectedNamePool = "Same as Language"
    private var selectedBrowser = "CHROME"

    private lateinit var spinnerEngine: Spinner
    private lateinit var btnNameAuto: Button
    private lateinit var btnNameManual: Button
    private lateinit var btnOpenLiveRange: Button
    private lateinit var btnOpenConsole: Button
    private lateinit var btnCount1: Button
    private lateinit var btnCount3: Button
    private lateinit var btnCount5: Button
    private lateinit var tvSelectedInfo: TextView
    private lateinit var etPassword: EditText
    private lateinit var btnTogglePass: Button
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var btnViewMyNumbers: Button
    private lateinit var btnBack: Button
    private lateinit var btnDeleteAll: Button
    private lateinit var btnMusic: Button
    private lateinit var btnSwitchWork: Button
    private lateinit var tvLogs: TextView
    private lateinit var scrollLogs: ScrollView
    private lateinit var listMyNumbers: LinearLayout

    private lateinit var layoutBatchAccounts: View
    private lateinit var listBatchAccounts: LinearLayout
    private lateinit var btnCopyAllBatch: Button

    private lateinit var btnConsoleBack: Button
    private lateinit var btnConsoleClear: Button
    private lateinit var tvConsoleStats: TextView
    private lateinit var listConsoleHits: LinearLayout
    private lateinit var scrollConsole: ScrollView
    private lateinit var etManualRangeConsole: EditText
    private lateinit var btnManualSelect: Button
    private lateinit var tvConsoleFooter: TextView

    private lateinit var listLiveFacebookRanges: LinearLayout
    private lateinit var tvLiveRangeStats: TextView

    private lateinit var webViewContainer: LinearLayout
    private lateinit var webView: WebView
    private lateinit var btnWvDesktop: Button
    private lateinit var btnWvReload: Button
    private lateinit var btnWvClear: Button
    private lateinit var tvWvStatus: TextView
    private var isDesktopMode = false
    private var isWebViewReady = false

    private var proxyHost = ""
    private var proxyPort = 0
    private var proxyUser = ""
    private var proxyPass = ""

    private var isPasswordVisible = false
    private var mediaPlayer: MediaPlayer? = null
    private var currentSongName = "Song.webm"
    private var isMusicPaused = false

    @Volatile private var isRunning = false

    private val consoleSeenIds = mutableSetOf<String>()
    private val consoleHits = mutableListOf<ConsoleHit>()
    private val liveFacebookRanges = mutableListOf<String>()
    private var consoleTotalSeen = 0
    private var consoleLastUpdate = "-"
    @Volatile private var isConsolePolling = false
    private var consolePollThread: Thread? = null

    private var nameMode = "AUTO"
    private var manualFirstName = ""
    private var manualLastName = ""
    private var selectedRange = ""
    private var selectedAccountCount = 1

    private val panelUsed = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val savedAccounts = mutableListOf<SavedAccount>()
    private val currentBatchAccounts = mutableListOf<SavedAccount>()

    data class ConsoleHit(val sid: String, val range: String, val time: Long, val message: String, val code: String)
    data class SavedAccount(val name: String, val number: String, val uid: String, val password: String, val cookie: String, var otpStatus: String = "None", var otpExpireTime: Long = 0L)

    companion object {
        private const val POLL_INTERVAL_MS = 5000L

        private const val GRAPHQL_URL = "https://www.facebook.com/api/graphql/"
        private const val LSD = "AdSrL5Ofo-_K7SUpB60P3tXh548"
        private const val JAZOEST = "22101"
        private const val DOC_ID_SEARCH = "29078541985133425"
        private const val DOC_ID_REGISTER = "27029416779977343"
        private const val CIPHER_TEXT = "AdB08Hzv-9jCoDzhFZZOGM-mu7GzZqIrKW2shgrG2EiKQjM65nlFGvfEdSi9HqSbAP0KQBcRcLI-dLGyJYtL5_6sTxWFsJwIzD62rLTeh8BIhxInM4xwO-ofqoCAXf9bv02Q4Q0AL99L4Ni1d9jvD1eOTND9NtgbugxGI1rqdVoihkdYyUWxkVuxYtqKGMQlEDLGb-jtkDXLfOQHjK4-3ePDlZXS4QH0WW5j_3tNwQIVEyexmzVTDNI4PtceU8yh1LQg8hdw2JtJ7w8u4B_sIwDfk9K"

        private const val MAX_NUM_ATTEMPTS = 30
        private const val OTP_WATCH_MS = 20 * 60 * 1000L
        private const val MAX_BATCH_PARALLEL = 3

        private const val CHROME_MOBILE_UA = "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Mobile Safari/537.36"
        private const val CHROME_DESKTOP_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/150.0.0.0 Safari/537.36"

        val LANGUAGE_OPTIONS = listOf(
            "Chinese" to "zh_CN", "Bengali" to "bn_BD", "Hindi" to "hi_IN",
            "English US" to "en_US", "English UK" to "en_GB",
            "Arabic" to "ar_AR", "French" to "fr_FR",
            "Cameroon (French)" to "fr_CM", "Cameroon (English)" to "en_CM",
            "Spanish (ES)" to "es_ES", "Spanish (LA)" to "es_LA",
            "Japanese" to "ja_JP", "Korean" to "ko_KR", "Dutch" to "nl_NL",
            "German" to "de_DE", "Italian" to "it_IT",
            "Portuguese BR" to "pt_BR", "Portuguese PT" to "pt_PT",
            "Russian" to "ru_RU", "Turkish" to "tr_TR",
            "Thai" to "th_TH", "Vietnamese" to "vi_VN",
            "Indonesian" to "id_ID", "Malay" to "ms_MY",
            "Filipino" to "tl_PH", "Swahili" to "sw_KE",
            "Afrikaans" to "af_ZA", "Hausa" to "ha_NG",
            "Yoruba" to "yo_NG", "Zulu" to "zu_ZA",
            "Persian" to "fa_IR", "Urdu" to "ur_PK",
            "Nepali" to "ne_NP", "Sinhala" to "si_LK",
            "Tamil" to "ta_IN", "Telugu" to "te_IN",
            "Marathi" to "mr_IN", "Gujarati" to "gu_IN",
            "Polish" to "pl_PL", "Romanian" to "ro_RO",
            "Greek" to "el_GR", "Czech" to "cs_CZ",
            "Hungarian" to "hu_HU", "Ukrainian" to "uk_UA",
            "Swedish" to "sv_SE", "Norwegian" to "nb_NO",
            "Danish" to "da_DK", "Finnish" to "fi_FI"
        )

        val NAME_POOL_OPTIONS = listOf(
            "Same as Language",
            "Chinese", "Bengali", "Hindi",
            "English", "Arabic", "French", "Spanish",
            "Japanese", "Korean", "Russian", "Portuguese",
            "Turkish", "Thai", "Vietnamese", "Indonesian",
            "Italian", "German", "Dutch"
        )

        private val CHINESE_FIRST = arrayOf(
            "雅婷","雅君","雅涵","雅萱","雅妤","雅甄","雅玲","雅芬","雅儀","雅柔",
            "雅蓉","雅瑄","雅穎","雅琪","雅晴","雅雯","雅潔","雅瑜","雅蓁","雅芸",
            "怡婷","怡君","怡涵","怡萱","怡妤","怡甄","怡玲","怡芬","怡儀","怡柔"
        )
        private val CHINESE_LAST = arrayOf("池")
        private val BENGALI_FIRST = arrayOf("Nusrat","Sadia","Mim","Tania","Sumaiya","Jannatul","Farzana","Sanjida","Tamanna","Mahiya","Lamia","Samia","Tasnim","Sabrina","Faria","Riya","Priya","Anika","Maliha","Humaira")
        private val BENGALI_LAST = arrayOf("Akter","Jahan","Islam","Khatun","Sultana","Rahman","Khan","Chowdhury","Begum","Mim")
        private val ARABIC_FIRST = arrayOf("Fatima","Aisha","Maryam","Zainab","Khadija","Amina","Noor","Layla","Sara","Huda","Rania","Hana","Yasmin","Salma","Dina","Lina","Maya","Rana","Samira","Farida")
        private val ARABIC_LAST = arrayOf("Al-Sayed","Al-Ahmad","Al-Hassan","Al-Ibrahim","Al-Rashid","Hassan","Ibrahim","Ahmed","Mahmoud","Ali")
        private val HINDI_FIRST = arrayOf("Priya","Anjali","Neha","Pooja","Shreya","Aarti","Kavita","Divya","Sakshi","Meera","Radha","Sneha","Nisha","Ritu","Ishita","Aditi","Ananya","Riya","Tanvi","Pari")
        private val HINDI_LAST = arrayOf("Sharma","Verma","Patel","Gupta","Singh","Kumar","Reddy","Nair","Iyer","Joshi")
        private val JAPANESE_FIRST = arrayOf("Yuki","Sakura","Hana","Aoi","Yui","Rin","Mei","Haruka","Ayaka","Nanami","Riko","Miyu","Kana","Mio","Yuna","Hinata","Akari","Miu","Sora","Kaede")
        private val JAPANESE_LAST = arrayOf("Sato","Suzuki","Takahashi","Tanaka","Watanabe","Ito","Yamamoto","Nakamura","Kobayashi","Kato")
        private val KOREAN_FIRST = arrayOf("Minji","Jiwoo","Seoyeon","Hayoon","Sua","Jiho","Yerin","Chaewon","Somin","Yuna","Hana","Jiyu","Soyul","Naeun","Hyejin","Eunji","Yujin","Dahye","Hani")
        private val KOREAN_LAST = arrayOf("Kim","Lee","Park","Choi","Jung","Kang","Cho","Yoon","Jang","Lim")
        private val RUSSIAN_FIRST = arrayOf("Anastasia","Ekaterina","Maria","Anna","Olga","Tatiana","Natalia","Irina","Elena","Svetlana","Yulia","Ksenia","Daria","Vera","Alina","Polina","Sofia","Viktoria")
        private val RUSSIAN_LAST = arrayOf("Ivanova","Petrova","Sidorova","Smirnova","Kuznetsova","Popova","Vasilyeva","Sokolova","Mikhailova","Novikova")
        private val SPANISH_FIRST = arrayOf("Maria","Sofia","Valentina","Camila","Isabella","Lucia","Martina","Daniela","Gabriela","Paula","Emma","Renata","Ximena","Antonella","Fernanda","Andrea","Carolina","Natalia","Valeria","Adriana")
        private val SPANISH_LAST = arrayOf("Garcia","Rodriguez","Martinez","Lopez","Gonzalez","Perez","Sanchez","Ramirez","Torres","Flores")
        private val FRENCH_FIRST = arrayOf("Camille","Lea","Manon","Chloe","Ines","Sarah","Louise","Emma","Jade","Alice","Lina","Rose","Anna","Juliette","Mathilde","Clara","Margaux","Charlotte","Eva","Zoe")
        private val FRENCH_LAST = arrayOf("Martin","Bernard","Dubois","Thomas","Robert","Richard","Petit","Durand","Leroy","Moreau")
        private val GERMAN_FIRST = arrayOf("Anna","Lena","Sophie","Marie","Emma","Mia","Hannah","Lea","Lina","Julia","Laura","Sarah","Lisa","Katharina","Johanna","Clara","Amelie","Leonie","Nina","Paula")
        private val GERMAN_LAST = arrayOf("Muller","Schmidt","Schneider","Fischer","Weber","Meyer","Wagner","Becker","Schulz","Hoffmann")
        private val PORTUGUESE_FIRST = arrayOf("Ana","Julia","Mariana","Beatriz","Larissa","Camila","Fernanda","Gabriela","Leticia","Sofia","Carolina","Isabela","Luana","Rafaela","Natalia","Bruna","Amanda","Jessica","Vitoria","Helena")
        private val PORTUGUESE_LAST = arrayOf("Silva","Santos","Oliveira","Souza","Rodrigues","Ferreira","Alves","Pereira","Lima","Gomes")
        private val TURKISH_FIRST = arrayOf("Ayse","Fatma","Emine","Hatice","Zeynep","Elif","Meryem","Sultan","Zehra","Havva","Esra","Derya","Merve","Buse","Ebru","Gul","Selin","Seda","Pinar","Yasemin")
        private val TURKISH_LAST = arrayOf("Yilmaz","Kaya","Demir","Sahin","Celik","Yildiz","Yildirim","Ozturk","Aydin","Ozdemir")
        private val THAI_FIRST = arrayOf("Siriporn","Nattaya","Pimchan","Kanya","Ploy","Mint","Aom","Fah","Nam","Jane","Prim","Kaimook","Fern","Ying","Bam","May","Pang","Wan","Praew","Fon")
        private val THAI_LAST = arrayOf("Srisuwan","Chaiyasit","Wongsuwan","Sukjai","Rattanakorn","Chaisuwan","Saengthong","Phumiphat","Kittisak","Boonsri")
        private val VIETNAMESE_FIRST = arrayOf("Linh","Huong","Mai","Lan","Hoa","Thao","Nhung","Yen","Trang","Phuong","Hanh","Ngoc","Thu","Hien","Diep","Quynh","Ngan","Nhi","Tien","My")
        private val VIETNAMESE_LAST = arrayOf("Nguyen","Tran","Le","Pham","Hoang","Huynh","Phan","Vu","Vo","Dang")
        private val INDONESIAN_FIRST = arrayOf("Siti","Dewi","Nur","Aisyah","Putri","Ayu","Intan","Rina","Lina","Ratu","Fitri","Sari","Indah","Wulan","Maya","Dian","Ratna","Yuni","Nadia","Anisa")
        private val INDONESIAN_LAST = arrayOf("Wijaya","Santoso","Kusuma","Saputra","Hidayat","Nugroho","Setiawan","Pratama","Gunawan","Siregar")
        private val ENGLISH_FIRST = arrayOf("Emma","Olivia","Ava","Isabella","Sophia","Mia","Charlotte","Amelia","Harper","Evelyn","Abigail","Emily","Ella","Elizabeth","Camila","Luna","Sofia","Avery","Mila","Aria")
        private val ENGLISH_LAST = arrayOf("Smith","Johnson","Williams","Brown","Jones","Garcia","Miller","Davis","Rodriguez","Martinez")
        private val ITALIAN_FIRST = arrayOf("Sofia","Giulia","Aurora","Alice","Ginevra","Emma","Giorgia","Greta","Beatrice","Anna","Chiara","Viola","Matilde","Ludovica","Azzurra","Camilla","Alessia","Sara","Martina","Francesca")
        private val ITALIAN_LAST = arrayOf("Rossi","Russo","Ferrari","Esposito","Bianchi","Romano","Colombo","Ricci","Marino","Greco")
        private val DUTCH_FIRST = arrayOf("Emma","Julia","Sophie","Anna","Lotte","Fenna","Saar","Lieke","Sanne","Maud","Tess","Eva","Nina","Mila","Yara","Isa","Nora","Zoe","Vera")
        private val DUTCH_LAST = arrayOf("De Jong","Jansen","De Vries","Van den Berg","Van Dijk","Bakker","Janssen","Visser","Smit","Meijer")

        private const val SEARCH_UA = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"
        private const val V3_LSD = "AdQBHXQf_CpDXYrPBnVibCGFQyU"
        private const val V3_JAZOEST = "22328"
        private const val V3_DATR = "Je3VakJWee5EYBLobwNP5C7b"
        private const val V3_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"
        private const val V4_LSD = "AdStgBFyiAViKGwS0THdixCPl0A"
        private const val V4_JAZOEST = "22330"
        private const val V4_DATR = "qqjpai3Wl39mKc84PgZyHEKr"
        private const val V4_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"
    }

    private fun getNamePool(poolName: String): Pair<Array<String>, Array<String>> {
        val key = if (poolName == "Same as Language") selectedLanguage else poolName
        return when (key) {
            "Chinese" -> CHINESE_FIRST to CHINESE_LAST
            "Bengali" -> BENGALI_FIRST to BENGALI_LAST
            "Arabic" -> ARABIC_FIRST to ARABIC_LAST
            "Hindi" -> HINDI_FIRST to HINDI_LAST
            "Japanese" -> JAPANESE_FIRST to JAPANESE_LAST
            "Korean" -> KOREAN_FIRST to KOREAN_LAST
            "Russian" -> RUSSIAN_FIRST to RUSSIAN_LAST
            "Spanish (ES)", "Spanish (LA)", "Spanish" -> SPANISH_FIRST to SPANISH_LAST
            "French", "Cameroon (French)" -> FRENCH_FIRST to FRENCH_LAST
            "German" -> GERMAN_FIRST to GERMAN_LAST
            "Portuguese BR", "Portuguese PT", "Portuguese" -> PORTUGUESE_FIRST to PORTUGUESE_LAST
            "Turkish" -> TURKISH_FIRST to TURKISH_LAST
            "Thai" -> THAI_FIRST to THAI_LAST
            "Vietnamese" -> VIETNAMESE_FIRST to VIETNAMESE_LAST
            "Indonesian" -> INDONESIAN_FIRST to INDONESIAN_LAST
            "Italian" -> ITALIAN_FIRST to ITALIAN_LAST
            "Dutch" -> DUTCH_FIRST to DUTCH_LAST
            "English", "English US", "English UK" -> ENGLISH_FIRST to ENGLISH_LAST
            else -> ENGLISH_FIRST to ENGLISH_LAST
        }
    }

    private fun getLanguageCodeFor(languageName: String): String =
        LANGUAGE_OPTIONS.find { it.first == languageName }?.second ?: "en_US"

    private fun buildAcceptLanguageFor(langCode: String): String {
        if (langCode == "en_US") return "en-US,en;q=0.9"
        val dashed = langCode.replace("_", "-")
        val base = dashed.substringBefore("-").lowercase()
        return "$dashed,$base;q=0.9,en-US;q=0.8,en;q=0.7"
    }

    private fun buildInsecureClient(jar: CookieJar?, useProxy: Boolean = false): OkHttpClient {
        val explicitProxy = useProxy && proxyHost.isNotEmpty() && proxyPort > 0
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(35, TimeUnit.SECONDS)
            .writeTimeout(35, TimeUnit.SECONDS)
        AntiCaptureShield.applyStrictSslPinning(builder, allowExplicitUserProxy = explicitProxy)
        if (explicitProxy) {
            val prx = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
            builder.proxy(prx)
            if (proxyUser.isNotEmpty() && proxyPass.isNotEmpty()) {
                builder.proxyAuthenticator { _, response ->
                    if (response.request.header("Proxy-Authorization") != null) return@proxyAuthenticator null
                    val credential = Credentials.basic(proxyUser, proxyPass)
                    response.request.newBuilder().header("Proxy-Authorization", credential).build()
                }
            }
        }
        if (jar != null) builder.cookieJar(jar)
        return builder.build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkForGitHubUpdate()
        bindViews()
        loadSavedAccounts()
        loadProxySettings()
        loadBrowserSettings()
        loadLanguageSettings()
        loadNamePoolSettings()
        setupWebView()

        updateNetworkModeButtons()
        updateBrowserModeUI()
        updateLanguageUI()
        updateNamePoolUI()
        updateSelectedInfoUI()
        updateNameModeButtons()
        updateAccountCountButtons()
        updateMusicButtonUI()

        BgOverlayService.onTriggerCreate = { onStatus -> createOneAccountFromBgOverlay(onStatus) }
        BgOverlayService.onUiStateChanged = { runOnUiThread { updateBgCreateUI() } }

        val savedOverlayPct = BgOverlayService.loadSavedSizePercent(this)
        seekBgOverlaySize.progress = (savedOverlayPct - 60).coerceIn(0, 120)
        updateBgSizeUI(savedOverlayPct)
        updateBgCreateUI()

        wireListeners()
        showPage("setup")

        adminGuard = AdminControlManager.attachActivityGuard(
            activity = this,
            workKey = AdminControlManager.WORK_FACEBOOK,
            allowSwitchWork = true
        )
    }

    override fun onResume() {
        super.onResume()
        AdminControlManager.refreshNow()
        BgOverlayService.onTriggerCreate = { onStatus -> createOneAccountFromBgOverlay(onStatus) }
        BgOverlayService.onUiStateChanged = { runOnUiThread { updateBgCreateUI() } }
        updateBgCreateUI()
    }

    private fun bindViews() {
        layoutCreate       = findViewById(R.id.layoutCreate)
        layoutMyNumbers    = findViewById(R.id.layoutMyNumbers)
        layoutLiveRange    = findViewById(R.id.layoutLiveRange)
        layoutLiveConsole  = findViewById(R.id.layoutLiveConsole)
        layoutBgCreate     = findViewById(R.id.layoutBgCreate)
        layoutBgPermLock   = findViewById(R.id.layoutBgPermLock)
        scrollBgCreateContent = findViewById(R.id.scrollBgCreateContent)
        btnBgCreate        = findViewById(R.id.btnBgCreate)
        tvBgOverlayBadge   = findViewById(R.id.tvBgOverlayBadge)
        tvBgPermOverlay    = findViewById(R.id.tvBgPermOverlay)
        tvBgPermNotif      = findViewById(R.id.tvBgPermNotif)
        btnBgGrantPermissions = findViewById(R.id.btnBgGrantPermissions)
        tvBgActiveConfig   = findViewById(R.id.tvBgActiveConfig)
        btnBgOverlayStart  = findViewById(R.id.btnBgOverlayStart)
        btnBgOverlayStop   = findViewById(R.id.btnBgOverlayStop)
        tvBgSizeValue      = findViewById(R.id.tvBgSizeValue)
        seekBgOverlaySize  = findViewById(R.id.seekBgOverlaySize)
        btnBgSizeSmall     = findViewById(R.id.btnBgSizeSmall)
        btnBgSizeNormal    = findViewById(R.id.btnBgSizeNormal)
        btnBgSizeLarge     = findViewById(R.id.btnBgSizeLarge)
        btnBgSizeXl        = findViewById(R.id.btnBgSizeXl)
        tvBgOverlayStatus  = findViewById(R.id.tvBgOverlayStatus)

        btnNetAuto         = findViewById(R.id.btnNetAuto)
        btnNetProxy        = findViewById(R.id.btnNetProxy)
        btnMainProxySetup  = findViewById(R.id.btnMainProxySetup)
        btnSelectLanguage  = findViewById(R.id.btnSelectLanguage)
        btnSelectNamePool  = findViewById(R.id.btnSelectNamePool)
        btnSelectBrowser   = findViewById(R.id.btnSelectBrowser)
        spinnerEngine      = findViewById(R.id.spinnerEngine)
        btnNameAuto        = findViewById(R.id.btnNameAuto)
        btnNameManual      = findViewById(R.id.btnNameManual)
        btnOpenLiveRange   = findViewById(R.id.btnOpenLiveRange)
        btnOpenConsole     = findViewById(R.id.btnOpenConsole)
        btnCount1          = findViewById(R.id.btnCount1)
        btnCount3          = findViewById(R.id.btnCount3)
        btnCount5          = findViewById(R.id.btnCount5)
        tvSelectedInfo     = findViewById(R.id.tvSelectedInfo)
        etPassword         = findViewById(R.id.etPassword)
        btnTogglePass      = findViewById(R.id.btnTogglePass)
        btnStart           = findViewById(R.id.btnStart)
        btnStop            = findViewById(R.id.btnStop)
        btnViewMyNumbers   = findViewById(R.id.btnViewMyNumbers)
        btnBack            = findViewById(R.id.btnBack)
        btnDeleteAll       = findViewById(R.id.btnDeleteAll)
        btnMusic           = findViewById(R.id.btnMusic)
        btnSwitchWork      = findViewById(R.id.btnSwitchWork)
        tvLogs             = findViewById(R.id.tvLogs)
        scrollLogs         = findViewById(R.id.scrollLogs)
        listMyNumbers      = findViewById(R.id.listMyNumbers)

        layoutBatchAccounts = findViewById(R.id.layoutBatchAccounts)
        listBatchAccounts   = findViewById(R.id.listBatchAccounts)
        btnCopyAllBatch     = findViewById(R.id.btnCopyAllBatch)

        btnConsoleBack      = findViewById(R.id.btnConsoleBack)
        btnConsoleClear     = findViewById(R.id.btnConsoleClear)
        tvConsoleStats      = findViewById(R.id.tvConsoleStats)
        listConsoleHits     = findViewById(R.id.listConsoleHits)
        scrollConsole       = findViewById(R.id.scrollConsole)
        etManualRangeConsole = findViewById(R.id.etManualRangeConsole)
        btnManualSelect     = findViewById(R.id.btnManualSelect)
        tvConsoleFooter     = findViewById(R.id.tvConsoleFooter)

        listLiveFacebookRanges = findViewById(R.id.listLiveFacebookRanges)
        tvLiveRangeStats       = findViewById(R.id.tvLiveRangeStats)

        webViewContainer    = findViewById(R.id.webViewContainer)
        webView             = findViewById(R.id.webView)
        btnWvDesktop        = findViewById(R.id.btnWvDesktop)
        btnWvReload         = findViewById(R.id.btnWvReload)
        btnWvClear          = findViewById(R.id.btnWvClear)
        tvWvStatus          = findViewById(R.id.tvWvStatus)

        spinnerEngine.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            arrayOf("V1 Limited", "V2 Stander", "V3 Graphql")
        )
    }

    private fun wireListeners() {
        btnBack.setOnClickListener            { showPage("setup") }
        btnViewMyNumbers.setOnClickListener   { showPage("create") }
        btnOpenLiveRange.setOnClickListener   { showPage("liverange"); startConsolePolling() }
        btnOpenConsole.setOnClickListener     { showPage("otp") }
        btnBgCreate.setOnClickListener        { showPage("bgcreate") }

        btnBgGrantPermissions.setOnClickListener { requestAllBgPermissions() }
        btnBgOverlayStart.setOnClickListener     { startBgOverlay() }
        btnBgOverlayStop.setOnClickListener      { stopBgOverlay() }
        btnBgSizeSmall.setOnClickListener        { applyBgSizePreset(75) }
        btnBgSizeNormal.setOnClickListener       { applyBgSizePreset(100) }
        btnBgSizeLarge.setOnClickListener        { applyBgSizePreset(130) }
        btnBgSizeXl.setOnClickListener           { applyBgSizePreset(160) }
        seekBgOverlaySize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val pct = (progress + 60).coerceIn(60, 180)
                updateBgSizeUI(pct)
                BgOverlayService.updateSizeRealtime(this@MainActivity, pct)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnSelectLanguage.setOnClickListener  { showLanguageDialog() }
        btnSelectNamePool.setOnClickListener  { showNamePoolDialog() }
        btnSelectBrowser.setOnClickListener   { showSelectBrowserDialog() }
        btnNetAuto.setOnClickListener         { networkMode = "AUTO"; updateNetworkModeButtons(); updateSelectedInfoUI() }
        btnNetProxy.setOnClickListener        { networkMode = "PROXY"; updateNetworkModeButtons(); updateSelectedInfoUI() }
        btnMainProxySetup.setOnClickListener  { showProxyDialog() }
        btnNameAuto.setOnClickListener        { nameMode = "AUTO"; updateNameModeButtons(); updateSelectedInfoUI() }
        btnNameManual.setOnClickListener      { showManualNameDialog() }
        btnCount1.setOnClickListener          { setAccountCount(1) }
        btnCount3.setOnClickListener          { setAccountCount(3) }
        btnCount5.setOnClickListener          { setAccountCount(5) }
        btnTogglePass.setOnClickListener      { togglePasswordVisibility() }
        btnMusic.setOnClickListener           { showMusicDialog() }
        btnSwitchWork.setOnClickListener      {
            startActivity(
                Intent(this, WorkSelectorActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            )
        }

        btnStart.setOnClickListener           { onStartProcess() }
        btnStop.setOnClickListener            { onStopProcess() }
        btnDeleteAll.setOnClickListener       { confirmDeleteAll() }
        btnCopyAllBatch.setOnClickListener    { copyAllBatchNumbers() }

        btnConsoleClear.setOnClickListener    {
            consoleHits.clear(); consoleSeenIds.clear(); consoleTotalSeen = 0
            renderConsoleHits(); updateConsoleStats()
        }
        btnConsoleBack.setOnClickListener     { showPage("setup") }
        btnManualSelect.setOnClickListener    { useManualRangeFromConsole() }

        btnWvDesktop.setOnClickListener       { toggleWebViewDesktop() }
        btnWvReload.setOnClickListener        { reloadWebView() }
        btnWvClear.setOnClickListener         { clearWebView() }
    }

    private fun showPage(target: String) {
        layoutCreate.visibility      = if (target == "setup")     View.VISIBLE else View.GONE
        layoutMyNumbers.visibility   = if (target == "create")    View.VISIBLE else View.GONE
        layoutLiveRange.visibility   = if (target == "liverange") View.VISIBLE else View.GONE
        layoutLiveConsole.visibility = if (target == "otp")       View.VISIBLE else View.GONE
        layoutBgCreate.visibility    = if (target == "bgcreate")  View.VISIBLE else View.GONE

        btnBack.setBackgroundResource(if (target == "setup") R.drawable.bg_nav_active_blue else R.drawable.bg_nav_inactive)
        btnViewMyNumbers.setBackgroundResource(if (target == "create") R.drawable.bg_nav_active_emerald else R.drawable.bg_nav_inactive)
        btnOpenLiveRange.setBackgroundResource(if (target == "liverange") R.drawable.bg_nav_active_violet else R.drawable.bg_nav_inactive)
        btnOpenConsole.setBackgroundResource(if (target == "otp") R.drawable.bg_nav_active_amber else R.drawable.bg_nav_inactive)
        btnBgCreate.setBackgroundResource(if (target == "bgcreate") R.drawable.bg_nav_active_teal else R.drawable.bg_nav_inactive)

        btnBack.setTextColor(Color.parseColor(if (target == "setup") "#FFFFFF" else "#60A5FA"))
        btnViewMyNumbers.setTextColor(Color.parseColor(if (target == "create") "#041E15" else "#6EE7B7"))
        btnOpenLiveRange.setTextColor(Color.parseColor(if (target == "liverange") "#FFFFFF" else "#C4B5FD"))
        btnOpenConsole.setTextColor(Color.parseColor(if (target == "otp") "#FFFFFF" else "#FCD34D"))
        btnBgCreate.setTextColor(Color.parseColor(if (target == "bgcreate") "#FFFFFF" else "#5EEAD4"))

        if (target == "create") {
            renderBatchAccounts()
        }
        if (target == "otp") {
            renderMyNumbersList()
        }
        if (target == "bgcreate") {
            updateBgCreateUI()
            if (!hasAllBgPermissions()) {
                requestAllBgPermissions()
            }
        }
    }

    private fun hasOverlayPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(this)
    }

    private fun hasNotifPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasAllBgPermissions(): Boolean = hasOverlayPermission() && hasNotifPermission()

    private fun requestAllBgPermissions() {
        if (!hasNotifPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 4091)
            return
        }
        if (!hasOverlayPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(intent)
                Toast.makeText(this, "Enable 'Display over other apps' permission", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open overlay settings", Toast.LENGTH_SHORT).show()
            }
            return
        }
        updateBgCreateUI()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 4091) {
            updateBgCreateUI()
            if (!hasOverlayPermission() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } catch (_: Exception) {}
            }
        }
    }

    private fun applyBgSizePreset(percent: Int) {
        val clamped = percent.coerceIn(60, 180)
        seekBgOverlaySize.progress = clamped - 60
        updateBgSizeUI(clamped)
        BgOverlayService.updateSizeRealtime(this, clamped)
    }

    private fun updateBgSizeUI(percent: Int) {
        tvBgSizeValue.text = "$percent%"
        fun stylePreset(btn: Button, active: Boolean) {
            btn.setBackgroundResource(if (active) R.drawable.bg_btn_solid_violet else R.drawable.bg_btn_unselected)
            btn.setTextColor(Color.parseColor(if (active) "#FFFFFF" else "#CBD5E1"))
        }
        stylePreset(btnBgSizeSmall, percent in 60..87)
        stylePreset(btnBgSizeNormal, percent in 88..114)
        stylePreset(btnBgSizeLarge, percent in 115..145)
        stylePreset(btnBgSizeXl, percent >= 146)
    }

    private fun updateBgCreateUI() {
        val overlayOk = hasOverlayPermission()
        val notifOk = hasNotifPermission()
        val allOk = overlayOk && notifOk

        layoutBgPermLock.visibility = if (allOk) View.GONE else View.VISIBLE
        scrollBgCreateContent.visibility = if (allOk) View.VISIBLE else View.GONE

        tvBgPermOverlay.text = if (overlayOk) "✅ Overlay Permission: Granted" else "⚠ Overlay Permission: Required"
        tvBgPermOverlay.setTextColor(Color.parseColor(if (overlayOk) "#34D399" else "#FDA4AF"))
        tvBgPermNotif.text = if (notifOk) "✅ Notification Permission: Granted" else "⚠ Notification Permission: Required"
        tvBgPermNotif.setTextColor(Color.parseColor(if (notifOk) "#34D399" else "#FDA4AF"))

        val running = BgOverlayService.isRunning
        tvBgOverlayBadge.text = if (running) "ACTIVE" else "STOPPED"
        tvBgOverlayBadge.setBackgroundResource(if (running) R.drawable.bg_badge_green else R.drawable.bg_badge_rose)
        tvBgOverlayBadge.setTextColor(Color.parseColor(if (running) "#34D399" else "#FDA4AF"))
        tvBgOverlayStatus.text = BgOverlayService.lastStatus

        val engineLabel = when (spinnerEngine.selectedItemPosition) {
            0 -> "V1 Limited"
            1 -> "V2 Stander"
            else -> "V3 Graphql"
        }
        val pw = etPassword.text.toString().ifEmpty { "arafat1234" }
        val nameSummary = if (nameMode == "MANUAL") "$manualFirstName $manualLastName (Manual)" else "$selectedNamePool (Auto)"
        val rangeSummary = if (selectedRange.isEmpty()) "Not selected (select in Range)" else "$selectedRange (${CountryLookup.formatLabel(selectedRange)})"
        tvBgActiveConfig.text =
            "Range: $rangeSummary\nEngine: $engineLabel | Lang: $selectedLanguage\nName: $nameSummary\nPassword: $pw | Mode: $networkMode"
    }

    private fun startBgOverlay() {
        if (!hasNotifPermission() || !hasOverlayPermission()) {
            requestAllBgPermissions()
            return
        }
        if (selectedRange.isEmpty()) {
            Toast.makeText(this, "Select range in Range/Console first!", Toast.LENGTH_LONG).show()
            return
        }
        if (networkMode == "PROXY" && (proxyHost.isEmpty() || proxyPort == 0)) {
            Toast.makeText(this, "Set proxy first!", Toast.LENGTH_SHORT).show()
            showProxyDialog()
            return
        }
        BgOverlayService.onTriggerCreate = { onStatus -> createOneAccountFromBgOverlay(onStatus) }
        val intent = Intent(this, BgOverlayService::class.java).apply { action = BgOverlayService.ACTION_START }
        ContextCompat.startForegroundService(this, intent)
        Toast.makeText(this, "Overlay Create started!", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ updateBgCreateUI() }, 250)
    }

    private fun stopBgOverlay() {
        try {
            stopService(Intent(this, BgOverlayService::class.java))
        } catch (_: Exception) {}
        Toast.makeText(this, "Overlay Create stopped", Toast.LENGTH_SHORT).show()
        Handler(Looper.getMainLooper()).postDelayed({ updateBgCreateUI() }, 200)
    }

    private fun createOneAccountFromBgOverlay(onStatus: (String, Boolean) -> Unit) {
        val range = selectedRange
        if (range.isEmpty()) {
            runOnUiThread { Toast.makeText(this, "Select range first!", Toast.LENGTH_LONG).show() }
            onStatus("⚠ Select range in Range/Console first", false)
            return
        }
        if (networkMode == "PROXY" && (proxyHost.isEmpty() || proxyPort == 0)) {
            runOnUiThread { Toast.makeText(this, "Set proxy first!", Toast.LENGTH_LONG).show() }
            onStatus("⚠ Proxy not configured", false)
            return
        }

        val customFirst = if (nameMode == "MANUAL") manualFirstName else null
        val customLast = if (nameMode == "MANUAL") manualLastName else null
        val password = etPassword.text.toString().ifEmpty { "arafat1234" }
        val version = when (spinnerEngine.selectedItemPosition) {
            0 -> "v1"
            1 -> "v2"
            else -> "v3"
        }
        val language = selectedLanguage
        val namePool = selectedNamePool
        val useProxyForCreation = (networkMode == "PROXY")

        onStatus("🔍 Finding free number ($range)...", true)
        Thread {
            val tag = "[BG-OVERLAY]"
            try {
                appendLog("\n$tag 🔍 Checking free number in $range...")
                var freePhone: String? = null
                for (attempt in 1..MAX_NUM_ATTEMPTS) {
                    if (!BgOverlayService.isRunning) {
                        appendLog("$tag ⏹ Overlay stopped")
                        onStatus("Overlay stopped", false)
                        return@Thread
                    }
                    val nd = fetchLiveNumber(range)
                    if (nd == null) { Thread.sleep(800); continue }
                    val fullNumber = nd.optString("full_number", "").trim().trimStart('+').replace(Regex("\\D"), "")
                    if (fullNumber.isEmpty() || fullNumber in panelUsed) { Thread.sleep(600); continue }
                    val (status, desc) = checkNumberGraphql(fullNumber)
                    when (status) {
                        "free"  -> { panelUsed.add(fullNumber); appendLog("$tag ✅ FREE: +$fullNumber"); freePhone = fullNumber }
                        "taken" -> { panelUsed.add(fullNumber); appendLog("$tag ⏭ Taken: +$fullNumber"); Thread.sleep(500) }
                        else    -> { appendLog("$tag ⚠ Error: $desc"); Thread.sleep(700) }
                    }
                    if (freePhone != null) break
                }

                if (freePhone == null) {
                    appendLog("$tag ✗ No free number")
                    runOnUiThread { Toast.makeText(this@MainActivity, "No free number found", Toast.LENGTH_SHORT).show() }
                    onStatus("✗ No free number found", false)
                    return@Thread
                }

                onStatus("🏗 Creating +$freePhone ($version)...", true)
                appendLog("$tag 🏗 Creating +$freePhone | Engine=$version | lang=$language")
                val res = createAccount(version, language, namePool, freePhone, password, customFirst, customLast, useProxy = useProxyForCreation)

                if (res.optBoolean("ok", false)) {
                    val uid = res.optString("uid")
                    val cookie = res.optString("cookie")
                    val name = res.optString("name")
                    appendLog("$tag ✓ CREATED: $name | +$freePhone | UID: $uid")
                    val newAcc = SavedAccount(name, freePhone, uid, password, cookie)
                    synchronized(savedAccounts) { savedAccounts.add(0, newAcc); saveAccountsToPrefs() }
                    synchronized(currentBatchAccounts) { currentBatchAccounts.add(0, newAcc) }
                    runOnUiThread {
                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("Number", "+$freePhone"))
                        Toast.makeText(this@MainActivity, "Number Copied: +$freePhone", Toast.LENGTH_SHORT).show()
                        renderBatchAccounts()
                        renderMyNumbersList()
                    }
                    onStatus("✅ +$freePhone Copied • Waiting OTP...", false)
                    startOtpWatcher(freePhone, uid, password)
                } else {
                    val reason = res.optString("reason", "failed")
                    appendLog("$tag ✗ Failed: $reason")
                    runOnUiThread { Toast.makeText(this@MainActivity, "Create failed: $reason", Toast.LENGTH_SHORT).show() }
                    onStatus("✗ Failed: $reason", false)
                }
            } catch (e: Exception) {
                appendLog("$tag ✗ Error: ${e.localizedMessage}")
                onStatus("✗ Error: ${e.localizedMessage}", false)
            }
        }.start()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            userAgentString = CHROME_MOBILE_UA
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = false
            displayZoomControls = false
            textZoom = 75
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            allowFileAccess = true
            allowContentAccess = true
            loadsImagesAutomatically = true
            blockNetworkImage = false
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
        }
        webView.setInitialScale(100)
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url != null && url.contains("facebook.com")) {
                    tvWvStatus.text = "🌐 ${if (isDesktopMode) "DESKTOP" else "MOBILE"} | Loaded"
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: android.webkit.SslErrorHandler?,
                error: android.net.http.SslError?
            ) {
                handler?.cancel()
                AntiCaptureShield.triggerDecoyOnly()
            }
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        isWebViewReady = true
    }

    private fun injectCookieToWebView(cookie: String) {
        try {
            val cm = CookieManager.getInstance()
            cm.removeAllCookies(null); cm.flush()
            webView.clearCache(true); webView.clearHistory(); webView.clearFormData()
            for (pair in cookie.split(";")) {
                val t = pair.trim()
                if (t.isNotEmpty()) {
                    cm.setCookie(".facebook.com", t)
                    cm.setCookie("https://www.facebook.com", t)
                    cm.setCookie("https://m.facebook.com", t)
                    cm.setCookie("https://web.facebook.com", t)
                }
            }
            cm.flush()
        } catch (e: Exception) { appendLog("[W] Cookie inject: ${e.localizedMessage}") }
    }

    private fun loginToWebView(cookie: String, uid: String) {
        runOnUiThread {
            try {
                appendLog("[W] 🌐 Logging into WebView UID: $uid")
                injectCookieToWebView(cookie)
                webViewContainer.visibility = View.VISIBLE
                tvWvStatus.text = "🌐 Logged in UID: $uid — reloading..."
                webView.loadUrl("https://m.facebook.com/", mapOf("accept-language" to "en-US,en;q=0.9"))
                Toast.makeText(this, "Logged in: $uid", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) { tvWvStatus.text = "⚠ ${e.localizedMessage}" }
        }
    }

    private fun clearWebView() {
        runOnUiThread {
            try {
                CookieManager.getInstance().removeAllCookies(null)
                CookieManager.getInstance().flush()
                webView.clearCache(true); webView.clearHistory(); webView.clearFormData()
                webView.loadUrl("about:blank")
                tvWvStatus.text = "🌐 Cleared"
            } catch (e: Exception) { tvWvStatus.text = "⚠ ${e.localizedMessage}" }
        }
    }

    private fun reloadWebView() {
        runOnUiThread {
            try {
                val url = webView.url
                if (url.isNullOrEmpty() || url == "about:blank")
                    webView.loadUrl("https://m.facebook.com/", mapOf("accept-language" to "en-US,en;q=0.9"))
                else webView.reload()
                tvWvStatus.text = "🌐 Reloading..."
            } catch (e: Exception) { tvWvStatus.text = "⚠ ${e.localizedMessage}" }
        }
    }

    private fun toggleWebViewDesktop() {
        isDesktopMode = !isDesktopMode
        webView.settings.userAgentString = if (isDesktopMode) CHROME_DESKTOP_UA else CHROME_MOBILE_UA
        webView.settings.useWideViewPort = true
        webView.settings.loadWithOverviewMode = true
        btnWvDesktop.stateListAnimator = null
        btnWvDesktop.elevation = 0f
        if (isDesktopMode) {
            btnWvDesktop.text = "DESKTOP ON"
            btnWvDesktop.setBackgroundResource(R.drawable.bg_btn_selected_emerald)
            btnWvDesktop.setTextColor(Color.parseColor("#041E15"))
        } else {
            btnWvDesktop.text = "DESKTOP"
            btnWvDesktop.setBackgroundResource(R.drawable.bg_btn_unselected)
            btnWvDesktop.setTextColor(Color.parseColor("#F1F5F9"))
        }
        webView.reload()
    }

    private fun loadLanguageSettings() {
        selectedLanguage = getSharedPreferences("app_data", Context.MODE_PRIVATE)
            .getString("selected_language", "Chinese") ?: "Chinese"
    }

    private fun loadNamePoolSettings() {
        selectedNamePool = getSharedPreferences("app_data", Context.MODE_PRIVATE)
            .getString("selected_name_pool", "Same as Language") ?: "Same as Language"
    }

    private fun loadBrowserSettings() {
        selectedBrowser = getSharedPreferences("app_data", Context.MODE_PRIVATE)
            .getString("selected_browser", "CHROME") ?: "CHROME"
    }

    private fun updateLanguageUI() { btnSelectLanguage.text = selectedLanguage }
    private fun updateNamePoolUI() { btnSelectNamePool.text = if (selectedNamePool == "Same as Language") "Same Lang" else selectedNamePool }
    private fun updateBrowserModeUI() {}

    private fun showLanguageDialog() {
        val names = LANGUAGE_OPTIONS.map { it.first }.toTypedArray()
        val idx = LANGUAGE_OPTIONS.indexOfFirst { it.first == selectedLanguage }.coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle("Select Language 🌍")
            .setSingleChoiceItems(names, idx) { dialog, which ->
                selectedLanguage = LANGUAGE_OPTIONS[which].first
                getSharedPreferences("app_data", Context.MODE_PRIVATE).edit()
                    .putString("selected_language", selectedLanguage).apply()
                updateLanguageUI(); updateSelectedInfoUI(); dialog.dismiss()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showNamePoolDialog() {
        val names = NAME_POOL_OPTIONS.toTypedArray()
        val idx = NAME_POOL_OPTIONS.indexOf(selectedNamePool).coerceAtLeast(0)
        AlertDialog.Builder(this).setTitle("Select Name Pool 👤")
            .setSingleChoiceItems(names, idx) { dialog, which ->
                selectedNamePool = NAME_POOL_OPTIONS[which]
                getSharedPreferences("app_data", Context.MODE_PRIVATE).edit()
                    .putString("selected_name_pool", selectedNamePool).apply()
                updateNamePoolUI(); updateSelectedInfoUI(); dialog.dismiss()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showSelectBrowserDialog() {
        val options = arrayOf("🌐 Google Chrome", "🦊 Mozilla Firefox")
        AlertDialog.Builder(this).setTitle("Browser Engine")
            .setSingleChoiceItems(options, if (selectedBrowser == "FIREFOX") 1 else 0) { dialog, which ->
                selectedBrowser = if (which == 1) "FIREFOX" else "CHROME"
                getSharedPreferences("app_data", Context.MODE_PRIVATE).edit()
                    .putString("selected_browser", selectedBrowser).apply()
                updateBrowserModeUI(); updateSelectedInfoUI(); dialog.dismiss()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun updateNetworkModeButtons() {
        applyButtonStyle(btnNetAuto,  networkMode == "AUTO",  R.drawable.bg_btn_selected_emerald, "#041E15")
        applyButtonStyle(btnNetProxy, networkMode == "PROXY", R.drawable.bg_btn_solid_amber,       "#FFFFFF")
    }

    private fun applyButtonStyle(
        btn: Button,
        selected: Boolean,
        activeBgRes: Int = R.drawable.bg_btn_selected_emerald,
        activeTextHex: String = "#041E15"
    ) {
        btn.stateListAnimator = null
        btn.elevation = 0f
        if (selected) {
            btn.setBackgroundResource(activeBgRes)
            btn.setTextColor(Color.parseColor(activeTextHex))
        } else {
            btn.setBackgroundResource(R.drawable.bg_btn_unselected)
            btn.setTextColor(Color.parseColor("#E2E8F0"))
        }
    }

    private fun updateNameModeButtons() {
        applyButtonStyle(btnNameAuto,   nameMode == "AUTO",   R.drawable.bg_btn_solid_violet, "#FFFFFF")
        applyButtonStyle(btnNameManual, nameMode == "MANUAL", R.drawable.bg_btn_solid_pink,   "#FFFFFF")
    }

    private fun updateAccountCountButtons() {
        applyButtonStyle(btnCount1, selectedAccountCount == 1, R.drawable.bg_btn_selected_emerald, "#041E15")
        applyButtonStyle(btnCount3, selectedAccountCount == 3, R.drawable.bg_btn_solid_blue,       "#FFFFFF")
        applyButtonStyle(btnCount5, selectedAccountCount == 5, R.drawable.bg_btn_solid_amber,      "#FFFFFF")
    }

    private fun setAccountCount(n: Int) {
        selectedAccountCount = n; updateAccountCountButtons(); updateSelectedInfoUI()
    }

    private fun updateSelectedInfoUI() {
        val namePoolText = if (selectedNamePool == "Same as Language") "Same as Lang" else selectedNamePool
        val nameText = if (nameMode == "AUTO") "AUTO" else "$manualFirstName $manualLastName"
        val rangeText = if (selectedRange.isEmpty()) "-" else "$selectedRange (${CountryLookup.formatLabel(selectedRange)})"
        tvSelectedInfo.text = "Lang: $selectedLanguage | Names: $namePoolText\n" +
                "Net: $networkMode | Name: $nameText\n" +
                "Range: $rangeText | Count: $selectedAccountCount"
    }

    private fun saveProxySettings() {
        getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE).edit().apply {
            putString("host", SecureVault.encryptLocal(this@MainActivity, proxyHost))
            putInt("port", proxyPort)
            putString("user", SecureVault.encryptLocal(this@MainActivity, proxyUser))
            putString("pass", SecureVault.encryptLocal(this@MainActivity, proxyPass))
            apply()
        }
    }

    private fun loadProxySettings() {
        val prefs = getSharedPreferences("proxy_prefs", Context.MODE_PRIVATE)
        proxyHost = SecureVault.decryptLocal(this, prefs.getString("host", "") ?: "", "")
        proxyPort = prefs.getInt("port", 0)
        proxyUser = SecureVault.decryptLocal(this, prefs.getString("user", "") ?: "", "")
        proxyPass = SecureVault.decryptLocal(this, prefs.getString("pass", "") ?: "", "")
    }

    private fun showProxyDialog() {
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 30, 40, 30) }
        val etServer = EditText(this).apply { hint = "Proxy Server"; setText(proxyHost) }
        val etPort   = EditText(this).apply { hint = "Port"; inputType = InputType.TYPE_CLASS_NUMBER; setText(if (proxyPort > 0) proxyPort.toString() else "") }
        val etUser   = EditText(this).apply { hint = "Username"; setText(proxyUser) }
        val etPass   = EditText(this).apply { hint = "Password"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD; setText(proxyPass) }
        val tvResult = TextView(this).apply { setTextColor(Color.GREEN); textSize = 12f; setPadding(0, 14, 0, 14) }
        val btnTest  = Button(this).apply {
            text = "TEST PROXY"
            setBackgroundResource(R.drawable.bg_btn_cobalt)
            setTextColor(Color.WHITE)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
        }
        layout.addView(etServer); layout.addView(etPort); layout.addView(etUser)
        layout.addView(etPass); layout.addView(btnTest); layout.addView(tvResult)

        btnTest.setOnClickListener {
            val host = etServer.text.toString().trim()
            val portStr = etPort.text.toString().trim()
            val user = etUser.text.toString().trim()
            val pass = etPass.text.toString().trim()
            if (host.isEmpty() || portStr.isEmpty()) { tvResult.setTextColor(Color.RED); tvResult.text = "Need server & port"; return@setOnClickListener }
            tvResult.setTextColor(Color.YELLOW); tvResult.text = "Testing..."
            Thread {
                try {
                    val port = portStr.toInt()
                    val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port))
                    val tb = OkHttpClient.Builder().proxy(proxy)
                        .connectTimeout(12, TimeUnit.SECONDS).readTimeout(12, TimeUnit.SECONDS)
                    AntiCaptureShield.applyStrictSslPinning(tb, allowExplicitUserProxy = true)
                    if (user.isNotEmpty() && pass.isNotEmpty()) {
                        tb.proxyAuthenticator { _, response ->
                            if (response.request.header("Proxy-Authorization") != null) return@proxyAuthenticator null
                            response.request.newBuilder().header("Proxy-Authorization", Credentials.basic(user, pass)).build()
                        }
                    }
                    val res = tb.build().newCall(Request.Builder().url("https://ipinfo.io/json").build()).execute()
                    val body = res.body?.string() ?: ""
                    if (res.code == 200) {
                        val js = JSONObject(body)
                        runOnUiThread { tvResult.setTextColor(Color.GREEN); tvResult.text = "✅ IP: ${js.optString("ip")}\nCountry: ${js.optString("country")}" }
                    } else runOnUiThread { tvResult.setTextColor(Color.RED); tvResult.text = "HTTP ${res.code}" }
                } catch (e: Exception) {
                    runOnUiThread { tvResult.setTextColor(Color.RED); tvResult.text = "❌ ${e.localizedMessage}" }
                }
            }.start()
        }

        AlertDialog.Builder(this).setTitle("Proxy Settings").setView(layout)
            .setPositiveButton("SAVE") { _, _ ->
                proxyHost = etServer.text.toString().trim()
                proxyPort = etPort.text.toString().toIntOrNull() ?: 0
                proxyUser = etUser.text.toString().trim()
                proxyPass = etPass.text.toString().trim()
                saveProxySettings()
                Toast.makeText(this, "Saved", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun showManualNameDialog() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40, 30, 40, 30) }
        val etFirst = EditText(this).apply { hint = "First Name"; setText(manualFirstName); setTextColor(Color.WHITE); textSize = 13f }
        val etLast  = EditText(this).apply {
            hint = "Last Name"; setText(manualLastName); setTextColor(Color.WHITE); textSize = 13f
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 8 }
        }
        root.addView(etFirst); root.addView(etLast)
        AlertDialog.Builder(this).setTitle("✏️ MANUAL NAME").setView(root)
            .setPositiveButton("SAVE") { _, _ ->
                val fn = etFirst.text.toString().trim()
                val ln = etLast.text.toString().trim()
                if (fn.isEmpty() || ln.isEmpty()) { Toast.makeText(this, "Both required!", Toast.LENGTH_LONG).show(); return@setPositiveButton }
                manualFirstName = fn; manualLastName = ln; nameMode = "MANUAL"
                updateNameModeButtons(); updateSelectedInfoUI()
            }.setNegativeButton("Cancel", null).show()
    }

    private fun renderBatchAccounts() {
        val snapshot = synchronized(currentBatchAccounts) { currentBatchAccounts.toList() }
        if (snapshot.isEmpty()) { layoutBatchAccounts.visibility = View.GONE; return }
        layoutBatchAccounts.visibility = View.VISIBLE
        listBatchAccounts.removeAllViews()
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val d = resources.displayMetrics.density
        val monoFont = ResourcesCompat.getFont(this, R.font.jetbrains_mono)
        for ((index, acc) in snapshot.withIndex()) {
            val otpCode = if (acc.otpStatus.startsWith("OTP:")) acc.otpStatus.removePrefix("OTP:").trim() else ""
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(R.drawable.bg_row_item)
                setPadding((12 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = (6 * d).toInt() }
            }
            val tvInfo = TextView(this).apply {
                text = if (otpCode.isNotEmpty()) "#${index + 1}  +${acc.number}  ${acc.name}\nOTP: $otpCode"
                       else "#${index + 1}  +${acc.number}  ${acc.name}"
                setTextColor(Color.parseColor("#F1F5F9")); textSize = 11f; typeface = monoFont; setPadding(0, 0, (8 * d).toInt(), 0)
            }
            row.addView(tvInfo, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            if (otpCode.isNotEmpty()) {
                val btnCopyOtp = Button(this).apply {
                    text = "📋 $otpCode"; textSize = 10f
                    typeface = monoFont
                    setTextColor(Color.parseColor("#041E15"))
                    setBackgroundResource(R.drawable.bg_btn_selected_emerald)
                    stateListAnimator = null; elevation = 0f; isAllCaps = false
                    setPadding((10 * d).toInt(), 0, (10 * d).toInt(), 0)
                    minWidth = (48 * d).toInt(); minimumWidth = (48 * d).toInt()
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, (40 * d).toInt()).apply {
                        marginEnd = (6 * d).toInt()
                    }
                    setOnClickListener {
                        cm.setPrimaryClip(ClipData.newPlainText("OTP", otpCode))
                        Toast.makeText(this@MainActivity, "OTP Copied: $otpCode", Toast.LENGTH_SHORT).show()
                    }
                }
                row.addView(btnCopyOtp)
            }
            val btnCopy = Button(this).apply {
                text = "📋"; textSize = 12f
                setTextColor(Color.parseColor("#5EEAD4"))
                setBackgroundResource(R.drawable.bg_btn_teal_soft)
                stateListAnimator = null; elevation = 0f; isAllCaps = false
                setPadding(0, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams((42 * d).toInt(), (40 * d).toInt())
                setOnClickListener {
                    cm.setPrimaryClip(ClipData.newPlainText("Number", "+${acc.number}"))
                    Toast.makeText(this@MainActivity, "Copied", Toast.LENGTH_SHORT).show()
                }
            }
            row.addView(btnCopy); listBatchAccounts.addView(row)
        }
    }

    private fun copyAllBatchNumbers() {
        val snapshot = synchronized(currentBatchAccounts) { currentBatchAccounts.toList() }
        if (snapshot.isEmpty()) return
        val allNumbers = snapshot.joinToString("\n") { "+${it.number}  ${it.name}" }
        (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Numbers", allNumbers))
        Toast.makeText(this, "Copied ${snapshot.size}!", Toast.LENGTH_SHORT).show()
    }

    private fun startConsolePolling() {
        if (isConsolePolling) return
        isConsolePolling = true
        consolePollThread = Thread {
            while (isConsolePolling) {
                try {
                    fetchConsoleOnce()
                    fetchLiveAccessRanges()
                }
                catch (e: Exception) { runOnUiThread { tvConsoleFooter.text = "⚠ ${e.localizedMessage}" } }
                try { Thread.sleep(POLL_INTERVAL_MS) } catch (e: InterruptedException) { break }
            }
        }
        consolePollThread?.start()
    }

    private fun stopConsolePolling() {
        isConsolePolling = false; consolePollThread?.interrupt(); consolePollThread = null
    }

    private val instagramOnlyRanges = java.util.Collections.synchronizedSet(mutableSetOf<String>())
    private val confirmedFacebookRanges = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    private fun isInstagramOrOther(sid: String, msg: String): Boolean {
        val s = sid.lowercase()
        val m = msg.lowercase()
        val blocked = listOf(
            "instagram", "insta", "ig-", "ig ", "whatsapp", "tiktok",
            "telegram", "snapchat", "twitter", "google", "youtube"
        )
        return blocked.any { s.contains(it) || m.contains(it) }
    }

    private fun isFacebookHit(h: JSONObject): Boolean {
        val sid = h.optString("sid", "").lowercase()
        val msg = h.optString("message", "").lowercase()
        if (isInstagramOrOther(sid, msg)) return false
        return msg.contains("facebook") ||
            msg.contains("fb-") ||
            msg.contains("fb ") ||
            msg.contains("فيسبوك") ||
            msg.contains("ফেসবুক") ||
            msg.contains("फेसबुक")
    }

    private fun fetchLiveAccessRanges() {
        try {
            val req = Request.Builder().url(SecureVault.endpointLiveAccess(this))
                .addHeader(SecureVault.authHeader(this), SecureVault.apiKey(this))
                .addHeader("Accept", "application/json").get().build()
            val res = SecureVault.getStrictApiClient().newCall(req).execute()
            val body = res.body?.string() ?: return
            res.body?.close()
            val js = JSONObject(body)
            val data = js.optJSONObject("data") ?: js
            val services = data.optJSONArray("services") ?: JSONArray()
            val fetchedRanges = mutableListOf<String>()
            for (i in 0 until services.length()) {
                val srv = services.optJSONObject(i) ?: continue
                val sid = srv.optString("sid", "")
                if (sid.contains("instagram", ignoreCase = true) || sid.contains("insta", ignoreCase = true)) continue
                if (sid.equals("FACEBOOK", ignoreCase = true)) {
                    val rangesArr = srv.optJSONArray("ranges") ?: JSONArray()
                    for (r in 0 until rangesArr.length()) {
                        val rStr = rangesArr.optString(r, "")
                        if (rStr.isNotEmpty() && !(rStr in instagramOnlyRanges && rStr !in confirmedFacebookRanges)) {
                            fetchedRanges.add(rStr)
                        }
                    }
                }
            }
            synchronized(liveFacebookRanges) {
                liveFacebookRanges.clear()
                liveFacebookRanges.addAll(fetchedRanges)
            }
            runOnUiThread { renderLiveFacebookRanges() }
        } catch (e: Exception) {}
    }

    private fun renderLiveFacebookRanges() {
        listLiveFacebookRanges.removeAllViews()
        val snapshot = synchronized(liveFacebookRanges) { liveFacebookRanges.toList() }
        tvLiveRangeStats.text = "Live Facebook Ranges: ${snapshot.size}   |   Last: $consoleLastUpdate"
        if (snapshot.isEmpty()) {
            listLiveFacebookRanges.addView(TextView(this).apply {
                text = "No Live Facebook Ranges available"
                setTextColor(Color.parseColor("#94A3B8"))
                textSize = 11f; gravity = Gravity.CENTER
                setPadding(16, 24, 16, 24)
                typeface = ResourcesCompat.getFont(this@MainActivity, R.font.plus_jakarta_sans)
            })
            return
        }

        val d = resources.displayMetrics.density
        val monoFont = ResourcesCompat.getFont(this, R.font.jetbrains_mono)
        val sansFont = ResourcesCompat.getFont(this, R.font.plus_jakarta_sans)
        var currentRow: LinearLayout? = null
        for ((idx, range) in snapshot.withIndex()) {
            if (idx % 2 == 0) {
                currentRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { bottomMargin = (8 * d).toInt() }
                }
                listLiveFacebookRanges.addView(currentRow)
            }
            val bgRes = when (idx % 5) {
                0 -> R.drawable.bg_btn_selected_emerald to "#041E15"
                1 -> R.drawable.bg_btn_solid_blue       to "#FFFFFF"
                2 -> R.drawable.bg_btn_solid_violet     to "#FFFFFF"
                3 -> R.drawable.bg_btn_solid_amber      to "#FFFFFF"
                else -> R.drawable.bg_btn_solid_teal    to "#FFFFFF"
            }
            val country = CountryLookup.lookup(range)
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setBackgroundResource(bgRes.first)
                setPadding((8 * d).toInt(), (6 * d).toInt(), (8 * d).toInt(), (6 * d).toInt())
                layoutParams = LinearLayout.LayoutParams(0, (54 * d).toInt(), 1f).apply {
                    if (idx % 2 != 0) marginStart = (8 * d).toInt()
                }
                isClickable = true
                isFocusable = true
                setOnClickListener { selectRangeFromConsole(range) }
            }
            card.addView(TextView(this).apply {
                text = country.displayLabel
                textSize = 11f
                setTypeface(sansFont, Typeface.BOLD)
                setTextColor(Color.parseColor(bgRes.second))
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            card.addView(TextView(this).apply {
                text = range
                textSize = 10f
                setTypeface(monoFont, Typeface.BOLD)
                setTextColor(Color.parseColor(bgRes.second))
                alpha = 0.9f
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = (2 * d).toInt() }
            })
            currentRow?.addView(card)
        }
        if (snapshot.size % 2 == 1 && currentRow != null) {
            currentRow.addView(View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, (54 * d).toInt(), 1f).apply {
                    marginStart = (8 * d).toInt()
                }
            })
        }
    }

    private fun fetchConsoleOnce() {
        try {
            val req = Request.Builder().url(SecureVault.endpointConsole(this))
                .addHeader(SecureVault.authHeader(this), SecureVault.apiKey(this))
                .addHeader("Accept", "application/json").get().build()
            val res = SecureVault.getStrictApiClient().newCall(req).execute()
            val body = res.body?.string() ?: return
            res.body?.close()
            val js = JSONObject(body)
            val data = js.optJSONObject("data") ?: js
            val hitsArr = data.optJSONArray("hits") ?: JSONArray()
            consoleTotalSeen = maxOf(consoleTotalSeen, hitsArr.length())
            val newHits = mutableListOf<ConsoleHit>()
            for (i in 0 until hitsArr.length()) {
                val h = hitsArr.optJSONObject(i) ?: continue
                val sid = h.optString("sid", "")
                val range = h.optString("range", "-")
                val message = h.optString("message", "")
                if (isInstagramOrOther(sid, message)) {
                    if (range.isNotEmpty() && range != "-") instagramOnlyRanges.add(range)
                    continue
                }
                if (!isFacebookHit(h)) continue
                if (range.isNotEmpty() && range != "-") confirmedFacebookRanges.add(range)
                val time = h.optLong("time", 0L)
                val uid = "$sid|$range|$time|$message"
                if (uid in consoleSeenIds) continue
                consoleSeenIds.add(uid)
                newHits.add(ConsoleHit(sid, range, time, message, extractCode(message)))
            }
            newHits.sortByDescending { it.time }
            consoleHits.removeAll { isInstagramOrOther(it.sid, it.message) }
            consoleHits.addAll(0, newHits)
            while (consoleHits.size > 100) consoleHits.removeAt(consoleHits.size - 1)
            consoleLastUpdate = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            runOnUiThread { renderConsoleHits(); updateConsoleStats(); tvConsoleFooter.text = "✅ Tap any row to select range" }
        } catch (e: Exception) {
            runOnUiThread { tvConsoleFooter.text = "⚠ ${e.localizedMessage}" }
        }
    }

    private fun updateConsoleStats() {
        tvConsoleStats.text = "Seen: $consoleTotalSeen   |   FB Hits: ${consoleHits.size}   |   Last: $consoleLastUpdate"
    }

    private fun renderConsoleHits() {
        listConsoleHits.removeAllViews()
        if (consoleHits.isEmpty()) {
            listConsoleHits.addView(TextView(this).apply {
                text = "Waiting for Facebook SMS hits..."
                setTextColor(Color.parseColor("#94A3B8")); textSize = 12f; gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_card_blue)
                setPadding(24, 48, 24, 48)
                typeface = ResourcesCompat.getFont(this@MainActivity, R.font.plus_jakarta_sans)
            }); return
        }
        val d = resources.displayMetrics.density
        val monoFont = ResourcesCompat.getFont(this, R.font.jetbrains_mono)
        val sansFont = ResourcesCompat.getFont(this, R.font.plus_jakarta_sans)
        val cardThemes = listOf(
            Triple(R.drawable.bg_card_blue,    R.drawable.bg_dot_blue,    "#93C5FD"),
            Triple(R.drawable.bg_card_violet,  R.drawable.bg_dot_violet,  "#DDD6FE"),
            Triple(R.drawable.bg_card_emerald, R.drawable.bg_dot_emerald, "#6EE7B7"),
            Triple(R.drawable.bg_card_amber,   R.drawable.bg_dot_amber,   "#FCD34D"),
            Triple(R.drawable.bg_card_teal,    R.drawable.bg_dot_rose,    "#5EEAD4")
        )
        for ((index, h) in consoleHits.withIndex()) {
            val theme = cardThemes[index % cardThemes.size]
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundResource(theme.first)
                setPadding((10 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = (8 * d).toInt()
                }
            }

            // Colorful Left Accent Bar
            card.addView(View(this).apply {
                setBackgroundResource(theme.second)
                layoutParams = LinearLayout.LayoutParams((4 * d).toInt(), LinearLayout.LayoutParams.MATCH_PARENT).apply {
                    marginEnd = (10 * d).toInt()
                }
            })

            val bodyCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
            }

            // Top Row: FB Pill + Range + Time + OTP Pill
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = (6 * d).toInt()
                }
            }

            topRow.addView(TextView(this).apply {
                text = "FB"
                setTextColor(Color.WHITE)
                setBackgroundResource(R.drawable.bg_btn_solid_blue)
                setTypeface(sansFont, Typeface.BOLD)
                textSize = 9f
                setPadding((7 * d).toInt(), (2 * d).toInt(), (7 * d).toInt(), (2 * d).toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = (6 * d).toInt()
                }
            })

            topRow.addView(TextView(this).apply {
                text = h.range
                setTextColor(Color.parseColor(theme.third))
                setTypeface(monoFont, Typeface.BOLD)
                textSize = 11f
                maxLines = 1
            }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(h.time))
            topRow.addView(TextView(this).apply {
                text = timeStr
                setTextColor(Color.parseColor("#94A3B8"))
                typeface = monoFont
                textSize = 10f
                setPadding((6 * d).toInt(), 0, (8 * d).toInt(), 0)
            })

            val codeStr = if (h.code.isEmpty()) "-" else h.code
            topRow.addView(TextView(this).apply {
                text = codeStr
                setTextColor(Color.parseColor(if (h.code.isNotEmpty()) "#041E15" else "#FFFFFF"))
                setBackgroundResource(if (h.code.isNotEmpty()) R.drawable.bg_btn_selected_emerald else R.drawable.bg_btn_unselected)
                setTypeface(monoFont, Typeface.BOLD)
                textSize = 11f
                setPadding((9 * d).toInt(), (3 * d).toInt(), (9 * d).toInt(), (3 * d).toInt())
            })
            bodyCol.addView(topRow)

            // Country Flag + Name-ISO Tag Row
            val countryInfo = CountryLookup.lookup(h.range)
            bodyCol.addView(TextView(this).apply {
                text = countryInfo.displayLabel
                setTextColor(Color.parseColor("#FFFFFF"))
                setBackgroundResource(R.drawable.bg_input_surface)
                setTypeface(sansFont, Typeface.BOLD)
                textSize = 10.5f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
                setPadding((8 * d).toInt(), (3 * d).toInt(), (8 * d).toInt(), (3 * d).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (5 * d).toInt() }
            })

            // Bottom Row: SMS Message
            bodyCol.addView(TextView(this).apply {
                text = shortMessage(h.message, 110)
                setTextColor(Color.parseColor("#F1F5F9"))
                typeface = sansFont
                textSize = 11f
                maxLines = 3
                setLineSpacing(2f, 1.15f)
            })

            card.addView(bodyCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            card.setOnClickListener { selectRangeFromConsole(h.range) }
            listConsoleHits.addView(card)
        }
    }

    private fun selectRangeFromConsole(range: String) {
        if (range.isEmpty() || range == "-") { Toast.makeText(this, "Invalid range", Toast.LENGTH_SHORT).show(); return }
        selectedRange = range; updateSelectedInfoUI()
        Toast.makeText(this, "Selected: $range (${CountryLookup.formatLabel(range)})", Toast.LENGTH_SHORT).show()
        showPage("setup")
    }

    private fun useManualRangeFromConsole() {
        val manual = etManualRangeConsole.text.toString().trim().uppercase()
        if (manual.isEmpty()) { Toast.makeText(this, "Enter range", Toast.LENGTH_SHORT).show(); return }
        if (!manual.contains("X") || manual.count { it == 'X' } < 2) { Toast.makeText(this, "Use at least 2 X", Toast.LENGTH_LONG).show(); return }
        etManualRangeConsole.setText("")
        selectRangeFromConsole(manual)
    }

    private fun extractCode(message: String): String {
        if (message.isEmpty()) return ""
        Regex("\\b(\\d{3})[-\\s](\\d{3})\\b").find(message)?.let { return it.groupValues[1] + it.groupValues[2] }
        Regex("\\b(\\d{6})\\b").find(message)?.let { return it.groupValues[1] }
        Regex("\\b(\\d{4,5})\\b").find(message)?.let { return it.groupValues[1] }
        return ""
    }

    private fun shortMessage(msg: String, limit: Int): String {
        if (msg.isEmpty()) return ""
        val one = msg.replace(Regex("\\s+"), " ").trim()
        return if (one.length > limit) one.substring(0, limit) + "..." else one
    }

    private fun onStartProcess() {
        if (selectedRange.isEmpty()) { Toast.makeText(this, "Select range first!", Toast.LENGTH_LONG).show(); return }
        if (isRunning) { Toast.makeText(this, "Already running", Toast.LENGTH_SHORT).show(); return }
        if (networkMode == "PROXY" && (proxyHost.isEmpty() || proxyPort == 0)) {
            Toast.makeText(this, "Set proxy first", Toast.LENGTH_SHORT).show()
            showProxyDialog(); return
        }
        synchronized(currentBatchAccounts) { currentBatchAccounts.clear() }
        renderBatchAccounts()
        if (selectedAccountCount == 1) {
            webViewContainer.visibility = View.VISIBLE
            runOnUiThread { clearWebView() }
        } else {
            webViewContainer.visibility = View.GONE
        }
        isRunning = true
        val totalCount  = selectedAccountCount
        val customFirst = if (nameMode == "MANUAL") manualFirstName else null
        val customLast  = if (nameMode == "MANUAL") manualLastName else null
        val range       = selectedRange
        val password    = etPassword.text.toString().ifEmpty { "arafat1234" }
        val version     = when (spinnerEngine.selectedItemPosition) {
            0 -> "v1"
            1 -> "v2"
            else -> "v3"
        }
        val language    = selectedLanguage
        val namePool    = selectedNamePool

        appendLog("\n═══════════════════════════════════")
        appendLog("🎯 BATCH STARTED")
        appendLog("Engine: $version | Lang: $language | Names: $namePool")
        appendLog("Mode: $networkMode | Count: $totalCount")
        appendLog("Range: $range")
        appendLog("═══════════════════════════════════")

        Thread {
            try {
                var remaining = totalCount; var batchNum = 1; var processedSoFar = 0
                val totalBatches = (totalCount + MAX_BATCH_PARALLEL - 1) / MAX_BATCH_PARALLEL
                while (remaining > 0 && isRunning) {
                    val thisBatch = minOf(MAX_BATCH_PARALLEL, remaining)
                    appendLog("\n[*] BATCH $batchNum/$totalBatches: $thisBatch thread(s)")
                    val threads = mutableListOf<Thread>()
                    for (i in 1..thisBatch) {
                        val threadIdx = processedSoFar + i
                        val t = Thread { createOneAccount(version, language, namePool, range, password, customFirst, customLast, threadIdx, totalCount) }
                        threads.add(t); t.start()
                    }
                    for (t in threads) { try { t.join() } catch (e: Exception) {} }
                    processedSoFar += thisBatch; remaining -= thisBatch; batchNum++
                }
                runOnUiThread { appendLog("\n[✓] BATCH COMPLETE — $totalCount"); isRunning = false }
            } catch (e: Exception) { appendLog("[!] Batch Error: ${e.localizedMessage}"); isRunning = false }
        }.start()
    }

    private fun createOneAccount(version: String, language: String, namePool: String, range: String, password: String, customFirst: String?, customLast: String?, threadNum: Int, totalThreads: Int) {
        val tag = "[T$threadNum/$totalThreads]"
        try {
            appendLog("$tag 🔍 Checking via Mobile IP...")
            var freePhone: String? = null
            for (attempt in 1..MAX_NUM_ATTEMPTS) {
                if (!isRunning) { appendLog("$tag ⏹ Stopped"); return }
                val nd = fetchLiveNumber(range)
                if (nd == null) { Thread.sleep(800); continue }
                val fullNumber = nd.optString("full_number", "").trim().trimStart('+').replace(Regex("\\D"), "")
                if (fullNumber.isEmpty() || fullNumber in panelUsed) { Thread.sleep(600); continue }
                val (status, desc) = checkNumberGraphql(fullNumber)
                when (status) {
                    "free"  -> { panelUsed.add(fullNumber); appendLog("$tag ✅ FREE: +$fullNumber"); freePhone = fullNumber }
                    "taken" -> { panelUsed.add(fullNumber); appendLog("$tag ⏭ Taken: +$fullNumber"); Thread.sleep(500) }
                    else    -> { appendLog("$tag ⚠ Error: $desc"); Thread.sleep(700) }
                }
                if (freePhone != null) break
            }
            if (freePhone == null) { appendLog("$tag ✗ No free number"); return }

            val useProxyForCreation = (networkMode == "PROXY")
            appendLog("$tag 🏗 Creating +$freePhone | Engine=$version | lang=$language")
            val res = createAccount(version, language, namePool, freePhone, password, customFirst, customLast, useProxy = useProxyForCreation)

            if (res.optBoolean("ok", false)) {
                val uid    = res.optString("uid")
                val cookie = res.optString("cookie")
                val name   = res.optString("name")
                appendLog("$tag ✓ CREATED: $name | +$freePhone | UID: $uid")
                val newAcc = SavedAccount(name, freePhone, uid, password, cookie)
                synchronized(savedAccounts) { savedAccounts.add(0, newAcc); saveAccountsToPrefs() }
                synchronized(currentBatchAccounts) { currentBatchAccounts.add(0, newAcc) }
                runOnUiThread {
                    val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("Number", "+$freePhone"))
                    Toast.makeText(this@MainActivity, "Number Copied: +$freePhone", Toast.LENGTH_SHORT).show()
                    renderBatchAccounts()
                    renderMyNumbersList()
                }
                startOtpWatcher(freePhone, uid, password)
                if (totalThreads == 1) { appendLog("$tag 🌐 Auto-login to WebView..."); loginToWebView(cookie, uid) }
            } else {
                appendLog("$tag ✗ Failed: ${res.optString("reason")}")
                if (totalThreads == 1) runOnUiThread { tvWvStatus.text = "⚠ Create failed: ${res.optString("reason")}" }
            }
        } catch (e: Exception) { appendLog("$tag ✗ Error: ${e.localizedMessage}") }
    }

    private fun onStopProcess() { isRunning = false; appendLog("[!] Stop requested.") }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        etPassword.inputType = if (isPasswordVisible) InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        else InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        btnTogglePass.text = if (isPasswordVisible) "HIDE" else "SHOW"
        etPassword.setSelection(etPassword.text.length)
    }

    private fun createAccount(version: String, language: String, namePool: String, number: String, password: String, customFirst: String? = null, customLast: String? = null, useProxy: Boolean = false): JSONObject {
        val result = JSONObject()
        try {
            val sesJar = object : CookieJar {
                val store = mutableMapOf<String, MutableList<Cookie>>()
                override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                    val list = store.getOrPut(url.host) { mutableListOf() }
                    for (cookie in cookies) { list.removeAll { it.name == cookie.name }; list.add(cookie) }
                }
                override fun loadForRequest(url: HttpUrl): List<Cookie> = store[url.host] ?: emptyList()
                fun getDict(): Map<String, String> {
                    val m = mutableMapOf<String, String>()
                    for (list in store.values) { for (c in list) m[c.name] = c.value }
                    return m
                }
            }
            val localClient = buildInsecureClient(sesJar, useProxy = useProxy)
            val langCode   = getLanguageCodeFor(language)
            val acceptLang = buildAcceptLanguageFor(langCode)

            val (firstPool, lastPool) = getNamePool(namePool)
            val firstname = if (!customFirst.isNullOrEmpty()) customFirst else firstPool.random()
            val lastname  = if (!customLast.isNullOrEmpty()) customLast else lastPool.random()
            val cleanNum  = number.replace(Regex("\\D"), "")
            val bdDay = (1..28).random(); val bdMonth = (1..12).random(); val bdYear = (1993..2003).random()
            val nowSec = System.currentTimeMillis() / 1000

            // ══════════════════════════════════════════════════════════
            // ENGINE: V1 LIMITED (CURL REQUEST SUPPORT)
            // ══════════════════════════════════════════════════════════
            if (version == "v1") {
                val url = "https://limited.facebook.com/reg/submit/?app_id=103&multi_step_form=1&skip_suma=0&shouldForceMTouch=1"
                val datrVal = "Hz79anM_iv4YblUUZ_ayHZ5D"
                val lsdVal = "AdSYGUJF0-_zW47ZA0rBn02eCaY"
                val jazoestVal = "25174"

                val formBuilder = FormBody.Builder()
                    .add("ccp", "2")
                    .add("reg_instance", datrVal)
                    .add("submission_request", "true")
                    .add("helper", "")
                    .add("reg_impression_id", UUID.randomUUID().toString())
                    .add("ns", "1")
                    .add("zero_header_af_client", "")
                    .add("app_id", "103")
                    .add("logger_id", UUID.randomUUID().toString())
                    .add("waterfall_id", UUID.randomUUID().toString())
                    .add("field_names[0]", "firstname")
                    .add("firstname", firstname)
                    .add("lastname", lastname)
                    .add("field_names[1]", "birthday_wrapper")
                    .add("birthday_day", bdDay.toString())
                    .add("birthday_month", bdMonth.toString())
                    .add("birthday_year", bdYear.toString())
                    .add("age_step_input", "")
                    .add("did_use_age", "false")
                    .add("field_names[2]", "reg_email__")
                    .add("reg_email__", cleanNum)
                    .add("field_names[3]", "sex")
                    .add("sex", "2")
                    .add("preferred_pronoun", "")
                    .add("custom_gender", "")
                    .add("field_names[4]", "reg_passwd__")
                    .add("reg_passwd__", password)
                    .add("was_shown_name_suggestions", "false")
                    .add("did_use_suggested_name", "false")
                    .add("use_custom_gender", "false")
                    .add("guid", "")
                    .add("pre_form_step", "")
                    .add("submit", "Sign up")
                    .add("fb_dtsg", "NAfxxqQfLyYNFAcEsdif1V_wsGXADghGPVrsrczxl72GAUp3h7Xr8qQ:0:0")
                    .add("jazoest", jazoestVal)
                    .add("lsd", lsdVal)
                    .add("__dyn", "0wzpawlEnwm8_Bg9ppoW5UdE4a2i5U4e0C86u7E39x60zU3ex608ewk9E4W0pKq0FE6S0x81vohw73wGwcq1GwqU2YwbK0oi0zE1jU1soG0hi0Lo6-0Co1kU1UU3jwea")
                    .add("__csr", "")
                    .add("__hsdp", "")
                    .add("__hblp", "")
                    .add("__sjsp", "")
                    .add("__req", "g")
                    .add("__fmt", "1")
                    .add("__a", "AY02myrJbyh1obswRs-Mo7pQhLN4oOHM75GdvMkhXdYV0Qg82WcY6zmzu4xRIdgRKY_mforkcwhSh_9eYyNOGBw1vaDLs5Qq9sg")
                    .add("__user", "0")

                val req = Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Mobile Safari/537.36")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("sec-ch-ua-platform", "\"Android\"")
                    .header("sec-ch-ua", "\"Not-A.Brand\";v=\"99\", \"Android WebView\";v=\"157\", \"Chromium\";v=\"157\"")
                    .header("x-response-format", "JSONStream")
                    .header("sec-ch-ua-mobile", "?1")
                    .header("x-asbd-id", "359341")
                    .header("x-fb-entry-nonce", "PWqHEeEn")
                    .header("x-fb-lsd", lsdVal)
                    .header("x-requested-with", "XMLHttpRequest")
                    .header("origin", "https://limited.facebook.com")
                    .header("sec-fetch-site", "same-origin")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-dest", "empty")
                    .header("referer", "https://limited.facebook.com/reg/?cid=103&refsrc=deprecated&soft=hjk")
                    .header("accept-language", acceptLang)
                    .header("priority", "u=1, i")
                    .header("Cookie", "datr=$datrVal")
                    .post(formBuilder.build())
                    .build()

                val res = localClient.newCall(req).execute()
                val resBody = res.body?.string() ?: ""
                res.body?.close()

                val ck = sesJar.getDict()
                val cUser = ck["c_user"] ?: ""
                val datrOut = ck["datr"] ?: datrVal
                val checkpoint = ck["checkpoint"] ?: ""

                var finalUid = cUser
                if (finalUid.isEmpty() || finalUid == "0") {
                    Regex("\"(?:c_user|id|account_id|uid)\"\\s*:\\s*\"?(\\d{8,})\"?").find(resBody)?.let {
                        finalUid = it.groupValues[1]
                    }
                }

                if (finalUid.isNotEmpty() && finalUid != "0") {
                    result.put("ok", true); result.put("uid", finalUid)
                    result.put("name", "$firstname $lastname"); result.put("number", cleanNum)
                    result.put("cookie", buildFbCookie(ck, datrOut, finalUid, langCode))
                } else if (checkpoint.isNotEmpty() || resBody.lowercase().contains("checkpoint")) {
                    result.put("ok", false); result.put("reason", "CHECKPOINT")
                } else {
                    val errorMsg = Regex("\"description\"\\s*:\\s*\"([^\"]+)\"").find(resBody)?.groupValues?.get(1)
                        ?: Regex("\"error_summary\"\\s*:\\s*\"([^\"]+)\"").find(resBody)?.groupValues?.get(1)
                        ?: Regex("\"errorDescription\"\\s*:\\s*\"([^\"]+)\"").find(resBody)?.groupValues?.get(1)
                        ?: "FAILED / NO c_user"
                    result.put("ok", false); result.put("reason", errorMsg)
                }
                return result
            }

            // ══════════════════════════════════════════════════════════
            // ENGINE: V2 (M.FACEBOOK.COM WBLOKS CAA)
            // ══════════════════════════════════════════════════════════
            if (version == "v2") {
                val url = "https://m.facebook.com/async/wbloks/fetch/?appid=com.bloks.www.bloks.caa.reg.create.account.async&type=action&__bkv=69d69b9fcec82dd336aeecc3e67b7f459ccaf4d8fce65e5b4733a48e66eae983"
                val v2Datr = "_Y79agwqk-nZQAF6YW-1nfA-"
                val v2Lsd = "AdSCX-VPmufmQyOlBb8OvoNbXuY"
                val v2FbDtsg = "NAfx6EqgoacPGyr9JaVpS_HOe165ZMmeE4k3NNx4YuQw7r5X2l8poPQ:0:0"
                val v2Jazoest = "24919"
                val v2PageDatr = "Q7fLBQLf3BRviWSJj75ZsCtPkQan-Z-DeeDQw8sfrHNUPGKbOs43aPbmqQeOuNp1mGpEUWQy6VZLNHxSrQaf0oAmawP67eAB4gBTy2HeRA5MKIUoSAsZucX7OQAoXjvM3XAWYdNitdeHQrM_vCcP_mtSs9yGib42jyXF3OVqZJbHu_4x9vdfHw"
                val v2WaterfallId = UUID.randomUUID().toString()
                val v2Birthday = "%02d-%02d-%04d".format(bdDay, bdMonth, bdYear)
                val v2EncPass = "#PWD_BROWSER:0:$nowSec:$password"

                val regInfo = JSONObject().apply {
                    put("first_name", firstname)
                    put("last_name", lastname)
                    put("full_name", "$firstname $lastname")
                    put("contactpoint", "+$cleanNum")
                    put("ar_contactpoint", JSONObject.NULL)
                    put("contactpoint_type", "phone")
                    put("is_using_unified_cp", false)
                    put("unified_cp_screen_variant", JSONObject.NULL)
                    put("is_cp_auto_confirmed", false)
                    put("is_cp_auto_confirmable", false)
                    put("is_cp_claimed", true)
                    put("confirmation_code", JSONObject.NULL)
                    put("birthday", v2Birthday)
                    put("birthday_derived_from_age", JSONObject.NULL)
                    put("age_range", "o18")
                    put("did_use_age", false)
                    put("os_shared_age_range", JSONObject.NULL)
                    put("gender", 2)
                    put("use_custom_gender", false)
                    put("custom_gender", JSONObject.NULL)
                    put("encrypted_password", v2EncPass)
                    put("username", JSONObject.NULL)
                    put("username_prefill", JSONObject.NULL)
                    put("accounts_list_client", JSONArray())
                    put("fb_conf_source", JSONObject.NULL)
                    put("device_id", JSONObject.NULL)
                    put("ig4a_qe_device_id", JSONObject.NULL)
                    put("family_device_id", JSONObject.NULL)
                    put("fdid_available_on_start", JSONObject.NULL)
                    put("fdid_rid_available_on_start", JSONObject.NULL)
                    put("asdid_available_on_start", JSONObject.NULL)
                    put("user_id", JSONObject.NULL)
                    put("safetynet_token", JSONObject.NULL)
                    put("skip_slow_rel_check", false)
                    put("safetynet_response", JSONObject.NULL)
                    put("machine_id", JSONObject.NULL)
                    put("profile_photo", JSONObject.NULL)
                    put("profile_photo_id", JSONObject.NULL)
                    put("profile_photo_upload_id", JSONObject.NULL)
                    put("avatar", JSONObject.NULL)
                    put("email_oauth_token_no_contact_perm", JSONObject.NULL)
                    put("email_oauth_token", JSONObject.NULL)
                    put("email_oauth_tokens", JSONArray())
                    put("sign_in_with_google_email", JSONObject.NULL)
                    put("should_skip_two_step_conf", JSONObject.NULL)
                    put("openid_tokens_for_testing", JSONObject.NULL)
                    put("opt_out_source_account_reg_info_logging_only", JSONObject.NULL)
                    put("encrypted_msisdn", JSONObject.NULL)
                    put("encrypted_msisdn_for_safetynet", JSONObject.NULL)
                    put("cached_headers_safetynet_info", JSONObject.NULL)
                    put("should_skip_headers_safetynet", JSONObject.NULL)
                    put("headers_last_infra_flow_id", JSONObject.NULL)
                    put("headers_last_infra_flow_id_safetynet", JSONObject.NULL)
                    put("headers_flow_id", UUID.randomUUID().toString())
                    put("was_headers_prefill_available", false)
                    put("sso_enabled", JSONObject.NULL)
                    put("existing_accounts", JSONObject.NULL)
                    put("used_ig_birthday", JSONObject.NULL)
                    put("create_new_to_app_account", JSONObject.NULL)
                    put("skip_session_info", JSONObject.NULL)
                    put("ck_error", JSONObject.NULL)
                    put("ck_id", JSONObject.NULL)
                    put("ck_nonce", JSONObject.NULL)
                    put("should_save_password", false)
                    put("fb_access_token", JSONObject.NULL)
                    put("is_msplit_reg", JSONObject.NULL)
                    put("is_spectra_reg", JSONObject.NULL)
                    put("dema_account_consent_given", JSONObject.NULL)
                    put("spectra_entry_source", JSONObject.NULL)
                    put("spectra_reg_token", JSONObject.NULL)
                    put("spectra_reg_guardian_id", JSONObject.NULL)
                    put("spectra_reg_guardian_logged_in_context", JSONObject.NULL)
                    put("spectra_requester_user_id", JSONObject.NULL)
                    put("user_id_of_msplit_creator", JSONObject.NULL)
                    put("msplit_creator_nonce", JSONObject.NULL)
                    put("dma_data_combination_consent_given", JSONObject.NULL)
                    put("xapp_accounts", JSONObject.NULL)
                    put("fb_device_id", JSONObject.NULL)
                    put("fb_machine_id", JSONObject.NULL)
                    put("ig_device_id", JSONObject.NULL)
                    put("ig_machine_id", JSONObject.NULL)
                    put("should_skip_nta_upsell", JSONObject.NULL)
                    put("big_blue_token", JSONObject.NULL)
                    put("caa_reg_flow_source", "login_home_native_integration_point")
                    put("ig_authorization_token", JSONObject.NULL)
                    put("full_sheet_flow", false)
                    put("crypted_user_id", JSONObject.NULL)
                    put("is_ca_late_teen", JSONObject.NULL)
                    put("is_early_teen", JSONObject.NULL)
                    put("is_caa_perf_enabled", false)
                    put("is_preform", true)
                    put("should_show_rel_error", false)
                    put("ignore_suma_check", false)
                    put("dismissed_login_upsell_with_cna", false)
                    put("ignore_existing_login", false)
                    put("ignore_existing_login_from_suma", false)
                    put("ignore_existing_login_after_errors", false)
                    put("suggested_first_name", JSONObject.NULL)
                    put("suggested_last_name", JSONObject.NULL)
                    put("suggested_full_name", JSONObject.NULL)
                    put("suggested_gender", JSONObject.NULL)
                    put("frl_authorization_token", JSONObject.NULL)
                    put("post_form_errors", JSONObject.NULL)
                    put("skip_step_without_errors", false)
                    put("existing_account_exact_match_checked", true)
                    put("existing_account_fuzzy_match_checked", false)
                    put("email_oauth_exists", false)
                    put("confirmation_code_send_error", JSONObject.NULL)
                    put("consent_jurisdiction_at_gate", JSONObject.NULL)
                    put("consent_jurisdiction_at_inflow", JSONObject.NULL)
                    put("pc_enforcement_outcome", JSONObject.NULL)
                    put("pc_inflow_decision", JSONObject.NULL)
                    put("is_too_young", false)
                    put("source_account_type", JSONObject.NULL)
                    put("whatsapp_installed_on_client", false)
                    put("confirmation_medium", JSONObject.NULL)
                    put("source_credentials_type", JSONObject.NULL)
                    put("source_cuid", JSONObject.NULL)
                    put("source_account_reg_info", JSONObject.NULL)
                    put("soap_creation_source", JSONObject.NULL)
                    put("source_account_type_to_reg_info", JSONObject.NULL)
                    put("registration_flow_id", UUID.randomUUID().toString())
                    put("should_skip_youth_tos", false)
                    put("is_youth_regulation_flow_complete", false)
                    put("is_on_cold_start", false)
                    put("email_prefilled", false)
                    put("cp_confirmed_by_auto_conf", false)
                    put("in_sowa_experiment", false)
                    put("youth_regulation_config", JSONObject.NULL)
                    put("conf_allow_back_nav_after_change_cp", JSONObject.NULL)
                    put("conf_bouncing_cliff_screen_type", JSONObject.NULL)
                    put("conf_show_bouncing_cliff", JSONObject.NULL)
                    put("eligible_to_flash_call_in_ig4a", false)
                    put("eligible_to_mo_sms_in_ig4a", false)
                    put("mo_sms_ent_id", JSONObject.NULL)
                    put("flash_call_permissions_status", JSONObject.NULL)
                    put("gms_incoming_call_retriever_eligibility", JSONObject.NULL)
                    put("attestation_result", JSONObject.NULL)
                    put("request_data_and_challenge_nonce_string", JSONObject.NULL)
                    put("confirmed_cp_and_code", JSONObject.NULL)
                    put("notification_callback_id", JSONObject.NULL)
                    put("reg_suma_state", 0)
                    put("is_msplit_neutral_choice", false)
                    put("msg_previous_cp", JSONObject.NULL)
                    put("ntp_import_source_info", JSONObject.NULL)
                    put("youth_consent_decision_time", JSONObject.NULL)
                    put("sk_pipa_consent_given", JSONObject.NULL)
                    put("should_show_spi_before_conf", true)
                    put("google_oauth_account", JSONObject.NULL)
                    put("is_reg_request_from_ig_suma", false)
                    put("is_toa_reg", false)
                    put("is_threads_public", false)
                    put("spc_import_flow", false)
                    put("caa_play_integrity_attestation_result", JSONObject.NULL)
                    put("client_known_key_hash", JSONObject.NULL)
                    put("flash_call_provider", JSONObject.NULL)
                    put("is_in_gms_experience", JSONObject.NULL)
                    put("flash_call_nonce_prefix_details", JSONObject.NULL)
                    put("spc_birthday_input", false)
                    put("failed_birthday_year_count", JSONObject.NULL)
                    put("user_presented_medium_source", JSONObject.NULL)
                    put("user_opted_out_of_ntp", JSONObject.NULL)
                    put("is_from_registration_reminder", false)
                    put("show_youth_reg_in_ig_spc", false)
                    put("fb_suma_is_high_confidence", JSONObject.NULL)
                    put("screen_visited", JSONArray(listOf(
                        "bloks.caa.reg.birthday",
                        "CAA_REG_CONTACT_POINT_PHONE",
                        "CAA_REG_PASSWORD",
                        "CAA_REG_SAVE_PASSWORD_CREDENTIALS"
                    )))
                    put("fb_email_login_upsell_skip_suma_post_tos", false)
                    put("fb_suma_is_from_email_login_upsell", false)
                    put("fb_suma_is_from_phone_login_upsell", false)
                    put("should_prefill_cp_in_ar", true)
                    put("ig_partially_created_account_user_id", JSONObject.NULL)
                    put("ig_partially_created_account_nonce", JSONObject.NULL)
                    put("ig_partially_created_account_nonce_expiry", JSONObject.NULL)
                    put("force_sessionless_nux_experience", false)
                    put("has_seen_suma_landing_page_pre_conf", false)
                    put("has_seen_suma_candidate_page_pre_conf", false)
                    put("has_seen_confirmation_screen", false)
                    put("suma_on_conf_threshold", -1)
                    put("should_show_error_msg", true)
                    put("th_profile_photo_token", JSONObject.NULL)
                    put("attempted_silent_auth_in_fb", false)
                    put("attempted_silent_auth_in_ig", false)
                    put("sa_prefetch_callback_id", JSONObject.NULL)
                    put("cp_suma_results_map", JSONObject.NULL)
                    put("source_username", JSONObject.NULL)
                    put("next_uri", JSONObject.NULL)
                    put("should_use_next_uri", JSONObject.NULL)
                    put("linking_entry_point", JSONObject.NULL)
                    put("fb_encrypted_partial_new_account_properties", JSONObject.NULL)
                    put("starter_pack_name", JSONObject.NULL)
                    put("starter_pack_creator_user_ids", JSONObject.NULL)
                    put("wa_data_bundle", JSONObject.NULL)
                    put("wa_encrypted_auth_data", JSONObject.NULL)
                    put("bloks_controller_source", JSONObject.NULL)
                    put("airwave_registration_code", JSONObject.NULL)
                    put("is_sessionless_nux", JSONObject.NULL)
                    put("login_contactpoint", JSONObject.NULL)
                    put("login_contactpoint_type", JSONObject.NULL)
                    put("should_show_bday_after_name_suggestions", JSONObject.NULL)
                    put("should_override_back_nav", false)
                    put("ig_footer_variant", "control")
                    put("ig_gender", JSONObject.NULL)
                    put("device_network_info", JSONObject.NULL)
                    put("is_from_web_lite_reg_controller", JSONObject.NULL)
                    put("login_form_siwg_email", JSONObject.NULL)
                    put("account_setup_waterfall_id", JSONObject.NULL)
                    put("is_wanted_suma_user", false)
                    put("device_zero_balance_state", JSONObject.NULL)
                    put("wa_to_ig_merged_tos_variant", JSONObject.NULL)
                    put("is_in_nta_single_form", false)
                    put("source_account_image_asset_id", JSONObject.NULL)
                    put("passkey_eligible_device", JSONObject.NULL)
                    put("nta_ac_opted_out", JSONObject.NULL)
                    put("nta_control_reason", JSONObject.NULL)
                    put("nta_risk_type", JSONObject.NULL)
                    put("nta_single_form_variant", JSONObject.NULL)
                    put("enable_survey", JSONObject.NULL)
                    put("phone_prefetch_outcome", JSONObject.NULL)
                    put("tos_accepted_on_profile_info", JSONObject.NULL)
                    put("restarted_after_ac_opt_out_logging_only", false)
                }

                val serverParams = JSONObject().apply {
                    put("event_request_id", UUID.randomUUID().toString())
                    put("should_ignore_suma_check", 0)
                    put("app_id", 0)
                    put("bloks_controller_source", "bk_caa_reg_tos_screen")
                    put("sa_prefetch_callback_id", "")
                    put("reg_info", regInfo.toString())
                    put("flow_info", "{\"flow_name\":\"new_to_meta_fb_default\",\"flow_type\":\"ntf\"}")
                    put("current_step", 7)
                    put("INTERNAL__latency_qpl_marker_id", 36707139)
                    put("INTERNAL__latency_qpl_instance_id", "194941227000087")
                    put("device_id", JSONObject.NULL)
                    put("family_device_id", JSONObject.NULL)
                    put("waterfall_id", v2WaterfallId)
                    put("offline_experiment_group", JSONObject.NULL)
                    put("layered_homepage_experiment_group", JSONObject.NULL)
                    put("is_platform_login", 0)
                    put("is_from_logged_in_switcher", 0)
                    put("is_from_logged_out", 0)
                    put("access_flow_version", "pre_mt_behavior")
                    put("login_surface", "login_home")
                    put("login_entry_point", "logged_out")
                }

                val clientInputParams = JSONObject().apply {
                    put("device_id", "")
                    put("waterfall_id", v2WaterfallId)
                    put("machine_id", "")
                    put("zero_balance_state", "")
                    put("ck_error", "")
                    put("ck_id", "")
                    put("ck_nonce", "")
                    put("has_dismissed_suma_pre_conf", 0)
                    put("should_ignore_existing_login", 0)
                    put("encrypted_msisdn", "")
                    put("headers_last_infra_flow_id", "")
                    put("reached_from_tos_screen", 1)
                    put("no_contact_perm_email_oauth_token", "")
                    put("failed_birthday_year_count", "{}")
                    put("ig_partially_created_account_user_id", 0)
                    put("ig_partially_created_account_nonce", "")
                    put("ig_partially_created_account_nonce_expiry", 0)
                    put("force_sessionless_nux_experience", 0)
                    put("passkey_eligible_device", 0)
                    put("cloud_trust_token", JSONObject.NULL)
                    put("network_bssid", JSONObject.NULL)
                    put("lois_settings", JSONObject().put("lois_token", ""))
                    put("aac", "")
                }

                val innerParams = JSONObject().apply {
                    put("server_params", serverParams)
                    put("client_input_params", clientInputParams)
                }
                val outerParams = JSONObject().put("params", innerParams.toString())

                val formBuilder = FormBody.Builder()
                    .add("__aaid", "0")
                    .add("__user", "0")
                    .add("__a", "1")
                    .add("__req", "z")
                    .add("__hs", "20775.BP:wbloks_caa_pkg.2.0...0")
                    .add("dpr", "3")
                    .add("__ccg", "EXCELLENT")
                    .add("__rev", "1055156383")
                    .add("__s", "i9m07b:vwgx46:de4hb6")
                    .add("__hsi", "7709475359033258970")
                    .add("__dyn", "0wzpawlE72fDg9ppo5S12wAxu13wqobE6u7E39x67o1g8hw23E52q1ew2io0D24o1MUaE1Do1u81x82ewnE3fwww5NyE25w8W0Lo6-0Co1kU1bo8Xw8S0QU3yw")
                    .add("fb_dtsg", v2FbDtsg)
                    .add("jazoest", v2Jazoest)
                    .add("lsd", v2Lsd)
                    .add("params", outerParams.toString())

                val req = Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Mobile Safari/537.36")
                    .header("sec-ch-ua-full-version-list", "")
                    .header("sec-ch-ua-platform", "\"Android\"")
                    .header("sec-ch-ua", "\"Not-A.Brand\";v=\"99\", \"Android WebView\";v=\"157\", \"Chromium\";v=\"157\"")
                    .header("sec-ch-ua-model", "\"\"")
                    .header("sec-ch-ua-mobile", "?1")
                    .header("x-fb-page-datr", v2PageDatr)
                    .header("sec-ch-prefers-color-scheme", "light")
                    .header("content-type", "application/x-www-form-urlencoded;charset=UTF-8")
                    .header("sec-ch-ua-platform-version", "\"\"")
                    .header("origin", "https://m.facebook.com")
                    .header("x-requested-with", "mark.via.gp")
                    .header("sec-fetch-site", "same-origin")
                    .header("sec-fetch-mode", "cors")
                    .header("sec-fetch-dest", "empty")
                    .header("referer", "https://m.facebook.com/reg/")
                    .header("accept-language", acceptLang)
                    .header("priority", "u=1, i")
                    .header("Cookie", "datr=$v2Datr")
                    .post(formBuilder.build())
                    .build()

                val res = localClient.newCall(req).execute()
                val resBody = res.body?.string() ?: ""
                res.body?.close()

                val ck = sesJar.getDict()
                val cUser = ck["c_user"] ?: ""
                val datrOut = ck["datr"] ?: v2Datr
                val checkpoint = ck["checkpoint"] ?: ""

                var finalUid = cUser
                if (finalUid.isEmpty() || finalUid == "0") {
                    Regex("(?:\\\\)?\"(?:c_user|id|account_id|uid)(?:\\\\)?\"\\s*:\\s*(?:\\\\)?\"?(\\d{8,})(?:\\\\)?\"?").find(resBody)?.let {
                        finalUid = it.groupValues[1]
                    }
                }

                if (finalUid.isNotEmpty() && finalUid != "0") {
                    result.put("ok", true); result.put("uid", finalUid)
                    result.put("name", "$firstname $lastname"); result.put("number", cleanNum)
                    result.put("cookie", buildFbCookie(ck, datrOut, finalUid, langCode))
                } else if (checkpoint.isNotEmpty() || resBody.lowercase().contains("checkpoint")) {
                    result.put("ok", false); result.put("reason", "CHECKPOINT")
                } else {
                    val errorMsg = Regex("(?:\\\\)?\"description(?:\\\\)?\"\\s*:\\s*(?:\\\\)?\"([^\"]+?)(?:\\\\)?\"").find(resBody)?.groupValues?.get(1)
                        ?: Regex("(?:\\\\)?\"error_summary(?:\\\\)?\"\\s*:\\s*(?:\\\\)?\"([^\"]+?)(?:\\\\)?\"").find(resBody)?.groupValues?.get(1)
                        ?: Regex("(?:\\\\)?\"errorDescription(?:\\\\)?\"\\s*:\\s*(?:\\\\)?\"([^\"]+?)(?:\\\\)?\"").find(resBody)?.groupValues?.get(1)
                        ?: "FAILED / NO c_user"
                    result.put("ok", false); result.put("reason", errorMsg)
                }
                return result
            }

            // ══════════════════════════════════════════════════════════
            // ENGINE: V3 GRAPHQL
            // ══════════════════════════════════════════════════════════
            val lsd = "AdQO4dqkAxXs9l-_AeUWXEBYyzg"
            val jazoest = "22386"
            val datr = "Q5P9au3ki-yCbDb3rvYmDK_L"
            val ua = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/148.0.0.0 Safari/537.36"
            val contactValue = cleanNum
            val formattedPass = "#PWD_BROWSER:0:$nowSec:$password"
            val nextUriVal = "https://www.facebook.com/caa/reg/confirmation/?reg_info=%7B%22contactpoint%22%3A%22%2B8801963656531%22%2C%22contactpoint_type%22%3A%22phone%22%2C%22is_cp_auto_confirmed%22%3Afalse%2C%22fb_conf_source%22%3Anull%2C%22confirmation_medium%22%3Anull%2C%22registration_flow_id%22%3A%22c7a330c8-71b3-4dfd-8d27-7e95d2eea521%22%7D&flow_info=%7B%22flow_name%22%3A%22new_to_meta_fb_default%22%2C%22flow_type%22%3A%22ntf%22%7D&current_step=9"
            val referer = "https://www.facebook.com/reg/?entry_point=login&next=https%3A%2F%2Fwww.facebook.com%2Fcaa%2Freg%2Fconfirmation%2F%3Freg_info%3D%257B%2522contactpoint%2522%253A%2522%252B8801963656531%2522%252C%2522contactpoint_type%2522%253A%2522phone%2522%252C%2522is_cp_auto_confirmed%2522%253Afalse%252C%2522fb_conf_source%2522%253Anull%252C%2522confirmation_medium%2522%253Anull%252C%2522registration_flow_id%2522%253A%2522c7a330c8-71b3-4dfd-8d27-7e95d2eea521%2522%257D%26flow_info%3D%257B%2522flow_name%2522%253A%2522new_to_meta_fb_default%2522%252C%2522flow_type%2522%253A%2522ntf%2522%257D%26current_step%3D9"

            val regData = JSONObject().apply {
                put("birthday_day", bdDay); put("birthday_month", bdMonth); put("birthday_year", bdYear)
                put("contactpoint", JSONObject().put("sensitive_string_value", contactValue))
                put("contactpoint_type", "PHONE"); put("custom_gender", ""); put("did_use_age", false)
                put("firstname", JSONObject().put("sensitive_string_value", firstname))
                put("fullname", JSONObject().put("sensitive_string_value", "")); put("ig_age_block_data", JSONObject.NULL)
                put("lastname", JSONObject().put("sensitive_string_value", lastname))
                put("preferred_pronoun", JSONObject.NULL)
                put("reg_passwd__", JSONObject().put("sensitive_string_value", formattedPass))
                put("sex", "FEMALE"); put("use_custom_gender", false)
                put("username", JSONObject().put("sensitive_string_value", ""))
            }

            val variables = JSONObject().apply {
                put("input", JSONObject().apply {
                    put("actor_id", "0")
                    put("client_mutation_id", UUID.randomUUID().toString())
                    put("machine_id", "")
                    put("next_uri", nextUriVal)
                    put("reg_data", regData)
                    put("sk_pipa_consent_given", JSONObject.NULL)
                    put("waterfall_id", UUID.randomUUID().toString())
                })
            }

            val fbBuilder = FormBody.Builder()
                .add("av", "0")
                .add("__user", "0")
                .add("__a", "1")
                .add("__req", "u")
                .add("__hs", "20775.HYP:comet_plat_default_pkg.2.1...0")
                .add("dpr", "2")
                .add("__ccg", "EXCELLENT")
                .add("__rev", "1055167106")
                .add("__s", "4fr0ud:b4oo7q:vvifaa")
                .add("__hsi", "7709480098099178552")
                .add("__dyn", "7xe6E5q5U5ObwKBAg5S1Dxu13w8CewSwMwNw9G2S0lW4o0B-q1ew2io2awpUO0n24oaEd82lwv89k2C1Fwc60D82IzXwae4UaEW0Loco5G0zK5o4q0HU1IEGdwtU2ewbS1Lwqo15E6O0iS2eU5SEb87u0xE2Tw3C8doS1DwTw")
                .add("__csr", "jMhOiqSYnf79sHJ4RtnlOLA-gHuD9hDeVdBKnkYBbiQBnqtFlRqtnl8KJRiRK9FrjmmmZGCTG8TaiTZr-_qkhrbqcIWJ9dTu8qitkBvH-kACAZt8IOzoyqHy97b9a8xmU8axiiq5royi4V8cU8UlxGmqq7UoBxi5EO268xecwh8rwKwBwMwlE138dU14oe84K5E4S5Uco2zw8e1XwWwaebG0XodRwCwNweFG1TwaRpo1no2rwCwfm48G3Tx62208zw5zw4Cwai03gG3GEkw2882lw02qUU02eEo08AE0qzw11a0pow09vU")
                .add("__hsdp", "g8_1AhhNok8EuCwEoS2zUjzouwkE8GCAy8-0Mp82bwSDo2fpxM067S08jw2AU0ddU03rTw0j080RC")
                .add("__hblp", "03s83qw28E1fU2Ew0-ew37o1Q80xe0ajwqU3_wRw12W0jW04nE7O2O0hW02-m02I2035a05AE4O0LU1f8lw0Maw9i09gU0jWw0Itw3mo3Gw")
                .add("__sjsp", "g8_1An4iBxgyxWq2xzoafxedxW1iwyGqi8zU31Aw8K3qtw8ZC70")
                .add("__comet_req", "102")
                .add("lsd", lsd)
                .add("jazoest", jazoest)
                .add("__spin_r", "1055167106")
                .add("__spin_b", "trunk")
                .add("__spin_t", "1795003213")
                .add("qpl_active_flow_ids", "250359044,516759801")
                .add("fb_api_caller_class", "RelayModern")
                .add("fb_api_req_friendly_name", "useCAARegistrationFormSubmitMutation")
                .add("server_timestamps", "true")
                .add("variables", variables.toString())
                .add("doc_id", DOC_ID_REGISTER)
                .add("fb_api_analytics_tags", "[\"qpl_active_flow_ids=250359044,516759801\"]")

            val req = Request.Builder().url(GRAPHQL_URL)
                .header("User-Agent", ua)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("sec-ch-ua-full-version-list", "\"Not-A.Brand\";v=\"99.0.0.0\", \"Chromium\";v=\"157.0.8103.50\"")
                .header("sec-ch-ua-platform", "\"Windows\"")
                .header("sec-ch-ua", "\"Not-A.Brand\";v=\"99\", \"Chromium\";v=\"157\"")
                .header("x-fb-friendly-name", "useCAARegistrationFormSubmitMutation")
                .header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-model", "\"itel S665L\"")
                .header("x-asbd-id", "359341")
                .header("x-fb-entry-nonce", "Sv4x4V2f")
                .header("x-fb-lsd", lsd)
                .header("sec-ch-prefers-color-scheme", "light")
                .header("sec-ch-ua-platform-version", "\"12.0.0\"")
                .header("origin", "https://www.facebook.com")
                .header("x-requested-with", "mark.via.gp")
                .header("sec-fetch-site", "same-origin")
                .header("sec-fetch-mode", "cors")
                .header("sec-fetch-dest", "empty")
                .header("referer", referer)
                .header("accept-language", acceptLang)
                .header("priority", "u=1, i")
                .header("Cookie", "datr=$datr")
                .post(fbBuilder.build())
                .build()

            val res = localClient.newCall(req).execute()
            val resBody = res.body?.string() ?: ""
            res.body?.close()

            val ck = sesJar.getDict()
            val cUser = ck["c_user"] ?: ""
            val datrOut = ck["datr"] ?: datr
            val checkpoint = ck["checkpoint"] ?: ""

            var finalUid = cUser
            if (finalUid.isEmpty() || finalUid == "0") {
                Regex("\"(?:c_user|id|account_id|uid)\"\\s*:\\s*\"?(\\d{8,})\"?").find(resBody)?.let {
                    finalUid = it.groupValues[1]
                }
            }

            if (finalUid.isNotEmpty() && finalUid != "0") {
                result.put("ok", true); result.put("uid", finalUid)
                result.put("name", "$firstname $lastname"); result.put("number", cleanNum)
                result.put("cookie", buildFbCookie(ck, datrOut, finalUid, langCode))
            } else if (checkpoint.isNotEmpty() || resBody.lowercase().contains("checkpoint")) {
                result.put("ok", false); result.put("reason", "CHECKPOINT")
            } else {
                val errorMsg = Regex("\"description\"\\s*:\\s*\"([^\"]+)\"").find(resBody)?.groupValues?.get(1)
                    ?: Regex("\"error_summary\"\\s*:\\s*\"([^\"]+)\"").find(resBody)?.groupValues?.get(1)
                    ?: Regex("\"errorDescription\"\\s*:\\s*\"([^\"]+)\"").find(resBody)?.groupValues?.get(1)
                    ?: "FAILED / NO c_user"
                result.put("ok", false); result.put("reason", errorMsg)
            }
        } catch (e: Exception) {
            result.put("ok", false); result.put("reason", e.localizedMessage ?: "exception")
        }
        return result
    }

    private fun buildFbCookie(ck: Map<String, String>, fallbackDatr: String, fallbackUid: String = "", langCode: String = "en_US"): String {
        val order = listOf("datr", "sb", "m_pixel_ratio", "wd", "locale", "ldu", "c_user", "xs", "fr", "presence")
        val parts = mutableListOf<String>()
        val used = mutableSetOf<String>()
        val map = ck.toMutableMap()
        if (!map.containsKey("datr") && fallbackDatr.isNotEmpty()) map["datr"] = fallbackDatr
        if ((map["c_user"].isNullOrEmpty() || map["c_user"] == "0") && fallbackUid.isNotEmpty()) map["c_user"] = fallbackUid
        if (!map.containsKey("locale")) map["locale"] = langCode
        if (!map.containsKey("ldu")) map["ldu"] = langCode

        for (k in order) {
            val v = map[k]
            if (!v.isNullOrEmpty()) { parts.add("$k=$v"); used.add(k) }
        }
        for ((k, v) in map) {
            if (k !in used && v.isNotEmpty()) parts.add("$k=$v")
        }
        return parts.joinToString("; ")
    }

    private fun startOtpWatcher(phone: String, uid: String, password: String) {
        Thread {
            try {
                appendLog("[~] OTP watching +$phone")
                val acc = synchronized(savedAccounts) { savedAccounts.find { it.number == phone } }
                val batchAcc = synchronized(currentBatchAccounts) { currentBatchAccounts.find { it.number == phone } }
                acc?.let { it.otpStatus = "Monitoring"; it.otpExpireTime = System.currentTimeMillis() + OTP_WATCH_MS; saveAccountsToPrefs() }
                if (batchAcc != null && batchAcc !== acc) { batchAcc.otpStatus = "Monitoring"; batchAcc.otpExpireTime = System.currentTimeMillis() + OTP_WATCH_MS }
                val pre = fetchOtps().mapNotNull { otpFingerprint(it, phone) }.toMutableSet()
                val deadline = System.currentTimeMillis() + OTP_WATCH_MS
                var found = false
                while (System.currentTimeMillis() < deadline) {
                    for (od in fetchOtps()) {
                        val rawNum = (od.optString("number").ifEmpty { od.optString("phone") }).replace(Regex("\\D"), "")
                        if (rawNum.isEmpty() || !(phone.contains(rawNum) || rawNum.contains(phone))) continue
                        val id = otpFingerprint(od, phone)
                        if (id != null && pre.contains(id)) continue
                        val msg = od.optString("message").ifEmpty { od.optString("sms").ifEmpty { od.optString("code") } }
                        val code = extractOtp(msg, od)
                        if (code.isNotEmpty()) {
                            appendLog("[★] OTP +$phone : $code")
                            saveToFile("OTP|+$phone|$code|$uid|$password\n")
                            acc?.let { it.otpStatus = "OTP: $code"; it.otpExpireTime = 0L; saveAccountsToPrefs() }
                            if (batchAcc != null && batchAcc !== acc) { batchAcc.otpStatus = "OTP: $code"; batchAcc.otpExpireTime = 0L }
                            runOnUiThread {
                                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("OTP", code))
                                Toast.makeText(this@MainActivity, "OTP Copied: $code", Toast.LENGTH_SHORT).show()
                                renderBatchAccounts()
                                renderMyNumbersList()
                            }
                            if (BgOverlayService.isRunning) {
                                BgOverlayService.updateStatusFromExternal("★ OTP Copied: $code (+$phone)", BgOverlayService.isCreating)
                            }
                            found = true; break
                        }
                    }
                    if (found) break
                    Thread.sleep(5000)
                }
                if (!found) {
                    acc?.let { it.otpStatus = "Expired"; it.otpExpireTime = 0L; saveAccountsToPrefs() }
                    if (batchAcc != null && batchAcc !== acc) { batchAcc.otpStatus = "Expired"; batchAcc.otpExpireTime = 0L }
                    runOnUiThread { renderBatchAccounts(); renderMyNumbersList() }
                }
            } catch (e: Exception) { appendLog("[!] OTP error: ${e.localizedMessage}") }
        }.start()
    }

    private fun fetchLiveNumber(rid: String): JSONObject? {
        return try {
            val jsonBody = JSONObject().put("rid", rid)
            val body = jsonBody.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val req = Request.Builder().url(SecureVault.endpointGetNum(this))
                .addHeader(SecureVault.authHeader(this), SecureVault.apiKey(this))
                .addHeader("Content-Type", "application/json").post(body).build()
            val res = SecureVault.getStrictApiClient().newCall(req).execute()
            val str = res.body?.string() ?: return null
            res.body?.close()
            val js = JSONObject(str)
            if (js.optJSONObject("meta")?.optInt("code", 0) == 200) js.optJSONObject("data") else null
        } catch (e: Exception) { null }
    }

    private fun checkNumberGraphql(phoneNumber: String): Pair<String, String> {
        return try {
            val cleanPhone = phoneNumber.replace(Regex("\\D"), "")
            val variables = JSONObject().apply {
                put("params", JSONObject().apply {
                    put("cipher_text", CIPHER_TEXT); put("context", "recover")
                    put("event_request_id", UUID.randomUUID().toString())
                    put("friend_name", ""); put("search_query", cleanPhone)
                    put("waterfall_id", UUID.randomUUID().toString())
                })
            }
            val form = FormBody.Builder().apply {
                add("av", "0").add("__aaid", "0").add("__user", "0").add("__a", "1").add("__req", "n")
                add("__hs", "20760.HYP:comet_loggedout_pkg.2.1...0").add("dpr", "2").add("__ccg", "EXCELLENT")
                add("__rev", "1052812464").add("__s", "c4enmh:xbdz2y:g1ljyy").add("__hsi", "7703807760327660954")
                add("__dyn", "7xe5Wxa13xu1syU8EKmhe5UqwCwpUkGqErxSawvEW3qi4FoiwNws8aorx6fzoy4U6m0x8txG4o461twa10Hwt89FE4Wqbx67k4obUyEpiwznwhXwZw9m6A48a8-m3G4UOi1AwgonzoO0AE3tx62G5UswoEpxy1YwBgaoOi3a2i11grzUeUmwvC6UgzEtAx5e2GU8U-U7O2q4Uox6ewbS2e9K1qxa1Jzu5o4qu1dwkVokylK2W1RwrUO4ohz8ek9zo8U5e3C1jhU2RwhoapocobGAwGwiUpwCwJK7oC1IwGwKgV3UcGzVpU9oO2LhA6bwlV4A2K7ESm13g3yxC11x6bwZwaCfwjScBz8qwjo4G1zypUowkGG3u2OfxuXxmbUdESi3d1Ki2G")
                add("__hsdp", "geaH1K4274MTK7AmswCUm227U70wdoj4Ag6e3Kaw-xpDo4m3mbxGb0w2wuEsq20zBgaol8V87x0GwmU6G3G2-2y7E2Twdmqb80A8510Dg5wEdRg1wU5G3rwBDUbofEkw8e0O89UeUy3qUWQ0xQ7o3_Aw55g1p98aong7SbwIwoU2sG10Kq36ezokxu13wmohwVwsQ8w4Ww4Ewro2VwbC1hwNw5-xx09G3i1Ngiwho2Vw4Zw6Iweq0gC0GUW2e582XwQG")
                add("__hblp", "3U2pwVyE1go12k1gwRyU14o88K0g21GwWxy5oqzEuwsE4i0RpEIw2gwvo5a3upkfwgU118aE8oaFUpwyUgxi9xi4pEcEW4UeEdogG7o5C5E3Zz84q2x0GwjVoeofo99ay8eE4q2-17w_ghCway1Lxa7U4p0t8ybwBxB384Obg6248jG2OdBwYUOdyEswMxa1hz8S2W0wA0OrDwywiEaU8Gg522y13xnAzEuxi78ycyEW1lwpoK5oJosxa2-9wFCxO0XUS1-x278orgc88VovxCqmVGx64oswiA4E9VEyCawzBxx0mE3qwPwhUSm2KdwJw4ZU3nxObw820x8oxK7U611CE4K68ao5OE6B1a1zw")
                add("__sjsp", "geaH2W5icK5i74MPBxV5DwVw85wko1zEK6EI2015i76wJgaol88w0CCwXy8")
                add("__comet_req", "15").add("lsd", LSD).add("jazoest", JAZOEST).add("__spin_r", "1052812464")
                add("__spin_b", "trunk").add("__spin_t", "1793682519").add("__crn", "comet.fbweb.CometCAAAccountSearchRoute")
                .add("qpl_active_flow_ids", "516759801").add("fb_api_caller_class", "RelayModern")
                .add("fb_api_req_friendly_name", "CAAFBAccountSearchViewQuery").add("server_timestamps", "true")
                .add("variables", variables.toString()).add("doc_id", DOC_ID_SEARCH)
                .add("fb_api_analytics_tags", "[\"qpl_active_flow_ids=516759801\"]")
            }.build()

            val req = Request.Builder().url(GRAPHQL_URL)
                .header("User-Agent", SEARCH_UA).header("Content-Type", "application/x-www-form-urlencoded")
                .header("sec-ch-ua-full-version-list", "\"Not:A-Brand\";v=\"8.0.0.0\", \"Chromium\";v=\"156.0.8078.47\"")
                .header("sec-ch-ua-platform", "\"macOS\"")
                .header("x-fb-navigation-chain", "CAAFBAccountSearchRoot.react,comet.caa.account_recovery.account_search,via_cold_start,1793682519168,436998,,,")
                .header("sec-ch-ua", "\"Not:A-Brand\";v=\"8\", \"Chromium\";v=\"156\"")
                .header("x-fb-friendly-name", "CAAFBAccountSearchViewQuery").header("sec-ch-ua-mobile", "?0")
                .header("sec-ch-ua-model", "\"itel S665L\"").header("x-asbd-id", "359341").header("x-fb-lsd", LSD)
                .header("sec-ch-prefers-color-scheme", "light").header("sec-ch-ua-platform-version", "\"12.0.0\"")
                .header("origin", "https://www.facebook.com").header("x-requested-with", "mark.via.gp")
                .header("sec-fetch-site", "same-origin").header("sec-fetch-mode", "cors").header("sec-fetch-dest", "empty")
                .header("referer", "https://www.facebook.com/login/identify/?ci=$CIPHER_TEXT")
                .header("accept-language", "en-US,en;q=0.9").header("priority", "u=1, i").post(form).build()

            val res = buildInsecureClient(null, useProxy = false).newCall(req).execute()
            if (res.code != 200) return Pair("error", "HTTP ${res.code}")
            var txt = res.body?.string()?.trim() ?: return Pair("error", "empty")
            if (txt.startsWith("for (;;);")) txt = txt.substring(9).trim()
            else if (txt.startsWith("for(;;);")) txt = txt.substring(8).trim()
            val firstLine = txt.split("\n").firstOrNull() ?: txt
            val js = try { JSONObject(firstLine) } catch (e: Exception) { return Pair("error", "Parse") }
            val search = js.optJSONObject("data")?.optJSONObject("caa_ar_fb_account_search") ?: JSONObject()
            val accounts = search.optJSONArray("accounts") ?: JSONArray()
            val desc = search.optJSONObject("error_content")?.optString("description", "") ?: ""
            if (desc.lowercase().contains("no account found") && accounts.length() == 0) return Pair("free", "No account")
            if (accounts.length() > 0) return Pair("taken", "${accounts.length()}")
            return Pair("taken", desc.ifEmpty { "Exists" })
        } catch (e: Exception) { Pair("error", e.localizedMessage ?: "unknown") }
    }

    private fun fetchOtps(): List<JSONObject> {
        return try {
            val req = Request.Builder().url(SecureVault.endpointOtp(this))
                .addHeader(SecureVault.authHeader(this), SecureVault.apiKey(this)).get().build()
            val res = SecureVault.getStrictApiClient().newCall(req).execute()
            if (res.code != 200) { res.body?.close(); return emptyList() }
            val txt = res.body?.string()?.trim() ?: return emptyList()
            res.body?.close()
            val list = mutableListOf<JSONObject>()
            if (txt.startsWith("[")) {
                val arr = JSONArray(txt)
                for (i in 0 until arr.length()) arr.optJSONObject(i)?.let { list.add(it) }
                return list
            }
            val js = JSONObject(txt)
            when (val data = js.opt("data")) {
                is JSONArray -> for (i in 0 until data.length()) data.optJSONObject(i)?.let { list.add(it) }
                is JSONObject -> {
                    val inner = data.optJSONArray("otps") ?: data.optJSONArray("hits")
                    if (inner != null) for (i in 0 until inner.length()) inner.optJSONObject(i)?.let { list.add(it) }
                }
            }
            list
        } catch (e: Exception) { emptyList() }
    }

    private fun otpFingerprint(od: JSONObject, phone: String): String? {
        val explicit = od.optString("otp_id","")
        if (explicit.isNotEmpty()) return explicit
        val msg = od.optString("message").ifEmpty { od.optString("sms").ifEmpty { od.optString("code") } }
        val sid = od.optString("sid").ifEmpty { od.optString("service") }.ifEmpty { "Facebook" }
        return "$phone|$sid|$msg".hashCode().toString()
    }

    private fun extractOtp(msg: String, od: JSONObject?): String {
        if (od != null) {
            val direct = od.optString("otp","").replace(Regex("\\D"),"")
            if (direct.length in 4..8) return direct
        }
        if (msg.isEmpty()) return ""
        Regex("(?:FB[-\\s:]?|code(?:\\s+is|\\s+est\\s+le)?[:\\s]+)(\\d{4,8})\\b", RegexOption.IGNORE_CASE).find(msg)?.let { return it.groupValues[1] }
        Regex("\\b(\\d{3})[\\s-](\\d{3})\\b").find(msg)?.let { return it.groupValues[1] + it.groupValues[2] }
        Regex("\\b(\\d{4,8})\\b").find(msg)?.let { return it.groupValues[1] }
        return ""
    }

    private fun saveToFile(data: String) {
        try { File(filesDir, "Created_Accounts.txt").appendText(data) }
        catch (e: Exception) { appendLog("[!] Save error: ${e.localizedMessage}") }
    }

    private fun loadSavedAccounts() {
        synchronized(savedAccounts) {
            savedAccounts.clear()
            val raw = getSharedPreferences("app_data", Context.MODE_PRIVATE).getString("saved_accounts", "[]") ?: "[]"
            val wasPlain = raw.trimStart().startsWith("[")
            val jsonStr = SecureVault.decryptLocal(this, raw, "[]")
            try {
                val arr = JSONArray(jsonStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    savedAccounts.add(SavedAccount(obj.optString("name"), obj.optString("number"), obj.optString("uid"), obj.optString("password"), obj.optString("cookie"), obj.optString("otpStatus","None"), obj.optLong("otpExpireTime",0L)))
                }
                if (wasPlain && savedAccounts.isNotEmpty()) {
                    saveAccountsToPrefs()
                }
            } catch (e: Exception) {}
        }
    }

    private fun saveAccountsToPrefs() {
        synchronized(savedAccounts) {
            val arr = JSONArray()
            for (acc in savedAccounts) {
                val obj = JSONObject()
                obj.put("name",acc.name); obj.put("number",acc.number); obj.put("uid",acc.uid)
                obj.put("password",acc.password); obj.put("cookie",acc.cookie)
                obj.put("otpStatus",acc.otpStatus); obj.put("otpExpireTime",acc.otpExpireTime)
                arr.put(obj)
            }
            val encrypted = SecureVault.encryptLocal(this, arr.toString())
            getSharedPreferences("app_data", Context.MODE_PRIVATE).edit().putString("saved_accounts", encrypted).apply()
        }
    }

    private fun renderMyNumbersList() {
        val snapshot = synchronized(savedAccounts) { savedAccounts.toList() }
        listMyNumbers.removeAllViews()
        val d = resources.displayMetrics.density
        val monoFont = ResourcesCompat.getFont(this, R.font.jetbrains_mono)
        val sansFont = ResourcesCompat.getFont(this, R.font.plus_jakarta_sans)

        if (snapshot.isEmpty()) {
            listMyNumbers.addView(TextView(this).apply {
                text = "No saved accounts yet.\nRun Create to generate accounts."
                setTextColor(Color.parseColor("#64748B"))
                textSize = 12f
                typeface = sansFont
                gravity = Gravity.CENTER
                setBackgroundResource(R.drawable.bg_card)
                setPadding((16 * d).toInt(), (28 * d).toInt(), (16 * d).toInt(), (28 * d).toInt())
            })
            return
        }

        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val now = System.currentTimeMillis()
        for (acc in snapshot) {
            val layout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card_blue)
                setPadding((14 * d).toInt(), (12 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = (10 * d).toInt()
                }
            }
            val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            row1.addView(TextView(this).apply {
                text = "+${acc.number}"
                setTextColor(Color.parseColor("#FFFFFF"))
                setTypeface(monoFont, Typeface.BOLD)
                textSize = 15f
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            row1.addView(TextView(this).apply {
                text = acc.name
                setTextColor(Color.parseColor("#93C5FD"))
                typeface = sansFont
                textSize = 12f
            })
            layout.addView(row1)

            val row2 = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (6 * d).toInt(), 0, (10 * d).toInt())
            }
            row2.addView(TextView(this).apply {
                text = "UID: ${acc.uid}"
                setTextColor(Color.parseColor("#CBD5E1"))
                typeface = monoFont
                textSize = 11f
            }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

            var displayStatus = acc.otpStatus
            if (acc.otpExpireTime > 0) {
                displayStatus = if (now < acc.otpExpireTime) {
                    val rem = (acc.otpExpireTime - now) / 1000; "${rem/60}m ${rem%60}s left"
                } else "Expired"
            }
            val otpCode = if (displayStatus.startsWith("OTP:")) displayStatus.removePrefix("OTP:").trim() else ""
            val (badgeBg, badgeFg) = when {
                otpCode.isNotEmpty()       -> R.drawable.bg_badge_green to "#34D399"
                displayStatus == "Expired" -> R.drawable.bg_badge_rose to "#FDA4AF"
                else                       -> R.drawable.bg_badge_amber to "#FBBF24"
            }
            row2.addView(TextView(this).apply {
                text = if (otpCode.isNotEmpty()) "📋 $displayStatus" else displayStatus
                setTextColor(Color.parseColor(badgeFg))
                setBackgroundResource(badgeBg)
                setTypeface(monoFont, Typeface.BOLD)
                textSize = 10f
                setPadding((8 * d).toInt(), (3 * d).toInt(), (8 * d).toInt(), (3 * d).toInt())
                if (otpCode.isNotEmpty()) {
                    setOnClickListener {
                        cm.setPrimaryClip(ClipData.newPlainText("OTP", otpCode))
                        Toast.makeText(this@MainActivity, "OTP Copied: $otpCode", Toast.LENGTH_SHORT).show()
                    }
                }
            })
            layout.addView(row2)

            val btnLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, (40 * d).toInt())
            }
            fun makeBtn(text: String, bgRes: Int, fg: String, action: () -> Unit): Button {
                return Button(this).apply {
                    this.text = text; textSize = 10f; setPadding(0, 0, 0, 0)
                    setTextColor(Color.parseColor(fg))
                    setBackgroundResource(bgRes)
                    setTypeface(sansFont, Typeface.BOLD)
                    stateListAnimator = null
                    elevation = 0f
                    isAllCaps = false
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f).apply {
                        setMargins((3 * d).toInt(), 0, (3 * d).toInt(), 0)
                    }
                    setOnClickListener { action() }
                }
            }
            btnLayout.addView(makeBtn("NUM",    R.drawable.bg_btn_solid_blue,       "#FFFFFF") { cm.setPrimaryClip(ClipData.newPlainText("Number","+${acc.number}")); Toast.makeText(this,"Copied!",Toast.LENGTH_SHORT).show() })
            if (otpCode.isNotEmpty()) {
                btnLayout.addView(makeBtn("OTP", R.drawable.bg_btn_selected_emerald, "#041E15") {
                    cm.setPrimaryClip(ClipData.newPlainText("OTP", otpCode))
                    Toast.makeText(this, "OTP Copied: $otpCode", Toast.LENGTH_SHORT).show()
                })
            }
            btnLayout.addView(makeBtn("UID",    R.drawable.bg_btn_solid_violet,     "#FFFFFF") { cm.setPrimaryClip(ClipData.newPlainText("UID",acc.uid)); Toast.makeText(this,"Copied!",Toast.LENGTH_SHORT).show() })
            btnLayout.addView(makeBtn("COOKIE", R.drawable.bg_btn_solid_teal,       "#FFFFFF") { cm.setPrimaryClip(ClipData.newPlainText("Cookie",acc.cookie)); Toast.makeText(this,"Copied!",Toast.LENGTH_SHORT).show() })
            btnLayout.addView(makeBtn("LOGIN",  R.drawable.bg_btn_selected_emerald, "#041E15") {
                runOnUiThread {
                    showPage("create")
                    webViewContainer.visibility = View.VISIBLE
                    loginToWebView(acc.cookie, acc.uid)
                }
            })
            btnLayout.addView(makeBtn("DEL",    R.drawable.bg_btn_stop,             "#FFFFFF") {
                AlertDialog.Builder(this).setTitle("Delete?").setMessage("Delete +${acc.number}?")
                    .setPositiveButton("Yes") { _,_ -> synchronized(savedAccounts) { savedAccounts.remove(acc); saveAccountsToPrefs() }; renderMyNumbersList() }
                    .setNegativeButton("No",null).show()
            })
            layout.addView(btnLayout); listMyNumbers.addView(layout)
        }
    }

    private fun confirmDeleteAll() {
        synchronized(savedAccounts) { if (savedAccounts.isEmpty()) return }
        AlertDialog.Builder(this).setTitle("Delete All?").setMessage("Delete all accounts?")
            .setPositiveButton("Yes") { _,_ -> synchronized(savedAccounts) { savedAccounts.clear(); saveAccountsToPrefs() }; renderMyNumbersList() }
            .setNegativeButton("No",null).show()
    }

    private fun updateMusicButtonUI() {
        runOnUiThread {
            btnMusic.stateListAnimator = null
            btnMusic.elevation = 0f
            btnMusic.isAllCaps = false
            when {
                mediaPlayer?.isPlaying == true -> {
                    btnMusic.text = "▶ Playing"
                    btnMusic.setBackgroundResource(R.drawable.bg_btn_selected_emerald)
                    btnMusic.setTextColor(Color.parseColor("#041E15"))
                }
                isMusicPaused && mediaPlayer != null -> {
                    btnMusic.text = "❚❚ Paused"
                    btnMusic.setBackgroundResource(R.drawable.bg_btn_solid_amber)
                    btnMusic.setTextColor(Color.parseColor("#FFFFFF"))
                }
                else -> {
                    btnMusic.text = "♫ Music"
                    btnMusic.setBackgroundResource(R.drawable.bg_btn_solid_violet)
                    btnMusic.setTextColor(Color.parseColor("#FFFFFF"))
                }
            }
        }
    }

    private fun formatMusicTime(ms: Int): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }

    private fun showMusicDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val d = resources.displayMetrics.density
        val sansFont = ResourcesCompat.getFont(this, R.font.plus_jakarta_sans)
        val monoFont = ResourcesCompat.getFont(this, R.font.jetbrains_mono)

        val tracks = try {
            assets.list("song")?.filter {
                it.endsWith(".webm", true) || it.endsWith(".mp3", true) || it.endsWith(".ogg", true) || it.endsWith(".wav", true)
            }?.takeIf { it.isNotEmpty() } ?: listOf("Song.webm", "old.webm")
        } catch (_: Exception) {
            listOf("Song.webm", "old.webm")
        }
        if (currentSongName !in tracks && tracks.isNotEmpty()) {
            currentSongName = tracks.first()
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding((16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt(), (16 * d).toInt())
        }

        // Header Row
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (14 * d).toInt()
            }
        }
        header.addView(View(this).apply {
            setBackgroundResource(R.drawable.bg_dot_violet)
            layoutParams = LinearLayout.LayoutParams((4 * d).toInt(), (20 * d).toInt()).apply {
                marginEnd = (10 * d).toInt()
            }
        })
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        titleCol.addView(TextView(this).apply {
            text = "MUSIC PLAYER"
            setTextColor(Color.parseColor("#F1F5F9"))
            setTypeface(sansFont, Typeface.BOLD)
            textSize = 15f
        })
        titleCol.addView(TextView(this).apply {
            text = "Looping Ambient Player"
            setTextColor(Color.parseColor("#C4B5FD"))
            typeface = sansFont
            textSize = 11f
        })
        header.addView(titleCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        val btnClose = Button(this).apply {
            text = "✕"
            textSize = 13f
            setTypeface(sansFont, Typeface.BOLD)
            setTextColor(Color.parseColor("#FDA4AF"))
            setBackgroundResource(R.drawable.bg_btn_rose_soft)
            stateListAnimator = null; elevation = 0f; isAllCaps = false
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams((38 * d).toInt(), (36 * d).toInt())
            setOnClickListener { dialog.dismiss() }
        }
        header.addView(btnClose)
        root.addView(header)

        // Now Playing Hero Card
        val heroCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_summary)
            setPadding((14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * d).toInt()
            }
        }

        val heroTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        val albumArt = TextView(this).apply {
            text = "♫"
            gravity = Gravity.CENTER
            textSize = 20f
            setTypeface(sansFont, Typeface.BOLD)
            setTextColor(Color.parseColor("#C4B5FD"))
            setBackgroundResource(R.drawable.bg_btn_violet_soft)
            layoutParams = LinearLayout.LayoutParams((48 * d).toInt(), (48 * d).toInt()).apply {
                marginEnd = (12 * d).toInt()
            }
        }
        heroTop.addView(albumArt)

        val nowPlayingInfo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val tvTrackTitle = TextView(this).apply {
            text = currentSongName.substringBeforeLast(".")
            setTextColor(Color.parseColor("#F1F5F9"))
            setTypeface(sansFont, Typeface.BOLD)
            textSize = 15f
            maxLines = 1
        }
        val tvTrackSub = TextView(this).apply {
            text = "$currentSongName  •  Loop ON"
            setTextColor(Color.parseColor("#94A3B8"))
            typeface = monoFont
            textSize = 10f
            maxLines = 1
        }
        nowPlayingInfo.addView(tvTrackTitle)
        nowPlayingInfo.addView(tvTrackSub)
        heroTop.addView(nowPlayingInfo, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

        // Flat Colorful 5-Bar Equalizer
        val eqBox = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams((36 * d).toInt(), (26 * d).toInt()).apply {
                marginEnd = (8 * d).toInt()
            }
        }
        val eqColors = listOf("#60A5FA", "#34D399", "#C4B5FD", "#FBBF24", "#5EEAD4")
        val eqBars = eqColors.map { hex ->
            View(this).apply {
                setBackgroundColor(Color.parseColor(hex))
                layoutParams = LinearLayout.LayoutParams((4 * d).toInt(), (6 * d).toInt()).apply {
                    setMargins((1.5f * d).toInt(), 0, (1.5f * d).toInt(), 0)
                }
            }.also { eqBox.addView(it) }
        }
        heroTop.addView(eqBox)

        val tvPlayBadge = TextView(this).apply {
            text = "STOPPED"
            setTypeface(monoFont, Typeface.BOLD)
            textSize = 9f
            setPadding((8 * d).toInt(), (4 * d).toInt(), (8 * d).toInt(), (4 * d).toInt())
        }
        heroTop.addView(tvPlayBadge)
        heroCard.addView(heroTop)

        // SeekBar + Timestamps
        var isUserSeeking = false
        val seekBar = SeekBar(this).apply {
            progressTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
            progressBackgroundTintList = ColorStateList.valueOf(Color.parseColor("#25314B"))
            thumbTintList = ColorStateList.valueOf(Color.parseColor("#34D399"))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (10 * d).toInt()
            }
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        mediaPlayer?.seekTo(progress)
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar?) { isUserSeeking = true }
                override fun onStopTrackingTouch(sb: SeekBar?) { isUserSeeking = false }
            })
        }
        heroCard.addView(seekBar)

        val timeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = (2 * d).toInt()
            }
        }
        val tvElapsed = TextView(this).apply {
            text = "00:00"
            setTextColor(Color.parseColor("#94A3B8"))
            typeface = monoFont
            textSize = 10f
        }
        val tvTotal = TextView(this).apply {
            text = "00:00"
            gravity = Gravity.END
            setTextColor(Color.parseColor("#94A3B8"))
            typeface = monoFont
            textSize = 10f
        }
        timeRow.addView(tvElapsed, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        timeRow.addView(tvTotal, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        heroCard.addView(timeRow)
        root.addView(heroCard)

        // Tracks Section Header
        val trackHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (8 * d).toInt()
            }
        }
        trackHeader.addView(View(this).apply {
            setBackgroundResource(R.drawable.bg_dot_emerald)
            layoutParams = LinearLayout.LayoutParams((7 * d).toInt(), (7 * d).toInt()).apply {
                marginEnd = (7 * d).toInt()
            }
        })
        trackHeader.addView(TextView(this).apply {
            text = "SELECT TRACK"
            setTextColor(Color.parseColor("#94A3B8"))
            setTypeface(sansFont, Typeface.BOLD)
            letterSpacing = 0.06f
            textSize = 10f
        })
        root.addView(trackHeader)

        val trackListContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = (12 * d).toInt()
            }
        }
        root.addView(trackListContainer)

        // Primary Action Controls
        val ctrlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        val btnPlayPause = Button(this).apply {
            textSize = 13f
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null; elevation = 0f; isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(0, (46 * d).toInt(), 1.4f).apply {
                marginEnd = (8 * d).toInt()
            }
        }
        val btnStopMusic = Button(this).apply {
            text = "■ STOP"
            textSize = 12f
            setTypeface(sansFont, Typeface.BOLD)
            setTextColor(Color.parseColor("#FDA4AF"))
            setBackgroundResource(R.drawable.bg_btn_rose_soft)
            stateListAnimator = null; elevation = 0f; isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(0, (46 * d).toInt(), 1f)
        }
        ctrlRow.addView(btnPlayPause)
        ctrlRow.addView(btnStopMusic)
        root.addView(ctrlRow)

        val eqPattern = arrayOf(
            intArrayOf(10, 22, 14, 24, 12),
            intArrayOf(20, 12, 24, 14, 22),
            intArrayOf(14, 24, 10, 18, 24),
            intArrayOf(24, 16, 22, 10, 16)
        )
        var eqTick = 0

        lateinit var refreshPlayerUI: () -> Unit

        fun renderTrackRows() {
            trackListContainer.removeAllViews()
            val playing = mediaPlayer?.isPlaying == true
            for ((index, file) in tracks.withIndex()) {
                val isSelected = (file == currentSongName)
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setBackgroundResource(if (isSelected) R.drawable.bg_row_highlight else R.drawable.bg_row_item)
                    setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                        bottomMargin = (6 * d).toInt()
                    }
                    setOnClickListener {
                        if (currentSongName != file || mediaPlayer == null) {
                            currentSongName = file
                            mediaPlayer?.release(); mediaPlayer = null
                            isMusicPaused = false
                            playMusic(currentSongName)
                        } else if (mediaPlayer?.isPlaying == false) {
                            playMusic(currentSongName)
                        }
                        refreshPlayerUI()
                    }
                }
                row.addView(TextView(this).apply {
                    text = String.format(Locale.US, "%02d", index + 1)
                    setTextColor(Color.parseColor(if (isSelected) "#34D399" else "#64748B"))
                    setTypeface(monoFont, Typeface.BOLD)
                    textSize = 11f
                    setPadding(0, 0, (10 * d).toInt(), 0)
                })
                row.addView(TextView(this).apply {
                    text = file.substringBeforeLast(".")
                    setTextColor(Color.parseColor(if (isSelected) "#F1F5F9" else "#CBD5E1"))
                    setTypeface(sansFont, if (isSelected) Typeface.BOLD else Typeface.NORMAL)
                    textSize = 13f
                }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                val statusLabel = when {
                    isSelected && playing -> "● PLAYING"
                    isSelected && isMusicPaused && mediaPlayer != null -> "PAUSED"
                    isSelected -> "SELECTED"
                    else -> "PLAY"
                }
                val (pillBg, pillFg) = when {
                    isSelected && playing -> R.drawable.bg_badge_green to "#34D399"
                    isSelected && isMusicPaused && mediaPlayer != null -> R.drawable.bg_badge_amber to "#FBBF24"
                    isSelected -> R.drawable.bg_btn_blue_soft to "#60A5FA"
                    else -> R.drawable.bg_btn_unselected to "#94A3B8"
                }
                row.addView(TextView(this).apply {
                    text = statusLabel
                    setTextColor(Color.parseColor(pillFg))
                    setBackgroundResource(pillBg)
                    setTypeface(monoFont, Typeface.BOLD)
                    textSize = 9f
                    setPadding((8 * d).toInt(), (4 * d).toInt(), (8 * d).toInt(), (4 * d).toInt())
                })
                trackListContainer.addView(row)
            }
        }

        refreshPlayerUI = {
            val mp = mediaPlayer
            val playing = mp?.isPlaying == true
            tvTrackTitle.text = currentSongName.substringBeforeLast(".")
            tvTrackSub.text = "$currentSongName  •  Loop ON"

            when {
                playing -> {
                    heroCard.setBackgroundResource(R.drawable.bg_card_emerald)
                    albumArt.setBackgroundResource(R.drawable.bg_btn_emerald_soft)
                    albumArt.setTextColor(Color.parseColor("#34D399"))
                    tvPlayBadge.text = "● PLAYING"
                    tvPlayBadge.setBackgroundResource(R.drawable.bg_badge_green)
                    tvPlayBadge.setTextColor(Color.parseColor("#34D399"))
                    btnPlayPause.text = "❚❚ PAUSE"
                    btnPlayPause.setBackgroundResource(R.drawable.bg_btn_amber_soft)
                    btnPlayPause.setTextColor(Color.parseColor("#FBBF24"))
                    seekBar.progressTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
                    seekBar.thumbTintList = ColorStateList.valueOf(Color.parseColor("#34D399"))
                }
                isMusicPaused && mp != null -> {
                    heroCard.setBackgroundResource(R.drawable.bg_card_summary)
                    albumArt.setBackgroundResource(R.drawable.bg_btn_amber_soft)
                    albumArt.setTextColor(Color.parseColor("#FBBF24"))
                    tvPlayBadge.text = "❚❚ PAUSED"
                    tvPlayBadge.setBackgroundResource(R.drawable.bg_badge_amber)
                    tvPlayBadge.setTextColor(Color.parseColor("#FBBF24"))
                    btnPlayPause.text = "▶ RESUME"
                    btnPlayPause.setBackgroundResource(R.drawable.bg_btn_start)
                    btnPlayPause.setTextColor(Color.parseColor("#041E15"))
                    seekBar.progressTintList = ColorStateList.valueOf(Color.parseColor("#F59E0B"))
                    seekBar.thumbTintList = ColorStateList.valueOf(Color.parseColor("#FBBF24"))
                }
                else -> {
                    heroCard.setBackgroundResource(R.drawable.bg_card_summary)
                    albumArt.setBackgroundResource(R.drawable.bg_btn_violet_soft)
                    albumArt.setTextColor(Color.parseColor("#C4B5FD"))
                    tvPlayBadge.text = "■ STOPPED"
                    tvPlayBadge.setBackgroundResource(R.drawable.bg_badge_rose)
                    tvPlayBadge.setTextColor(Color.parseColor("#FDA4AF"))
                    btnPlayPause.text = "▶ PLAY"
                    btnPlayPause.setBackgroundResource(R.drawable.bg_btn_start)
                    btnPlayPause.setTextColor(Color.parseColor("#041E15"))
                    seekBar.progressTintList = ColorStateList.valueOf(Color.parseColor("#8B5CF6"))
                    seekBar.thumbTintList = ColorStateList.valueOf(Color.parseColor("#C4B5FD"))
                }
            }

            if (mp != null) {
                val dur = try { mp.duration } catch (_: Exception) { 0 }
                val pos = try { mp.currentPosition } catch (_: Exception) { 0 }
                seekBar.isEnabled = dur > 0
                seekBar.max = dur.coerceAtLeast(1)
                if (!isUserSeeking) seekBar.progress = pos.coerceIn(0, seekBar.max)
                tvElapsed.text = formatMusicTime(pos)
                tvTotal.text = formatMusicTime(dur)
            } else {
                seekBar.isEnabled = false
                seekBar.max = 100
                seekBar.progress = 0
                tvElapsed.text = "00:00"
                tvTotal.text = "00:00"
            }

            if (playing) {
                val pattern = eqPattern[eqTick % eqPattern.size]
                eqTick++
                eqBars.forEachIndexed { idx, bar ->
                    val lp = bar.layoutParams
                    lp.height = (pattern[idx] * d).toInt()
                    bar.layoutParams = lp
                }
            } else {
                eqBars.forEach { bar ->
                    val lp = bar.layoutParams
                    lp.height = (5 * d).toInt()
                    bar.layoutParams = lp
                }
            }

            renderTrackRows()
            updateMusicButtonUI()
        }

        btnPlayPause.setOnClickListener {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                isMusicPaused = true
            } else {
                isMusicPaused = false
                playMusic(currentSongName)
            }
            refreshPlayerUI()
        }

        btnStopMusic.setOnClickListener {
            try { mediaPlayer?.stop() } catch (_: Exception) {}
            try { mediaPlayer?.release() } catch (_: Exception) {}
            mediaPlayer = null
            isMusicPaused = false
            refreshPlayerUI()
        }

        val handler = Handler(Looper.getMainLooper())
        val ticker = object : Runnable {
            override fun run() {
                if (dialog.isShowing) {
                    val mp = mediaPlayer
                    if (mp != null && mp.isPlaying) {
                        val dur = try { mp.duration } catch (_: Exception) { 0 }
                        val pos = try { mp.currentPosition } catch (_: Exception) { 0 }
                        seekBar.max = dur.coerceAtLeast(1)
                        if (!isUserSeeking) seekBar.progress = pos.coerceIn(0, seekBar.max)
                        tvElapsed.text = formatMusicTime(pos)
                        tvTotal.text = formatMusicTime(dur)
                        val pattern = eqPattern[eqTick % eqPattern.size]
                        eqTick++
                        eqBars.forEachIndexed { idx, bar ->
                            val lp = bar.layoutParams
                            lp.height = (pattern[idx] * d).toInt()
                            bar.layoutParams = lp
                        }
                    }
                    handler.postDelayed(this, 250L)
                }
            }
        }
        dialog.setOnDismissListener {
            handler.removeCallbacks(ticker)
            updateMusicButtonUI()
        }

        refreshPlayerUI()
        dialog.setContentView(root)
        dialog.show()
        dialog.window?.setLayout((resources.displayMetrics.widthPixels * 0.92).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        handler.postDelayed(ticker, 250L)
    }

    private fun playMusic(fileName: String) {
        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
                val afd = assets.openFd("song/$fileName")
                mediaPlayer?.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                mediaPlayer?.isLooping = true
                mediaPlayer?.prepare()
            }
            if (mediaPlayer?.isPlaying == false) {
                mediaPlayer?.start()
                isMusicPaused = false
                updateMusicButtonUI()
                Toast.makeText(this, "Playing: $fileName", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            isMusicPaused = false
            mediaPlayer?.release()
            mediaPlayer = null
            updateMusicButtonUI()
            appendLog("[!] Music Error: ${e.message}")
        }
    }

    private fun checkForGitHubUpdate() {
        Thread {
            try {
                val apiUrl = SecureVault.githubLatestReleaseUrl(this)
                val req = Request.Builder().url(apiUrl).header("Accept","application/vnd.github.v3+json").get().build()
                val res = SecureVault.getStrictApiClient().newCall(req).execute()
                if (res.code != 200) { res.body?.close(); return@Thread }
                val jsonStr = res.body?.string() ?: return@Thread
                res.body?.close()
                val json = JSONObject(jsonStr)
                val tagName = json.optString("tag_name","").trim()
                val latestVersion = tagName.replace(Regex("[^0-9.]"),"")
                val currentVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
                val assets = json.optJSONArray("assets") ?: return@Thread
                var apkUrl = ""
                for (i in 0 until assets.length()) {
                    val name = assets.getJSONObject(i).optString("name","")
                    if (name.endsWith(".apk",true)) { apkUrl = assets.getJSONObject(i).optString("browser_download_url",""); break }
                }
                if (apkUrl.isNotEmpty() && isNewerVersion(latestVersion, currentVersion)) {
                    runOnUiThread { showForceUpdateDialog(tagName, apkUrl) }
                }
            } catch (e: Exception) {}
        }.start()
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        val rParts = remote.replace(Regex("[^0-9.]"),"").split(".").mapNotNull { it.toIntOrNull() }
        val lParts = local.replace(Regex("[^0-9.]"),"").split(".").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i){0}; val l = lParts.getOrElse(i){0}
            if (r > l) return true; if (r < l) return false
        }
        return false
    }

    private fun showForceUpdateDialog(versionTag: String, apkUrl: String) {
        if (isFinishing) return
        AlertDialog.Builder(this).setTitle("UPDATE 🚀").setMessage("New version ($versionTag)!")
            .setCancelable(false).setPositiveButton("UPDATE") { dialog,_ -> dialog.dismiss(); openApkUrl(apkUrl) }
            .create().also { it.setCanceledOnTouchOutside(false); it.show() }
    }

    private fun openApkUrl(url: String) {
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (e: Exception) { Toast.makeText(this,"Cannot open",Toast.LENGTH_LONG).show() }
    }

    private fun appendLog(msg: String) {
        runOnUiThread {
            val line = "$msg\n"
            val colorHex = when {
                msg.contains("✓") || msg.contains("✅") || msg.contains("[✓]") -> "#34D399"
                msg.contains("✗") || msg.contains("⚠") || msg.contains("[!]") -> "#FB7185"
                msg.contains("★") || msg.contains("🎯") -> "#FBBF24"
                msg.contains("🌐") || msg.contains("[*]") || msg.contains("[~]") -> "#60A5FA"
                msg.contains("═") -> "#A78BFA"
                else -> "#E2E8F0"
            }
            val sp = SpannableString(line).apply {
                setSpan(ForegroundColorSpan(Color.parseColor(colorHex)), 0, line.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            tvLogs.append(sp)
            scrollLogs.post { scrollLogs.fullScroll(ScrollView.FOCUS_DOWN) }
        }
    }

    override fun onDestroy() {
        adminGuard?.detach()
        adminGuard = null
        BgOverlayService.onUiStateChanged = null
        if (isFinishing) {
            BgOverlayService.onTriggerCreate = null
        }
        stopConsolePolling()
        try { webView.destroy() } catch (e: Exception) {}
        try { mediaPlayer?.release() } catch (e: Exception) {}
        super.onDestroy()
    }
}
