package com.mytask.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
        private val NAME_KEY = stringPreferencesKey("student_name")
        private val PROGRAM_KEY = stringPreferencesKey("student_program")
    }

    val profile: Flow<UserProfile?> =
        context.userProfileDataStore.data.map { preferences: Preferences ->
            val name = preferences[NAME_KEY]?.trim().orEmpty()
            val program = preferences[PROGRAM_KEY]?.trim().orEmpty()

            if (name.isBlank() || program.isBlank()) {
                null
            } else {
                UserProfile(
                    name = name,
                    program = program
                )
            }
        }

    suspend fun saveProfile(
        name: String,
        program: String
    ) {
        context.userProfileDataStore.edit { preferences ->
            preferences[NAME_KEY] = name.trim()
            preferences[PROGRAM_KEY] = program.trim()
        }
    }

    suspend fun clearProfile() {
        context.userProfileDataStore.edit { preferences ->
            preferences.remove(NAME_KEY)
            preferences.remove(PROGRAM_KEY)
        }
    }
}
