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

    companion object {
        private val PROMPT_SHOWN_KEY =
            booleanPreferencesKey("academic_template_prompt_shown")
    }

    val promptShown: Flow<Boolean> =
        context.templatePreferencesDataStore.data.map { preferences ->
            preferences[PROMPT_SHOWN_KEY] ?: false
        }

    suspend fun markPromptShown() {
        context.templatePreferencesDataStore.edit { preferences ->
            preferences[PROMPT_SHOWN_KEY] = true
        }
    }
}
