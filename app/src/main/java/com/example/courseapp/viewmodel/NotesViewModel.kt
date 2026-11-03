package com.example.courseapp.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.courseapp.data.model.NoteCategory
import com.example.courseapp.data.model.NoteCategoryDraft
import com.example.courseapp.data.model.NoteDraft
import com.example.courseapp.data.model.NoteEditorDocument
import com.example.courseapp.data.model.NotePhoto
import com.example.courseapp.data.repository.NoteFolder
import com.example.courseapp.data.repository.NoteRepository
import com.example.courseapp.data.storage.UnsupportedImageException
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class NoteMessage(val text: String, val isError: Boolean = false)

/**
 * 随手记的页面状态与业务入口。
 *
 * 数据以 Room 的 Flow 为唯一真相：保存/移动/删除后 UI 自动刷新，
 * 不依赖内存列表，进程被回收后仍可从数据库恢复。
 */
@HiltViewModel
class NotesViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    val folders: StateFlow<List<NoteFolder>> = repository.folders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val categories: StateFlow<List<NoteCategory>> = repository.categories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _message = MutableSharedFlow<NoteMessage>(extraBufferCapacity = 8)
    val message = _message.asSharedFlow()

    init {
        viewModelScope.launch {
            runCatching {
                repository.ensureUncategorized()
                repository.cleanupTemp()
            }.onFailure { notify("初始化随手记失败：${it.userMessage()}", error = true) }
            _ready.value = true
        }
    }

    fun notify(text: String, error: Boolean = false) {
        _message.tryEmit(NoteMessage(text, error))
    }

    fun createCategory(draft: NoteCategoryDraft, onCreated: (NoteCategory) -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { repository.createCategory(draft) }
                .onSuccess { category ->
                    onCreated(category)
                    notify("已创建「${category.name}」")
                }
                .onFailure { notify(it.userMessage(), error = true) }
            _busy.value = false
        }
    }

    fun updateCategory(categoryId: String, draft: NoteCategoryDraft, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { repository.updateCategory(categoryId, draft) }
                .onSuccess {
                    onDone()
                    notify("分类已更新")
                }
                .onFailure { notify(it.userMessage(), error = true) }
            _busy.value = false
        }
    }

    /** @param removePhotos false = 照片迁移到未归类；true = 连照片一起删除。 */
    fun deleteCategory(categoryId: String, removePhotos: Boolean, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { repository.deleteCategory(categoryId, removePhotos) }
                .onSuccess {
                    onDone()
                    notify(if (removePhotos) "分类和照片已删除" else "分类已删除，照片已转入未归类")
                }
                .onFailure { notify(it.userMessage(), error = true) }
            _busy.value = false
        }
    }

    fun movePhotos(photoIds: List<String>, targetCategoryId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { repository.movePhotos(photoIds, targetCategoryId) }
                .onSuccess {
                    onDone()
                    notify(if (photoIds.size == 1) "照片已移动" else "已移动 ${photoIds.size} 张照片")
                }
                .onFailure { notify(it.userMessage(), error = true) }
            _busy.value = false
        }
    }

    fun updatePhotoMeta(photoId: String, title: String, note: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.updatePhotoMeta(photoId, title, note) }
                .onSuccess {
                    onDone()
                    notify("照片信息已更新")
                }
                .onFailure { notify(it.userMessage(), error = true) }
        }
    }

    fun deletePhoto(photoId: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            runCatching { repository.deletePhoto(photoId) }
                .onSuccess {
                    onDone()
                    notify("照片已删除")
                }
                .onFailure { notify(it.userMessage(), error = true) }
            _busy.value = false
        }
    }

    fun deletePhotos(photoIds: List<String>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _busy.value = true
            var failed = 0
            photoIds.forEach { id ->
                runCatching { repository.deletePhoto(id) }.onFailure { failed++ }
            }
            _busy.value = false
            onDone()
            if (failed == 0) notify("已删除 ${photoIds.size} 张照片")
            else notify("有 $failed 张照片删除失败", error = true)
        }
    }

    /** 渲染编辑结果到临时文件；未编辑（isIdentity）时返回 null。 */
    fun renderDraft(draft: NoteDraft, document: NoteEditorDocument, onRendered: (File?) -> Unit) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            runCatching { repository.renderDraft(draft, document) }
                .onSuccess { rendered ->
                    onRendered(rendered)
                    if (rendered == null) notify("已保留原图，未做合成")
                }
                .onFailure { notify("合成编辑结果失败：${it.userMessage()}", error = true) }
            _busy.value = false
        }
    }

    fun saveDraft(
        draft: NoteDraft,
        categoryId: String,
        document: NoteEditorDocument,
        onSaved: (NotePhoto) -> Unit = {}
    ) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            runCatching { repository.saveDraft(draft, categoryId, document) }
                .onSuccess { photo ->
                    onSaved(photo)
                    notify("照片已保存")
                }
                .onFailure { notify("保存失败：${it.userMessage()}", error = true) }
            _busy.value = false
        }
    }

    /** 批量归档：串行提交，任意一张失败不影响已经成功的照片。 */
    fun saveDrafts(
        drafts: List<NoteDraft>,
        categoryId: String,
        documents: Map<String, NoteEditorDocument>,
        onFinished: (saved: Int, failed: Int) -> Unit
    ) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            var saved = 0
            var failed = 0
            drafts.forEach { draft ->
                runCatching {
                    repository.saveDraft(
                        draft = draft,
                        categoryId = categoryId,
                        document = documents[draft.id] ?: NoteEditorDocument()
                    )
                }.onSuccess { saved++ }.onFailure { failed++ }
            }
            _busy.value = false
            onFinished(saved, failed)
            if (failed == 0) notify("已保存 $saved 张照片")
            else notify("已保存 $saved 张，$failed 张失败", error = true)
        }
    }

    fun createCameraDraft(file: java.io.File, onCreated: (NoteDraft) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.draftFromCamera(file) }
                .onSuccess(onCreated)
                .onFailure {
                    file.delete()
                    notify("照片读取失败：${it.userMessage()}", error = true)
                }
        }
    }

    fun importUris(uris: List<Uri>, onImported: (List<NoteDraft>) -> Unit) {
        viewModelScope.launch {
            _busy.value = true
            val drafts = mutableListOf<NoteDraft>()
            var failed = 0
            uris.forEach { uri ->
                runCatching { repository.importUri(uri) }
                    .onSuccess { drafts += it }
                    .onFailure { failed++ }
            }
            _busy.value = false
            onImported(drafts)
            if (failed > 0) notify("$failed 张图片无法导入，请换一张试试", error = true)
        }
    }

    fun savePhotoEdit(photoId: String, document: NoteEditorDocument, onSaved: (NotePhoto) -> Unit = {}) {
        if (_busy.value) return
        viewModelScope.launch {
            _busy.value = true
            runCatching { repository.savePhotoEdit(photoId, document) }
                .onSuccess { photo ->
                    onSaved(photo)
                    notify(if (document.isIdentity) "已恢复原图" else "编辑已保存")
                }
                .onFailure { notify("保存编辑失败：${it.userMessage()}", error = true) }
            _busy.value = false
        }
    }

    fun loadDocument(
        photoId: String,
        onLoaded: (NoteEditorDocument) -> Unit,
        onError: () -> Unit = {}
    ) {
        viewModelScope.launch {
            runCatching { repository.document(photoId) }
                .onSuccess(onLoaded)
                .onFailure {
                    notify("读取编辑记录失败：${it.userMessage()}", error = true)
                    onError()
                }
        }
    }

    fun exportPhoto(photo: NotePhoto, onDone: (Uri?) -> Unit = {}) {
        viewModelScope.launch {
            runCatching { repository.exportPhoto(photo) }
                .onSuccess { uri ->
                    onDone(uri)
                    if (uri != null) notify("已导出到系统相册")
                    else notify("当前系统版本不支持直接导出，请使用分享保存", error = true)
                }
                .onFailure { notify("导出失败：${it.userMessage()}", error = true) }
        }
    }

    fun shareUri(photo: NotePhoto): Uri? = repository.shareUri(photo)

    fun discardDraft(draft: NoteDraft) {
        viewModelScope.launch { repository.deleteDraftFiles(draft) }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is UnsupportedImageException -> message ?: "图片处理失败"
        is IllegalArgumentException -> message ?: "输入不合法"
        is IllegalStateException -> message ?: "操作失败，请稍后重试"
        else -> message ?: "操作失败，请稍后重试"
    }
}
