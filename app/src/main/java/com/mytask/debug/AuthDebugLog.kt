package com.mytask.debug

import android.util.Log

/**
 * Temporary diagnostics for the authentication/session flow.
 * Does not log passwords, tokens, emails, or full user identifiers.
 */
object AuthDebugLog {
    const val TAG = "MyTaskAuth"

    fun d(message: String) {
        Log.d(TAG, message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    fun uid(uid: String?): String {
        if (uid.isNullOrBlank()) return "none"
        return if (uid.length <= 6) uid else "...${uid.takeLast(6)}"
    }
}
