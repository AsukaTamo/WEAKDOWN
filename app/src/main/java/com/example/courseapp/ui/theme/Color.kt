package com.example.courseapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── 语义色 ──
// 只保留给"深色底上一小块状态色"用的常量（Snackbar、FAB 关闭态）。
// 页面内的正文/强调色一律走 AppColors 的语义 token。
val Success = Color(0xFF34D399)
val Error = Color(0xFFEF4444)

// ── 课程卡配色：按课程类型，深浅各一套 ──
// 浅色跟随「鸢尾蓝」的冷色系，深色跟随「火山橙」的暖色系；
// 渐变终点刻意压深，保证白色文字在整块卡片上都过 4.5:1。
// 自定义颜色（course.customColor）不经过这里，用户选什么就是什么。
val CourseRequiredStart = Color(0xFF0F64B5)
val CourseRequiredEnd = Color(0xFF1A72C6)
val CourseElectiveStart = Color(0xFF4B3FA0)
val CourseElectiveEnd = Color(0xFF5A4EB4)
val CourseLabStart = Color(0xFF0E6B72)
val CourseLabEnd = Color(0xFF17808A)
val CourseCustomStart = Color(0xFF9C5C1A)
val CourseCustomEnd = Color(0xFFA2601C)

val CourseRequiredStartDark = Color(0xFFB8432F)
val CourseRequiredEndDark = Color(0xFFC24E37)
val CourseElectiveStartDark = Color(0xFF8E3A62)
val CourseElectiveEndDark = Color(0xFF9E4570)
val CourseLabStartDark = Color(0xFF6B6130)
val CourseLabEndDark = Color(0xFF7A6E38)
val CourseCustomStartDark = Color(0xFF8A4A2E)
val CourseCustomEndDark = Color(0xFF995638)
