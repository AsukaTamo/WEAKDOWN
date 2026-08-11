package com.example.courseapp.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Test

class ImportOverwritePolicyTest {
    @Test
    fun emptyCurrentSemesterImportsImmediately() {
        val action = ImportOverwritePolicy.actionFor(
            currentSemesterCourseCount = 0,
            selectedCourseCount = 3
        )

        assertEquals(ImportOverwriteAction.ImportNow, action)
    }

    @Test
    fun nonEmptyCurrentSemesterRequiresOverwriteConfirmation() {
        val action = ImportOverwritePolicy.actionFor(
            currentSemesterCourseCount = 2,
            selectedCourseCount = 3
        )

        assertEquals(ImportOverwriteAction.ConfirmOverwrite, action)
    }

    @Test
    fun noSelectedCoursesDoesNothing() {
        val action = ImportOverwritePolicy.actionFor(
            currentSemesterCourseCount = 2,
            selectedCourseCount = 0
        )

        assertEquals(ImportOverwriteAction.Ignore, action)
    }
}
