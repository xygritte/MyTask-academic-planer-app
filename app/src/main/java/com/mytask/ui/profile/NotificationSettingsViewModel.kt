package com.mytask.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytask.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class NotificationSettingsViewModel
@Inject constructor(
    private val settingsRepository:
    SettingsRepository
) : ViewModel() {

    /**
     * Jumlah hari sebelum deadline
     * untuk mulai menampilkan
     * notifikasi permanen.
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

    /**
     * Status notifikasi "Tugas Aktif".
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

    /**
     * Mengubah jumlah hari pengingat.
     */
    fun setTaskReminderDays(
        days: Int
    ) {

        viewModelScope.launch {

            settingsRepository
                .setTaskReminderDays(
                    days
                )
        }
    }

    /**
     * Mengaktifkan / menonaktifkan
     * notifikasi tugas aktif.
     */
    fun setActiveTaskNotification(
        enabled: Boolean
    ) {

        viewModelScope.launch {

            settingsRepository
                .setActiveTaskNotification(
                    enabled
                )
        }
    }
}