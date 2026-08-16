package com.mytask.debug

import android.util.Log

/**
 * Temporary full-app diagnostics for debugging the authentication, session,
 * local database, cloud sync, notifications, and lifecycle.
 * Never log passwords, ID tokens, email addresses, or full user IDs.
 */
object AppDebugLog {
    const val TAG = "MyTaskTrace"

    fun d(area: String, message: String) {
        Log.d(TAG, "[$area] $message")
    }

    fun e(area: String, message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[$area] $message", throwable)
    }

    fun uid(uid: String?): String {
        if (uid.isNullOrBlank()) return "none"
        return if (uid.length <= 6) uid else "...${uid.takeLast(6)}"
    }
}
