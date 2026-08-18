package com.mytask.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mytask.data.local.dao.CourseDao
import com.mytask.data.local.dao.ScheduleDao
import com.mytask.data.local.dao.SyncTombstoneDao
import com.mytask.data.local.dao.TaskDao
import com.mytask.data.local.entity.CourseEntity
import com.mytask.data.local.entity.ScheduleEntity
import com.mytask.data.local.entity.SyncTombstoneEntity
import com.mytask.data.local.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        CourseEntity::class,
        ScheduleEntity::class,
        SyncTombstoneEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MyTaskDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun courseDao(): CourseDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun syncTombstoneDao(): SyncTombstoneDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN completedAt INTEGER")
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

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    ALTER TABLE schedules
                    ADD COLUMN timeRangesJson TEXT NOT NULL DEFAULT ''
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE courses ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE tasks ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE schedules ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sync_tombstones (
                        entityType TEXT NOT NULL,
                        entityId INTEGER NOT NULL,
                        deletedAt INTEGER NOT NULL,
                        PRIMARY KEY(entityType, entityId)
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_sync_tombstones_deletedAt ON sync_tombstones(deletedAt)")
                val now = System.currentTimeMillis()
                database.execSQL("UPDATE courses SET updatedAt = ? WHERE updatedAt = 0", arrayOf(now))
                database.execSQL("UPDATE tasks SET updatedAt = ? WHERE updatedAt = 0", arrayOf(now))
                database.execSQL("UPDATE schedules SET updatedAt = ? WHERE updatedAt = 0", arrayOf(now))
            }
        }

        fun builder(context: Context): RoomDatabase.Builder<MyTaskDatabase> =
            Room.databaseBuilder(context, MyTaskDatabase::class.java, "mytask_db")
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
    }
}
