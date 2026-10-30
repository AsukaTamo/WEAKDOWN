package com.example.courseapp.ui.profile

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackContentTest {

    @Test
    fun `subject carries the chosen category`() {
        assertEquals("WEAKDOWN 反馈 · 功能建议", feedbackSubject(FeedbackKind.Suggestion))
        assertEquals("WEAKDOWN 反馈 · 问题反馈", feedbackSubject(FeedbackKind.Bug))
        assertEquals("WEAKDOWN 反馈 · 其他", feedbackSubject(FeedbackKind.Other))
    }

    @Test
    fun `body keeps content, contact and device info`() {
        val body = buildFeedbackBody(
            kind = FeedbackKind.Bug,
            content = "导入后少了两门课",
            contact = "someone@example.com",
            appVersion = "v2.4.2",
            deviceInfo = "vivo V2520A · Android 16 (API 36)"
        )

        assertTrue(body.contains("【问题反馈】"))
        assertTrue(body.contains("导入后少了两门课"))
        assertTrue(body.contains("联系方式：someone@example.com"))
        assertTrue(body.contains("版本：v2.4.2"))
        assertTrue(body.contains("设备：vivo V2520A · Android 16 (API 36)"))
    }

    @Test
    fun `body drops optional parts when they are absent`() {
        val body = buildFeedbackBody(
            kind = FeedbackKind.Other,
            content = "一点想法",
            contact = "",
            appVersion = "v2.4.2",
            deviceInfo = null
        )

        assertFalse(body.contains("联系方式"))
        assertFalse(body.contains("设备："))
    }

    @Test
    fun `feedback goes to the developer mailbox and repo link is the project page`() {
        assertEquals("lingfangod666@gmail.com", FEEDBACK_EMAIL)
        assertEquals("https://github.com/AsukaTamo/WEAKDOWN", PROJECT_REPO_URL)
    }
}
