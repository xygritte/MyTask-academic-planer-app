package com.mytask.data.repository

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.templatePreferencesDataStore by preferencesDataStore(
    name = "mytask_template_preferences"
)

@Singleton
class TemplatePreferenceRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private fun promptKey(uid: String) =
        booleanPreferencesKey("academic_template_prompt_v2_shown_$uid")

    private val appliedTemplatesKey = stringSetPreferencesKey("applied_template_versions")

    fun promptShown(uid: String): Flow<Boolean> =
        context.templatePreferencesDataStore.data.map { preferences: Preferences ->
            preferences[promptKey(uid)] ?: false
        }

    suspend fun markPromptShown(uid: String) {
        context.templatePreferencesDataStore.edit { preferences ->
            preferences[promptKey(uid)] = true
        }
    }

    fun appliedTemplateKeys(): Flow<Set<String>> =
        context.templatePreferencesDataStore.data.map { preferences ->
            preferences[appliedTemplatesKey] ?: emptySet()
        }

    suspend fun isTemplateApplied(templateId: String, version: Int): Boolean =
        appliedTemplateKeys().map { it.contains("$templateId@$version") }.first()

    suspend fun markTemplateApplied(templateId: String, version: Int) {
        val key = "$templateId@$version"
        context.templatePreferencesDataStore.edit { preferences ->
            val current = (preferences[appliedTemplatesKey] ?: emptySet()).toMutableSet()
            current.add(key)
            preferences[appliedTemplatesKey] = current
        }
    }
}
