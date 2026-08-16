package com.mytask.ui.login

/**
 * Pure decision model for the top-level authentication gate.
 * It is deliberately independent from Android/Firebase so the most important
 * login/restore rules can be verified with fast JVM unit tests.
 */
enum class AuthGateDecision {
    AUTHENTICATED_WORKSPACE,
    GUEST_WORKSPACE,
    LOGIN_SCREEN,
    OFFLINE_LOGIN
}

fun decideAuthGate(
    isOnline: Boolean,
    hasFirebaseUser: Boolean,
    hasGuestProfile: Boolean,
    cloudRestoreSucceeded: Boolean
): AuthGateDecision {
    if (!isOnline) {
        return AuthGateDecision.OFFLINE_LOGIN
    }

    if (hasFirebaseUser) {
        return if (cloudRestoreSucceeded) {
            AuthGateDecision.AUTHENTICATED_WORKSPACE
        } else {
            AuthGateDecision.LOGIN_SCREEN
        }
    }

    return if (hasGuestProfile) {
        AuthGateDecision.GUEST_WORKSPACE
    } else {
        AuthGateDecision.LOGIN_SCREEN
    }
}
