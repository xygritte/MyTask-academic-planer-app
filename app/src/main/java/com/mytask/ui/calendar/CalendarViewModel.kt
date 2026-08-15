package com.mytask.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytask.data.local.entity.TaskEntity
import com.mytask.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class CalendarViewModel @Inject constructor(
    repository: TaskRepository
) : ViewModel() {

    val tasks: StateFlow<List<TaskEntity>> =
        repository.getAllTasks()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    fun isSameDay(
        date1: Date?,
        date2: Date
    ): Boolean {

        if (date1 == null) {
            return false
        }

        val first = Calendar.getInstance().apply {
            time = date1
        }

        val second = Calendar.getInstance().apply {
            time = date2
        }

        return first.get(Calendar.YEAR) == second.get(Calendar.YEAR) &&
                first.get(Calendar.DAY_OF_YEAR) == second.get(Calendar.DAY_OF_YEAR)
    }
}