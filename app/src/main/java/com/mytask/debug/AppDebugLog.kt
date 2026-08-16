package com.mytask.debug

import android.util.Log
import com.mytask.BuildConfig

/**
 * Full-app diagnostics used during development.
 * Release builds do not emit debug logs, avoiding unnecessary Logcat work and
 * accidental exposure of internal state in production.
 * Never log passwords, ID tokens, email addresses, or full user IDs.
 */
object AppDebugLog {
    const val TAG = "MyTaskTrace"

    fun d(area: String, message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "[$area] $message")
        }
    }

    fun e(area: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "[$area] $message", throwable)
        }
    }

    fun uid(uid: String?): String {
        if (uid.isNullOrBlank()) return "none"
        return if (uid.length <= 6) uid else "...${uid.takeLast(6)}"
    }
}
