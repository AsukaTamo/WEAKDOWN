package com.example.courseapp.data.repository

import android.net.Uri
import com.example.courseapp.data.db.NoteDao
import com.example.courseapp.data.model.NoteCategory
import com.example.courseapp.data.model.NoteCategoryDraft
import com.example.courseapp.data.model.NoteDraft
import com.example.courseapp.data.model.NoteEditorDocument
import com.example.courseapp.data.model.NotePhoto
import com.example.courseapp.data.model.UNCATEGORIZED_CATEGORY_ID
import com.example.courseapp.data.model.toEntity
import com.example.courseapp.data.model.uncategorizedCategory
import com.example.courseapp.data.storage.NoteImageStore
import com.example.courseapp.data.storage.RenderedImage
import com.example.courseapp.data.storage.UnsupportedImageException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** 首页一张分类文件夹卡片所需的全部数据。 */
data class NoteFolder(
    val category: NoteCategory,
    val photos: List<NotePhoto>,
    val latestAt: Long
) {
    val count: Int get() = photos.size
    val covers: List<String> get() = photos.take(3).map { it.displayRelativePath }
}

@Singleton
class NoteRepository @Inject constructor(
    private val dao: NoteDao,
    private val store: NoteImageStore
) {

    val categories: Flow<List<NoteCategory>> = dao.observeCategories()

    val folders: Flow<List<NoteFolder>> = combine(
        dao.observeCategories(),
        dao.observePhotos()
    ) { categories, photos ->
        val byCategory = photos.groupBy { it.categoryId }
        categories.map { category ->
            val list = byCategory[category.id].orEmpty()
            NoteFolder(
                category = category,
                photos = list,
                latestAt = list.maxOfOrNull { it.capturedAt } ?: category.updatedAt
            )
        }
    }

    val allPhotos: Flow<List<NotePhoto>> = dao.observePhotos()

    suspend fun ensureUncategorized() {
        if (dao.category(UNCATEGORIZED_CATEGORY_ID) == null) {
            dao.saveCategory(uncategorizedCategory())
        }
    }

    suspend fun category(id: String): NoteCategory? = dao.category(id)

    suspend fun createCategory(draft: NoteCategoryDraft): NoteCategory {
        val clean = draft.normalized()
        require(clean.isValid) { "请输入分类名称" }
        val now = System.currentTimeMillis()
        val category = NoteCategory(
            name = clean.name,
            iconKey = clean.iconKey,
            colorValue = clean.colorValue,
            createdAt = now,
            updatedAt = now,
            sortOrder = dao.maxUserSortOrder() + 10
        )
        dao.saveCategory(category)
        return category
    }

    suspend fun updateCategory(id: String, draft: NoteCategoryDraft) {
        val clean = draft.normalized()
        require(clean.isValid) { "请输入分类名称" }
        val changed = dao.updateCategory(
            id = id,
            name = clean.name,
            iconKey = clean.iconKey,
            colorValue = clean.colorValue,
            now = System.currentTimeMillis()
        )
        if (changed == 0) throw IllegalStateException("系统分类不能修改")
    }

    /**
     * 删除分类。
     * @param removePhotos true = 连照片一起删除；false = 照片迁移到「未归类」。
     */
    suspend fun deleteCategory(id: String, removePhotos: Boolean) =
        withContext(Dispatchers.IO) {
            val category = dao.category(id) ?: return@withContext
            require(!category.isSystem) { "「未归类」不能删除" }
            ensureUncategorized()
            val photos = dao.photosIn(id)
            if (removePhotos) {
                dao.deleteCategoryWithPhotos(id)
                photos.forEach { store.deletePhotoFiles(it) }
            } else {
                dao.migratePhotosAndDeleteCategory(id, UNCATEGORIZED_CATEGORY_ID, System.currentTimeMillis())
            }
        }

    suspend fun movePhotos(photoIds: List<String>, targetCategoryId: String) =
        withContext(Dispatchers.IO) {
            if (photoIds.isEmpty()) return@withContext
            if (targetCategoryId == UNCATEGORIZED_CATEGORY_ID) {
                ensureUncategorized()
            } else if (dao.category(targetCategoryId) == null) {
                throw IllegalStateException("目标分类不存在")
            }
            val sources = dao.photosByIds(photoIds).map { it.categoryId }.distinct()
            dao.movePhotos(photoIds, targetCategoryId, 0L)
            val now = System.currentTimeMillis()
            dao.touchCategory(targetCategoryId, now)
            sources.filter { it != targetCategoryId }.forEach { dao.touchCategory(it, now) }
        }

    suspend fun deletePhoto(photoId: String) = withContext(Dispatchers.IO) {
        val photo = dao.photo(photoId) ?: return@withContext
        dao.deletePhotoData(photoId)
        store.deletePhotoFiles(photo)
        dao.touchCategory(photo.categoryId, System.currentTimeMillis())
    }

    suspend fun updatePhotoMeta(photoId: String, title: String, note: String) {
        val cleanTitle = title.trim().ifBlank { "课堂照片" }.take(40)
        dao.updatePhotoMeta(photoId, cleanTitle, note.trim())
    }

    suspend fun document(photoId: String): NoteEditorDocument =
        dao.document(photoId)?.toDomain() ?: NoteEditorDocument()

    /** CameraX 写好的临时文件 → 草稿（读取尺寸与 EXIF 方向，不动原字节）。 */
    suspend fun draftFromCamera(file: File): NoteDraft =
        store.draftFromCapturedFile(file, System.currentTimeMillis())

    /** Photo Picker 返回的 URI → 私有临时文件草稿。 */
    suspend fun importUri(uri: Uri): NoteDraft =
        store.importFromUri(uri, UUID.randomUUID().toString())

    suspend fun renderDraft(draft: NoteDraft, document: NoteEditorDocument): File? =
        withContext(Dispatchers.IO) {
            if (document.isIdentity) return@withContext null
            val source = File(draft.sourcePath)
            if (!source.exists()) throw UnsupportedImageException("照片文件已丢失，请重新拍摄")
            val target = File(store.tempDir(), "edited_${draft.id}_${System.currentTimeMillis()}.jpg")
            store.renderDocument(source, document, target)
            target
        }

    /**
     * 归档一张新照片：移动文件 → 生成缩略图 → 写数据库。
     * 任意一步失败都会清理刚写出的文件，不留下孤儿记录。
     */
    suspend fun saveDraft(
        draft: NoteDraft,
        categoryId: String,
        document: NoteEditorDocument
    ): NotePhoto = withContext(Dispatchers.IO) {
        ensureUncategorized()
        if (categoryId != UNCATEGORIZED_CATEGORY_ID && dao.category(categoryId) == null) {
            throw IllegalStateException("目标分类不存在")
        }
        val photoId = UUID.randomUUID().toString()
        var originalRelative: String? = null
        var editedRelative: String? = null
        var previewRelative: String? = null
        try {
            originalRelative = store.adoptIntoOriginals(draft, photoId)
            var width = draft.width
            var height = draft.height
            val editedTemp = draft.editedPath?.let(::File)
            if (editedTemp != null && editedTemp.exists()) {
                val target = File(store.exportsDir(), "${photoId}_edited.jpg")
                if (!editedTemp.renameTo(target)) {
                    editedTemp.copyTo(target, overwrite = true)
                    editedTemp.delete()
                }
                editedRelative = store.relativeOf(target)
                previewRelative = store.writePreview(target, 0, photoId)
                val info = store.readInfo(target)
                width = info.uprightWidth
                height = info.uprightHeight
            } else {
                val originalFile = store.resolve(originalRelative)
                    ?: throw UnsupportedImageException("原图保存失败")
                previewRelative = store.writePreview(originalFile, draft.rotation, photoId)
            }
            val now = System.currentTimeMillis()
            val photo = NotePhoto(
                id = photoId,
                categoryId = categoryId,
                originalFileName = File(originalRelative).name,
                originalRelativePath = originalRelative,
                editedRelativePath = editedRelative,
                previewRelativePath = previewRelative,
                title = draft.title,
                note = "",
                capturedAt = draft.capturedAt,
                createdAt = now,
                width = width,
                height = height,
                mimeType = draft.mimeType,
                originalRotation = draft.rotation
            )
            if (!document.isIdentity) {
                dao.saveDocument(document.copy(updatedAt = now).toEntity(photoId))
            }
            dao.savePhoto(photo)
            dao.touchCategory(categoryId, now)
            photo
        } catch (error: Exception) {
            runCatching { dao.deletePhotoData(photoId) }
            // 保存失败不能吞掉用户刚拍/刚导入的内容：
            // 把已经移入 originals / exports 的文件尽量还原成草稿，让用户可以直接重试。
            runCatching {
                val originalTarget = originalRelative?.let(store::resolve)
                val draftSource = File(draft.sourcePath)
                if (originalTarget != null && originalTarget.exists()) {
                    if (draftSource.exists()) draftSource.delete()
                    if (!originalTarget.renameTo(draftSource)) {
                        originalTarget.copyTo(draftSource, overwrite = true)
                        originalTarget.delete()
                    }
                }
            }
            runCatching {
                val editedTarget = editedRelative?.let(store::resolve)
                val editedDraft = draft.editedPath?.let(::File)
                if (editedTarget != null && editedDraft != null && editedTarget.exists()) {
                    if (editedDraft.exists()) editedDraft.delete()
                    if (!editedTarget.renameTo(editedDraft)) {
                        editedTarget.copyTo(editedDraft, overwrite = true)
                        editedTarget.delete()
                    }
                }
            }
            previewRelative?.let { store.resolve(it)?.delete() }
            throw error
        }
    }

    /**
     * 重新编辑已有照片：重写 exports 合成图与缩略图，更新编辑文档。
     * 原图不参与任何写操作。document.isIdentity 时清空编辑结果、回到原图。
     */
    suspend fun savePhotoEdit(photoId: String, document: NoteEditorDocument): NotePhoto =
        withContext(Dispatchers.IO) {
            val photo = dao.photo(photoId) ?: throw IllegalStateException("照片不存在")
            val original = store.resolve(photo.originalRelativePath)
                ?: throw UnsupportedImageException("原图文件已丢失")
            val now = System.currentTimeMillis()
            if (document.isIdentity) {
                store.resolve(photo.editedRelativePath)?.delete()
                dao.deleteDocument(photoId)
                val preview = store.writePreview(original, photo.originalRotation, photoId)
                dao.updateEditedPhoto(
                    id = photoId,
                    editedRelativePath = null,
                    previewRelativePath = preview,
                    width = photo.width,
                    height = photo.height
                )
                dao.touchCategory(photo.categoryId, now)
                return@withContext dao.photo(photoId) ?: photo
            }
            val target = File(store.exportsDir(), "${photoId}_edited.jpg")
            val rendered = store.renderDocument(original, document, target)
            val preview = store.writePreview(target, 0, photoId)
            dao.saveDocument(document.copy(updatedAt = now).toEntity(photoId))
            dao.updateEditedPhoto(
                id = photoId,
                editedRelativePath = store.relativeOf(target),
                previewRelativePath = preview,
                width = rendered.width,
                height = rendered.height
            )
            dao.touchCategory(photo.categoryId, now)
            dao.photo(photoId) ?: photo
        }

    suspend fun exportPhoto(photo: NotePhoto): Uri? = withContext(Dispatchers.IO) {
        val file = store.resolve(photo.displayRelativePath)
            ?: throw UnsupportedImageException("图片文件已丢失，无法导出")
        if (!file.exists()) throw UnsupportedImageException("图片文件已丢失，无法导出")
        store.exportToMediaStore(
            file = file,
            displayName = NoteImageStore.safeExportName(photo.title),
            mimeType = if (photo.isEdited) "image/jpeg" else photo.mimeType
        )
    }

    fun shareUri(photo: NotePhoto): Uri? = store.shareUri(photo)

    suspend fun cleanupTemp() {
        store.cleanupStaleTemp()
    }

    suspend fun deleteDraftFiles(draft: NoteDraft) = withContext(Dispatchers.IO) {
        File(draft.sourcePath).takeIf { it.parentFile == store.tempDir() }?.delete()
        draft.editedPath?.let { path ->
            File(path).takeIf { it.parentFile == store.tempDir() }?.delete()
        }
    }
}
