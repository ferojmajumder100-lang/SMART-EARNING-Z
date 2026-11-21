package com.my.apk

import android.app.Application

class SmartEarningApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AntiCaptureShield.init(this)
        AdminControlManager.init(this)
    }
}
