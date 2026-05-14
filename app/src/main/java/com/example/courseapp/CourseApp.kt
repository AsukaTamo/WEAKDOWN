package com.example.courseapp

import android.app.Application
import android.util.Log
import android.webkit.WebView
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CourseApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // WebView data directory (prevents crash on multi-process apps)
        try {
            WebView.setDataDirectorySuffix("webview_data")
        } catch (_: Exception) {}

        // Log uncaught exceptions for debugging
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("CourseApp", "Uncaught exception on ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
