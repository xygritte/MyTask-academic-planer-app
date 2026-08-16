package com.mytask.data.repository

/**
 * Pure account-scoping rules for online and local academic data.
 *
 * Every authenticated account gets its own Firestore backup document and its
 * own local JSON cache name. Guest data is intentionally scoped separately.
 */
object AccountDataScope {
    fun firestoreDocumentPath(uid: String): String {
        require(uid.isNotBlank())
        return "users/$uid/backups/academic"
    }

    fun localJsonFileName(uid: String): String {
        require(uid.isNotBlank())
        val safeUid = uid.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return "mytask_data_$safeUid.json"
    }
}
