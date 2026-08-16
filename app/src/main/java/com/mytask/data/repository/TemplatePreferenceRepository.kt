package com.mytask.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.templatePreferencesDataStore by preferencesDataStore(
    name = "mytask_template_preferences"
)

class TemplatePreferenceRepository(
    private val context: Context
) {

    private fun promptKey(uid: String) =
        booleanPreferencesKey(
            "academic_template_prompt_shown_$uid"
        )

    fun promptShown(
        uid: String
    ): Flow<Boolean> =
        context.templatePreferencesDataStore.data.map { preferences ->
            preferences[promptKey(uid)] ?: false
        }

    suspend fun markPromptShown(
        uid: String
    ) {
        context.templatePreferencesDataStore.edit { preferences ->
            preferences[promptKey(uid)] = true
        }
    }
}
