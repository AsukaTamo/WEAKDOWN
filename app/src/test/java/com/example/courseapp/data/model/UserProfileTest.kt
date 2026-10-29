package com.example.courseapp.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileTest {

    @Test
    fun `grade label appends suffix once`() {
        assertEquals("", UserProfile().gradeLabel)
        assertEquals("2023级", UserProfile(grade = "2023").gradeLabel)
        assertEquals("2023级", UserProfile(grade = "2023级").gradeLabel)
    }

    @Test
    fun `detail line drops missing parts and separator`() {
        assertEquals(
            "计算机科学与技术 · 2023级",
            UserProfile(major = "计算机科学与技术", grade = "2023").detailLine
        )
        assertEquals("2023级", UserProfile(grade = "2023").detailLine)
        assertEquals("计算机科学与技术", UserProfile(major = "计算机科学与技术").detailLine)
        assertEquals("", UserProfile().detailLine)
    }

    @Test
    fun `avatar falls back to login glyph when name is empty`() {
        assertEquals("张", UserProfile(name = "张同学").avatarText)
        assertEquals("登", UserProfile(name = "  ").avatarText)
        assertTrue(UserProfile(name = "张同学").isSet)
        assertFalse(UserProfile(name = "").isSet)
    }

    @Test
    fun `unknown school falls back to the default emblem school`() {
        assertEquals(Schools.Default, Schools.byId("not-a-school"))
        assertEquals("武大", Schools.byId("whu").shortName)
        assertEquals(Schools.Default.id, UserProfile().schoolId)
    }
}
