package com.mytask.di

import android.content.Context
import androidx.room.Room
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
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MyTaskDatabase =
        Room.databaseBuilder(context, MyTaskDatabase::class.java, "mytask_db").build()

    @Provides fun provideTaskDao(db: MyTaskDatabase) = db.taskDao()
    @Provides fun provideCourseDao(db: MyTaskDatabase) = db.courseDao()
    @Provides fun provideScheduleDao(db: MyTaskDatabase) = db.scheduleDao()
}