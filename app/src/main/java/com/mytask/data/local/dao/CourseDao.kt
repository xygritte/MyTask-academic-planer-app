package com.mytask.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.mytask.data.local.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Query("SELECT * FROM courses ORDER BY name ASC")
    fun getAllCourses(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses ORDER BY name ASC")
    suspend fun getAllCoursesSnapshot(): List<CourseEntity>

    @Query("SELECT * FROM courses WHERE id = :id")
    fun getCourseById(id: Long): Flow<CourseEntity?>

    @Query("SELECT COUNT(*) FROM courses")
    fun getCourseCount(): Flow<Int>

    @Insert
    suspend fun insert(course: CourseEntity): Long

    @Insert
    suspend fun insertAll(courses: List<CourseEntity>)

    @Update
    suspend fun update(course: CourseEntity)

    @Delete
    suspend fun delete(course: CourseEntity)

    @Query("DELETE FROM courses")
    suspend fun deleteAll()
}