package com.example.courseapp.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.example.courseapp.data.model.NoteCategory
import com.example.courseapp.data.model.NoteEditDocumentEntity
import com.example.courseapp.data.model.NotePhoto
import kotlinx.coroutines.flow.Flow

/**
 * 随手记数据访问。所有文件操作都在 [com.example.courseapp.data.repository.NoteRepository]，
 * UI 不直接碰 DAO 或文件系统。
 */
@Dao
interface NoteDao {

    @Query("SELECT * FROM note_categories ORDER BY sortOrder ASC, createdAt ASC")
    fun observeCategories(): Flow<List<NoteCategory>>

    @Query("SELECT * FROM note_photos ORDER BY capturedAt DESC, createdAt DESC")
    fun observePhotos(): Flow<List<NotePhoto>>

    @Query("SELECT * FROM note_categories WHERE id = :id")
    suspend fun category(id: String): NoteCategory?

    @Query("SELECT * FROM note_categories WHERE id = :id")
    fun observeCategory(id: String): Flow<NoteCategory?>

    @Query("SELECT COUNT(*) FROM note_categories")
    suspend fun categoryCount(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM note_categories WHERE isSystem = 0")
    suspend fun maxUserSortOrder(): Int

    @Query("SELECT * FROM note_photos WHERE id = :id")
    suspend fun photo(id: String): NotePhoto?

    @Query("SELECT * FROM note_photos WHERE categoryId = :categoryId ORDER BY capturedAt DESC, createdAt DESC")
    suspend fun photosIn(categoryId: String): List<NotePhoto>

    @Query("SELECT * FROM note_photos WHERE id IN (:ids)")
    suspend fun photosByIds(ids: List<String>): List<NotePhoto>

    @Query("SELECT * FROM note_edit_documents WHERE photoId = :photoId")
    suspend fun document(photoId: String): NoteEditDocumentEntity?

    @Upsert
    suspend fun saveCategory(category: NoteCategory)

    @Upsert
    suspend fun savePhoto(photo: NotePhoto)

    @Upsert
    suspend fun savePhotos(photos: List<NotePhoto>)

    @Upsert
    suspend fun saveDocument(document: NoteEditDocumentEntity)

    @Query(
        "UPDATE note_categories SET name = :name, iconKey = :iconKey, colorValue = :colorValue, " +
            "updatedAt = :now WHERE id = :id AND isSystem = 0"
    )
    suspend fun updateCategory(id: String, name: String, iconKey: String, colorValue: Int, now: Long): Int

    @Query("UPDATE note_categories SET updatedAt = :now WHERE id = :id")
    suspend fun touchCategory(id: String, now: Long)

    @Query("UPDATE note_photos SET categoryId = :categoryId, sortOrder = :sortOrder WHERE id IN (:ids)")
    suspend fun movePhotos(ids: List<String>, categoryId: String, sortOrder: Long = 0L)

    @Query("UPDATE note_photos SET title = :title, note = :note WHERE id = :id")
    suspend fun updatePhotoMeta(id: String, title: String, note: String)

    @Query(
        "UPDATE note_photos SET editedRelativePath = :editedRelativePath, " +
            "previewRelativePath = :previewRelativePath, width = :width, height = :height WHERE id = :id"
    )
    suspend fun updateEditedPhoto(
        id: String,
        editedRelativePath: String?,
        previewRelativePath: String?,
        width: Int,
        height: Int
    )

    @Query("DELETE FROM note_photos WHERE id = :id")
    suspend fun deletePhotoRow(id: String)

    @Query("DELETE FROM note_edit_documents WHERE photoId = :photoId")
    suspend fun deleteDocument(photoId: String)

    @Query("DELETE FROM note_categories WHERE id = :id AND isSystem = 0")
    suspend fun deleteCategoryRow(id: String)

    @Transaction
    suspend fun deletePhotoData(id: String) {
        deleteDocument(id)
        deletePhotoRow(id)
    }

    /** 删除分类前把照片迁到未归类；只改归属，不重复复制原图。 */
    @Transaction
    suspend fun migratePhotosAndDeleteCategory(sourceId: String, targetId: String, now: Long) {
        val photos = photosIn(sourceId)
        if (photos.isNotEmpty()) {
            movePhotos(photos.map { it.id }, targetId, 0L)
            touchCategory(targetId, now)
        }
        deleteCategoryRow(sourceId)
    }

    /** 删除分类并删除其中所有照片记录；文件由 Repository 在事务成功后清理。 */
    @Transaction
    suspend fun deleteCategoryWithPhotos(id: String) {
        photosIn(id).forEach { deletePhotoData(it.id) }
        deleteCategoryRow(id)
    }
}
