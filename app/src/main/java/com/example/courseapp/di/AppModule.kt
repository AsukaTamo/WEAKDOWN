package com.example.courseapp.di

import android.content.Context
import androidx.room.Room
import com.example.courseapp.data.db.AppDatabase
import com.example.courseapp.data.db.TodoDao
import com.example.courseapp.data.db.CourseDao
import com.example.courseapp.data.db.SemesterDao
import com.example.courseapp.data.db.TimeSlotTemplateDao
import com.example.courseapp.data.db.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "course_db")
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8
            )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    @Singleton
    fun provideTodoDao(db: AppDatabase): TodoDao = db.todoDao()

    @Provides
    @Singleton
    fun provideCourseDao(db: AppDatabase): CourseDao = db.courseDao()

    @Provides
    @Singleton
    fun provideSemesterDao(db: AppDatabase): SemesterDao = db.semesterDao()

    @Provides
    @Singleton
    fun provideTimeSlotTemplateDao(db: AppDatabase): TimeSlotTemplateDao = db.timeSlotTemplateDao()

    @Provides
    @Singleton
    fun provideNoteDao(db: AppDatabase): NoteDao = db.noteDao()
}
