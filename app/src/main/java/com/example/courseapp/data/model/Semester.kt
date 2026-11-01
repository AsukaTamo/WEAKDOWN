package com.example.courseapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "semesters")
data class Semester(
    @PrimaryKey
    val id: String, // e.g. "2025-2026-2"
    val name: String, // e.g. "2025-2026学年 第二学期"
    val startDate: String, // "2025-02-24"
    val totalWeeks: Int = 18,
    val isActive: Boolean = false,
    val backgroundUri: String = "", // 内部存储中的图片文件路径
    val scrimAlpha: Float = 0.4f // 遮罩层不透明度 0~1
) {
    companion object {
        /**
         * 旧版主键是「把学期名里的非数字字符去掉」，"2026-2027学年 第一学期" 和
         * "2026-2027学年 第二学期" 都会得到 "2026-2027"，插入时 REPLACE 互相覆盖。
         * 只认这种「学年区间」形状，避免把旧版兜底的时间戳主键也当成被覆盖的学期。
         */
        private val LEGACY_GENERATED_ID = Regex("^\\d{4}-\\d{4}$")

        fun isLegacyGeneratedId(id: String): Boolean = LEGACY_GENERATED_ID.matches(id)
    }
}
