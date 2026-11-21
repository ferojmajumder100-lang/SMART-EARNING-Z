package com.my.apk

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebViewDatabase
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import org.json.JSONArray
import org.json.JSONObject

class MetaToIgActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_INPUT = "arafat_insta_auto_v9"
        private const val PREFS_STATE = "arafat_insta_auto_v9_state"
        private const val START_URL = "https://www.instagram.com/accounts/login/?mtn"
    }

    private var adminGuard: AdminControlManager.GuardHandle? = null
    private lateinit var inputPrefs: SharedPreferences
    private lateinit var statePrefs: SharedPreferences
    private val ui = Handler(Looper.getMainLooper())
    private val stateLock = Any()

    // Tabs & Panels
    private lateinit var btnTabControl: Button
    private lateinit var btnTabWebView: Button
    private lateinit var scrollControlPanel: ScrollView
    private lateinit var layoutWebViewPanel: LinearLayout

    // Control Panel Views
    private lateinit var etEmailList: EditText
    private lateinit var etPassword: EditText
    private lateinit var tvEmailCount: TextView
    private lateinit var btnStartProcess: Button
    private lateinit var btnStopProcess: Button
    private lateinit var tvStatusMsg: TextView

    // Success Panel Views
    private lateinit var successPanel: LinearLayout
    private lateinit var tvSuccessCount: TextView
    private lateinit var successListLayout: LinearLayout
    private lateinit var btnCopyAll: Button
    private lateinit var btnReset: Button

    // WebView Panel Views
    private lateinit var webView: WebView
    private lateinit var tvWebStatus: TextView
    private lateinit var btnLoadFullMail: Button
    private lateinit var btnWebReload: Button
    private lateinit var btnWebClear: Button
    private lateinit var btnWebStop: Button

    private var activeTab = "control"
    private var cachedScript: String? = null
    private var isStartingClear = false
    @Volatile
    private var isFullMailBusy = false
    @Volatile
    private var stopFullMailRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputPrefs = getSharedPreferences(PREFS_INPUT, MODE_PRIVATE)
        statePrefs = getSharedPreferences(PREFS_STATE, MODE_PRIVATE)

        buildUi()
        loadSavedInputs()
        setupWebView()
        refreshResultsUi()

        adminGuard = AdminControlManager.attachActivityGuard(
            activity = this,
            workKey = AdminControlManager.WORK_INSTAGRAM,
            allowSwitchWork = true
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (activeTab == "webview") {
                    switchTab("control")
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        AdminControlManager.refreshNow()
        refreshResultsUi()
    }

    override fun onDestroy() {
        stopFullMailRequested = true
        adminGuard?.detach()
        adminGuard = null
        try {
            webView.stopLoading()
            webView.removeJavascriptInterface("AndroidExtensionBridge")
            webView.destroy()
        } catch (_: Exception) {
        }
        super.onDestroy()
    }

    private fun buildUi() {
        val d = resources.displayMetrics.density
        val sansFont = ResourcesCompat.getFont(this, R.font.plus_jakarta_sans)
        val monoFont = ResourcesCompat.getFont(this, R.font.jetbrains_mono)

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0A0E1A"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Top Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#141E36"))
            setPadding((14 * d).toInt(), 0, (14 * d).toInt(), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (58 * d).toInt()
            )
        }
        header.addView(View(this).apply {
            setBackgroundResource(R.drawable.bg_dot_rose)
            layoutParams = LinearLayout.LayoutParams((5 * d).toInt(), (24 * d).toInt()).apply {
                marginEnd = (10 * d).toInt()
            }
        })
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(this).apply {
            text = "META TO IG"
            textSize = 15f
            setTextColor(Color.parseColor("#FFFFFF"))
            setTypeface(sansFont, Typeface.BOLD)
        })
        titleCol.addView(TextView(this).apply {
            text = "ARAFAT INSTA AUTO v9.0 • Cookie Saver"
            textSize = 10f
            setTextColor(Color.parseColor("#F472B6"))
            setTypeface(sansFont, Typeface.BOLD)
        })
        header.addView(titleCol)

        val btnSwitchWork = Button(this).apply {
            text = "⇄ Work"
            textSize = 12f
            setTextColor(Color.parseColor("#FFFFFF"))
            setBackgroundResource(R.drawable.bg_btn_solid_blue)
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            setPadding((14 * d).toInt(), 0, (14 * d).toInt(), 0)
            minWidth = (48 * d).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (40 * d).toInt()
            )
            setOnClickListener {
                startActivity(
                    Intent(this@MetaToIgActivity, WorkSelectorActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                )
            }
        }
        header.addView(btnSwitchWork)
        outer.addView(header)

        // 4-color Instagram/Meta accent strip
        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (3 * d).toInt()
            )
        }
        listOf("#F09433", "#E6683C", "#DC2743", "#BC1888").forEach { hex ->
            strip.addView(View(this).apply {
                setBackgroundColor(Color.parseColor(hex))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            })
        }
        outer.addView(strip)

        // Mode Tabs: CONTROL PANEL vs LIVE WEBVIEW
        val tabBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
        }
        btnTabControl = Button(this).apply {
            text = "🎛️ CONTROL & RESULTS"
            textSize = 11f
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(0, (42 * d).toInt(), 1f).apply {
                marginEnd = (6 * d).toInt()
            }
            setOnClickListener { switchTab("control") }
        }
        btnTabWebView = Button(this).apply {
            text = "🌐 LIVE WEBVIEW"
            textSize = 11f
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(0, (42 * d).toInt(), 1f)
            setOnClickListener { switchTab("webview") }
        }
        tabBar.addView(btnTabControl)
        tabBar.addView(btnTabWebView)
        outer.addView(tabBar)

        // Content container holding both panels
        val bodyFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        // ==============================
        // PANEL 1: CONTROL PANEL (popup.html v9.0)
        // ==============================
        scrollControlPanel = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        val controlRoot = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((14 * d).toInt(), (12 * d).toInt(), (14 * d).toInt(), (24 * d).toInt())
        }

        // Banner Card
        val heroCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundResource(R.drawable.bg_card_rose)
            setPadding((14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * d).toInt() }
        }
        heroCard.addView(TextView(this).apply {
            text = "🔥 ARAFAT INSTA AUTO 🔥"
            textSize = 16f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFFFFF"))
            setTypeface(sansFont, Typeface.BOLD)
        })
        heroCard.addView(TextView(this).apply {
            text = "v9.0 — COOKIE SAVER"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FDA4AF"))
            setTypeface(monoFont, Typeface.BOLD)
            setPadding(0, (4 * d).toInt(), 0, 0)
        })
        controlRoot.addView(heroCard)

        // Input Form Card
        val formCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card)
            setPadding((14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * d).toInt() }
        }

        formCard.addView(TextView(this).apply {
            text = "EMAIL LIST (line by line)"
            textSize = 10f
            setTextColor(Color.parseColor("#94A3B8"))
            setTypeface(sansFont, Typeface.BOLD)
            setPadding(0, 0, 0, (6 * d).toInt())
        })

        etEmailList = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            isSingleLine = false
            minLines = 4
            maxLines = 8
            gravity = Gravity.TOP or Gravity.START
            textSize = 12f
            typeface = monoFont
            setTextColor(Color.parseColor("#F1F5F9"))
            setHintTextColor(Color.parseColor("#64748B"))
            hint = "user1@example.com\nuser2@example.com\nuser3@example.com"
            setBackgroundResource(R.drawable.bg_input_surface)
            setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * d).toInt() }
        }
        formCard.addView(etEmailList)

        formCard.addView(TextView(this).apply {
            text = "PASSWORD (one for all)"
            textSize = 10f
            setTextColor(Color.parseColor("#94A3B8"))
            setTypeface(sansFont, Typeface.BOLD)
            setPadding(0, 0, 0, (6 * d).toInt())
        })

        etPassword = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            textSize = 12f
            typeface = monoFont
            setTextColor(Color.parseColor("#F1F5F9"))
            setHintTextColor(Color.parseColor("#64748B"))
            hint = "Enter password"
            setBackgroundResource(R.drawable.bg_input_surface)
            setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (48 * d).toInt()
            ).apply { bottomMargin = (12 * d).toInt() }
        }
        formCard.addView(etPassword)

        // Stats Row
        val statsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundResource(R.drawable.bg_input_surface)
            setPadding((12 * d).toInt(), (9 * d).toInt(), (12 * d).toInt(), (9 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * d).toInt() }
        }
        tvEmailCount = TextView(this).apply {
            text = "Emails: 0"
            textSize = 11f
            setTextColor(Color.parseColor("#FBBF24"))
            setTypeface(sansFont, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        statsRow.addView(tvEmailCount)
        statsRow.addView(TextView(this).apply {
            text = "v9.0"
            textSize = 11f
            setTextColor(Color.parseColor("#F472B6"))
            setTypeface(monoFont, Typeface.BOLD)
        })
        formCard.addView(statsRow)

        btnStartProcess = Button(this).apply {
            text = "🚀 START PROCESS"
            textSize = 13f
            setTextColor(Color.parseColor("#FFFFFF"))
            setBackgroundResource(R.drawable.bg_btn_solid_pink)
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (50 * d).toInt()
            )
            setOnClickListener { startProcess() }
        }
        formCard.addView(btnStartProcess)

        btnStopProcess = Button(this).apply {
            text = "⏹ STOP PROCESS"
            textSize = 12f
            setTextColor(Color.parseColor("#FFFFFF"))
            setBackgroundResource(R.drawable.bg_btn_stop)
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (46 * d).toInt()
            ).apply { topMargin = (8 * d).toInt() }
            setOnClickListener { stopProcess() }
        }
        formCard.addView(btnStopProcess)

        tvStatusMsg = TextView(this).apply {
            text = "Ready to start"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#94A3B8"))
            setBackgroundResource(R.drawable.bg_input_surface)
            setTypeface(sansFont, Typeface.BOLD)
            setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = (12 * d).toInt() }
        }
        formCard.addView(tvStatusMsg)

        controlRoot.addView(formCard)

        // Success Panel (Matching popup.html)
        successPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setBackgroundResource(R.drawable.bg_card_emerald)
            setPadding((14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (12 * d).toInt() }
        }

        successPanel.addView(TextView(this).apply {
            text = "🎉 SUCCESSFUL ALL ACCOUNT 🎉"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#4ADE80"))
            setTypeface(sansFont, Typeface.BOLD)
            setPadding(0, 0, 0, (4 * d).toInt())
        })

        tvSuccessCount = TextView(this).apply {
            text = "Total: 0 accounts"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#86EFAC"))
            setTypeface(sansFont, Typeface.BOLD)
            setPadding(0, 0, 0, (8 * d).toInt())
        }
        successPanel.addView(tvSuccessCount)

        successPanel.addView(TextView(this).apply {
            text = "Format: username → password → fullcookie → email"
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#94A3B8"))
            setBackgroundResource(R.drawable.bg_input_surface)
            setTypeface(monoFont, Typeface.NORMAL)
            setPadding((10 * d).toInt(), (7 * d).toInt(), (10 * d).toInt(), (7 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * d).toInt() }
        })

        successListLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_input_surface)
            setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (10 * d).toInt() }
        }
        successPanel.addView(successListLayout)

        btnCopyAll = Button(this).apply {
            text = "📋 COPY ALL"
            textSize = 12f
            setTextColor(Color.parseColor("#041E15"))
            setBackgroundResource(R.drawable.bg_btn_selected_emerald)
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (48 * d).toInt()
            ).apply { bottomMargin = (8 * d).toInt() }
            setOnClickListener { copyAllResults() }
        }
        successPanel.addView(btnCopyAll)

        btnReset = Button(this).apply {
            text = "🗑️ Clear & Reset"
            textSize = 11f
            setTextColor(Color.parseColor("#CBD5E1"))
            setBackgroundResource(R.drawable.bg_btn_unselected)
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (44 * d).toInt()
            )
            setOnClickListener { confirmResetResults() }
        }
        successPanel.addView(btnReset)

        controlRoot.addView(successPanel)

        controlRoot.addView(TextView(this).apply {
            text = "Powered by ARAFAT"
            textSize = 10f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#64748B"))
            setTypeface(sansFont, Typeface.BOLD)
            setPadding(0, (4 * d).toInt(), 0, (4 * d).toInt())
        })

        scrollControlPanel.addView(controlRoot)
        bodyFrame.addView(scrollControlPanel)

        // ==============================
        // PANEL 2: LIVE WEBVIEW
        // ==============================
        layoutWebViewPanel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val webToolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#111A2E"))
            setPadding((10 * d).toInt(), (6 * d).toInt(), (10 * d).toInt(), (6 * d).toInt())
        }
        tvWebStatus = TextView(this).apply {
            text = "🌐 Ready"
            textSize = 11f
            setTextColor(Color.parseColor("#E2E8F0"))
            setTypeface(monoFont, Typeface.BOLD)
            maxLines = 1
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = (6 * d).toInt()
            }
        }
        webToolbar.addView(tvWebStatus)

        btnWebReload = Button(this).apply {
            text = "↻"
            textSize = 13f
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.bg_btn_solid_blue)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            setPadding((10 * d).toInt(), 0, (10 * d).toInt(), 0)
            layoutParams = LinearLayout.LayoutParams((42 * d).toInt(), (36 * d).toInt()).apply {
                marginEnd = (5 * d).toInt()
            }
            setOnClickListener { webView.loadUrl(START_URL) }
        }
        webToolbar.addView(btnWebReload)

        btnWebClear = Button(this).apply {
            text = "🧹"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.bg_btn_solid_violet)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            setPadding((10 * d).toInt(), 0, (10 * d).toInt(), 0)
            layoutParams = LinearLayout.LayoutParams((42 * d).toInt(), (36 * d).toInt()).apply {
                marginEnd = (5 * d).toInt()
            }
            setOnClickListener {
                setStatus("🧹 Nuclear WebView Clear running...", "info")
                performNuclearWebViewClear {
                    setStatus("✅ WebView data cleared!", "success")
                    Toast.makeText(this@MetaToIgActivity, "WebView Cleared!", Toast.LENGTH_SHORT).show()
                }
            }
        }
        webToolbar.addView(btnWebClear)

        btnWebStop = Button(this).apply {
            text = "⏹"
            textSize = 12f
            setTextColor(Color.WHITE)
            setBackgroundResource(R.drawable.bg_btn_stop)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            setPadding((10 * d).toInt(), 0, (10 * d).toInt(), 0)
            layoutParams = LinearLayout.LayoutParams((42 * d).toInt(), (36 * d).toInt())
            setOnClickListener {
                stopProcess()
                switchTab("control")
            }
        }
        webToolbar.addView(btnWebStop)

        layoutWebViewPanel.addView(webToolbar)

        // Load Full Mail Action Bar right above WebView
        val fullMailBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#0D1426"))
            setPadding((10 * d).toInt(), (5 * d).toInt(), (10 * d).toInt(), (6 * d).toInt())
        }
        btnLoadFullMail = Button(this).apply {
            text = "📧 Load Full Mail (Auto Meta → IG)"
            textSize = 11.5f
            setTextColor(Color.parseColor("#041E15"))
            setBackgroundResource(R.drawable.bg_btn_selected_emerald)
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (38 * d).toInt()
            )
            setOnClickListener { showLoadFullMailDialog() }
        }
        fullMailBar.addView(btnLoadFullMail)
        layoutWebViewPanel.addView(fullMailBar)

        webView = WebView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }
        layoutWebViewPanel.addView(webView)
        bodyFrame.addView(layoutWebViewPanel)

        outer.addView(bodyFrame)
        setContentView(outer)

        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateEmailCountAndSave()
            }
        }
        etEmailList.addTextChangedListener(watcher)
        etPassword.addTextChangedListener(watcher)

        switchTab("control")
    }

    private fun switchTab(tab: String) {
        activeTab = tab
        if (tab == "webview") {
            scrollControlPanel.visibility = View.GONE
            layoutWebViewPanel.visibility = View.VISIBLE
            btnTabControl.setBackgroundResource(R.drawable.bg_btn_unselected)
            btnTabControl.setTextColor(Color.parseColor("#CBD5E1"))
            btnTabWebView.setBackgroundResource(R.drawable.bg_btn_solid_pink)
            btnTabWebView.setTextColor(Color.parseColor("#FFFFFF"))
        } else {
            layoutWebViewPanel.visibility = View.GONE
            scrollControlPanel.visibility = View.VISIBLE
            btnTabControl.setBackgroundResource(R.drawable.bg_btn_solid_pink)
            btnTabControl.setTextColor(Color.parseColor("#FFFFFF"))
            btnTabWebView.setBackgroundResource(R.drawable.bg_btn_unselected)
            btnTabWebView.setTextColor(Color.parseColor("#CBD5E1"))
            refreshResultsUi()
        }
    }

    private fun loadSavedInputs() {
        val savedEmails = inputPrefs.getString("emails", "") ?: ""
        val savedPassword = inputPrefs.getString("password", "") ?: ""
        if (savedEmails.isNotEmpty()) etEmailList.setText(savedEmails)
        if (savedPassword.isNotEmpty()) etPassword.setText(savedPassword)
        updateEmailCountOnly()
    }

    private fun updateEmailCountOnly() {
        val lines = etEmailList.text.toString().trim()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        tvEmailCount.text = "Emails: ${lines.size}"
    }

    private fun updateEmailCountAndSave() {
        updateEmailCountOnly()
        inputPrefs.edit()
            .putString("emails", etEmailList.text.toString())
            .putString("password", etPassword.text.toString())
            .apply()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.cacheMode = WebSettings.LOAD_DEFAULT
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.setSupportMultipleWindows(false)
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

        // Clean mobile Chrome User-Agent (removes "; wv" token so Instagram/Meta behave identically to Chrome)
        val defaultUa = settings.userAgentString ?: ""
        if (defaultUa.contains("; wv")) {
            settings.userAgentString = defaultUa.replace("; wv", "")
        }

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.addJavascriptInterface(AndroidExtensionBridge(), "AndroidExtensionBridge")
        webView.webChromeClient = WebChromeClient()
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: android.webkit.WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                val lower = url.lowercase()
                if (lower.startsWith("http://") || lower.startsWith("https://") || lower == "about:blank") {
                    return false
                }
                // Block instagram:// or intent:// app deep-links so everything stays inside WebView
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url.isNullOrBlank()) return
                val lower = url.lowercase()
                if (lower.contains("instagram.com") || lower.contains("facebook.com") || lower.contains("meta.com")) {
                    val state = readStateJson()
                    if (state.optBoolean("isRunning", false)) {
                        injectContentScript()
                    }
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                handler?.cancel()
                AntiCaptureShield.triggerDecoyOnly()
            }
        }
    }

    private fun loadExtensionScript(): String {
        cachedScript?.let { return it }
        val script = assets.open("meta_to_ig_content.js").bufferedReader().use { it.readText() }
        cachedScript = script
        return script
    }

    private fun injectContentScript() {
        try {
            val script = loadExtensionScript()
            webView.evaluateJavascript(script, null)
        } catch (e: Exception) {
            setStatus("❌ Script inject error: ${e.message}", "error")
        }
    }

    // ==========================================
    // 🚀 START PROCESS (First-time Nuclear Clear -> Load Instagram ?mtn)
    // ==========================================
    private fun startProcess() {
        if (isStartingClear) {
            Toast.makeText(this, "Clearing WebView data, please wait...", Toast.LENGTH_SHORT).show()
            return
        }

        val emails = etEmailList.text.toString()
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val password = etPassword.text.toString().trim()

        if (emails.isEmpty()) {
            Toast.makeText(this, "Please enter at least one email.", Toast.LENGTH_SHORT).show()
            return
        }
        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter a password.", Toast.LENGTH_SHORT).show()
            return
        }

        updateEmailCountAndSave()

        val accountsArr = JSONArray()
        for (email in emails) {
            accountsArr.put("$email:$password")
        }

        val patch = JSONObject().apply {
            put("savedResults", JSONArray())
            put("successEmails", JSONArray())
            put("accounts", accountsArr)
            put("currentIndex", 0)
            put("isRunning", true)
            put("totalAccounts", emails.size)
        }
        mergeStateJson(patch)
        refreshResultsUi()

        isStartingClear = true
        btnStartProcess.isEnabled = false
        setStatus("🧹 Nuclear Clear: Clearing WebView cookies & storage...", "info")

        performNuclearWebViewClear {
            isStartingClear = false
            btnStartProcess.isEnabled = true
            setStatus("✅ Cleared! Opening Instagram...", "success")
            switchTab("webview")
            webView.loadUrl(START_URL)
        }
    }

    private fun stopProcess() {
        stopFullMailRequested = true
        val patch = JSONObject().apply {
            put("isRunning", false)
        }
        mergeStateJson(patch)
        try {
            webView.stopLoading()
        } catch (_: Exception) {
        }
        setStatus("⏹ Process stopped.", "normal")
        refreshResultsUi()
    }

    // ==========================================
    // 📧 LOAD FULL MAIL (AUTO META CREATE -> AUTO IG)
    // ==========================================
    private fun showLoadFullMailDialog() {
        if (isFullMailBusy || isStartingClear) {
            Toast.makeText(this, "অটো প্রসেস চলছে, একটু অপেক্ষা করুন...", Toast.LENGTH_SHORT).show()
            return
        }

        val d = resources.displayMetrics.density
        val sansFont = ResourcesCompat.getFont(this, R.font.plus_jakarta_sans)
        val monoFont = ResourcesCompat.getFont(this, R.font.jetbrains_mono)

        val musePrefs = getSharedPreferences("MusePrefs", MODE_PRIVATE)
        val rawCk = musePrefs.getString("cookie", "") ?: ""
        val savedCookie = SecureVault.decryptLocal(this, rawCk, "")
        val defaultPw = etPassword.text.toString().trim().ifEmpty { "arafat1234" }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0E1528"))
            setPadding((16 * d).toInt(), (14 * d).toInt(), (16 * d).toInt(), (14 * d).toInt())
        }

        container.addView(TextView(this).apply {
            text = "📧 LOAD FULL MAIL (META → IG)"
            textSize = 14f
            setTextColor(Color.parseColor("#34D399"))
            setTypeface(sansFont, Typeface.BOLD)
            setPadding(0, 0, 0, (4 * d).toInt())
        })

        container.addView(TextView(this).apply {
            text = "আগে অটো Meta ID ক্রিয়েট + OTP কনফার্ম হবে, তারপর অটো Instagram তৈরি হবে।"
            textSize = 10.5f
            setTextColor(Color.parseColor("#94A3B8"))
            setTypeface(sansFont, Typeface.NORMAL)
            setPadding(0, 0, 0, (10 * d).toInt())
        })

        // Full Hotmail Input
        container.addView(TextView(this).apply {
            text = "FULL HOTMAIL (email|pass|refresh_token|client_id)"
            textSize = 10f
            setTextColor(Color.parseColor("#93C5FD"))
            setTypeface(sansFont, Typeface.BOLD)
            setPadding(0, 0, 0, (4 * d).toInt())
        })

        val etFullMail = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            isSingleLine = false
            setHorizontallyScrolling(true)
            gravity = Gravity.TOP or Gravity.START
            textSize = 10.5f
            typeface = monoFont
            setTextColor(Color.parseColor("#F8FAFC"))
            setHintTextColor(Color.parseColor("#64748B"))
            hint = "example@hotmail.com|pass|refresh_token|client_id"
            setBackgroundResource(R.drawable.bg_input_surface)
            setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (76 * d).toInt()
            )
        }
        container.addView(etFullMail)

        val tvMailPreview = TextView(this).apply {
            text = "১টি (বা একাধিক লাইন) Full Hotmail দিন"
            textSize = 10f
            typeface = monoFont
            setTextColor(Color.parseColor("#94A3B8"))
            setPadding((2 * d).toInt(), (4 * d).toInt(), 0, (8 * d).toInt())
        }
        container.addView(tvMailPreview)

        etFullMail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val parsed = parseHotmailLines(s?.toString() ?: "")
                when {
                    parsed.isEmpty() -> {
                        tvMailPreview.text = "⚠️ ফরম্যাট: email|pass|refresh_token|client_id"
                        tvMailPreview.setTextColor(Color.parseColor("#FBBF24"))
                    }
                    parsed.size == 1 -> {
                        tvMailPreview.text = "✅ Mail: ${parsed[0].email}"
                        tvMailPreview.setTextColor(Color.parseColor("#34D399"))
                    }
                    else -> {
                        tvMailPreview.text = "✅ Ready: ${parsed.size} Hotmail"
                        tvMailPreview.setTextColor(Color.parseColor("#34D399"))
                    }
                }
            }
        })

        // Password Input
        container.addView(TextView(this).apply {
            text = "PASSWORD (Meta & IG)"
            textSize = 10f
            setTextColor(Color.parseColor("#93C5FD"))
            setTypeface(sansFont, Typeface.BOLD)
            setPadding(0, 0, 0, (4 * d).toInt())
        })

        val etDialogPass = EditText(this).apply {
            setText(defaultPw)
            isSingleLine = true
            textSize = 11.5f
            typeface = monoFont
            setTextColor(Color.parseColor("#F8FAFC"))
            setHintTextColor(Color.parseColor("#64748B"))
            hint = "Enter password"
            setBackgroundResource(R.drawable.bg_input_surface)
            setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (42 * d).toInt()
            ).apply { bottomMargin = (8 * d).toInt() }
        }
        container.addView(etDialogPass)

        // Muse Cookie Input (Auto-loaded from Work Meta saved cookie)
        container.addView(TextView(this).apply {
            text = if (savedCookie.isNotEmpty()) {
                "MUSE COOKIE (✅ Saved Loaded)"
            } else {
                "MUSE COOKIE (hatch_native_auth_csrf=...)"
            }
            textSize = 10f
            setTextColor(if (savedCookie.isNotEmpty()) Color.parseColor("#34D399") else Color.parseColor("#FBBF24"))
            setTypeface(sansFont, Typeface.BOLD)
            setPadding(0, 0, 0, (4 * d).toInt())
        })

        val etDialogCookie = EditText(this).apply {
            setText(savedCookie)
            isSingleLine = true
            textSize = 10.5f
            typeface = monoFont
            setTextColor(Color.parseColor("#F8FAFC"))
            setHintTextColor(Color.parseColor("#64748B"))
            hint = "Paste Muse cookie (hatch_native_auth_csrf=...)"
            setBackgroundResource(R.drawable.bg_input_surface)
            setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (40 * d).toInt()
            ).apply { bottomMargin = (12 * d).toInt() }
        }
        container.addView(etDialogCookie)

        val btnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val btnCancel = Button(this).apply {
            text = "Cancel"
            textSize = 11.5f
            setTextColor(Color.parseColor("#CBD5E1"))
            setBackgroundResource(R.drawable.bg_btn_unselected)
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(0, (44 * d).toInt(), 0.7f).apply {
                marginEnd = (6 * d).toInt()
            }
        }
        btnRow.addView(btnCancel)

        val btnStartAuto = Button(this).apply {
            text = "🚀 START (META → IG)"
            textSize = 12f
            setTextColor(Color.parseColor("#041E15"))
            setBackgroundResource(R.drawable.bg_btn_selected_emerald)
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(0, (44 * d).toInt(), 1.3f)
        }
        btnRow.addView(btnStartAuto)
        container.addView(btnRow)

        val dialog = AlertDialog.Builder(this)
            .setView(container)
            .setCancelable(true)
            .create()

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnStartAuto.setOnClickListener {
            val credsList = parseHotmailLines(etFullMail.text.toString())
            if (credsList.isEmpty()) {
                Toast.makeText(
                    this,
                    "সঠিক Full Hotmail দিন: email|pass|refresh_token|client_id",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            val pw = etDialogPass.text.toString().trim()
            if (pw.length < 6) {
                Toast.makeText(this, "Password কমপক্ষে ৬ অক্ষরের দিন!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val ck = etDialogCookie.text.toString().trim()
            if (ck.length < 20 || (!ck.contains("hatch_native_auth_csrf") && !ck.contains("csrftoken"))) {
                Toast.makeText(this, "সঠিক Muse Cookie দিন (Work Meta তে সেভ করলেও হবে)!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Save cookie back into MusePrefs so user doesn't need to re-paste
            musePrefs.edit().putString("cookie", SecureVault.encryptLocal(this, ck)).apply()
            dialog.dismiss()
            runFullMailAutoMetaToIg(credsList, pw, ck)
        }

        dialog.show()
    }

    private fun parseHotmailLines(raw: String): List<MetaCreator.HotmailCredentials> {
        return raw.replace("\r\n", "\n").replace("\r", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { MetaCreator.parseHotmailLine(it) }
    }

    private fun extractMuseCsrf(cookie: String): String {
        for (part in cookie.split(";")) {
            if (part.trim().startsWith("hatch_native_auth_csrf=")) {
                return part.split("=", limit = 2)[1].split(".")[0]
            }
        }
        return ""
    }

    private fun setFullMailButtonBusy(busy: Boolean, label: String = "📧 Load Full Mail (Auto Meta → IG)") {
        isFullMailBusy = busy
        ui.post {
            btnLoadFullMail.isEnabled = !busy
            btnLoadFullMail.text = label
            if (busy) {
                btnLoadFullMail.setBackgroundResource(R.drawable.bg_btn_unselected)
                btnLoadFullMail.setTextColor(Color.parseColor("#CBD5E1"))
            } else {
                btnLoadFullMail.setBackgroundResource(R.drawable.bg_btn_selected_emerald)
                btnLoadFullMail.setTextColor(Color.parseColor("#041E15"))
            }
        }
    }

    private fun runFullMailAutoMetaToIg(
        credsList: List<MetaCreator.HotmailCredentials>,
        password: String,
        cookie: String
    ) {
        stopFullMailRequested = false
        setFullMailButtonBusy(true, "⏳ Auto Meta Creating (0/${credsList.size})...")
        setStatus("📧 Phase 1: Auto Meta Create শুরু হয়েছে...", "info")

        val csrf = extractMuseCsrf(cookie)

        Thread {
            val createdEmails = mutableListOf<String>()
            var lastError = ""
            try {
                for (i in credsList.indices) {
                    if (stopFullMailRequested) break
                    val creds = credsList[i]
                    val num = i + 1
                    val tag = "[$num/${credsList.size}]"
                    setFullMailButtonBusy(true, "⏳ Meta $tag: ${creds.email.substringBefore("@")}")

                    val sessionCookie = arrayOf(cookie)
                    val triedCodes = mutableSetOf<String>()

                    // 1. Pre-check existing inbox login codes so old codes are skipped
                    setStatus("🔎 $tag Checking inbox...", "info")
                    val preCheck = MetaCreator.fetchMetaLoginOtpFromHotmail(creds)
                    if (preCheck.ok && preCheck.allLoginOtps.isNotEmpty()) {
                        triedCodes.addAll(preCheck.allLoginOtps)
                    }

                    // 2. Create Meta ID
                    setStatus("🛠️ $tag Creating Meta ID: ${creds.email}", "info")
                    val metaRes = MetaCreator.createMeta(creds.email, password, "BD")
                    if (!metaRes.success) {
                        lastError = "Meta Failed: ${metaRes.error}"
                        setStatus("❌ $tag $lastError", "error")
                        continue
                    }

                    // 3. Send Muse OTP
                    setStatus("📨 $tag Sending Muse OTP...", "info")
                    val sendRes = MetaCreator.museSendOtp(creds.email, "BD", cookie, csrf, sessionCookie)
                    if (sendRes != "OK") {
                        lastError = "Muse Send Failed: $sendRes"
                        setStatus("❌ $tag $lastError", "error")
                        continue
                    }

                    // 4. Auto 5-attempt Hotmail OTP fetch + Auto Confirm
                    try {
                        Thread.sleep(2500L)
                    } catch (_: InterruptedException) {
                        break
                    }

                    var confirmed = false
                    for (attempt in 1..5) {
                        if (stopFullMailRequested) break
                        setStatus("📩 $tag Auto OTP ($attempt/5)...", "info")
                        val mailRes = MetaCreator.fetchMetaLoginOtpFromHotmail(creds, triedCodes)
                        if (mailRes.ok && mailRes.loginOtp.isNotEmpty()) {
                            val otp = mailRes.loginOtp
                            triedCodes.add(otp)
                            setStatus("🔑 $tag OTP $otp — Confirming...", "info")
                            val confRes = MetaCreator.museConfirmOtp(otp, creds.email, sessionCookie[0], csrf)
                            if (confRes == "OK") {
                                confirmed = true
                                createdEmails.add(creds.email)
                                setStatus("✅ $tag Meta Confirmed! (${creds.email})", "success")
                                break
                            } else {
                                lastError = "OTP Confirm Failed: $confRes"
                            }
                        } else if (!mailRes.ok) {
                            lastError = "Hotmail API: ${mailRes.error}"
                        }

                        if (attempt < 5) {
                            try {
                                Thread.sleep(3000L)
                            } catch (_: InterruptedException) {
                                break
                            }
                        }
                    }

                    if (!confirmed && lastError.isEmpty()) {
                        lastError = "৫ বার চেক করেও OTP পাওয়া যায়নি (BAD)"
                    }
                }

                if (stopFullMailRequested) {
                    setStatus("⏹ Stopped by user.", "normal")
                    setFullMailButtonBusy(false)
                    return@Thread
                }

                if (createdEmails.isEmpty()) {
                    val errMsg = if (lastError.isNotEmpty()) lastError else "Meta create failed"
                    setStatus("❌ Meta Create Failed: $errMsg", "error")
                    ui.post {
                        Toast.makeText(this@MetaToIgActivity, "❌ $errMsg", Toast.LENGTH_LONG).show()
                    }
                    setFullMailButtonBusy(false)
                    return@Thread
                }

                // PHASE 2: AUTO LAUNCH INSTAGRAM IN WEBVIEW WITH CREATED META EMAIL & PASSWORD
                ui.post {
                    etEmailList.setText(createdEmails.joinToString("\n"))
                    etPassword.setText(password)
                    updateEmailCountAndSave()

                    val accountsArr = JSONArray()
                    for (em in createdEmails) {
                        accountsArr.put("$em:$password")
                    }

                    // Keep previously saved IG results intact so each Full Mail run adds to COPY ALL
                    val patch = JSONObject().apply {
                        put("accounts", accountsArr)
                        put("currentIndex", 0)
                        put("isRunning", true)
                        put("totalAccounts", createdEmails.size)
                    }
                    mergeStateJson(patch)
                    refreshResultsUi()

                    isStartingClear = true
                    setFullMailButtonBusy(false)
                    setStatus("🧹 Meta Done! Clearing WebView & starting IG...", "info")

                    performNuclearWebViewClear {
                        isStartingClear = false
                        setStatus("🚀 Starting Instagram Auto (${createdEmails[0]})...", "success")
                        switchTab("webview")
                        webView.loadUrl(START_URL)
                    }
                }
            } catch (e: Exception) {
                setStatus("❌ Error: ${e.message}", "error")
                setFullMailButtonBusy(false)
            }
        }.start()
    }

    // ==========================================
    // 🧹 NUCLEAR WEBVIEW CLEAR (First-start + Between Accounts)
    // ==========================================
    private fun performNuclearWebViewClear(onComplete: () -> Unit) {
        ui.post {
            try {
                webView.stopLoading()
                webView.loadUrl("about:blank")
            } catch (_: Exception) {
            }

            try {
                webView.clearCache(true)
                webView.clearFormData()
                webView.clearHistory()
                WebStorage.getInstance().deleteAllData()
                WebViewDatabase.getInstance(applicationContext).clearFormData()
            } catch (_: Exception) {
            }

            val cm = CookieManager.getInstance()
            cm.removeSessionCookies(null)
            cm.removeAllCookies {
                cm.flush()
                // Second delayed purge pass to ensure no residual cookies survive
                ui.postDelayed({
                    try {
                        cm.removeSessionCookies(null)
                        cm.removeAllCookies {
                            cm.flush()
                            try {
                                WebStorage.getInstance().deleteAllData()
                                webView.clearCache(true)
                            } catch (_: Exception) {
                            }
                            onComplete()
                        }
                    } catch (_: Exception) {
                        onComplete()
                    }
                }, 350L)
            }
        }
    }

    // ==========================================
    // 💾 EXTENSION STATE PERSISTENCE (chrome.storage.local)
    // ==========================================
    private fun readStateJson(): JSONObject {
        synchronized(stateLock) {
            val raw = statePrefs.getString("state_json", "{}") ?: "{}"
            return try {
                JSONObject(raw)
            } catch (_: Exception) {
                JSONObject()
            }
        }
    }

    private fun mergeStateJson(patch: JSONObject) {
        synchronized(stateLock) {
            val current = readStateJson()
            val keys = patch.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                current.put(k, patch.get(k))
            }
            statePrefs.edit().putString("state_json", current.toString()).commit()
        }
        ui.post { refreshResultsUi() }
    }

    private fun refreshResultsUi() {
        val state = readStateJson()
        val running = state.optBoolean("isRunning", false)
        btnStopProcess.visibility = if (running) View.VISIBLE else View.GONE

        var results = state.optJSONArray("savedResults") ?: JSONArray()
        val successEmails = state.optJSONArray("successEmails") ?: JSONArray()

        if (results.length() == 0 && successEmails.length() > 0) {
            val fallback = JSONArray()
            for (i in 0 until successEmails.length()) {
                fallback.put(JSONObject().apply {
                    put("username", "?")
                    put("password", "?")
                    put("fullcookie", "")
                    put("email", successEmails.optString(i, "?"))
                })
            }
            results = fallback
        }

        if (results.length() == 0) {
            successPanel.visibility = View.GONE
            return
        }

        successPanel.visibility = View.VISIBLE
        tvSuccessCount.text = "Total: ${results.length()} accounts"
        successListLayout.removeAllViews()

        val d = resources.displayMetrics.density
        val monoFont = ResourcesCompat.getFont(this, R.font.jetbrains_mono)

        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i) ?: continue
            val username = item.optString("username", "?")
            val email = item.optString("email", "?")
            val cookieLen = item.optString("fullcookie", "").length
            val tv = TextView(this).apply {
                text = "${i + 1}. $username | $email (cookie: ${cookieLen}c)"
                textSize = 11f
                typeface = monoFont
                setTextColor(Color.parseColor("#A7F3D0"))
                setPadding((4 * d).toInt(), (5 * d).toInt(), (4 * d).toInt(), (5 * d).toInt())
            }
            successListLayout.addView(tv)
        }

        if (!running) {
            setStatus("✅ ${results.length()} account(s) done!", "success")
        }
    }

    private fun copyAllResults() {
        val state = readStateJson()
        val results = state.optJSONArray("savedResults") ?: JSONArray()
        if (results.length() == 0) {
            btnCopyAll.text = "❌ No results"
            ui.postDelayed({ btnCopyAll.text = "📋 COPY ALL" }, 2000)
            return
        }

        // Exact format: username \t password \t fullcookie \t email
        val lines = mutableListOf<String>()
        for (i in 0 until results.length()) {
            val r = results.optJSONObject(i) ?: continue
            val username = r.optString("username", "?").ifEmpty { "?" }
            val password = r.optString("password", "?").ifEmpty { "?" }
            val cookie = r.optString("fullcookie", "?").ifEmpty { "?" }
            val email = r.optString("email", "?").ifEmpty { "?" }
            lines.add("$username\t$password\t$cookie\t$email")
        }

        val text = lines.joinToString("\n")
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("MetaToIgAccounts", text))
        btnCopyAll.text = "✅ COPIED ${results.length()}!"
        Toast.makeText(this, "Copied ${results.length()} accounts!", Toast.LENGTH_SHORT).show()
        ui.postDelayed({ btnCopyAll.text = "📋 COPY ALL" }, 2500)
    }

    private fun confirmResetResults() {
        AlertDialog.Builder(this)
            .setTitle("Clear & Reset")
            .setMessage("Clear all results and reset?")
            .setPositiveButton("Yes") { _, _ ->
                mergeStateJson(JSONObject().apply {
                    put("savedResults", JSONArray())
                    put("successEmails", JSONArray())
                    put("isRunning", false)
                    put("currentIndex", 0)
                })
                setStatus("Ready to start", "normal")
                refreshResultsUi()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setStatus(msg: String, type: String) {
        ui.post {
            tvStatusMsg.text = msg
            tvWebStatus.text = msg
            when (type) {
                "success" -> {
                    tvStatusMsg.setTextColor(Color.parseColor("#4ADE80"))
                    tvStatusMsg.setBackgroundResource(R.drawable.bg_badge_green)
                }
                "error" -> {
                    tvStatusMsg.setTextColor(Color.parseColor("#FDA4AF"))
                    tvStatusMsg.setBackgroundResource(R.drawable.bg_badge_rose)
                }
                "info" -> {
                    tvStatusMsg.setTextColor(Color.parseColor("#93C5FD"))
                    tvStatusMsg.setBackgroundResource(R.drawable.bg_btn_blue_soft)
                }
                else -> {
                    tvStatusMsg.setTextColor(Color.parseColor("#94A3B8"))
                    tvStatusMsg.setBackgroundResource(R.drawable.bg_input_surface)
                }
            }
        }
    }

    // ==========================================
    // 🌉 @JavascriptInterface BRIDGE FOR content.js
    // ==========================================
    inner class AndroidExtensionBridge {

        @JavascriptInterface
        fun storageGet(): String {
            return readStateJson().toString()
        }

        @JavascriptInterface
        fun storageSet(jsonStr: String) {
            try {
                val patch = JSONObject(jsonStr)
                mergeStateJson(patch)
            } catch (_: Exception) {
            }
        }

        @JavascriptInterface
        fun getFullCookie(): String {
            val cm = CookieManager.getInstance()
            cm.flush()

            val merged = LinkedHashMap<String, String>()
            val urls = listOf(
                "https://www.instagram.com/",
                "https://instagram.com/",
                "https://m.instagram.com/",
                "https://i.instagram.com/"
            )
            for (u in urls) {
                val raw = try { cm.getCookie(u) } catch (_: Exception) { null }
                if (!raw.isNullOrBlank()) {
                    for (part in raw.split(";")) {
                        val trimmed = part.trim()
                        val eq = trimmed.indexOf('=')
                        if (eq > 0) {
                            val name = trimmed.substring(0, eq).trim()
                            val value = trimmed.substring(eq + 1).trim()
                            merged[name] = value
                        }
                    }
                }
            }

            val cookieStr = merged.entries.joinToString("; ") { "${it.key}=${it.value}" }
            return JSONObject().apply {
                put("success", true)
                put("fullCookie", cookieStr)
                put("count", merged.size)
                put("hasSessionId", merged.containsKey("sessionid"))
                put("hasRur", merged.containsKey("rur"))
                put("hasCsrf", merged.containsKey("csrftoken"))
                put("hasDsUserId", merged.containsKey("ds_user_id"))
            }.toString()
        }

        @JavascriptInterface
        fun clearDataAsync(callbackId: String) {
            ui.post {
                val cm = CookieManager.getInstance()
                // PASS 1 (matching background.js TASK 1-4)
                cm.removeSessionCookies(null)
                cm.removeAllCookies {
                    cm.flush()
                    try {
                        WebStorage.getInstance().deleteAllData()
                        webView.clearCache(true)
                        webView.clearFormData()
                    } catch (_: Exception) {
                    }
                    // PASS 2 after 400ms (matching background.js TASK 5)
                    ui.postDelayed({
                        cm.removeSessionCookies(null)
                        cm.removeAllCookies {
                            cm.flush()
                            // PASS 3 after 400ms (matching background.js TASK 6)
                            ui.postDelayed({
                                cm.removeSessionCookies(null)
                                cm.removeAllCookies {
                                    cm.flush()
                                    val payload = JSONObject().apply { put("status", "done") }.toString()
                                    val escaped = payload.replace("\\", "\\\\").replace("'", "\\'")
                                    try {
                                        webView.evaluateJavascript(
                                            "window.__arafatResolveMsg && window.__arafatResolveMsg('$callbackId', '$escaped');",
                                            null
                                        )
                                    } catch (_: Exception) {
                                    }
                                }
                            }, 400L)
                        }
                    }, 400L)
                }
            }
        }

        @JavascriptInterface
        fun onNotify(msg: String, type: String) {
            setStatus(msg, type)
        }
    }
}
