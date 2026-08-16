package com.mytask.data.repository

import org.json.JSONObject
import org.junit.Assert.assertEquals
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
        val json = JSONObject()
            .put("app", "MyTask")
            .put("version", 1)
            .put("courses", emptyList<Any>())
            .put("tasks", emptyList<Any>())
            .put("schedules", emptyList<Any>())

        assertEquals("MyTask", json.getString("app"))
        assertEquals(1, json.getInt("version"))
        assertTrue(json.has("courses"))
        assertTrue(json.has("tasks"))
        assertTrue(json.has("schedules"))
    }
}
