package com.my.apk

import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.res.ResourcesCompat
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

object AdminControlManager {

    const val WORK_FACEBOOK = "facebook"
    const val WORK_META = "meta"
    const val WORK_INSTAGRAM = "instagram"

    private const val PREFS_NAME = "smart_earning_z_admin_ctl"
    private const val KEY_APP_ENABLED = "app_enabled"
    private const val KEY_APP_OFF_TITLE = "app_off_title"
    private const val KEY_APP_OFF_MSG = "app_off_msg"
    private const val KEY_ALL_WORKS_ENABLED = "all_works_enabled"
    private const val KEY_WORK_FB_ENABLED = "work_fb_enabled"
    private const val KEY_WORK_META_ENABLED = "work_meta_enabled"
    private const val KEY_WORK_IG_ENABLED = "work_ig_enabled"
    private const val KEY_WORK_OFF_MSG = "work_off_msg"
    private const val KEY_LAST_NOTICE_ID = "last_delivered_notice_id"
    private const val KEY_SEEN_NOTICE_IDS = "seen_notice_ids_csv"

    private const val NOTICE_CHANNEL_ID = "smart_earning_z_admin_notices"
    private const val NOTICE_CHANNEL_NAME = "Admin Notices & Updates"
    private const val POLL_INTERVAL_MS = 2500L

    data class ControlState(
        val appEnabled: Boolean = true,
        val appOffTitle: String = "APP TEMPORARILY OFF",
        val appOffMessage: String = "অ্যাডমিন সাময়িকভাবে অ্যাপটি বন্ধ রেখেছেন। চালু হলে অটোমেটিক খুলে যাবে।",
        val allWorksEnabled: Boolean = true,
        val workFacebookEnabled: Boolean = true,
        val workMetaEnabled: Boolean = true,
        val workInstagramEnabled: Boolean = true,
        val workOffMessage: String = "এই কাজটি বর্তমানে অ্যাডমিন বন্ধ রেখেছেন।",
        val updatedAt: Long = 0L
    ) {
        fun isWorkAllowed(workKey: String): Boolean {
            if (!appEnabled || !allWorksEnabled) return false
            return when (workKey) {
                WORK_FACEBOOK -> workFacebookEnabled
                WORK_META -> workMetaEnabled
                WORK_INSTAGRAM -> workInstagramEnabled
                else -> true
            }
        }
    }

    data class NoticeData(
        val noticeId: String = "",
        val title: String = "",
        val body: String = "",
        val link: String = "",
        val active: Boolean = false,
        val sentAt: Long = 0L
    )

    class GuardHandle internal constructor(
        private val listener: (ControlState) -> Unit,
        private val cleanup: () -> Unit
    ) {
        fun detach() {
            removeListener(listener)
            cleanup()
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val listeners = CopyOnWriteArrayList<(ControlState) -> Unit>()
    private val initialized = AtomicBoolean(false)
    private val immediateExecutor = Executors.newSingleThreadExecutor()
    private val pollStarted = AtomicBoolean(false)

    @Volatile
    private var appContext: Context? = null

    @Volatile
    var currentState: ControlState = ControlState()
        private set

    fun init(context: Context) {
        val ctx = context.applicationContext
        appContext = ctx
        if (initialized.compareAndSet(false, true)) {
            loadCachedState(ctx)
            ensureNoticeChannel(ctx)
            startPollingLoop()
        }
    }

    fun addListener(listener: (ControlState) -> Unit) {
        if (!listeners.contains(listener)) {
            listeners.add(listener)
        }
        val stateSnapshot = currentState
        mainHandler.post { listener(stateSnapshot) }
    }

    fun removeListener(listener: (ControlState) -> Unit) {
        listeners.remove(listener)
    }

    fun refreshNow() {
        val ctx = appContext ?: return
        immediateExecutor.execute {
            fetchControlAndNoticeOnce(ctx)
        }
    }

    fun isAppAllowed(): Boolean = currentState.appEnabled

    fun isWorkAllowed(workKey: String): Boolean = currentState.isWorkAllowed(workKey)

    fun attachActivityGuard(
        activity: Activity,
        workKey: String?,
        allowSwitchWork: Boolean
    ): GuardHandle {
        init(activity)
        val rootContent = activity.findViewById<ViewGroup>(android.R.id.content)
        val d = activity.resources.displayMetrics.density
        val sansFont = ResourcesCompat.getFont(activity, R.font.plus_jakarta_sans)
        val monoFont = ResourcesCompat.getFont(activity, R.font.jetbrains_mono)

        val overlay = FrameLayout(activity).apply {
            setBackgroundColor(Color.parseColor("#F50B1120"))
            isClickable = true
            isFocusable = true
            visibility = View.GONE
            elevation = 100f * d
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val card = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundResource(R.drawable.bg_card_blue)
            setPadding((22 * d).toInt(), (26 * d).toInt(), (22 * d).toInt(), (26 * d).toInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                marginStart = (22 * d).toInt()
                marginEnd = (22 * d).toInt()
            }
        }

        val tvBadge = TextView(activity).apply {
            text = "ADMIN CONTROL"
            textSize = 11f
            setTextColor(Color.parseColor("#FDA4AF"))
            setBackgroundResource(R.drawable.bg_badge_rose)
            setTypeface(monoFont, Typeface.BOLD)
            setPadding((14 * d).toInt(), (6 * d).toInt(), (14 * d).toInt(), (6 * d).toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (14 * d).toInt() }
        }
        card.addView(tvBadge)

        val tvTitle = TextView(activity).apply {
            text = "APP TEMPORARILY OFF"
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFFFFF"))
            setTypeface(sansFont, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (10 * d).toInt() }
        }
        card.addView(tvTitle)

        val tvMessage = TextView(activity).apply {
            text = "অ্যাডমিন সাময়িকভাবে অ্যাপটি বন্ধ রেখেছেন।"
            textSize = 13f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#CBD5E1"))
            typeface = sansFont
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (18 * d).toInt() }
        }
        card.addView(tvMessage)

        val tvLiveHint = TextView(activity).apply {
            text = "● Real-time Sync Active • চালু হলে অটোমেটিক খুলে যাবে"
            textSize = 11f
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#60A5FA"))
            setTypeface(monoFont, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = (16 * d).toInt() }
        }
        card.addView(tvLiveHint)

        val btnSwitch = Button(activity).apply {
            text = "⇄ Switch Work"
            textSize = 13f
            setTextColor(Color.parseColor("#FFFFFF"))
            setBackgroundResource(R.drawable.bg_btn_solid_blue)
            setTypeface(sansFont, Typeface.BOLD)
            stateListAnimator = null
            elevation = 0f
            isAllCaps = false
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (48 * d).toInt()
            ).apply { bottomMargin = (10 * d).toInt() }
            setOnClickListener {
                activity.startActivity(
                    Intent(activity, WorkSelectorActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                )
            }
        }
        card.addView(btnSwitch)

        val btnRetry = Button(activity).apply {
            text = "↻ CHECK STATUS NOW"
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
            )
            setOnClickListener { refreshNow() }
        }
        card.addView(btnRetry)

        overlay.addView(card)
        rootContent.addView(overlay)

        val listener: (ControlState) -> Unit = { state ->
            if (!activity.isFinishing && !activity.isDestroyed) {
                when {
                    !state.appEnabled -> {
                        tvBadge.text = "FULL APP OFF"
                        tvTitle.text = state.appOffTitle.ifBlank { "APP TEMPORARILY OFF" }
                        tvMessage.text = state.appOffMessage.ifBlank {
                            "অ্যাডমিন সাময়িকভাবে অ্যাপটি বন্ধ রেখেছেন। চালু হলে অটোমেটিক খুলে যাবে।"
                        }
                        btnSwitch.visibility = View.GONE
                        overlay.visibility = View.VISIBLE
                        overlay.bringToFront()
                    }
                    workKey != null && !state.isWorkAllowed(workKey) -> {
                        tvBadge.text = if (!state.allWorksEnabled) "ALL WORK OFF" else "WORK OFF"
                        val workLabel = when (workKey) {
                            WORK_FACEBOOK -> "WORK FACEBOOK OFF"
                            WORK_META -> "WORK META OFF"
                            else -> "WORK TEMPORARILY OFF"
                        }
                        tvTitle.text = if (!state.allWorksEnabled) "ALL WORKS PAUSED" else workLabel
                        tvMessage.text = state.workOffMessage.ifBlank {
                            "এই কাজটি বর্তমানে অ্যাডমিন বন্ধ রেখেছেন। চালু হলে অটোমেটিক খুলে যাবে।"
                        }
                        btnSwitch.visibility = if (allowSwitchWork) View.VISIBLE else View.GONE
                        overlay.visibility = View.VISIBLE
                        overlay.bringToFront()
                    }
                    else -> {
                        overlay.visibility = View.GONE
                    }
                }
            }
        }

        addListener(listener)
        return GuardHandle(listener) {
            mainHandler.post {
                try {
                    rootContent.removeView(overlay)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun startPollingLoop() {
        if (!pollStarted.compareAndSet(false, true)) return
        val thread = Thread {
            while (true) {
                val ctx = appContext
                if (ctx != null) {
                    fetchControlAndNoticeOnce(ctx)
                }
                try {
                    Thread.sleep(POLL_INTERVAL_MS)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }
        thread.isDaemon = true
        thread.name = "AdminControlPoller"
        thread.start()
    }

    private fun fetchControlAndNoticeOnce(context: Context) {
        fetchControlDocument(context)
        fetchNoticeDocument(context)
    }

    private fun fetchControlDocument(context: Context) {
        try {
            val url = SecureVault.rtdbUrl(context, "app_config/control")
            val req = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("Cache-Control", "no-cache")
                .get()
                .build()
            SecureVault.getStrictApiClient().newCall(req).execute().use { res ->
                if (!res.isSuccessful) return
                val raw = res.body?.string()?.trim() ?: return
                if (raw.isEmpty() || raw == "null") return
                val root = JSONObject(raw)

                val prev = currentState
                val next = ControlState(
                    appEnabled = root.optBoolean("appEnabled", true),
                    appOffTitle = root.optString("appOffTitle", prev.appOffTitle),
                    appOffMessage = root.optString("appOffMessage", prev.appOffMessage),
                    allWorksEnabled = root.optBoolean("allWorksEnabled", true),
                    workFacebookEnabled = root.optBoolean("workFacebookEnabled", true),
                    workMetaEnabled = root.optBoolean("workMetaEnabled", true),
                    workInstagramEnabled = root.optBoolean("workInstagramEnabled", true),
                    workOffMessage = root.optString("workOffMessage", prev.workOffMessage),
                    updatedAt = root.optLong("updatedAt", 0L)
                )

                if (next != prev) {
                    currentState = next
                    saveCachedState(context, next)
                    enforceOverlayStopIfNeeded(context, next)
                    notifyListeners(next)
                } else {
                    enforceOverlayStopIfNeeded(context, next)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun enforceOverlayStopIfNeeded(context: Context, state: ControlState) {
        if ((!state.appEnabled || !state.allWorksEnabled || !state.workFacebookEnabled) && BgOverlayService.isRunning) {
            try {
                val stopIntent = Intent(context, BgOverlayService::class.java).apply {
                    action = BgOverlayService.ACTION_STOP
                }
                context.startService(stopIntent)
            } catch (_: Exception) {
            }
        }
    }

    private fun fetchNoticeDocument(context: Context) {
        try {
            val url = SecureVault.rtdbUrl(context, "app_config/notice")
            val req = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .addHeader("Cache-Control", "no-cache")
                .get()
                .build()
            SecureVault.getStrictApiClient().newCall(req).execute().use { res ->
                if (!res.isSuccessful) return
                val raw = res.body?.string()?.trim() ?: return
                if (raw.isEmpty() || raw == "null") return
                val root = JSONObject(raw)

                val notice = NoticeData(
                    noticeId = root.optString("noticeId", "").trim(),
                    title = root.optString("title", "").trim(),
                    body = root.optString("body", "").trim(),
                    link = root.optString("link", "").trim(),
                    active = root.optBoolean("active", false),
                    sentAt = root.optLong("sentAt", 0L)
                )

                if (!notice.active || notice.noticeId.isEmpty()) return
                if (notice.title.isEmpty() && notice.body.isEmpty()) return

                deliverNoticeOncePerDevice(context, notice)
            }
        } catch (_: Exception) {
        }
    }

    @Synchronized
    private fun deliverNoticeOncePerDevice(context: Context, notice: NoticeData) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastId = prefs.getString(KEY_LAST_NOTICE_ID, "") ?: ""
        if (lastId == notice.noticeId) return

        val seenCsv = prefs.getString(KEY_SEEN_NOTICE_IDS, "") ?: ""
        val seenList = seenCsv.split(",").filter { it.isNotBlank() }.toMutableList()
        if (seenList.contains(notice.noticeId)) return

        // Persist BEFORE notifying so a given noticeId can strictly fire ONLY ONCE on this device
        seenList.add(notice.noticeId)
        while (seenList.size > 100) {
            seenList.removeAt(0)
        }
        prefs.edit()
            .putString(KEY_LAST_NOTICE_ID, notice.noticeId)
            .putString(KEY_SEEN_NOTICE_IDS, seenList.joinToString(","))
            .commit()

        showTopBarNotification(context, notice)
    }

    private fun showTopBarNotification(context: Context, notice: NoticeData) {
        try {
            ensureNoticeChannel(context)
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val clickIntent = if (
                notice.link.startsWith("https://", ignoreCase = true) ||
                notice.link.startsWith("http://", ignoreCase = true)
            ) {
                Intent(Intent.ACTION_VIEW, Uri.parse(notice.link)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(context, WorkSelectorActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                }
            }

            val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
            val reqCode = (notice.noticeId.hashCode() and 0x7FFFFFFF)
            val pendingIntent = PendingIntent.getActivity(context, reqCode, clickIntent, piFlags)

            val largeIcon = try {
                BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
            } catch (_: Exception) {
                null
            }

            val title = notice.title.ifBlank { "SMART EARNING Z" }
            val text = notice.body.ifBlank { title }

            val builder = NotificationCompat.Builder(context, NOTICE_CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setContentIntent(pendingIntent)

            if (largeIcon != null) {
                builder.setLargeIcon(largeIcon)
            }

            val notifId = 9000 + (reqCode % 50000)
            nm.notify(notifId, builder.build())
        } catch (_: Exception) {
        }
    }

    private fun ensureNoticeChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(NOTICE_CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    NOTICE_CHANNEL_ID,
                    NOTICE_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Important announcements and alerts from Smart Earning Z"
                    enableLights(true)
                    enableVibration(true)
                    lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    private fun notifyListeners(state: ControlState) {
        mainHandler.post {
            for (listener in listeners) {
                try {
                    listener(state)
                } catch (_: Exception) {
                }
            }
        }
    }

    private fun loadCachedState(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        currentState = ControlState(
            appEnabled = prefs.getBoolean(KEY_APP_ENABLED, true),
            appOffTitle = prefs.getString(KEY_APP_OFF_TITLE, "APP TEMPORARILY OFF") ?: "APP TEMPORARILY OFF",
            appOffMessage = prefs.getString(
                KEY_APP_OFF_MSG,
                "অ্যাডমিন সাময়িকভাবে অ্যাপটি বন্ধ রেখেছেন। চালু হলে অটোমেটিক খুলে যাবে।"
            ) ?: "অ্যাডমিন সাময়িকভাবে অ্যাপটি বন্ধ রেখেছেন। চালু হলে অটোমেটিক খুলে যাবে।",
            allWorksEnabled = prefs.getBoolean(KEY_ALL_WORKS_ENABLED, true),
            workFacebookEnabled = prefs.getBoolean(KEY_WORK_FB_ENABLED, true),
            workMetaEnabled = prefs.getBoolean(KEY_WORK_META_ENABLED, true),
            workInstagramEnabled = prefs.getBoolean(KEY_WORK_IG_ENABLED, true),
            workOffMessage = prefs.getString(
                KEY_WORK_OFF_MSG,
                "এই কাজটি বর্তমানে অ্যাডমিন বন্ধ রেখেছেন।"
            ) ?: "এই কাজটি বর্তমানে অ্যাডমিন বন্ধ রেখেছেন।"
        )
    }

    private fun saveCachedState(context: Context, state: ControlState) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_APP_ENABLED, state.appEnabled)
            .putString(KEY_APP_OFF_TITLE, state.appOffTitle)
            .putString(KEY_APP_OFF_MSG, state.appOffMessage)
            .putBoolean(KEY_ALL_WORKS_ENABLED, state.allWorksEnabled)
            .putBoolean(KEY_WORK_FB_ENABLED, state.workFacebookEnabled)
            .putBoolean(KEY_WORK_META_ENABLED, state.workMetaEnabled)
            .putBoolean(KEY_WORK_IG_ENABLED, state.workInstagramEnabled)
            .putString(KEY_WORK_OFF_MSG, state.workOffMessage)
            .apply()
    }

    private fun readBool(fields: JSONObject, key: String, fallback: Boolean): Boolean {
        val obj = fields.optJSONObject(key) ?: return fallback
        return if (obj.has("booleanValue")) obj.optBoolean("booleanValue", fallback) else fallback
    }

    private fun readString(fields: JSONObject, key: String, fallback: String): String {
        val obj = fields.optJSONObject(key) ?: return fallback
        return if (obj.has("stringValue")) obj.optString("stringValue", fallback) else fallback
    }

    private fun readLong(fields: JSONObject, key: String, fallback: Long): Long {
        val obj = fields.optJSONObject(key) ?: return fallback
        return when {
            obj.has("integerValue") -> obj.optString("integerValue", "").toLongOrNull() ?: fallback
            obj.has("doubleValue") -> obj.optDouble("doubleValue", fallback.toDouble()).toLong()
            else -> fallback
        }
    }
}
