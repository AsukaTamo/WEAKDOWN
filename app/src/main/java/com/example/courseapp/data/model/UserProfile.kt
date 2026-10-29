package com.example.courseapp.data.model

/**
 * 「我的」页展示的个人信息。
 *
 * 当前阶段不做真实账号体系：用户直接编辑并保存在本地，[name] 为空即视为未登录，
 * 页面会显示登录/完善信息入口；学校默认中国地质大学（武汉），决定背景水印。
 */
data class UserProfile(
    val name: String = "",
    val schoolId: String = Schools.Default.id,
    val major: String = "",
    val grade: String = "",
    val avatarUri: String = ""
) {
    val school: School get() = Schools.byId(schoolId)

    val isSet: Boolean get() = name.isNotBlank()

    val gradeLabel: String
        get() = when {
            grade.isBlank() -> ""
            grade.endsWith("级") -> grade
            else -> "${grade}级"
        }

    /** "计算机科学与技术 · 2023级"，缺项时自动省略分隔点。 */
    val detailLine: String
        get() = listOf(major.trim(), gradeLabel).filter { it.isNotEmpty() }.joinToString(" · ")

    val avatarText: String
        get() = name.trim().firstOrNull()?.toString() ?: "登"
}
