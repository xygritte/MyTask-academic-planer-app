package com.mytask.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.mytask.debug.AuthDebugLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userProfileDataStore by preferencesDataStore(
    name = "mytask_user_profile"
)

data class UserProfile(
    val name: String,
    val program: String
)

@Singleton
class UserProfileRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private val UID_KEY =
            stringPreferencesKey("student_uid")

        private val NAME_KEY =
            stringPreferencesKey("student_name")

        private val PROGRAM_KEY =
            stringPreferencesKey("student_program")

        private val PROFILE_PHOTO_URI_KEY =
            stringPreferencesKey("profile_photo_uri")

        private val RESTORE_PENDING_KEY =
            booleanPreferencesKey("restore_cloud_data_pending")
    }

    val profile: Flow<UserProfile?> =
        context.userProfileDataStore.data.map { preferences: Preferences ->
            val name = preferences[NAME_KEY]
                ?.trim()
                .orEmpty()

            val program = preferences[PROGRAM_KEY]
                ?.trim()
                .orEmpty()

            if (name.isBlank() || program.isBlank()) {
                null
            } else {
                UserProfile(
                    name = name,
                    program = program
                )
            }
        }

    val uid: Flow<String?> =
        context.userProfileDataStore.data.map { preferences ->
            preferences[UID_KEY]
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }

    val profilePhotoUri: Flow<String?> =
        context.userProfileDataStore.data.map { preferences ->
            preferences[PROFILE_PHOTO_URI_KEY]
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }

    /**
     * Always emits the current persisted restore flag.
     *
     * This must NOT filter out false values because MainActivity reads the
     * value with first(). Filtering false would make first() wait forever on
     * a normal app reopen after a previous restore had already completed.
     *
     * Restore is still triggered only once for a new authenticated session:
     * MainActivity is keyed by Firebase UID, not by this flag.
     */
    val restorePending: Flow<Boolean> =
        context.userProfileDataStore.data
            .map { preferences -> preferences[RESTORE_PENDING_KEY] ?: false }

    suspend fun saveProfile(
        uid: String,
        name: String,
        program: String
    ) {
        context.userProfileDataStore.edit { preferences ->
            preferences[UID_KEY] = uid.trim()
            preferences[NAME_KEY] = name.trim()
            preferences[PROGRAM_KEY] = program.trim()
        }
        AuthDebugLog.d(
            "PROFILE_STORE save: uid=${AuthDebugLog.uid(uid)} namePresent=${name.isNotBlank()} programPresent=${program.isNotBlank()}"
        )
    }

    suspend fun saveAuthenticatedSession(
        uid: String,
        name: String,
        program: String
    ) {
        context.userProfileDataStore.edit { preferences ->
            preferences[UID_KEY] = uid.trim()
            preferences[NAME_KEY] = name.trim()
            preferences[PROGRAM_KEY] = program.trim()
            preferences[RESTORE_PENDING_KEY] = true
        }
        AuthDebugLog.d(
            "PROFILE_STORE authenticated session saved: uid=${AuthDebugLog.uid(uid)} restorePending=true"
        )
    }

    suspend fun saveGuestProfile(
        name: String,
        program: String
    ) {
        saveProfile(
            uid = "guest",
            name = name,
            program = program
        )
        AuthDebugLog.d("PROFILE_STORE guest profile saved")
    }

    suspend fun saveProfilePhotoUri(uri: String?) {
        context.userProfileDataStore.edit { preferences ->
            if (uri.isNullOrBlank()) {
                preferences.remove(PROFILE_PHOTO_URI_KEY)
            } else {
                preferences[PROFILE_PHOTO_URI_KEY] = uri
            }
        }
        AuthDebugLog.d("PROFILE_STORE profile photo ${if (uri.isNullOrBlank()) "cleared" else "saved"}")
    }

    suspend fun markCloudRestorePending() {
        context.userProfileDataStore.edit { preferences ->
            preferences[RESTORE_PENDING_KEY] = true
        }
        AuthDebugLog.d("PROFILE_STORE restorePending=true")
    }

    suspend fun clearCloudRestorePending() {
        context.userProfileDataStore.edit { preferences ->
            preferences[RESTORE_PENDING_KEY] = false
        }
        AuthDebugLog.d("PROFILE_STORE restorePending=false")
    }

    suspend fun clearProfile() {
        context.userProfileDataStore.edit { preferences ->
            preferences.remove(UID_KEY)
            preferences.remove(NAME_KEY)
            preferences.remove(PROGRAM_KEY)
            preferences.remove(PROFILE_PHOTO_URI_KEY)
            preferences.remove(RESTORE_PENDING_KEY)
        }
        AuthDebugLog.d("PROFILE_STORE clearProfile")
    }
}
