package com.example.courseapp.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * 一张课堂照片。
 *
 * 文件约定（相对 [com.example.courseapp.data.storage.NoteImageStore] 的 notebook 根目录）：
 * - [originalRelativePath] 原图，只读，不因编辑被覆盖；
 * - [editedRelativePath] 编辑合成图（唯一可被重新编辑覆盖的文件）；
 * - [previewRelativePath] 列表/卡片缩略图，可重建，不作为原始数据。
 *
 * [originalRotation] 是导入时从 EXIF 读到的方向（0/90/180/270），原图字节保持原样；
 * 显示与导出时再应用该旋转。字段名与语义见执行手册 5.1，并额外保留方向信息。
 */
@Entity(
    tableName = "note_photos",
    indices = [Index("categoryId"), Index("capturedAt")]
)
data class NotePhoto(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val categoryId: String = UNCATEGORIZED_CATEGORY_ID,
    val originalFileName: String,
    val originalRelativePath: String,
    val editedRelativePath: String? = null,
    val previewRelativePath: String? = null,
    val title: String = "课堂照片",
    val note: String = "",
    val capturedAt: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis(),
    val width: Int = 0,
    val height: Int = 0,
    val mimeType: String = "image/jpeg",
    val originalRotation: Int = 0,
    val sortOrder: Long = 0L
) {
    /** 详情页/编辑页优先展示合成图。 */
    val displayRelativePath: String get() = editedRelativePath ?: originalRelativePath
    val isEdited: Boolean get() = editedRelativePath != null
}

/** 拍照/导入之后、尚未选择分类的草稿。文件放在 notebook/tmp 下。 */
data class NoteDraft(
    val id: String,
    val sourcePath: String,
    val capturedAt: Long,
    val width: Int,
    val height: Int,
    val rotation: Int,
    val mimeType: String,
    val title: String = "课堂照片",
    /** 编辑完成后的临时合成图；未编辑则为空。 */
    val editedPath: String? = null
)
