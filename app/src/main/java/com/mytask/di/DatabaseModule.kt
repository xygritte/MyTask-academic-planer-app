package com.mytask.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mytask.data.local.MyTaskDatabase
import com.mytask.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                "ALTER TABLE tasks ADD COLUMN completedAt INTEGER"
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS schedules_new (
                    id INTEGER NOT NULL PRIMARY KEY,
                    courseId INTEGER,
                    dayOfWeek INTEGER NOT NULL,
                    startMinutes INTEGER NOT NULL,
                    endMinutes INTEGER NOT NULL,
                    room TEXT NOT NULL DEFAULT ''
                )
                """.trimIndent()
            )

            database.execSQL(
                """
                INSERT INTO schedules_new(id, courseId, dayOfWeek, startMinutes, endMinutes, room)
                SELECT
                    id,
                    courseId,
                    dayOfWeek,
                    COALESCE(CAST(substr(startTime, 1, 2) AS INTEGER) * 60 + CAST(substr(startTime, 4, 2) AS INTEGER), 0),
                    COALESCE(CAST(substr(endTime, 1, 2) AS INTEGER) * 60 + CAST(substr(endTime, 4, 2) AS INTEGER), 0),
                    room
                FROM schedules
                """.trimIndent()
            )

            database.execSQL("DROP TABLE schedules")
            database.execSQL("ALTER TABLE schedules_new RENAME TO schedules")
        }
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MyTaskDatabase =
        Room.databaseBuilder(context, MyTaskDatabase::class.java, "mytask_db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()

    @Provides fun provideTaskDao(db: MyTaskDatabase) = db.taskDao()
    @Provides fun provideCourseDao(db: MyTaskDatabase) = db.courseDao()
    @Provides fun provideScheduleDao(db: MyTaskDatabase) = db.scheduleDao()
}
