package com.example.courseapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

/** 系统「未归类」分类的主键；初始化时创建，用户不可删除。 */
const val UNCATEGORIZED_CATEGORY_ID = "system-uncategorized"

/** 分类图标可选值。UI 层负责映射到具体图标，数据库只保存稳定 key。 */
val NoteCategoryIconKeys = listOf("book", "code", "language", "math", "science", "star", "note", "palette")

/** 分类主题色可选值（ARGB）。 */
val NoteCategoryColors = listOf(
    0xFF2F68B5.toInt(),
    0xFF458A83.toInt(),
    0xFFB48A4C.toInt(),
    0xFF8D78B8.toInt(),
    0xFFD07875.toInt(),
    0xFF71889A.toInt()
)

@Entity(tableName = "note_categories")
data class NoteCategory(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val iconKey: String = NoteCategoryIconKeys.first(),
    val colorValue: Int = NoteCategoryColors.first(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0,
    val isSystem: Boolean = false
) {
    val isUncategorized: Boolean get() = id == UNCATEGORIZED_CATEGORY_ID
}

fun uncategorizedCategory(now: Long = System.currentTimeMillis()): NoteCategory = NoteCategory(
    id = UNCATEGORIZED_CATEGORY_ID,
    name = "未归类",
    iconKey = "inbox",
    colorValue = 0xFF8493A3.toInt(),
    createdAt = now,
    updatedAt = now,
    sortOrder = Int.MAX_VALUE,
    isSystem = true
)

/** 创建/编辑分类时的输入；集中做一次 trim 与长度约束。 */
data class NoteCategoryDraft(
    val name: String = "",
    val iconKey: String = NoteCategoryIconKeys.first(),
    val colorValue: Int = NoteCategoryColors.first()
) {
    fun normalized(): NoteCategoryDraft = copy(name = name.trim().take(18))
    val isValid: Boolean get() = name.trim().isNotEmpty()
}
