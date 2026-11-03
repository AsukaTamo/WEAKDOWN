package com.example.courseapp.ui.notes

import com.example.courseapp.data.model.NotePhoto
import com.example.courseapp.data.repository.NoteFolder

/**
 * 首页筛选/搜索的纯逻辑，便于单元测试。
 * 分类名命中会保留整个文件夹；照片标题/备注命中只保留匹配照片的计数展示，
 * 搜索结果里再单独给出“匹配的照片”入口。
 */
fun filterNoteFolders(
    folders: List<NoteFolder>,
    query: String,
    categoryId: String? = null
): List<NoteFolder> {
    val keyword = query.trim()
    return folders.filter { folder ->
        val categoryMatches = categoryId == null || folder.category.id == categoryId
        if (!categoryMatches) return@filter false
        if (keyword.isEmpty()) return@filter true
        folder.category.name.contains(keyword, ignoreCase = true) ||
            folder.photos.any { it.matchesKeyword(keyword) }
    }
}

fun matchingNotePhotos(folders: List<NoteFolder>, query: String): List<NotePhoto> {
    val keyword = query.trim()
    if (keyword.isEmpty()) return emptyList()
    return folders.flatMap { it.photos }
        .filter { it.matchesKeyword(keyword) }
        .sortedByDescending { it.capturedAt }
}

fun filterNotePhotos(photos: List<NotePhoto>, query: String): List<NotePhoto> {
    val keyword = query.trim()
    if (keyword.isEmpty()) return photos
    return photos.filter { it.matchesKeyword(keyword) }
}

internal fun NotePhoto.matchesKeyword(keyword: String): Boolean =
    title.contains(keyword, ignoreCase = true) ||
        note.contains(keyword, ignoreCase = true)
