package com.example.courseapp.network

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {
    @GET
    suspend fun fetchPage(@Url url: String): ResponseBody

    @GET("schedule")
    suspend fun getSchedule(@Query("semester") semester: String): ResponseBody
}
