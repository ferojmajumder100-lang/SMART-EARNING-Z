package com.my.apk

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.res.ResourcesCompat
import kotlin.math.abs

class BgOverlayService : Service() {

    companion object {
        const val ACTION_START = "com.my.apk.BG_OVERLAY_START"
        const val ACTION_STOP = "com.my.apk.BG_OVERLAY_STOP"
        private const val CHANNEL_ID = "bg_overlay_create_channel"
        private const val NOTIF_ID = 7701
        private const val PREFS_NAME = "bg_overlay_prefs"
        private const val KEY_SIZE_PCT = "overlay_size_pct"
        private const val KEY_POS_X = "overlay_pos_x"
        private const val KEY_POS_Y = "overlay_pos_y"

        @Volatile
        var isRunning: Boolean = false
            private set

        @Volatile
        var isCreating: Boolean = false

        @Volatile
        var lastStatus: String = "Overlay stopped"
            private set

        @Volatile
        var currentSizePercent: Int = 100
            private set

        @Volatile
        var onTriggerCreate: (((String, Boolean) -> Unit) -> Unit)? = null

        @Volatile
        var onUiStateChanged: (() -> Unit)? = null

        @Volatile
        private var activeInstance: BgOverlayService? = null

        fun loadSavedSizePercent(context: Context): Int {
            val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_SIZE_PCT, 100)
                .coerceIn(60, 180)
            currentSizePercent = saved
            return saved
        }

        fun updateSizeRealtime(context: Context, percent: Int) {
            val clamped = percent.coerceIn(60, 180)
            currentSizePercent = clamped
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putInt(KEY_SIZE_PCT, clamped)
                .apply()
            activeInstance?.applySizeOnMainThread(clamped)
        }

        fun updateStatusFromExternal(status: String, busy: Boolean) {
            isCreating = busy
            lastStatus = status
            activeInstance?.applyStatusOnMainThread(status, busy)
            onUiStateChanged?.invoke()
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var overlayButton: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        loadSavedSizePercent(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }

        AdminControlManager.init(this)
        if (!AdminControlManager.isWorkAllowed(AdminControlManager.WORK_FACEBOOK)) {
            Toast.makeText(this, "Work Facebook currently OFF by Admin", Toast.LENGTH_SHORT).show()
            stopSelf()
            return START_NOT_STICKY
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Overlay permission required!", Toast.LENGTH_SHORT).show()
            stopSelf()
            return START_NOT_STICKY
        }

        ensureNotificationChannel()
        val notif = buildNotification("Floating Create active • Tap button to create")
        val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIF_ID, notif, fgsType)

        if (overlayButton == null) {
            createOverlayView()
        } else {
            applySizeOnMainThread(currentSizePercent)
        }

        isRunning = true
        lastStatus = "Overlay active • Tap Create anywhere"
        onUiStateChanged?.invoke()
        return START_STICKY
    }

    private fun createOverlayView() {
        val d = resources.displayMetrics.density
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedX = prefs.getInt(KEY_POS_X, (24 * d).toInt())
        val savedY = prefs.getInt(KEY_POS_Y, (220 * d).toInt())

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val scale = currentSizePercent / 100f
        val diameter = (68f * d * scale).toInt().coerceAtLeast((48 * d).toInt())

        val params = WindowManager.LayoutParams(
            diameter,
            diameter,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = savedX
            y = savedY
        }
        layoutParams = params

        val sansFont = ResourcesCompat.getFont(this, R.font.plus_jakarta_sans)
        val btn = TextView(this).apply {
            text = "Create"
            gravity = Gravity.CENTER
            setTextColor(Color.parseColor("#FFFFFF"))
            setTypeface(sansFont, Typeface.BOLD)
            textSize = (12f * scale).coerceIn(9f, 20f)
            setBackgroundResource(R.drawable.bg_overlay_create_btn)
            isSingleLine = true
            contentDescription = "Create Facebook account floating button"
            setPadding((4 * d * scale).toInt(), 0, (4 * d * scale).toInt(), 0)
        }

        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        btn.setOnTouchListener { v, event ->
            val lp = layoutParams ?: return@setOnTouchListener false
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = lp.x
                    initialY = lp.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - initialTouchX
                    val dy = event.rawY - initialTouchY
                    if (!isDragging && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val metrics = resources.displayMetrics
                        val maxX = (metrics.widthPixels - lp.width).coerceAtLeast(0)
                        val maxY = (metrics.heightPixels - lp.height).coerceAtLeast(0)
                        lp.x = (initialX + dx.toInt()).coerceIn(0, maxX)
                        lp.y = (initialY + dy.toInt()).coerceIn(0, maxY)
                        try {
                            windowManager?.updateViewLayout(v, lp)
                        } catch (_: Exception) {
                        }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isDragging) {
                        prefs.edit()
                            .putInt(KEY_POS_X, lp.x)
                            .putInt(KEY_POS_Y, lp.y)
                            .apply()
                    } else {
                        v.performClick()
                        handleCreateButtonTap()
                    }
                    true
                }
                else -> false
            }
        }

        overlayButton = btn
        try {
            windowManager?.addView(btn, params)
        } catch (e: Exception) {
            Toast.makeText(this, "Overlay error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            overlayButton = null
            stopSelf()
        }
    }

    private fun handleCreateButtonTap() {
        if (!AdminControlManager.isWorkAllowed(AdminControlManager.WORK_FACEBOOK)) {
            Toast.makeText(this, "Work Facebook currently OFF by Admin", Toast.LENGTH_SHORT).show()
            stopSelf()
            return
        }
        if (isCreating) {
            Toast.makeText(this, "Creating account... please wait", Toast.LENGTH_SHORT).show()
            return
        }
        val trigger = onTriggerCreate
        if (trigger == null) {
            Toast.makeText(this, "Open SMART EARNING Z to sync settings first", Toast.LENGTH_SHORT).show()
            val launch = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            }
            startActivity(launch)
            return
        }
        trigger.invoke { status, busy ->
            updateStatusFromExternal(status, busy)
        }
    }

    private fun applySizeOnMainThread(percent: Int) {
        mainHandler.post {
            val btn = overlayButton ?: return@post
            val lp = layoutParams ?: return@post
            val d = resources.displayMetrics.density
            val scale = percent / 100f
            val diameter = (68f * d * scale).toInt().coerceAtLeast((48 * d).toInt())
            lp.width = diameter
            lp.height = diameter
            btn.textSize = (12f * scale).coerceIn(9f, 20f)
            btn.setPadding((4 * d * scale).toInt(), 0, (4 * d * scale).toInt(), 0)
            val metrics = resources.displayMetrics
            lp.x = lp.x.coerceIn(0, (metrics.widthPixels - lp.width).coerceAtLeast(0))
            lp.y = lp.y.coerceIn(0, (metrics.heightPixels - lp.height).coerceAtLeast(0))
            try {
                windowManager?.updateViewLayout(btn, lp)
            } catch (_: Exception) {
            }
        }
    }

    private fun applyStatusOnMainThread(status: String, busy: Boolean) {
        mainHandler.post {
            val btn = overlayButton
            if (btn != null) {
                btn.text = if (busy) "Wait..." else "Create"
                btn.setBackgroundResource(
                    if (busy) R.drawable.bg_overlay_create_busy else R.drawable.bg_overlay_create_btn
                )
            }
            try {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.notify(NOTIF_ID, buildNotification(status))
            } catch (_: Exception) {
            }
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val ch = NotificationChannel(
                    CHANNEL_ID,
                    "Background Overlay Create",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps the floating Create button active in background"
                    setShowBadge(false)
                }
                nm.createNotificationChannel(ch)
            }
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        val piFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val openPending = PendingIntent.getActivity(this, 0, openIntent, piFlags)

        val stopIntent = Intent(this, BgOverlayService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 1, stopIntent, piFlags)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("SMART EARNING Z • Bg Create")
            .setContentText(contentText)
            .setOngoing(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openPending)
            .addAction(0, "Stop Overlay", stopPending)
            .build()
    }

    override fun onDestroy() {
        activeInstance = null
        isRunning = false
        isCreating = false
        lastStatus = "Overlay stopped"
        overlayButton?.let { btn ->
            try {
                windowManager?.removeView(btn)
            } catch (_: Exception) {
            }
        }
        overlayButton = null
        layoutParams = null
        onUiStateChanged?.invoke()
        super.onDestroy()
    }
}
