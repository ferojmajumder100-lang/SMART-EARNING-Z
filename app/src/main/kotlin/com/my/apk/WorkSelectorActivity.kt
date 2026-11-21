package com.my.apk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat

class WorkSelectorActivity : AppCompatActivity() {

    private var adminGuard: AdminControlManager.GuardHandle? = null
    private lateinit var tvAllWorksBanner: TextView
    private lateinit var fbCard: LinearLayout
    private lateinit var tvFbBadge: TextView
    private lateinit var btnOpenFb: Button
    private lateinit var metaCard: LinearLayout
    private lateinit var tvMetaBadge: TextView
    private lateinit var btnOpenMeta: Button
    private lateinit var metaToIgCard: LinearLayout
    private lateinit var tvMetaToIgBadge: TextView
    private lateinit var btnOpenMetaToIg: Button

    private val adminStateListener: (AdminControlManager.ControlState) -> Unit = { state ->
        applyWorkStates(state)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AdminControlManager.init(this)

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

        // Header
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#141E36"))
            setPadding((16 * d).toInt(), 0, (16 * d).toInt(), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (64 * d).toInt()
            )
        }
        header.addView(View(this).apply {
            setBackgroundResource(R.drawable.bg_dot_blue)
            layoutParams = LinearLayout.LayoutParams((5 * d).toInt(), (26 * d).toInt()).apply {
                marginEnd = (10 * d).toInt()
            }
        })
        val headerCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        headerCol.addView(TextView(this).apply {
            text = "SMART EARNING Z"
            textSize = 17f
            setTextColor(Color.parseColor("#FFFFFF"))
            setTypeface(sansFont, Typeface.BOLD)
        })
        headerCol.addView(TextView(this).apply {
            text = "Select Work Mode"
            textSize = 11f
            setTextColor(Color.parseColor("#60A5FA"))
            setTypeface(sansFont, Typeface.BOLD)
        })
        header.addView(headerCol)
        outer.addView(header)

        // 4-color accent strip
        val strip = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (3 * d).toInt()
            )
        }
        listOf("#3B82F6", "#10B981", "#8B5CF6", "#F59E0B").forEach { hex ->
            strip.addView(View(this).apply {
                setBackgroundColor(Color.parseColor(hex))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
            })
        }
        outer.addView(strip)

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding((18 * d).toInt(), (24 * d).toInt(), (18 * d).toInt(), (24 * d).toInt())
        }

        tvAllWorksBanner = TextView(this).apply {
            text = "সব কাজ বর্তমানে অ্যাডমিন বন্ধ রেখেছেন।"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FDA4AF"))
            setBackgroundResource(R.drawable.bg_card_rose)
            setTypeface(sansFont, Typeface.BOLD)
            setPadding((14 * d).toInt(), (12 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (14 * d).toInt() }
        }
        content.addView(tvAllWorksBanner)

        content.addView(TextView(this).apply {
            text = "CHOOSE WORKSPACE"
            textSize = 12f
            letterSpacing = 0.08f
            setTextColor(Color.parseColor("#94A3B8"))
            setTypeface(monoFont, Typeface.BOLD)
            setPadding(0, 0, 0, (14 * d).toInt())
        })

        // Option 1: Work Facebook
        fbCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_blue)
            setPadding((18 * d).toInt(), (18 * d).toInt(), (18 * d).toInt(), (18 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (16 * d).toInt() }
        }
        val fbTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        fbTop.addView(TextView(this).apply {
            text = "1. Work Facebook"
            textSize = 18f
            setTextColor(Color.parseColor("#FFFFFF"))
            setTypeface(sansFont, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        tvFbBadge = TextView(this).apply {
            text = "FB SUITE"
            textSize = 10f
            setTextColor(Color.parseColor("#93C5FD"))
            setBackgroundResource(R.drawable.bg_btn_blue_soft)
            setTypeface(monoFont, Typeface.BOLD)
            setPadding((10 * d).toInt(), (4 * d).toInt(), (10 * d).toInt(), (4 * d).toInt())
        }
        fbTop.addView(tvFbBadge)
        fbCard.addView(fbTop)

        fbCard.addView(TextView(this).apply {
            text = "V1 Limited • V2 Stander • V3 Graphql • Live Range • OTP Watcher & Console"
            textSize = 12f
            setTextColor(Color.parseColor("#CBD5E1"))
            typeface = sansFont
            setPadding(0, (8 * d).toInt(), 0, (14 * d).toInt())
        })

        btnOpenFb = Button(this).apply {
            text = "OPEN WORK FACEBOOK"
            textSize = 13f
            setTextColor(Color.parseColor("#FFFFFF"))
            setBackgroundResource(R.drawable.bg_btn_solid_blue)
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (50 * d).toInt()
            )
            setOnClickListener { openWorkFacebook() }
        }
        fbCard.addView(btnOpenFb)
        fbCard.setOnClickListener { openWorkFacebook() }
        content.addView(fbCard)

        // Option 2: Work Meta
        metaCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_emerald)
            setPadding((18 * d).toInt(), (18 * d).toInt(), (18 * d).toInt(), (18 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (18 * d).toInt() }
        }
        val metaTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        metaTop.addView(TextView(this).apply {
            text = "2. Work Meta"
            textSize = 18f
            setTextColor(Color.parseColor("#FFFFFF"))
            setTypeface(sansFont, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        tvMetaBadge = TextView(this).apply {
            text = "MUSE TOOLS"
            textSize = 10f
            setTextColor(Color.parseColor("#34D399"))
            setBackgroundResource(R.drawable.bg_badge_green)
            setTypeface(monoFont, Typeface.BOLD)
            setPadding((10 * d).toInt(), (4 * d).toInt(), (10 * d).toInt(), (4 * d).toInt())
        }
        metaTop.addView(tvMetaBadge)
        metaCard.addView(metaTop)

        metaCard.addView(TextView(this).apply {
            text = "Muse Tool Premium • Manual Email • Meta ID Creator • Manual OTP Confirm"
            textSize = 12f
            setTextColor(Color.parseColor("#CBD5E1"))
            typeface = sansFont
            setPadding(0, (8 * d).toInt(), 0, (14 * d).toInt())
        })

        btnOpenMeta = Button(this).apply {
            text = "OPEN WORK META"
            textSize = 13f
            setTextColor(Color.parseColor("#041E15"))
            setBackgroundResource(R.drawable.bg_btn_selected_emerald)
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (50 * d).toInt()
            )
            setOnClickListener { openWorkMeta() }
        }
        metaCard.addView(btnOpenMeta)
        metaCard.setOnClickListener { openWorkMeta() }
        content.addView(metaCard)

        // Option 3: Meta To Ig
        metaToIgCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundResource(R.drawable.bg_card_rose)
            setPadding((18 * d).toInt(), (18 * d).toInt(), (18 * d).toInt(), (18 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (18 * d).toInt() }
        }
        val metaToIgTop = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        metaToIgTop.addView(TextView(this).apply {
            text = "3. Meta To Ig"
            textSize = 18f
            setTextColor(Color.parseColor("#FFFFFF"))
            setTypeface(sansFont, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        })
        tvMetaToIgBadge = TextView(this).apply {
            text = "INSTA AUTO"
            textSize = 10f
            setTextColor(Color.parseColor("#FDA4AF"))
            setBackgroundResource(R.drawable.bg_btn_rose_soft)
            setTypeface(monoFont, Typeface.BOLD)
            setPadding((10 * d).toInt(), (4 * d).toInt(), (10 * d).toInt(), (4 * d).toInt())
        }
        metaToIgTop.addView(tvMetaToIgBadge)
        metaToIgCard.addView(metaToIgTop)

        metaToIgCard.addView(TextView(this).apply {
            text = "Arafat Insta Auto v9.0 • WebView Auto • Nuclear Clear & Full Cookie Saver"
            textSize = 12f
            setTextColor(Color.parseColor("#CBD5E1"))
            typeface = sansFont
            setPadding(0, (8 * d).toInt(), 0, (14 * d).toInt())
        })

        btnOpenMetaToIg = Button(this).apply {
            text = "OPEN META TO IG"
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
            setOnClickListener { openMetaToIg() }
        }
        metaToIgCard.addView(btnOpenMetaToIg)
        metaToIgCard.setOnClickListener { openMetaToIg() }
        content.addView(metaToIgCard)

        content.addView(TextView(this).apply {
            text = "ভেতরে থাকা '⇄ Work' বাটনে ট্যাপ করে যেকোনো সময় কাজ পরিবর্তন করতে পারবেন।"
            textSize = 12f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#94A3B8"))
            typeface = sansFont
            setBackgroundResource(R.drawable.bg_card)
            setPadding((14 * d).toInt(), (12 * d).toInt(), (14 * d).toInt(), (12 * d).toInt())
        })

        scroll.addView(content)
        outer.addView(scroll)
        setContentView(outer)

        adminGuard = AdminControlManager.attachActivityGuard(
            activity = this,
            workKey = null,
            allowSwitchWork = false
        )
        AdminControlManager.addListener(adminStateListener)
        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        AdminControlManager.refreshNow()
    }

    override fun onDestroy() {
        AdminControlManager.removeListener(adminStateListener)
        adminGuard?.detach()
        adminGuard = null
        super.onDestroy()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    9101
                )
            }
        }
    }

    private fun applyWorkStates(state: AdminControlManager.ControlState) {
        val fbAllowed = state.isWorkAllowed(AdminControlManager.WORK_FACEBOOK)
        val metaAllowed = state.isWorkAllowed(AdminControlManager.WORK_META)
        val metaToIgAllowed = state.isWorkAllowed(AdminControlManager.WORK_INSTAGRAM)

        if (!state.allWorksEnabled) {
            tvAllWorksBanner.text = state.workOffMessage.ifBlank {
                "সব কাজ বর্তমানে অ্যাডমিন বন্ধ রেখেছেন।"
            }
            tvAllWorksBanner.visibility = View.VISIBLE
        } else {
            tvAllWorksBanner.visibility = View.GONE
        }

        if (fbAllowed) {
            fbCard.alpha = 1f
            tvFbBadge.text = "FB SUITE"
            tvFbBadge.setTextColor(Color.parseColor("#93C5FD"))
            tvFbBadge.setBackgroundResource(R.drawable.bg_btn_blue_soft)
            btnOpenFb.text = "OPEN WORK FACEBOOK"
            btnOpenFb.setTextColor(Color.parseColor("#FFFFFF"))
            btnOpenFb.setBackgroundResource(R.drawable.bg_btn_solid_blue)
        } else {
            fbCard.alpha = 0.65f
            tvFbBadge.text = "OFF"
            tvFbBadge.setTextColor(Color.parseColor("#FDA4AF"))
            tvFbBadge.setBackgroundResource(R.drawable.bg_badge_rose)
            btnOpenFb.text = "WORK FACEBOOK OFF"
            btnOpenFb.setTextColor(Color.parseColor("#CBD5E1"))
            btnOpenFb.setBackgroundResource(R.drawable.bg_btn_unselected)
        }

        if (metaAllowed) {
            metaCard.alpha = 1f
            tvMetaBadge.text = "MUSE TOOLS"
            tvMetaBadge.setTextColor(Color.parseColor("#34D399"))
            tvMetaBadge.setBackgroundResource(R.drawable.bg_badge_green)
            btnOpenMeta.text = "OPEN WORK META"
            btnOpenMeta.setTextColor(Color.parseColor("#041E15"))
            btnOpenMeta.setBackgroundResource(R.drawable.bg_btn_selected_emerald)
        } else {
            metaCard.alpha = 0.65f
            tvMetaBadge.text = "OFF"
            tvMetaBadge.setTextColor(Color.parseColor("#FDA4AF"))
            tvMetaBadge.setBackgroundResource(R.drawable.bg_badge_rose)
            btnOpenMeta.text = "WORK META OFF"
            btnOpenMeta.setTextColor(Color.parseColor("#CBD5E1"))
            btnOpenMeta.setBackgroundResource(R.drawable.bg_btn_unselected)
        }

        if (metaToIgAllowed) {
            metaToIgCard.alpha = 1f
            tvMetaToIgBadge.text = "INSTA AUTO"
            tvMetaToIgBadge.setTextColor(Color.parseColor("#FDA4AF"))
            tvMetaToIgBadge.setBackgroundResource(R.drawable.bg_btn_rose_soft)
            btnOpenMetaToIg.text = "OPEN META TO IG"
            btnOpenMetaToIg.setTextColor(Color.parseColor("#FFFFFF"))
            btnOpenMetaToIg.setBackgroundResource(R.drawable.bg_btn_solid_pink)
        } else {
            metaToIgCard.alpha = 0.65f
            tvMetaToIgBadge.text = "OFF"
            tvMetaToIgBadge.setTextColor(Color.parseColor("#FDA4AF"))
            tvMetaToIgBadge.setBackgroundResource(R.drawable.bg_badge_rose)
            btnOpenMetaToIg.text = "META TO IG OFF"
            btnOpenMetaToIg.setTextColor(Color.parseColor("#CBD5E1"))
            btnOpenMetaToIg.setBackgroundResource(R.drawable.bg_btn_unselected)
        }
    }

    private fun openWorkFacebook() {
        val state = AdminControlManager.currentState
        if (!state.isWorkAllowed(AdminControlManager.WORK_FACEBOOK)) {
            Toast.makeText(
                this,
                state.workOffMessage.ifBlank { "Work Facebook বর্তমানে বন্ধ আছে।" },
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    private fun openWorkMeta() {
        val state = AdminControlManager.currentState
        if (!state.isWorkAllowed(AdminControlManager.WORK_META)) {
            Toast.makeText(
                this,
                state.workOffMessage.ifBlank { "Work Meta বর্তমানে বন্ধ আছে।" },
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        startActivity(
            Intent(this, MetaWorkActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }

    private fun openMetaToIg() {
        val state = AdminControlManager.currentState
        if (!state.isWorkAllowed(AdminControlManager.WORK_INSTAGRAM)) {
            Toast.makeText(
                this,
                state.workOffMessage.ifBlank { "Meta To Ig বর্তমানে বন্ধ আছে।" },
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        startActivity(
            Intent(this, MetaToIgActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        )
    }
}
