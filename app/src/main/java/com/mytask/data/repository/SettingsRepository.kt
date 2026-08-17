package com.mytask.data.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
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
        private val TASK_REMINDER_DAYS = intPreferencesKey("task_reminder_days")
        private val ACTIVE_TASK_NOTIFICATION = booleanPreferencesKey("active_task_notification")
        private val DISABLED_SCHEDULE_NOTIFICATIONS = stringSetPreferencesKey("disabled_schedule_notifications")
    }

    val taskReminderDays: Flow<Int> =
        context.settingsDataStore.data.map { preferences -> preferences[TASK_REMINDER_DAYS] ?: 1 }

    val activeTaskNotification: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences -> preferences[ACTIVE_TASK_NOTIFICATION] ?: true }

    fun scheduleNotificationEnabled(scheduleId: Long): Flow<Boolean> =
        context.settingsDataStore.data.map { preferences ->
            scheduleId.toString() !in (preferences[DISABLED_SCHEDULE_NOTIFICATIONS] ?: emptySet())
        }

    fun disabledScheduleNotificationIds(): Flow<Set<Long>> =
        context.settingsDataStore.data.map { preferences ->
            (preferences[DISABLED_SCHEDULE_NOTIFICATIONS] ?: emptySet())
                .mapNotNull { it.toLongOrNull() }
                .toSet()
        }

    suspend fun setTaskReminderDays(days: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[TASK_REMINDER_DAYS] = days.coerceIn(0, 30)
        }
    }

    suspend fun setActiveTaskNotification(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[ACTIVE_TASK_NOTIFICATION] = enabled
        }
    }

    suspend fun setScheduleNotificationEnabled(scheduleId: Long, enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            val disabled = (preferences[DISABLED_SCHEDULE_NOTIFICATIONS] ?: emptySet()).toMutableSet()
            if (enabled) disabled.remove(scheduleId.toString())
            else disabled.add(scheduleId.toString())

            if (disabled.isEmpty()) preferences.remove(DISABLED_SCHEDULE_NOTIFICATIONS)
            else preferences[DISABLED_SCHEDULE_NOTIFICATIONS] = disabled
        }
    }
}