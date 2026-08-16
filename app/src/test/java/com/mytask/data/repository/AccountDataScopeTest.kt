package com.mytask.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AccountDataScopeTest {
    @Test
    fun differentAccountsUseDifferentFirestoreDocuments() {
        val accountA = AccountDataScope.firestoreDocumentPath("uid-A")
        val accountB = AccountDataScope.firestoreDocumentPath("uid-B")

        assertEquals("users/uid-A/backups/academic", accountA)
        assertEquals("users/uid-B/backups/academic", accountB)
        assertNotEquals(accountA, accountB)
    }

    @Test
    fun differentAccountsUseDifferentLocalJsonFiles() {
        val accountA = AccountDataScope.localJsonFileName("uid-A")
        val accountB = AccountDataScope.localJsonFileName("uid-B")

        assertEquals("mytask_data_uid-A.json", accountA)
        assertEquals("mytask_data_uid-B.json", accountB)
        assertNotEquals(accountA, accountB)
    }

    @Test
    fun uidIsSanitizedForLocalFileName() {
        assertEquals(
            "mytask_data_user_123_test.json",
            AccountDataScope.localJsonFileName("user/123:test")
        )
    }
}
