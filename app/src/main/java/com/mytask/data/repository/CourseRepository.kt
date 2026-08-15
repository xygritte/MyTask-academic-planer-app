package com.mytask.data.repository

import com.mytask.data.local.dao.CourseDao
import com.mytask.data.local.entity.CourseEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val courseDao: CourseDao
) {

    fun getAllCourses(): Flow<List<CourseEntity>> {
        return courseDao.getAllCourses()
    }

    fun getCourseCount(): Flow<Int> {
        return courseDao.getCourseCount()
    }

    fun getCourseById(id: Long): Flow<CourseEntity?> {
        return courseDao.getCourseById(id)
    }

    suspend fun addCourse(course: CourseEntity) {
        courseDao.insert(course)
    }

    suspend fun updateCourse(course: CourseEntity) {
        courseDao.update(course)
    }

    suspend fun deleteCourse(course: CourseEntity) {
        courseDao.delete(course)
    }
}