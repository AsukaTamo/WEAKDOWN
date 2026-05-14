package com.example.courseapp.data.db

import androidx.room.TypeConverter
import com.example.courseapp.data.model.CourseType

class Converters {
    @TypeConverter
    fun fromCourseType(value: CourseType): String = value.name

    @TypeConverter
    fun toCourseType(value: String): CourseType = CourseType.valueOf(value)
}
