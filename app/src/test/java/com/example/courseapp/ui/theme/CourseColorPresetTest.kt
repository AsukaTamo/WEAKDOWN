package com.example.courseapp.ui.theme

import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.CourseType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseColorPresetTest {

    @Test
    fun `每种课程类型都有默认色卡`() {
        assertEquals(CourseColorPreset.ICE, presetForType(CourseType.REQUIRED))
        assertEquals(CourseColorPreset.AMBER, presetForType(CourseType.ELECTIVE))
        assertEquals(CourseColorPreset.CORAL, presetForType(CourseType.PUBLIC_ELECTIVE))
        assertEquals(CourseColorPreset.MINT, presetForType(CourseType.LAB))
        assertEquals(CourseColorPreset.SLATE, presetForType(CourseType.CUSTOM))
    }

    @Test
    fun `色卡 key 可往返存储`() {
        for (preset in CourseColorPreset.entries) {
            assertEquals(preset, parseStoredCourseColor(preset.storageKey))
        }
        assertEquals(CourseColorPreset.ICE, parseStoredCourseColor(" ICE "))
        assertNull(parseStoredCourseColor(""))
        assertNull(parseStoredCourseColor("not-a-preset"))
    }

    @Test
    fun `老版 hex 颜色按色相迁移到最接近的色卡`() {
        // 旧版 ColorPickerRow 的预设色 + 旧 CourseType.colorHex
        assertEquals(CourseColorPreset.ICE, parseStoredCourseColor("#2196F3"))
        assertEquals(CourseColorPreset.ICE, parseStoredCourseColor("#5BA3E6"))
        assertEquals(CourseColorPreset.ICE, parseStoredCourseColor("#3F51B5"))
        assertEquals(CourseColorPreset.TARO, parseStoredCourseColor("#9C27B0"))
        assertEquals(CourseColorPreset.TARO, parseStoredCourseColor("#7E57C2"))
        assertEquals(CourseColorPreset.MINT, parseStoredCourseColor("#4CAF50"))
        assertEquals(CourseColorPreset.MINT, parseStoredCourseColor("#26A69A"))
        assertEquals(CourseColorPreset.AMBER, parseStoredCourseColor("#FF9800"))
        assertEquals(CourseColorPreset.AMBER, parseStoredCourseColor("#E8883A"))
        assertEquals(CourseColorPreset.CORAL, parseStoredCourseColor("#EF5350"))
        // 低饱和度（灰）落到冷灰蓝
        assertEquals(CourseColorPreset.SLATE, parseStoredCourseColor("#9AA0A6"))
    }

    @Test
    fun `显式色卡优先于类型默认色`() {
        val labCourse = Course(
            name = "操作系统实验",
            dayOfWeek = 0,
            startSlot = 0,
            type = CourseType.LAB
        )
        assertNull(labCourse.explicitColorPreset())
        assertEquals(CourseColorPreset.MINT, labCourse.colorPreset())

        val coralLab = labCourse.copy(customColor = "coral")
        assertEquals(CourseColorPreset.CORAL, coralLab.explicitColorPreset())
        assertEquals(CourseColorPreset.CORAL, coralLab.colorPreset())
    }

    @Test
    fun `卡片圆角落在 10-12dp`() {
        val corner = ScheduleDimensions.CourseCorner
        assertTrue("课程卡圆角 $corner 不在 10-12dp 之间", corner.value in 10f..12f)
    }
}
