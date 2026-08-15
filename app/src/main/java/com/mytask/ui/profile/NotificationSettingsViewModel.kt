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