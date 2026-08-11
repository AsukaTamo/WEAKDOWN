package com.example.courseapp.viewmodel

enum class ImportOverwriteAction {
    ImportNow,
    ConfirmOverwrite,
    Ignore
}

object ImportOverwritePolicy {
    fun actionFor(
        currentSemesterCourseCount: Int,
        selectedCourseCount: Int
    ): ImportOverwriteAction {
        if (selectedCourseCount <= 0) return ImportOverwriteAction.Ignore
        return if (currentSemesterCourseCount > 0) {
            ImportOverwriteAction.ConfirmOverwrite
        } else {
            ImportOverwriteAction.ImportNow
        }
    }
}
