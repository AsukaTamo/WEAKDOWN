package com.example.courseapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── 语义色 ──
// Material 配色方案（Theme.kt）、Snackbar、课程类型兜底色共用这几个。
val Accent = Color(0xFFFF9800)
val Success = Color(0xFF4CAF50)
val Error = Color(0xFFF44336)

// 少数地方仍需要"深色卡片底色"和"深色文字"两个常量
val CardDark = Color(0xFF1A1D26)
val TextPrimary = Color(0xFF1A1D26)

// ── 课程卡片渐变（按课程名匹配，见 CourseCard.courseGradientColors） ──
// 操作系统: 浅蓝
val CourseOsStart = Color(0xFF5BA3E6)
val CourseOsEnd = Color(0xFF81C4F8)
// 算法设计: 深蓝
val CourseAlgoStart = Color(0xFF3A6FC4)
val CourseAlgoEnd = Color(0xFF5B93E0)
// 数据库: 紫色
val CourseDbStart = Color(0xFF7E57C2)
val CourseDbEnd = Color(0xFF9E80D8)
// 人工智能: 深紫
val CourseAiStart = Color(0xFF5C3D9E)
val CourseAiEnd = Color(0xFF7E62C0)
// 网络/实验: 青绿
val CourseNetStart = Color(0xFF26A69A)
val CourseNetEnd = Color(0xFF4DB6AC)
// 职业规划: 橙色
val CourseCareerStart = Color(0xFFE8883A)
val CourseCareerEnd = Color(0xFFF0A862)
// 软件工程: 灰蓝
val CourseSeStart = Color(0xFF5C7A99)
val CourseSeEnd = Color(0xFF7E9AB8)
// 编译原理: 靛蓝
val CourseCompilerStart = Color(0xFF3F51B5)
val CourseCompilerEnd = Color(0xFF6576C8)

// ── 课程类型兜底色（课程名没命中上面的关键词时使用） ──
val CourseRequired = Color(0xFF5BA3E6)
val CourseElective = Color(0xFF7E57C2)
val CourseLab = Color(0xFF26A69A)
val CourseCustom = Color(0xFFE8883A)
val CourseRequiredDark = Color(0xFF3A6FC4)
val CourseElectiveDark = Color(0xFF5C3D9E)
val CourseLabDark = Color(0xFF1E8A80)
val CourseCustomDark = Color(0xFFD07030)
