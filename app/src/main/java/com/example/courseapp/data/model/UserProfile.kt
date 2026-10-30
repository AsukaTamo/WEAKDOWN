package com.example.courseapp.data.model

/**
 * 「我的」页展示的个人信息。
 *
 * 当前阶段不做真实账号体系：用户直接编辑并保存在本地，[name] 为空即视为未登录，
 * 页面会显示登录/完善信息入口。
 *
 * 学校默认留空：[school] 为 null 时「我的」页不画任何校徽，避免默认顶着别的学校的校徽；
 * 用户在个人信息里选好学校后，背景水印才跟着变成对应校徽。
 */
data class UserProfile(
    val name: String = "",
    val schoolId: String = "",
    val major: String = "",
    val grade: String = "",
    val avatarUri: String = ""
) {
    val school: School? get() = Schools.byIdOrNull(schoolId)

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
