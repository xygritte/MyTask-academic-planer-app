package com.mytask.ui.login

import org.junit.Assert.assertEquals
import org.junit.Test

class AuthGatePolicyTest {

    @Test
    fun onlineAuthenticatedUser_entersWorkspace_onlyWhenRestoreSucceeds() {
        assertEquals(
            AuthGateDecision.AUTHENTICATED_WORKSPACE,
            decideAuthGate(
                isOnline = true,
                hasFirebaseUser = true,
                hasGuestProfile = false,
                cloudRestoreSucceeded = true
            )
        )

        assertEquals(
            AuthGateDecision.LOGIN_SCREEN,
            decideAuthGate(
                isOnline = true,
                hasFirebaseUser = true,
                hasGuestProfile = false,
                cloudRestoreSucceeded = false
            )
        )
    }

    @Test
    fun offlineUser_isAlwaysRestrictedToOfflineLoginGate() {
        assertEquals(
            AuthGateDecision.OFFLINE_LOGIN,
            decideAuthGate(
                isOnline = false,
                hasFirebaseUser = true,
                hasGuestProfile = true,
                cloudRestoreSucceeded = true
            )
        )

        assertEquals(
            AuthGateDecision.OFFLINE_LOGIN,
            decideAuthGate(
                isOnline = false,
                hasFirebaseUser = false,
                hasGuestProfile = true,
                cloudRestoreSucceeded = false
            )
        )
    }

    @Test
    fun onlineGuestWithoutFirebase_usesGuestWorkspace() {
        assertEquals(
            AuthGateDecision.GUEST_WORKSPACE,
            decideAuthGate(
                isOnline = true,
                hasFirebaseUser = false,
                hasGuestProfile = true,
                cloudRestoreSucceeded = false
            )
        )
    }

    @Test
    fun onlineWithoutAuthenticatedUserOrGuestProfile_showsLogin() {
        assertEquals(
            AuthGateDecision.LOGIN_SCREEN,
            decideAuthGate(
                isOnline = true,
                hasFirebaseUser = false,
                hasGuestProfile = false,
                cloudRestoreSucceeded = false
            )
        )
    }
}
