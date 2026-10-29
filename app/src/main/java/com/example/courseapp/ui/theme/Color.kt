package com.example.courseapp.ui.theme

import androidx.compose.ui.graphics.Color

// ── 语义色 ──
// 只保留给"深色底上一小块状态色"用的常量（Snackbar、FAB 关闭态）。
// 页面内的正文/强调色一律走 AppColors 的语义 token。
val Success = Color(0xFF34D399)
val Error = Color(0xFFEF4444)

// 课程卡配色见 CourseColors.kt：6 套液态玻璃色卡 + 课程类型默认色。
