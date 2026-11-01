package com.example.courseapp.data.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SemesterTest {

    @Test
    fun `legacy id generated from semester name is detected`() {
        // 旧版把名称里的非数字字符去掉，同学年的两个学期都会得到这个值。
        assertTrue(Semester.isLegacyGeneratedId("2026-2027"))
        assertTrue(Semester.isLegacyGeneratedId("2025-2026"))
    }

    @Test
    fun `current and unrelated ids are not treated as legacy`() {
        assertFalse(Semester.isLegacyGeneratedId("2025-2026-2"))
        assertFalse(Semester.isLegacyGeneratedId("3f2a1c4e-8b7d-4c1a-9e2f-5d6b7c8a9f01"))
        // 旧版用时间戳兜底生成的主键不会互相覆盖，不应该被误报成"已被合并"。
        assertFalse(Semester.isLegacyGeneratedId("1761999999999"))
        assertFalse(Semester.isLegacyGeneratedId("2026-2027学年 第一学期"))
        assertFalse(Semester.isLegacyGeneratedId(""))
    }
}
