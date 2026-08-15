package com.mytask.ui.course

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.repository.CourseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class CourseViewModel @Inject constructor(
    private val repository: CourseRepository
) : ViewModel() {

    val courses: StateFlow<List<CourseEntity>> =
        repository.getAllCourses()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    fun getCourseById(id: Long): StateFlow<CourseEntity?> =
        repository.getCourseById(id)
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                null
            )

    fun addCourse(
        name: String,
        code: String,
        lecturer: String,
        room: String,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {

            repository.addCourse(
                CourseEntity(
                    name = name,
                    code = code,
                    lecturer = lecturer,
                    room = room
                )
            )

            onSaved()
        }
    }

    fun updateCourse(
        course: CourseEntity,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {

            repository.updateCourse(course)

            onSaved()
        }
    }

    fun deleteCourse(
        course: CourseEntity
    ) {
        viewModelScope.launch {
            repository.deleteCourse(course)
        }
    }
}