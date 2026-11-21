package com.my.apk

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import org.json.JSONArray
import org.json.JSONObject

class MetaWorkActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        private const val MIN_HOTMAIL_BATCH = 1
        private const val MAX_HOTMAIL_BATCH = 10
        private const val KEY_LAST_BATCH = "last_hotmail_batch_json"
    }

    private var adminGuard: AdminControlManager.GuardHandle? = null

    private lateinit var etCookie: EditText
    private lateinit var etEmail: EditText
    private lateinit var etHotmailFull: EditText
    private lateinit var tvHotmailPreview: TextView
    private lateinit var tvMailLabel: TextView
    private lateinit var btnModeManual: Button
    private lateinit var btnModeHotmail: Button
    private lateinit var etPassword: EditText
    private lateinit var btnSaveCookie: Button
    private lateinit var btnStartMeta: Button
    private lateinit var btnSwitchWork: Button
    private lateinit var listLayout: LinearLayout
    private lateinit var prefs: SharedPreferences

    private val ui = Handler(Looper.getMainLooper())
    @Volatile
    private var isBusy = false
    @Volatile
    private var stopBatchRequested = false
    private var accountCounter = 0
    private var isHotmailMode = false

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = getSharedPreferences("MusePrefs", MODE_PRIVATE)

        val d = resources.displayMetrics.density
        val sansFont = ResourcesCompat.getFont(this, R.font.plus_jakarta_sans)
        val monoFont = ResourcesCompat.getFont(this, R.font.jetbrains_mono)

        val outer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0E1528"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        }

        // Compact Top Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#141E36"))
            setPadding((14 * d).toInt(), 0, (14 * d).toInt(), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (52 * d).toInt()
            )
        }
        header.addView(View(this).apply {
            setBackgroundResource(R.drawable.bg_dot_emerald)
            layoutParams = LinearLayout.LayoutParams((4 * d).toInt(), (22 * d).toInt()).apply {
                marginEnd = (8 * d).toInt()
            }
        })
        val titleCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        titleCol.addView(TextView(this).apply {
            text = "MUSE TOOL PREMIUM"
            textSize = 14f
            setTextColor(Color.parseColor("#FFFFFF"))
            setTypeface(sansFont, Typeface.BOLD)
        })
        titleCol.addView(TextView(this).apply {
            text = "Work Meta • Manual & Multi Hotmail (1-10)"
            textSize = 10f
            setTextColor(Color.parseColor("#34D399"))
            setTypeface(sansFont, Typeface.BOLD)
        })
        header.addView(titleCol)

        btnSwitchWork = Button(this).apply {
            text = "⇄ Work"
            textSize = 11f
            setTextColor(Color.parseColor("#FFFFFF"))
            setBackgroundResource(R.drawable.bg_btn_solid_blue)
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            setPadding((12 * d).toInt(), 0, (12 * d).toInt(), 0)
            minWidth = (48 * d).toInt()
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (36 * d).toInt()
            )
            setOnClickListener { switchWorkMode() }
        }
        header.addView(btnSwitchWork)
        outer.addView(header)

        // 4-color accent strip
        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (3 * d).toInt()
            )
        }
        listOf("#10B981", "#3B82F6", "#8B5CF6", "#F59E0B").forEach { hex ->
            strip.addView(View(this).apply {
                setBackgroundColor(Color.parseColor(hex))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            })
        }
        outer.addView(strip)

        val mainScroll = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((10 * d).toInt(), (10 * d).toInt(), (10 * d).toInt(), (16 * d).toInt())
        }

        // Compact Form Card (fits cleanly on 1 screen with results below)
        val formCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_blue)
            setPadding((11 * d).toInt(), (10 * d).toInt(), (11 * d).toInt(), (10 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (10 * d).toInt() }
        }

        // Cookie Row (Compact input + Save button side-by-side)
        formCard.addView(TextView(this).apply {
            text = "COOKIE"
            textSize = 10f
            setTextColor(Color.parseColor("#93C5FD"))
            setTypeface(sansFont, Typeface.BOLD)
            setPadding(0, 0, 0, (4 * d).toInt())
        })

        val cookieRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (9 * d).toInt() }
        }

        etCookie = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            isSingleLine = true
            textSize = 10.5f
            typeface = monoFont
            setTextColor(Color.parseColor("#F8FAFC"))
            setHintTextColor(Color.parseColor("#64748B"))
            hint = "Paste Muse cookie (hatch_native_auth_csrf=...)"
            setBackgroundResource(R.drawable.bg_input_surface)
            setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(0, (40 * d).toInt(), 1f).apply {
                marginEnd = (6 * d).toInt()
            }
            val rawCk = prefs.getString("cookie", "") ?: ""
            val savedCk = SecureVault.decryptLocal(this@MetaWorkActivity, rawCk, "")
            if (savedCk.isNotEmpty()) setText(savedCk)
        }
        cookieRow.addView(etCookie)

        btnSaveCookie = Button(this).apply {
            text = "SAVE"
            textSize = 11f
            setTextColor(Color.parseColor("#FFFFFF"))
            setBackgroundResource(R.drawable.bg_btn_solid_violet)
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            setPadding((12 * d).toInt(), 0, (12 * d).toInt(), 0)
            setOnClickListener(this@MetaWorkActivity)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (40 * d).toInt()
            )
        }
        cookieRow.addView(btnSaveCookie)
        formCard.addView(cookieRow)

        // Mail Mode Switcher
        val modeRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (8 * d).toInt() }
        }
        btnModeManual = Button(this).apply {
            text = "📧 MANUAL MAIL"
            textSize = 10.5f
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(0, (38 * d).toInt(), 1f).apply {
                marginEnd = (6 * d).toInt()
            }
            setOnClickListener { setMailMode(false) }
        }
        btnModeHotmail = Button(this).apply {
            text = "🔥 HOTMAIL AUTO (1-10)"
            textSize = 10.5f
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(0, (38 * d).toInt(), 1f)
            setOnClickListener { setMailMode(true) }
        }
        modeRow.addView(btnModeManual)
        modeRow.addView(btnModeHotmail)
        formCard.addView(modeRow)

        tvMailLabel = TextView(this).apply {
            text = "MANUAL EMAIL"
            textSize = 10f
            setTextColor(Color.parseColor("#34D399"))
            setTypeface(sansFont, Typeface.BOLD)
            setPadding(0, 0, 0, (4 * d).toInt())
        }
        formCard.addView(tvMailLabel)

        etEmail = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            isSingleLine = true
            textSize = 11.5f
            typeface = monoFont
            setTextColor(Color.parseColor("#F8FAFC"))
            setHintTextColor(Color.parseColor("#64748B"))
            hint = "Enter manual email (example@gmail.com)"
            setBackgroundResource(R.drawable.bg_input_surface)
            setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (42 * d).toInt()
            )
        }
        formCard.addView(etEmail)

        // Compact scrollable multi-line Hotmail box (no wrapping so each long token stays 1 neat line!)
        etHotmailFull = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
            isSingleLine = false
            setHorizontallyScrolling(true)
            gravity = Gravity.TOP or Gravity.START
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = true
            textSize = 10f
            typeface = monoFont
            setTextColor(Color.parseColor("#F8FAFC"))
            setHintTextColor(Color.parseColor("#64748B"))
            hint = "email1@hotmail.com|pass|refresh_token|client_id\nemail2@hotmail.com|pass|refresh_token|client_id\n(Min 1 • Max 10 line by line)"
            setBackgroundResource(R.drawable.bg_input_surface)
            setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (86 * d).toInt()
            )
            visibility = View.GONE
            setOnTouchListener { v, event ->
                v.parent?.requestDisallowInterceptTouchEvent(true)
                if ((event.action and MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                }
                false
            }
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    updateHotmailPreview()
                }
            })
        }
        formCard.addView(etHotmailFull)

        tvHotmailPreview = TextView(this).apply {
            text = "Loaded: 0/10 Hotmail (Min 1 • Max 10)"
            textSize = 10f
            setTextColor(Color.parseColor("#94A3B8"))
            typeface = monoFont
            setPadding((2 * d).toInt(), (4 * d).toInt(), 0, 0)
            visibility = View.GONE
        }
        formCard.addView(tvHotmailPreview)

        // Password + Start Row (Compact layout)
        val pwLabel = TextView(this).apply {
            text = "PASSWORD"
            textSize = 10f
            setTextColor(Color.parseColor("#93C5FD"))
            setTypeface(sansFont, Typeface.BOLD)
            setPadding(0, (8 * d).toInt(), 0, (4 * d).toInt())
        }
        formCard.addView(pwLabel)

        val actionRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        etPassword = EditText(this).apply {
            setText("arafat1234")
            isSingleLine = true
            textSize = 11.5f
            typeface = monoFont
            setTextColor(Color.parseColor("#F8FAFC"))
            setBackgroundResource(R.drawable.bg_input_surface)
            setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(0, (44 * d).toInt(), 0.95f).apply {
                marginEnd = (6 * d).toInt()
            }
        }
        actionRow.addView(etPassword)

        btnStartMeta = Button(this).apply {
            text = "🚀 START CREATE"
            textSize = 12f
            setTypeface(sansFont, Typeface.BOLD)
            setTextColor(Color.parseColor("#041E15"))
            setBackgroundResource(R.drawable.bg_btn_selected_emerald)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            setOnClickListener(this@MetaWorkActivity)
            layoutParams = LinearLayout.LayoutParams(0, (44 * d).toInt(), 1.3f)
        }
        actionRow.addView(btnStartMeta)
        formCard.addView(actionRow)

        root.addView(formCard)

        listLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        root.addView(listLayout)

        mainScroll.addView(root)
        outer.addView(mainScroll)
        setContentView(outer)

        setMailMode(prefs.getBoolean("hotmail_mode", false))
        restoreLastHotmailBatchIfPresent()

        adminGuard = AdminControlManager.attachActivityGuard(
            activity = this,
            workKey = AdminControlManager.WORK_META,
            allowSwitchWork = true
        )
    }

    override fun onResume() {
        super.onResume()
        AdminControlManager.refreshNow()
    }

    private fun setMailMode(hotmail: Boolean) {
        isHotmailMode = hotmail
        prefs.edit().putBoolean("hotmail_mode", hotmail).apply()
        if (hotmail) {
            btnModeManual.setBackgroundResource(R.drawable.bg_btn_unselected)
            btnModeManual.setTextColor(Color.parseColor("#CBD5E1"))
            btnModeHotmail.setBackgroundResource(R.drawable.bg_btn_selected_emerald)
            btnModeHotmail.setTextColor(Color.parseColor("#041E15"))
            tvMailLabel.text = "MULTI HOTMAIL (Line by line • Min 1, Max 10)"
            etEmail.visibility = View.GONE
            etHotmailFull.visibility = View.VISIBLE
            tvHotmailPreview.visibility = View.VISIBLE
            updateHotmailPreview()
        } else {
            btnModeManual.setBackgroundResource(R.drawable.bg_btn_solid_blue)
            btnModeManual.setTextColor(Color.parseColor("#FFFFFF"))
            btnModeHotmail.setBackgroundResource(R.drawable.bg_btn_unselected)
            btnModeHotmail.setTextColor(Color.parseColor("#CBD5E1"))
            tvMailLabel.text = "MANUAL EMAIL"
            etEmail.visibility = View.VISIBLE
            etHotmailFull.visibility = View.GONE
            tvHotmailPreview.visibility = View.GONE
        }
    }

    private data class HotmailParseSummary(
        val totalNonEmpty: Int,
        val validCreds: List<MetaCreator.HotmailCredentials>,
        val invalidCount: Int
    )

    private fun parseHotmailLines(raw: String): HotmailParseSummary {
        val lines = raw.replace("\r\n", "\n").replace("\r", "\n")
            .split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val valid = mutableListOf<MetaCreator.HotmailCredentials>()
        var invalid = 0
        for (line in lines) {
            val parsed = MetaCreator.parseHotmailLine(line)
            if (parsed != null) {
                valid.add(parsed)
            } else {
                invalid += 1
            }
        }
        return HotmailParseSummary(lines.size, valid, invalid)
    }

    private fun updateHotmailPreview() {
        val raw = etHotmailFull.text.toString().trim()
        if (raw.isEmpty()) {
            tvHotmailPreview.text = "Loaded: 0/$MAX_HOTMAIL_BATCH Hotmail (Min 1 • Max 10)"
            tvHotmailPreview.setTextColor(Color.parseColor("#94A3B8"))
            return
        }
        val summary = parseHotmailLines(raw)
        when {
            summary.validCreds.size > MAX_HOTMAIL_BATCH -> {
                tvHotmailPreview.text = "⚠️ সর্বোচ্চ ১০টি দেওয়া যাবে! (আপনি দিয়েছেন ${summary.validCreds.size}টি)"
                tvHotmailPreview.setTextColor(Color.parseColor("#F87171"))
            }
            summary.validCreds.isEmpty() -> {
                tvHotmailPreview.text = "⚠️ ফরম্যাট ভুল! দিন: email|pass|refresh_token|client_id"
                tvHotmailPreview.setTextColor(Color.parseColor("#FBBF24"))
            }
            summary.invalidCount > 0 -> {
                tvHotmailPreview.text = "✅ Ready: ${summary.validCreds.size}/$MAX_HOTMAIL_BATCH (⚠️ ${summary.invalidCount} invalid skipped)"
                tvHotmailPreview.setTextColor(Color.parseColor("#FBBF24"))
            }
            else -> {
                tvHotmailPreview.text = "✅ Ready: ${summary.validCreds.size}/$MAX_HOTMAIL_BATCH Hotmail"
                tvHotmailPreview.setTextColor(Color.parseColor("#34D399"))
            }
        }
    }

    private fun switchWorkMode() {
        startActivity(
            Intent(this, WorkSelectorActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    override fun onClick(v: View) {
        if (v == btnSaveCookie) {
            val ck = etCookie.text.toString().trim()
            if (ck.length < 20) {
                Toast.makeText(this, "Invalid Cookie Length!", Toast.LENGTH_SHORT).show()
                return
            }
            if (!ck.contains("hatch_native_auth_csrf") && !ck.contains("csrftoken")) {
                Toast.makeText(this, "Missing CSRF Token!", Toast.LENGTH_SHORT).show()
                return
            }
            prefs.edit().putString("cookie", SecureVault.encryptLocal(this, ck)).apply()
            Toast.makeText(this, "Cookie Saved!", Toast.LENGTH_SHORT).show()
        } else if (v == btnStartMeta) {
            if (isBusy) {
                Toast.makeText(this, "Please wait...", Toast.LENGTH_SHORT).show()
                return
            }
            val ck = etCookie.text.toString().trim()
            if (!ck.contains("hatch_native_auth_csrf") && !ck.contains("csrftoken")) {
                Toast.makeText(this, "Save Valid Cookie First!", Toast.LENGTH_SHORT).show()
                return
            }
            val pw = etPassword.text.toString().trim()
            if (pw.length < 6) {
                Toast.makeText(this, "Password কমপক্ষে ৬ অক্ষরের দিন!", Toast.LENGTH_SHORT).show()
                return
            }

            // Support auto-detecting Hotmail line(s) even if pasted in Manual box
            val manualRaw = etEmail.text.toString().trim()
            val fullRaw = etHotmailFull.text.toString().trim()
            val detectedFromManual = if (!isHotmailMode && manualRaw.contains("|")) {
                parseHotmailLines(manualRaw).validCreds
            } else {
                emptyList()
            }

            if (isHotmailMode || detectedFromManual.isNotEmpty()) {
                val credsList = if (detectedFromManual.isNotEmpty()) {
                    etHotmailFull.setText(manualRaw)
                    etEmail.setText("")
                    setMailMode(true)
                    detectedFromManual
                } else {
                    parseHotmailLines(fullRaw).validCreds
                }

                if (credsList.size < MIN_HOTMAIL_BATCH) {
                    Toast.makeText(
                        this,
                        "কমপক্ষে ১টি সঠিক Hotmail দিন (email|pass|refresh_token|client_id)",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
                if (credsList.size > MAX_HOTMAIL_BATCH) {
                    Toast.makeText(
                        this,
                        "সর্বোচ্চ ১০টি Hotmail একসাথে দেওয়া যাবে! (${credsList.size}টি দেওয়া হয়েছে)",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                startHotmailBatch(credsList, pw, ck)
            } else {
                if (manualRaw.isEmpty() || !manualRaw.contains("@") || !manualRaw.contains(".")) {
                    Toast.makeText(this, "Valid Manual Email দিন!", Toast.LENGTH_SHORT).show()
                    return
                }
                accountCounter += 1
                startManualMetaAccount(accountCounter, manualRaw, pw, ck)
            }
        }
    }

    private fun setStartButtonBusy(busy: Boolean, label: String = "⏳ CREATING...") {
        isBusy = busy
        ui.post {
            btnStartMeta.isEnabled = !busy
            if (busy) {
                btnStartMeta.text = label
                btnStartMeta.setBackgroundResource(R.drawable.bg_btn_unselected)
                btnStartMeta.setTextColor(Color.parseColor("#CBD5E1"))
            } else {
                btnStartMeta.text = "🚀 START CREATE"
                btnStartMeta.setBackgroundResource(R.drawable.bg_btn_selected_emerald)
                btnStartMeta.setTextColor(Color.parseColor("#041E15"))
            }
        }
    }

    // =====================================================
    // MODE 1: MANUAL MAIL CREATE + MANUAL OTP VERIFICATION
    // (Shows ONLY the latest created account on screen)
    // =====================================================
    private fun startManualMetaAccount(accId: Int, email: String, password: String, cookie: String) {
        setStartButtonBusy(true, "⏳ CREATING...")
        val d = resources.displayMetrics.density
        val sansFont = ResourcesCompat.getFont(this, R.font.plus_jakarta_sans)
        val monoFont = ResourcesCompat.getFont(this, R.font.jetbrains_mono)

        // Keep ONLY the latest created account on screen
        listLayout.removeAllViews()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_emerald)
            setPadding((12 * d).toInt(), (10 * d).toInt(), (12 * d).toInt(), (10 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val tvTitle = TextView(this).apply {
            text = "ACCOUNT #$accId • $email"
            setTextColor(Color.parseColor("#34D399"))
            textSize = 12f
            setTypeface(sansFont, Typeface.BOLD)
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.MIDDLE
        }
        card.addView(tvTitle)

        val tvLog = TextView(this).apply {
            textSize = 10.5f
            typeface = monoFont
            setTextColor(Color.parseColor("#E2E8F0"))
            setPadding(0, (6 * d).toInt(), 0, (8 * d).toInt())
        }
        card.addView(tvLog)

        val btnCopyEmail = createMiniCopyButton("📋 COPY EMAIL", d, sansFont) {
            copyTextToClipboard("Email", email, "Copied: $email")
        }
        card.addView(btnCopyEmail)

        val otpBox = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setBackgroundResource(R.drawable.bg_card)
            setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
        }

        otpBox.addView(TextView(this).apply {
            text = "MANUAL OTP VERIFICATION"
            textSize = 10f
            setTextColor(Color.parseColor("#FBBF24"))
            setTypeface(sansFont, Typeface.BOLD)
            setPadding(0, 0, 0, (5 * d).toInt())
        })

        val etManualOtp = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            isSingleLine = true
            textSize = 12f
            typeface = monoFont
            setTextColor(Color.parseColor("#F8FAFC"))
            setHintTextColor(Color.parseColor("#64748B"))
            hint = "আপনার ইমেইলের OTP কোডটি লিখুন..."
            setBackgroundResource(R.drawable.bg_input_surface)
            setPadding((10 * d).toInt(), (8 * d).toInt(), (10 * d).toInt(), (8 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (42 * d).toInt()
            ).apply { bottomMargin = (6 * d).toInt() }
        }
        otpBox.addView(etManualOtp)

        val otpBtnRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val btnConfirmOtp = Button(this).apply {
            text = "CONFIRM OTP"
            textSize = 11f
            setTextColor(Color.parseColor("#041E15"))
            setTypeface(sansFont, Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_btn_selected_emerald)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(0, (42 * d).toInt(), 1.2f).apply {
                marginEnd = (6 * d).toInt()
            }
        }
        otpBtnRow.addView(btnConfirmOtp)

        val btnResendOtp = Button(this).apply {
            text = "RESEND OTP"
            textSize = 11f
            setTextColor(Color.WHITE)
            setTypeface(sansFont, Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_btn_solid_blue)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(0, (42 * d).toInt(), 1f)
        }
        otpBtnRow.addView(btnResendOtp)

        otpBox.addView(otpBtnRow)
        card.addView(otpBox)
        listLayout.addView(card)

        val csrf = extractCsrf(cookie)
        val sessionCookie = arrayOf(cookie)
        var otpAttempts = 0

        btnResendOtp.setOnClickListener {
            btnResendOtp.isEnabled = false
            btnConfirmOtp.isEnabled = false
            updateLog(tvLog, "🔄 Resending Muse OTP...")
            Thread {
                val resendRes = MetaCreator.museSendOtp(email, "BD", sessionCookie[0], csrf, sessionCookie)
                ui.post {
                    btnResendOtp.isEnabled = true
                    btnConfirmOtp.isEnabled = true
                    if (resendRes == "OK") {
                        updateLog(tvLog, "✅ OTP Resent! নতুন OTP দিন।")
                        etManualOtp.setText("")
                        etManualOtp.requestFocus()
                    } else {
                        updateLog(tvLog, "❌ Resend Failed: $resendRes")
                    }
                }
            }.start()
        }

        btnConfirmOtp.setOnClickListener {
            val otp = etManualOtp.text.toString().trim()
            if (otp.length < 4) {
                Toast.makeText(this@MetaWorkActivity, "সঠিক OTP কোড দিন!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            otpAttempts += 1
            btnConfirmOtp.isEnabled = false
            btnResendOtp.isEnabled = false
            updateLog(tvLog, "⏳ Confirming OTP #$otpAttempts ($otp)...")
            Thread {
                val confRes = MetaCreator.museConfirmOtp(otp, email, sessionCookie[0], csrf)
                ui.post {
                    if (confRes == "OK") {
                        updateLog(tvLog, "🎉 SUCCESS! Account Created.")
                        tvTitle.text = "✅ ACCOUNT #$accId (SUCCESS) • $email"
                        btnCopyEmail.setBackgroundResource(R.drawable.bg_btn_selected_emerald)
                        btnCopyEmail.setTextColor(Color.parseColor("#041E15"))
                        btnCopyEmail.text = "📋 COPY EMAIL ($email)"
                        otpBox.visibility = View.GONE
                        etEmail.setText("")
                    } else {
                        updateLog(tvLog, "❌ Confirm Failed: $confRes")
                        updateLog(tvLog, "🔁 আবার নতুন OTP দিয়ে CONFIRM OTP চাপুন।")
                        etManualOtp.setText("")
                        btnConfirmOtp.isEnabled = true
                        btnResendOtp.isEnabled = true
                    }
                }
            }.start()
        }

        Thread {
            try {
                updateLog(tvLog, "📧 Manual Email: $email")
                updateLog(tvLog, "Creating Meta ID...")
                val metaRes = MetaCreator.createMeta(email, password, "BD")
                if (!metaRes.success) {
                    updateLog(tvLog, "❌ Meta ID Failed: ${metaRes.error}")
                    return@Thread
                }
                updateLog(tvLog, "✅ Meta UID: ${metaRes.uid}")

                updateLog(tvLog, "Muse OTP Sending...")
                val sendRes = MetaCreator.museSendOtp(email, "BD", cookie, csrf, sessionCookie)
                if (sendRes != "OK") {
                    updateLog(tvLog, "❌ Muse Send: $sendRes")
                    return@Thread
                }
                updateLog(tvLog, "✅ Muse OTP Sent! নিচে আপনার OTP দিয়ে CONFIRM OTP চাপুন।")
                ui.post {
                    otpBox.visibility = View.VISIBLE
                    etManualOtp.requestFocus()
                }
            } catch (e: Exception) {
                updateLog(tvLog, "❌ Exception: ${e.message}")
            } finally {
                setStartButtonBusy(false)
            }
        }.start()
    }

    // =====================================================
    // MODE 2: MULTI HOTMAIL AUTO BATCH (MIN 1 • MAX 10)
    // Compact 1-screen fit • Last batch only • 1 COPY ALL MAIL btn
    // =====================================================
    private data class BatchRowState(
        val email: String,
        var status: String, // PENDING, RUNNING, SUCCESS, BAD
        var note: String = ""
    )

    private fun startHotmailBatch(
        credsList: List<MetaCreator.HotmailCredentials>,
        password: String,
        cookie: String
    ) {
        stopBatchRequested = false
        setStartButtonBusy(true, "⏳ 0/${credsList.size} RUNNING...")

        val d = resources.displayMetrics.density
        val sansFont = ResourcesCompat.getFont(this, R.font.plus_jakarta_sans)
        val monoFont = ResourcesCompat.getFont(this, R.font.jetbrains_mono)

        // Replace previous results so ONLY the latest create batch stays on screen
        listLayout.removeAllViews()

        val rowStates = credsList.map { BatchRowState(it.email, "PENDING", "Waiting...") }.toMutableList()
        val successEmails = mutableListOf<String>()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_emerald)
            setPadding((10 * d).toInt(), (9 * d).toInt(), (10 * d).toInt(), (9 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Top compact summary + mini COPY ALL MAIL button on the same header area
        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (5 * d).toInt() }
        }

        val tvBatchTitle = TextView(this).apply {
            text = "🔥 LAST BATCH (0/${credsList.size})"
            textSize = 11.5f
            setTextColor(Color.parseColor("#34D399"))
            setTypeface(sansFont, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerRow.addView(tvBatchTitle)

        val btnCopyAllMail = Button(this).apply {
            text = "📋 COPY ALL MAIL (0)"
            textSize = 10f
            setTextColor(Color.parseColor("#041E15"))
            setTypeface(sansFont, Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_btn_selected_emerald)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            visibility = View.GONE
            setPadding((10 * d).toInt(), 0, (10 * d).toInt(), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (34 * d).toInt()
            )
            setOnClickListener {
                if (successEmails.isEmpty()) {
                    Toast.makeText(this@MetaWorkActivity, "কোনো Success Mail নেই!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val joined = successEmails.joinToString("\n")
                copyTextToClipboard("MetaSuccessMails", joined, "✅ Copied ${successEmails.size} mail(s)!")
            }
        }
        headerRow.addView(btnCopyAllMail)
        card.addView(headerRow)

        val tvLiveTicker = TextView(this).apply {
            text = "Starting batch (${credsList.size} Hotmail)..."
            textSize = 10f
            typeface = monoFont
            setTextColor(Color.parseColor("#CBD5E1"))
            maxLines = 1
            ellipsize = TextUtils.TruncateAt.END
            setPadding(0, 0, 0, (6 * d).toInt())
        }
        card.addView(tvLiveTicker)

        // Compact container holding 1 slim mini row per email (10 rows fit cleanly on 1 screen)
        val rowsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_input_surface)
            setPadding((8 * d).toInt(), (5 * d).toInt(), (8 * d).toInt(), (5 * d).toInt())
        }

        val rowViews = mutableListOf<TextView>()
        for (i in credsList.indices) {
            val tvRow = TextView(this).apply {
                text = "⏳ ${i + 1}. ${credsList[i].email}"
                textSize = 10.5f
                typeface = monoFont
                setTextColor(Color.parseColor("#94A3B8"))
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.MIDDLE
                setPadding((2 * d).toInt(), (3 * d).toInt(), (2 * d).toInt(), (3 * d).toInt())
                setOnClickListener {
                    val st = rowStates[i]
                    if (st.status == "SUCCESS") {
                        copyTextToClipboard("Email", st.email, "Copied: ${st.email}")
                    }
                }
            }
            rowViews.add(tvRow)
            rowsContainer.addView(tvRow)
        }
        card.addView(rowsContainer)
        listLayout.addView(card)

        val csrf = extractCsrf(cookie)

        fun refreshBatchHeader(doneCount: Int, okCount: Int, badCount: Int) {
            ui.post {
                tvBatchTitle.text = "🔥 LAST BATCH • ✅ $okCount  ❌ $badCount  ($doneCount/${credsList.size})"
                if (okCount > 0) {
                    btnCopyAllMail.visibility = View.VISIBLE
                    btnCopyAllMail.text = "📋 COPY ALL MAIL ($okCount)"
                } else {
                    btnCopyAllMail.visibility = View.GONE
                }
            }
        }

        fun updateRowUi(index: Int, status: String, note: String) {
            rowStates[index].status = status
            rowStates[index].note = note
            val email = rowStates[index].email
            ui.post {
                val tv = rowViews[index]
                when (status) {
                    "RUNNING" -> {
                        tv.text = "🔄 ${index + 1}. $email • $note"
                        tv.setTextColor(Color.parseColor("#93C5FD"))
                    }
                    "SUCCESS" -> {
                        tv.text = "✅ ${index + 1}. $email"
                        tv.setTextColor(Color.parseColor("#4ADE80"))
                    }
                    "BAD" -> {
                        tv.text = "❌ ${index + 1}. $email • $note"
                        tv.setTextColor(Color.parseColor("#F87171"))
                    }
                    else -> {
                        tv.text = "⏳ ${index + 1}. $email"
                        tv.setTextColor(Color.parseColor("#94A3B8"))
                    }
                }
            }
        }

        fun setTicker(msg: String) {
            ui.post { tvLiveTicker.text = msg }
        }

        Thread {
            var okCount = 0
            var badCount = 0
            try {
                for (i in credsList.indices) {
                    if (stopBatchRequested) break
                    val creds = credsList[i]
                    val num = i + 1
                    setStartButtonBusy(true, "⏳ $num/${credsList.size} WORKING...")
                    updateRowUi(i, "RUNNING", "Checking inbox")
                    setTicker("[$num/${credsList.size}] ${creds.email} — Checking old OTP...")

                    val sessionCookie = arrayOf(cookie)
                    val triedCodes = mutableSetOf<String>()

                    // 1. Pre-check existing inbox login codes so old codes are skipped
                    val preCheck = MetaCreator.fetchMetaLoginOtpFromHotmail(creds)
                    if (preCheck.ok && preCheck.allLoginOtps.isNotEmpty()) {
                        triedCodes.addAll(preCheck.allLoginOtps)
                    }

                    // 2. Create Meta ID
                    updateRowUi(i, "RUNNING", "Creating Meta")
                    setTicker("[$num/${credsList.size}] ${creds.email} — Creating Meta ID...")
                    val metaRes = MetaCreator.createMeta(creds.email, password, "BD")
                    if (!metaRes.success) {
                        badCount += 1
                        updateRowUi(i, "BAD", "Meta Failed")
                        setTicker("[$num/${credsList.size}] ❌ Meta Failed: ${metaRes.error}")
                        refreshBatchHeader(num, okCount, badCount)
                        continue
                    }

                    // 3. Send Muse OTP
                    updateRowUi(i, "RUNNING", "Sending OTP")
                    setTicker("[$num/${credsList.size}] ${creds.email} — Sending Muse OTP...")
                    val sendRes = MetaCreator.museSendOtp(creds.email, "BD", cookie, csrf, sessionCookie)
                    if (sendRes != "OK") {
                        badCount += 1
                        updateRowUi(i, "BAD", "OTP Send Failed")
                        setTicker("[$num/${credsList.size}] ❌ Muse Send Failed: $sendRes")
                        refreshBatchHeader(num, okCount, badCount)
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
                        if (stopBatchRequested) break
                        updateRowUi(i, "RUNNING", "OTP $attempt/5")
                        setTicker("[$num/${credsList.size}] 📩 Auto OTP Request ($attempt/5)...")

                        val mailRes = MetaCreator.fetchMetaLoginOtpFromHotmail(creds, triedCodes)
                        if (mailRes.ok && mailRes.loginOtp.isNotEmpty()) {
                            val otp = mailRes.loginOtp
                            triedCodes.add(otp)
                            updateRowUi(i, "RUNNING", "Confirm $otp")
                            setTicker("[$num/${credsList.size}] 🔑 OTP $otp found — Confirming...")

                            val confRes = MetaCreator.museConfirmOtp(otp, creds.email, sessionCookie[0], csrf)
                            if (confRes == "OK") {
                                confirmed = true
                                okCount += 1
                                successEmails.add(creds.email)
                                updateRowUi(i, "SUCCESS", "OK")
                                setTicker("[$num/${credsList.size}] ✅ SUCCESS: ${creds.email}")
                                break
                            } else {
                                setTicker("[$num/${credsList.size}] ❌ Confirm ($otp) Failed: $confRes")
                            }
                        }

                        if (attempt < 5) {
                            try {
                                Thread.sleep(3000L)
                            } catch (_: InterruptedException) {
                                break
                            }
                        }
                    }

                    if (!confirmed) {
                        badCount += 1
                        updateRowUi(i, "BAD", "No OTP (5/5)")
                        setTicker("[$num/${credsList.size}] ❌ No OTP after 5 tries — BAD")
                    }

                    refreshBatchHeader(num, okCount, badCount)
                    saveLastHotmailBatch(rowStates, successEmails)
                }

                saveLastHotmailBatch(rowStates, successEmails)
                ui.post {
                    tvLiveTicker.text = if (okCount > 0) {
                        "🎉 Completed! ✅ $okCount Success • ❌ $badCount Bad (Tap COPY ALL MAIL)"
                    } else {
                        "⚠️ Completed! কোনো Success Account হয়নি (❌ $badCount Bad)"
                    }
                    tvLiveTicker.setTextColor(
                        if (okCount > 0) Color.parseColor("#4ADE80") else Color.parseColor("#F87171")
                    )
                    etHotmailFull.setText("")
                }
            } catch (e: Exception) {
                setTicker("❌ Batch Error: ${e.message}")
            } finally {
                setStartButtonBusy(false)
            }
        }.start()
    }

    private fun saveLastHotmailBatch(rows: List<BatchRowState>, successEmails: List<String>) {
        try {
            val rowsArr = JSONArray()
            for (r in rows) {
                rowsArr.put(JSONObject().apply {
                    put("email", r.email)
                    put("status", r.status)
                    put("note", r.note)
                })
            }
            val okArr = JSONArray()
            for (e in successEmails) okArr.put(e)

            val rootObj = JSONObject().apply {
                put("rows", rowsArr)
                put("successEmails", okArr)
            }
            prefs.edit().putString(KEY_LAST_BATCH, rootObj.toString()).apply()
        } catch (_: Exception) {
        }
    }

    private fun restoreLastHotmailBatchIfPresent() {
        val raw = prefs.getString(KEY_LAST_BATCH, "") ?: ""
        if (raw.isBlank()) return
        try {
            val rootObj = JSONObject(raw)
            val rowsArr = rootObj.optJSONArray("rows") ?: return
            if (rowsArr.length() == 0) return
            val okArr = rootObj.optJSONArray("successEmails") ?: JSONArray()
            val successEmails = mutableListOf<String>()
            for (i in 0 until okArr.length()) {
                val em = okArr.optString(i, "").trim()
                if (em.isNotEmpty()) successEmails.add(em)
            }

            val d = resources.displayMetrics.density
            val sansFont = ResourcesCompat.getFont(this, R.font.plus_jakarta_sans)
            val monoFont = ResourcesCompat.getFont(this, R.font.jetbrains_mono)

            listLayout.removeAllViews()
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_card_emerald)
                setPadding((10 * d).toInt(), (9 * d).toInt(), (10 * d).toInt(), (9 * d).toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }

            var okCount = 0
            var badCount = 0
            for (i in 0 until rowsArr.length()) {
                val st = rowsArr.optJSONObject(i)?.optString("status", "") ?: ""
                if (st == "SUCCESS") okCount++ else badCount++
            }

            val headerRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (6 * d).toInt() }
            }

            headerRow.addView(TextView(this).apply {
                text = "🔥 LAST BATCH • ✅ $okCount  ❌ $badCount"
                textSize = 11.5f
                setTextColor(Color.parseColor("#34D399"))
                setTypeface(sansFont, Typeface.BOLD)
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            if (successEmails.isNotEmpty()) {
                headerRow.addView(Button(this).apply {
                    text = "📋 COPY ALL MAIL (${successEmails.size})"
                    textSize = 10f
                    setTextColor(Color.parseColor("#041E15"))
                    setTypeface(sansFont, Typeface.BOLD)
                    setBackgroundResource(R.drawable.bg_btn_selected_emerald)
                    stateListAnimator = null
                    elevation = 0f
                    isAllCaps = false
                    setPadding((10 * d).toInt(), 0, (10 * d).toInt(), 0)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        (34 * d).toInt()
                    )
                    setOnClickListener {
                        copyTextToClipboard(
                            "MetaSuccessMails",
                            successEmails.joinToString("\n"),
                            "✅ Copied ${successEmails.size} mail(s)!"
                        )
                    }
                })
            }
            card.addView(headerRow)

            val rowsContainer = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundResource(R.drawable.bg_input_surface)
                setPadding((8 * d).toInt(), (5 * d).toInt(), (8 * d).toInt(), (5 * d).toInt())
            }

            for (i in 0 until rowsArr.length()) {
                val item = rowsArr.optJSONObject(i) ?: continue
                val email = item.optString("email", "")
                val status = item.optString("status", "BAD")
                val note = item.optString("note", "")
                val tvRow = TextView(this).apply {
                    text = if (status == "SUCCESS") {
                        "✅ ${i + 1}. $email"
                    } else {
                        "❌ ${i + 1}. $email • ${note.ifEmpty { "BAD" }}"
                    }
                    textSize = 10.5f
                    typeface = monoFont
                    setTextColor(
                        if (status == "SUCCESS") Color.parseColor("#4ADE80") else Color.parseColor("#F87171")
                    )
                    maxLines = 1
                    ellipsize = TextUtils.TruncateAt.MIDDLE
                    setPadding((2 * d).toInt(), (3 * d).toInt(), (2 * d).toInt(), (3 * d).toInt())
                    setOnClickListener {
                        if (status == "SUCCESS") {
                            copyTextToClipboard("Email", email, "Copied: $email")
                        }
                    }
                }
                rowsContainer.addView(tvRow)
            }

            card.addView(rowsContainer)
            listLayout.addView(card)
        } catch (_: Exception) {
        }
    }

    private fun createMiniCopyButton(
        label: String,
        d: Float,
        sansFont: Typeface?,
        onClickAction: () -> Unit
    ): Button {
        return Button(this).apply {
            text = label
            textSize = 10.5f
            setTextColor(Color.WHITE)
            setTypeface(sansFont, Typeface.BOLD)
            setBackgroundResource(R.drawable.bg_btn_solid_violet)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (38 * d).toInt()
            ).apply { bottomMargin = (8 * d).toInt() }
            setOnClickListener { onClickAction() }
        }
    }

    private fun copyTextToClipboard(label: String, text: String, toastMsg: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(this, toastMsg, Toast.LENGTH_SHORT).show()
    }

    private fun extractCsrf(cookie: String): String {
        for (part in cookie.split(";")) {
            if (part.trim().startsWith("hatch_native_auth_csrf=")) {
                return part.split("=", limit = 2)[1].split(".")[0]
            }
        }
        return ""
    }

    private fun updateLog(log: TextView, msg: String) {
        ui.post { log.append("$msg\n") }
    }

    override fun onDestroy() {
        stopBatchRequested = true
        adminGuard?.detach()
        adminGuard = null
        super.onDestroy()
    }
}
