package com.mytask.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(
    name = "mytask_settings"
)

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {

        private val TASK_REMINDER_DAYS =
            intPreferencesKey(
                "task_reminder_days"
            )

        private val ACTIVE_TASK_NOTIFICATION =
            booleanPreferencesKey(
                "active_task_notification"
            )
    }

    val taskReminderDays: Flow<Int> =
        context.settingsDataStore.data.map { preferences ->

            preferences[
                TASK_REMINDER_DAYS
            ] ?: 1
        }

    val activeTaskNotification: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences ->

            preferences[
                ACTIVE_TASK_NOTIFICATION
            ] ?: true
        }

    suspend fun setTaskReminderDays(
        days: Int
    ) {

        context.settingsDataStore.edit { preferences ->

            preferences[
                TASK_REMINDER_DAYS
            ] =
                days.coerceIn(
                    0,
                    30
                )
        }
    }

    suspend fun setActiveTaskNotification(
        enabled: Boolean
    ) {

        context.settingsDataStore.edit { preferences ->

            preferences[
                ACTIVE_TASK_NOTIFICATION
            ] =
                enabled
        }
    }
}