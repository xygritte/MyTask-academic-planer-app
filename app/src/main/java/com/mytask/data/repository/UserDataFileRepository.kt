package com.mytask.data.repository

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class UserDataFile(
    val uri: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long,
    val modifiedAt: Long
)

private val Context.userDataFileStore by preferencesDataStore(name = "mytask_user_files")

@Singleton
class UserDataFileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val filesKey = stringPreferencesKey("files_json")

    val files: Flow<List<UserDataFile>> = context.userDataFileStore.data.map { preferences ->
        parse(preferences[filesKey].orEmpty())
            .filter { file ->
                runCatching {
                    context.contentResolver.persistedUriPermissions.any { it.uri.toString() == file.uri } ||
                        context.contentResolver.query(Uri.parse(file.uri), null, null, null, null)?.use { true } == true
                }.getOrDefault(false)
            }
            .sortedByDescending { it.modifiedAt }
    }

    suspend fun remember(uri: Uri) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
        val name = queryDisplayName(uri) ?: uri.lastPathSegment ?: "MyTask File"
        val size = querySize(uri)
        val modified = queryModified(uri)
        val entry = UserDataFile(uri.toString(), name, mimeType, size, modified)

        context.userDataFileStore.edit { preferences ->
            val current = parse(preferences[filesKey].orEmpty())
                .filterNot { it.uri == entry.uri }
                .toMutableList()
            current.add(entry)
            preferences[filesKey] = serialize(current)
        }
    }

    suspend fun remove(uri: String) {
        context.userDataFileStore.edit { preferences ->
            val current = parse(preferences[filesKey].orEmpty()).filterNot { it.uri == uri }
            preferences[filesKey] = serialize(current)
        }
    }

    private fun queryDisplayName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, arrayOf("_display_name"), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    }.getOrNull()

    private fun querySize(uri: Uri): Long = runCatching {
        context.contentResolver.query(uri, arrayOf("_size"), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else 0L
        } ?: 0L
    }.getOrDefault(0L)

    private fun queryModified(uri: Uri): Long = runCatching {
        context.contentResolver.query(uri, arrayOf("last_modified"), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getLong(0) else System.currentTimeMillis()
        } ?: System.currentTimeMillis()
    }.getOrDefault(System.currentTimeMillis())

    private fun parse(json: String): List<UserDataFile> = runCatching {
        if (json.isBlank()) return emptyList()
        val array = JSONArray(json)
        buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    UserDataFile(
                        uri = item.optString("uri"),
                        name = item.optString("name", "MyTask File"),
                        mimeType = item.optString("mimeType", "application/octet-stream"),
                        sizeBytes = item.optLong("sizeBytes", 0L),
                        modifiedAt = item.optLong("modifiedAt", 0L)
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    private fun serialize(files: List<UserDataFile>): String = JSONArray().apply {
        files.forEach { file ->
            put(JSONObject().apply {
                put("uri", file.uri)
                put("name", file.name)
                put("mimeType", file.mimeType)
                put("sizeBytes", file.sizeBytes)
                put("modifiedAt", file.modifiedAt)
            })
        }
    }.toString()
}
