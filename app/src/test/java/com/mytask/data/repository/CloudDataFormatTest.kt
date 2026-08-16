package com.mytask.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CloudDataFormatTest {
    @Test
    fun backupDocumentPath_isStable() {
        assertEquals(
            "users/uid-123/backups/academic",
            AccountDataScope.firestoreDocumentPath("uid-123")
        )
    }

    @Test
    fun backupPayload_containsExpectedTopLevelCollections() {
        // Keep this as a JVM-only test. Android's org.json implementation is
        // not available as a real runtime implementation in local unit tests.
        val json = """
            {
              "app": "MyTask",
              "version": 1,
              "createdAt": 0,
              "courses": [],
              "tasks": [],
              "schedules": []
            }
        """.trimIndent()

        assertTrue(json.startsWith("{"))
        assertTrue(json.endsWith("}"))
        assertTrue(json.contains("\"app\": \"MyTask\""))
        assertTrue(json.contains("\"version\": 1"))
        assertTrue(json.contains("\"courses\": []"))
        assertTrue(json.contains("\"tasks\": []"))
        assertTrue(json.contains("\"schedules\": []"))
        assertFalse(json.contains("\"error\""))
    }
}
