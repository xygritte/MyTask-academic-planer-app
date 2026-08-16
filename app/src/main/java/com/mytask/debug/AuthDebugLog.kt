package com.mytask.debug

/**
 * Backward-compatible authentication diagnostics.
 * The tag is unified with the full application trace so one Logcat filter
 * captures the complete activity flow.
 */
object AuthDebugLog {
    const val TAG = AppDebugLog.TAG

    fun d(message: String) {
        AppDebugLog.d("AUTH", message)
    }

    fun e(message: String, throwable: Throwable? = null) {
        AppDebugLog.e("AUTH", message, throwable)
    }

    fun uid(uid: String?): String =
        AppDebugLog.uid(uid)
}
