package com.mytask.ui.profile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mytask.Notification.ReminderScheduler
import com.mytask.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationSettingsViewModel
@Inject constructor(
    application:
    Application,

    private val settingsRepository:
    SettingsRepository

) : AndroidViewModel(
    application
) {

    private val appContext =
        application.applicationContext


    /*
     * ==========================================
     * JUMLAH HARI PENGINGAT
     * ==========================================
     */

    val taskReminderDays =

        settingsRepository
            .taskReminderDays
            .stateIn(

                scope =
                    viewModelScope,

                started =
                    SharingStarted
                        .WhileSubscribed(
                            5_000
                        ),

                initialValue =
                    1
            )


    /*
     * ==========================================
     * TUGAS AKTIF
     * ==========================================
     */

    val activeTaskNotification =

        settingsRepository
            .activeTaskNotification
            .stateIn(

                scope =
                    viewModelScope,

                started =
                    SharingStarted
                        .WhileSubscribed(
                            5_000
                        ),

                initialValue =
                    true
            )


    /*
     * ==========================================
     * SET H-X
     * ==========================================
     */

    fun setTaskReminderDays(
        days: Int
    ) {

        viewModelScope.launch {

            settingsRepository
                .setTaskReminderDays(
                    days
                )

            /*
             * Langsung sinkronkan notifikasi.
             *
             * Tidak perlu menunggu tengah malam.
             */
            ReminderScheduler
                .syncToday(
                    appContext
                )
        }
    }


    /*
     * ==========================================
     * SET TUGAS AKTIF
     * ==========================================
     */

    fun setActiveTaskNotification(
        enabled: Boolean
    ) {

        viewModelScope.launch {

            settingsRepository
                .setActiveTaskNotification(
                    enabled
                )

            /*
             * Kalau OFF, worker akan langsung
             * membatalkan notifikasi Tugas Aktif.
             *
             * Kalau ON, worker akan langsung
             * membuatnya kembali jika ada tugas.
             */
            ReminderScheduler
                .syncToday(
                    appContext
                )
        }
    }
}