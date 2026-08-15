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

        /**
         * Berapa hari sebelum deadline
         * notifikasi permanen mulai muncul.
         *
         * Contoh:
         * 0 = hari deadline
         * 1 = 1 hari sebelum deadline
         * 3 = 3 hari sebelum deadline
         */
        private val TASK_REMINDER_DAYS =
            intPreferencesKey(
                "task_reminder_days"
            )

        /**
         * Menentukan apakah notifikasi
         * "Tugas Aktif" diaktifkan.
         */
        private val ACTIVE_TASK_NOTIFICATION =
            booleanPreferencesKey(
                "active_task_notification"
            )
    }

    /**
     * Nilai default:
     * 1 hari sebelum deadline.
     */
    val taskReminderDays: Flow<Int> =
        context.settingsDataStore.data.map { preferences ->

            preferences[
                TASK_REMINDER_DAYS
            ] ?: 1
        }

    /**
     * Nilai default:
     * true
     */
    val activeTaskNotification: Flow<Boolean> =
        context.settingsDataStore.data.map { preferences ->

            preferences[
                ACTIVE_TASK_NOTIFICATION
            ] ?: true
        }

    /**
     * Mengatur berapa hari sebelum deadline
     * notifikasi permanen mulai muncul.
     *
     * Rentang:
     * 0 sampai 30 hari.
     */
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

    /**
     * Mengaktifkan / menonaktifkan
     * notifikasi "Tugas Aktif".
     */
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