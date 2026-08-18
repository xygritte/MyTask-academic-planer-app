package com.mytask.data.repository

import androidx.room.withTransaction
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.dao.CourseDao
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.SyncTombstoneEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CourseRepository @Inject constructor(
    private val courseDao: CourseDao,
    private val database: MyTaskDatabase
) {

    fun getAllCourses(): Flow<List<CourseEntity>> = courseDao.getAllCourses()

    fun getCourseCount(): Flow<Int> = courseDao.getCourseCount()

    fun getCourseById(id: Long): Flow<CourseEntity?> = courseDao.getCourseById(id)

    suspend fun addCourse(course: CourseEntity) {
        courseDao.insert(course.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun updateCourse(course: CourseEntity) {
        courseDao.update(course.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteCourse(course: CourseEntity) {
        database.withTransaction {
            courseDao.delete(course)
            database.syncTombstoneDao().upsert(
                SyncTombstoneEntity("course", course.id, System.currentTimeMillis())
            )
        }
    }
}
