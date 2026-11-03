package com.example.courseapp.ui.notes

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DriveFileMove
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.StickyNote2
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.courseapp.data.model.NoteCategory
import com.example.courseapp.data.model.NoteCategoryDraft
import com.example.courseapp.data.model.NoteDraft
import com.example.courseapp.data.model.NoteEditorDocument
import com.example.courseapp.data.model.NotePhoto
import com.example.courseapp.data.model.UNCATEGORIZED_CATEGORY_ID
import com.example.courseapp.data.repository.NoteFolder
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.theme.AppColors
import com.example.courseapp.ui.theme.appColors
import com.example.courseapp.viewmodel.NotesViewModel
import kotlinx.coroutines.launch
import java.io.File

/** 随手记内部页面（相机/预览/编辑器是覆盖在整屏上的 Dialog，不占这里的导航栈）。 */
private sealed interface NotePage {
    data object Home : NotePage
    data class Folder(val categoryId: String) : NotePage
    data class Photo(val photoId: String) : NotePage
    data object Camera : NotePage
    data class Preview(val draftId: String) : NotePage
}

internal sealed interface NoteEditorTarget {
    data class Draft(val draft: NoteDraft) : NoteEditorTarget
    data class Existing(val photo: NotePhoto) : NoteEditorTarget
}

internal sealed interface NotePickerRequest {
    data class NewPhoto(val draft: NoteDraft) : NotePickerRequest
    data class Batch(val drafts: List<NoteDraft>) : NotePickerRequest
    data class Move(val photoIds: List<String>) : NotePickerRequest
}

internal data class CategoryEditorRequest(
    val editing: NoteCategory? = null,
    val fromPicker: Boolean = false
)

private fun findActivity(context: android.content.Context): android.app.Activity? {
    var current = context
    while (current is android.content.ContextWrapper) {
        if (current is android.app.Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
fun NotesScreen(
    isDarkMode: Boolean,
    contentPadding: PaddingValues,
    chrome: NotesChromeState,
    viewModel: NotesViewModel = hiltViewModel()
) {
    val colors = appColors(isDarkMode)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val folders by viewModel.folders.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    var page by remember { mutableStateOf<NotePage>(NotePage.Home) }
    var cameraReturnPage by remember { mutableStateOf<NotePage>(NotePage.Home) }
    var homeQuery by rememberSaveable { mutableStateOf("") }
    var homeFilter by remember { mutableStateOf<String?>(null) }
    var folderQuery by remember { mutableStateOf("") }
    var selection by remember { mutableStateOf<Set<String>>(emptySet()) }

    var pendingDrafts by remember { mutableStateOf<List<NoteDraft>>(emptyList()) }
    var activeDraftId by remember { mutableStateOf<String?>(null) }
    var draftDocuments by remember { mutableStateOf<Map<String, NoteEditorDocument>>(emptyMap()) }
    var pickerRequest by remember { mutableStateOf<NotePickerRequest?>(null) }
    var pickerSelected by remember { mutableStateOf<String?>(null) }
    var editorTarget by remember { mutableStateOf<NoteEditorTarget?>(null) }
    var editorDocument by remember { mutableStateOf<NoteEditorDocument?>(null) }
    var categoryEditorRequest by remember { mutableStateOf<CategoryEditorRequest?>(null) }
    var categoryActionsTarget by remember { mutableStateOf<NoteCategory?>(null) }
    var deleteCategoryTarget by remember { mutableStateOf<NoteCategory?>(null) }
    var renamePhotoTarget by remember { mutableStateOf<NotePhoto?>(null) }
    var deletePhotoTarget by remember { mutableStateOf<NotePhoto?>(null) }
    var deleteSelectionConfirm by remember { mutableStateOf(false) }
    var permissionIntro by remember { mutableStateOf(false) }
    var permissionBlocked by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.message.collect { message ->
            snackbarHostState.showSnackbar(message.text)
        }
    }

    val categoriesById = remember(categories) { categories.associateBy { it.id } }
    val foldersById = remember(folders) { folders.associateBy { it.category.id } }
    val userFolders = remember(folders) { folders.filterNot { it.category.isSystem } }
    val currentFolder: NoteFolder? = (page as? NotePage.Folder)?.let { foldersById[it.categoryId] }
    val currentPhoto: NotePhoto? = (page as? NotePage.Photo)?.let { photo ->
        folders.flatMap { it.photos }.firstOrNull { it.id == photo.photoId }
    }
    val photoFolder: NoteFolder? = currentPhoto?.let { foldersById[it.categoryId] }
    val activeDraft = pendingDrafts.firstOrNull { it.id == activeDraftId }
    // 从某个分类进入拍摄时，归档默认落在原分类，而不是每次都回到「未归类」。
    val preferredCategoryId: String? = (cameraReturnPage as? NotePage.Folder)?.categoryId
    val pickerCategoryCounts = remember(folders) { folders.associate { it.category.id to it.count } }

    fun openCamera() {
        chrome.showLayoutToggle = false
        cameraReturnPage = page
        page = NotePage.Camera
    }

    fun startCameraFlow() {
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED -> openCamera()

            else -> permissionIntro = true
        }
    }

    /** 关闭拍摄会话：未归档的临时草稿一并清理，避免留下无主文件。 */
    fun closeCameraSession() {
        pendingDrafts.forEach { viewModel.discardDraft(it) }
        pendingDrafts = emptyList()
        activeDraftId = null
        draftDocuments = emptyMap()
        page = cameraReturnPage
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) openCamera() else permissionBlocked = true
    }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(9)
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris) { drafts ->
                if (drafts.isNotEmpty()) {
                    pendingDrafts = pendingDrafts + drafts
                    activeDraftId = drafts.first().id
                    page = NotePage.Preview(drafts.first().id)
                }
            }
        }
    }

    fun launchPhotoPicker() {
        photoPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    fun openPicker(request: NotePickerRequest, defaultCategoryId: String?) {
        pickerRequest = request
        pickerSelected = defaultCategoryId ?: UNCATEGORIZED_CATEGORY_ID
    }

    fun finishNewPhoto(draft: NoteDraft, categoryId: String) {
        NoteImageCache.invalidate()
        val remaining = pendingDrafts.filterNot { it.id == draft.id }
        pendingDrafts = remaining
        draftDocuments = draftDocuments - draft.id
        pickerRequest = null
        if (remaining.isNotEmpty()) {
            activeDraftId = remaining.first().id
            page = NotePage.Preview(remaining.first().id)
        } else {
            activeDraftId = null
            page = NotePage.Folder(categoryId)
        }
    }

    fun back() {
        when (val current = page) {
            NotePage.Home -> Unit
            is NotePage.Folder -> {
                selection = emptySet()
                page = NotePage.Home
            }
            is NotePage.Photo -> page = NotePage.Folder(currentPhoto?.categoryId ?: UNCATEGORIZED_CATEGORY_ID)
            is NotePage.Camera, is NotePage.Preview -> page = cameraReturnPage
        }
    }

    LaunchedEffect(page) {
        val showToggle = page is NotePage.Home || page is NotePage.Folder
        chrome.showLayoutToggle = showToggle
        if (showToggle) chrome.syncDescription()
    }
    LaunchedEffect(page) {
        if (page !is NotePage.Folder) selection = emptySet()
    }

    BackHandler(enabled = page is NotePage.Folder || page is NotePage.Photo) { back() }

    Box(Modifier.fillMaxSize()) {
        when (val current = page) {
            NotePage.Home -> HomePage(
                folders = folders,
                userFolders = userFolders,
                query = homeQuery,
                onQueryChange = { homeQuery = it },
                filterCategoryId = homeFilter,
                onFilterChange = { homeFilter = it },
                isGrid = chrome.isGrid,
                colors = colors,
                ready = ready,
                contentPadding = contentPadding,
                onCreateCategory = { categoryEditorRequest = CategoryEditorRequest() },
                onOpenCategory = { categoryId ->
                    chrome.isGrid = true
                    chrome.syncDescription()
                    folderQuery = ""
                    selection = emptySet()
                    page = NotePage.Folder(categoryId)
                },
                onManageCategory = { categoryActionsTarget = it },
                onOpenPhoto = { page = NotePage.Photo(it) },
                onImport = { launchPhotoPicker() },
                onCamera = { startCameraFlow() }
            )

            is NotePage.Folder -> FolderPage(
                folder = currentFolder,
                query = folderQuery,
                onQueryChange = { folderQuery = it },
                isGrid = chrome.isGrid,
                colors = colors,
                contentPadding = contentPadding,
                selection = selection,
                ready = ready,
                onBack = { back() },
                onToggleLayout = { chrome.toggleLayout() },
                onManageCategory = { currentFolder?.let { categoryActionsTarget = it.category } },
                onOpenPhoto = { photoId ->
                    if (selection.isEmpty()) page = NotePage.Photo(photoId)
                    else selection = if (photoId in selection) selection - photoId else selection + photoId
                },
                onLongPressPhoto = { photoId -> selection = selection + photoId },
                onClearSelection = { selection = emptySet() },
                onMoveSelection = {
                    if (selection.isNotEmpty()) {
                        openPicker(NotePickerRequest.Move(selection.toList()), currentFolder?.category?.id)
                    }
                },
                onDeleteSelection = { if (selection.isNotEmpty()) deleteSelectionConfirm = true },
                onExportSelection = {
                    selection.forEach { id ->
                        folders.flatMap { it.photos }.firstOrNull { it.id == id }?.let { viewModel.exportPhoto(it) }
                    }
                },
                onAddPhoto = { startCameraFlow() },
                onImport = { launchPhotoPicker() }
            )

            is NotePage.Photo -> PhotoDetailPage(
                photo = currentPhoto,
                folderName = currentPhoto?.let { categoriesById[it.categoryId]?.name ?: "未归类" } ?: "",
                photos = photoFolder?.photos.orEmpty(),
                colors = colors,
                contentPadding = contentPadding,
                onBack = { back() },
                onOpenPhoto = { page = NotePage.Photo(it) },
                onEdit = { photo ->
                    editorDocument = null
                    editorTarget = NoteEditorTarget.Existing(photo)
                    viewModel.loadDocument(
                        photoId = photo.id,
                        onLoaded = { document -> editorDocument = document },
                        onError = { editorTarget = null }
                    )
                },
                onMove = { photo -> openPicker(NotePickerRequest.Move(listOf(photo.id)), photo.categoryId) },
                onDelete = { photo -> deletePhotoTarget = photo },
                onExport = { photo -> viewModel.exportPhoto(photo) },
                onShare = { photo ->
                    val uri = viewModel.shareUri(photo)
                    if (uri == null) {
                        viewModel.notify("图片文件已丢失，无法分享", error = true)
                    } else {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "image/*"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        runCatching {
                            context.startActivity(Intent.createChooser(intent, "分享照片"))
                        }.onFailure { viewModel.notify("没有可用的分享应用", error = true) }
                    }
                },
                onRename = { photo -> renamePhotoTarget = photo }
            )

            NotePage.Camera, is NotePage.Preview -> Unit
        }

        if (ready && folders.isEmpty()) {
            // 数据库尚未写入「未归类」的短暂空窗，给出加载态而不是误判为空。
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
        }

        if (page is NotePage.Home || page is NotePage.Folder) {
            NoteCaptureDock(
                colors = colors,
                bottomPadding = contentPadding.calculateBottomPadding(),
                onCamera = { startCameraFlow() },
                onImport = { launchPhotoPicker() }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding() + 82.dp)
        ) { data ->
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 380.dp)
                    .shadow(8.dp, RoundedCornerShape(20.dp), clip = false)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceRaised)
                    .padding(start = 16.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    data.visuals.message,
                    color = colors.textPrimary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    modifier = Modifier.weight(1f, fill = false)
                )
                data.visuals.actionLabel?.let { label ->
                    TextButton(onClick = { data.performAction() }) {
                        Text(label, color = colors.accentText, fontSize = 12.sp)
                    }
                }
            }
        }

        NoteBusyOverlay(busy, colors)
    }

    // ── 全屏相机 / 预览 / 编辑器：覆盖顶栏与底栏，避免全局 chrome 干扰拍摄流程 ──
    if (page is NotePage.Camera) {
        FullScreenNoteDialog(onDismiss = { closeCameraSession() }) {
            NoteCameraScreen(
                isDarkMode = isDarkMode,
                capturedCount = pendingDrafts.size,
                latestDraft = pendingDrafts.lastOrNull(),
                onClose = { closeCameraSession() },
                onImport = { launchPhotoPicker() },
                onCaptured = { file ->
                    viewModel.createCameraDraft(file) { draft ->
                        if (page is NotePage.Camera) {
                            pendingDrafts = pendingDrafts + draft
                            draftDocuments = draftDocuments + (draft.id to NoteEditorDocument())
                        } else {
                            // 相机页已关闭：释放刚拍的临时文件，避免留下无主图片。
                            viewModel.discardDraft(draft)
                        }
                    }
                },
                onOpenPreview = {
                    val last = pendingDrafts.lastOrNull()
                    if (last != null) {
                        activeDraftId = last.id
                        page = NotePage.Preview(last.id)
                    }
                }
            )
        }
    }

    if (page is NotePage.Preview && activeDraft != null) {
        FullScreenNoteDialog(onDismiss = { closeCameraSession() }) {
            NoteCapturePreviewScreen(
                draft = activeDraft,
                drafts = pendingDrafts,
                colors = colors,
                onClose = { closeCameraSession() },
                onContinueShooting = { page = NotePage.Camera },
                onSelectDraft = { activeDraftId = it.id },
                onRetake = {
                    viewModel.discardDraft(activeDraft)
                    val remaining = pendingDrafts.filterNot { it.id == activeDraft.id }
                    pendingDrafts = remaining
                    draftDocuments = draftDocuments - activeDraft.id
                    if (remaining.isEmpty()) {
                        page = NotePage.Camera
                    } else {
                        activeDraftId = remaining.last().id
                        page = NotePage.Preview(remaining.last().id)
                    }
                },
                onEdit = {
                    editorDocument = draftDocuments[activeDraft.id] ?: NoteEditorDocument()
                    editorTarget = NoteEditorTarget.Draft(activeDraft)
                },
                onSkipEdit = {
                    openPicker(NotePickerRequest.NewPhoto(activeDraft), preferredCategoryId)
                },
                onBatchSave = {
                    openPicker(NotePickerRequest.Batch(pendingDrafts), preferredCategoryId)
                }
            )
        }
    }

    editorTarget?.let { target ->
        val sourceFile = when (target) {
            is NoteEditorTarget.Draft -> File(target.draft.sourcePath)
            is NoteEditorTarget.Existing -> NoteImageCache.fileFor(
                context,
                target.photo.originalRelativePath
            )
        }
        val initial: NoteEditorDocument? = when (target) {
            is NoteEditorTarget.Draft -> draftDocuments[target.draft.id] ?: NoteEditorDocument()
            // 已有照片要先读出编辑文档，否则会用空白文档覆盖用户之前的标注。
            is NoteEditorTarget.Existing -> editorDocument
        }
        when {
            !sourceFile.exists() -> {
                LaunchedEffect(target) {
                    viewModel.notify("原图文件已丢失，无法编辑", error = true)
                    editorTarget = null
                }
            }

            initial == null -> {
                FullScreenNoteDialog(onDismiss = { editorTarget = null }) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(colors.canvas),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = colors.accent)
                            Spacer(Modifier.height(12.dp))
                            Text("正在读取编辑记录…", color = colors.textSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }

            else -> {
                FullScreenNoteDialog(
                    // 返回键不直接丢弃编辑结果；由编辑器内的「取消」走二次确认。
                    onDismiss = {},
                    dismissOnBackPress = false
                ) {
                    Box(Modifier.fillMaxSize()) {
                        NoteEditorScreen(
                        isDarkMode = isDarkMode,
                        sourceFile = sourceFile,
                        sourceRotation = when (target) {
                            is NoteEditorTarget.Draft -> target.draft.rotation
                            is NoteEditorTarget.Existing -> target.photo.originalRotation
                        },
                        initialDocument = initial,
                        onCancel = { editorTarget = null },
                        onDone = { document ->
                            when (target) {
                                is NoteEditorTarget.Draft -> {
                                    viewModel.renderDraft(target.draft, document) { edited ->
                                        val updated = target.draft.copy(editedPath = edited?.absolutePath)
                                        draftDocuments = draftDocuments + (updated.id to document)
                                        pendingDrafts = pendingDrafts.map {
                                            if (it.id == updated.id) updated else it
                                        }
                                        activeDraftId = updated.id
                                        editorTarget = null
                                        openPicker(
                                            NotePickerRequest.NewPhoto(updated),
                                            preferredCategoryId
                                        )
                                    }
                                }

                                is NoteEditorTarget.Existing -> {
                                    viewModel.savePhotoEdit(target.photo.id, document) {
                                        NoteImageCache.invalidate()
                                        editorTarget = null
                                        page = NotePage.Photo(target.photo.id)
                                    }
                                }
                            }
                        }
                        )
                        if (busy) {
                            NoteBusyOverlay(
                                visible = true,
                                colors = colors,
                                label = "正在保存编辑结果…"
                            )
                        }
                    }
                }
            }
        }
    }

    pickerRequest?.let { request ->
        NoteCategoryPickerSheet(
            request = request,
            categories = categories,
            selectedCategoryId = pickerSelected,
            photoCounts = pickerCategoryCounts,
            colors = colors,
            onSelect = { pickerSelected = it },
            onCreateCategory = {
                categoryEditorRequest = CategoryEditorRequest(fromPicker = true)
            },
            onDismiss = { pickerRequest = null },
            onConfirm = {
                val target = pickerSelected ?: UNCATEGORIZED_CATEGORY_ID
                when (request) {
                    is NotePickerRequest.NewPhoto -> {
                        viewModel.saveDraft(
                            draft = request.draft,
                            categoryId = target,
                            document = draftDocuments[request.draft.id] ?: NoteEditorDocument()
                        ) { finishNewPhoto(request.draft, target) }
                    }

                    is NotePickerRequest.Batch -> {
                        viewModel.saveDrafts(
                            drafts = request.drafts,
                            categoryId = target,
                            documents = draftDocuments
                        ) { saved, _ ->
                            if (saved > 0) {
                                NoteImageCache.invalidate()
                                pendingDrafts = emptyList()
                                activeDraftId = null
                                pickerRequest = null
                                page = NotePage.Folder(target)
                            }
                        }
                    }

                    is NotePickerRequest.Move -> {
                        viewModel.movePhotos(request.photoIds, target) {
                            pickerRequest = null
                            selection = emptySet()
                        }
                    }
                }
            },
            onSkip = {
                when (request) {
                    is NotePickerRequest.NewPhoto -> {
                        viewModel.saveDraft(
                            draft = request.draft,
                            categoryId = UNCATEGORIZED_CATEGORY_ID,
                            document = draftDocuments[request.draft.id] ?: NoteEditorDocument()
                        ) { finishNewPhoto(request.draft, UNCATEGORIZED_CATEGORY_ID) }
                    }

                    is NotePickerRequest.Batch -> {
                        viewModel.saveDrafts(
                            drafts = request.drafts,
                            categoryId = UNCATEGORIZED_CATEGORY_ID,
                            documents = draftDocuments
                        ) { saved, _ ->
                            if (saved > 0) {
                                NoteImageCache.invalidate()
                                pendingDrafts = emptyList()
                                activeDraftId = null
                                pickerRequest = null
                                page = NotePage.Folder(UNCATEGORIZED_CATEGORY_ID)
                            }
                        }
                    }

                    is NotePickerRequest.Move -> Unit
                }
            }
        )
    }

    categoryEditorRequest?.let { request ->
        NoteCategoryEditorDialog(
            initial = request.editing,
            colors = colors,
            onDismiss = { categoryEditorRequest = null },
            onConfirm = { draft ->
                if (request.editing == null) {
                    viewModel.createCategory(draft) { created ->
                        if (request.fromPicker) pickerSelected = created.id
                        categoryEditorRequest = null
                    }
                } else {
                    viewModel.updateCategory(request.editing.id, draft) {
                        categoryEditorRequest = null
                    }
                }
            }
        )
    }

    categoryActionsTarget?.let { category ->
        NoteCategoryActionsDialog(
            category = category,
            photoCount = foldersById[category.id]?.count ?: 0,
            colors = colors,
            onDismiss = { categoryActionsTarget = null },
            onRename = {
                categoryActionsTarget = null
                categoryEditorRequest = CategoryEditorRequest(editing = category)
            },
            onDelete = {
                categoryActionsTarget = null
                deleteCategoryTarget = category
            }
        )
    }

    deleteCategoryTarget?.let { category ->
        NoteDeleteCategoryDialog(
            category = category,
            photoCount = foldersById[category.id]?.count ?: 0,
            colors = colors,
            onDismiss = { deleteCategoryTarget = null },
            onMigrate = {
                viewModel.deleteCategory(category.id, removePhotos = false) {
                    deleteCategoryTarget = null
                    if ((page as? NotePage.Folder)?.categoryId == category.id) page = NotePage.Home
                }
            },
            onDeleteAll = {
                viewModel.deleteCategory(category.id, removePhotos = true) {
                    deleteCategoryTarget = null
                    if ((page as? NotePage.Folder)?.categoryId == category.id) page = NotePage.Home
                }
            }
        )
    }

    renamePhotoTarget?.let { photo ->
        NotePhotoMetaDialog(
            photo = photo,
            colors = colors,
            onDismiss = { renamePhotoTarget = null },
            onConfirm = { title, note ->
                viewModel.updatePhotoMeta(photo.id, title, note) { renamePhotoTarget = null }
            }
        )
    }

    deletePhotoTarget?.let { photo ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deletePhotoTarget = null },
            title = { Text("删除这张照片？") },
            text = {
                Text("「${photo.title}」及其编辑记录会被删除，无法恢复。原始文件也会从应用私有目录清理。")
            },
            confirmButton = {
                TextButton(onClick = {
                    deletePhotoTarget = null
                    viewModel.deletePhoto(photo.id) {
                        if ((page as? NotePage.Photo)?.photoId == photo.id) {
                            page = NotePage.Folder(photo.categoryId)
                        }
                    }
                }) { Text("删除", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { deletePhotoTarget = null }) { Text("取消") }
            },
            containerColor = colors.surfaceRaised
        )
    }

    if (deleteSelectionConfirm) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { deleteSelectionConfirm = false },
            title = { Text("删除 ${selection.size} 张照片？") },
            text = { Text("删除后无法恢复，编辑记录和缩略图会一起清理。") },
            confirmButton = {
                TextButton(onClick = {
                    deleteSelectionConfirm = false
                    viewModel.deletePhotos(selection.toList()) { selection = emptySet() }
                }) { Text("删除", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { deleteSelectionConfirm = false }) { Text("取消") }
            },
            containerColor = colors.surfaceRaised
        )
    }

    if (permissionIntro) {
        NotePermissionDialog(
            colors = colors,
            title = "拍摄课堂照片",
            message = "随手记需要使用相机拍摄 PPT、板书和课堂资料。照片只保存在应用内，由你决定何时导出。",
            primaryLabel = "允许使用相机",
            onPrimary = {
                permissionIntro = false
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            secondaryLabel = "从相册导入",
            onSecondary = {
                permissionIntro = false
                launchPhotoPicker()
            },
            onDismiss = { permissionIntro = false }
        )
    }

    if (permissionBlocked) {
        NotePermissionDialog(
            colors = colors,
            title = "无法打开相机",
            message = "相机权限被拒绝。你可以重新授权，或到系统设置中开启相机权限；也可以先使用相册导入照片。",
            primaryLabel = "重新授权",
            onPrimary = {
                permissionBlocked = false
                permissionLauncher.launch(Manifest.permission.CAMERA)
            },
            secondaryLabel = "打开系统设置",
            onSecondary = {
                permissionBlocked = false
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null)
                )
                runCatching { context.startActivity(intent) }
            },
            tertiaryLabel = "从相册导入",
            onTertiary = {
                permissionBlocked = false
                launchPhotoPicker()
            },
            onDismiss = { permissionBlocked = false }
        )
    }
}

@Composable
private fun FullScreenNoteDialog(
    onDismiss: () -> Unit,
    dismissOnBackPress: Boolean = true,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = false
        )
    ) {
        content()
    }
}

// ── 首页 ──────────────────────────────────────────────────────────────────

@Composable
private fun HomePage(
    folders: List<NoteFolder>,
    userFolders: List<NoteFolder>,
    query: String,
    onQueryChange: (String) -> Unit,
    filterCategoryId: String?,
    onFilterChange: (String?) -> Unit,
    isGrid: Boolean,
    colors: AppColors,
    ready: Boolean,
    contentPadding: PaddingValues,
    onCreateCategory: () -> Unit,
    onOpenCategory: (String) -> Unit,
    onManageCategory: (NoteCategory) -> Unit,
    onOpenPhoto: (String) -> Unit,
    onImport: () -> Unit,
    onCamera: () -> Unit
) {
    val scroll = rememberScrollState()
    val visibleFolders = remember(folders, query, filterCategoryId) {
        filterNoteFolders(folders, query, filterCategoryId)
    }
    val matchedPhotos = remember(folders, query) { matchingNotePhotos(folders, query) }
    val chips = remember(folders) {
        // 「未归类」永远排在自定义分类之后，「全部」由 null 表示。
        folders.sortedBy { if (it.category.isSystem) 1 else 0 }
    }
    val userCategoryEmpty = ready && userFolders.isEmpty()
    val showEmpty = ready && folders.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 108.dp
            )
    ) {
        MorphingSearchBar(
            value = query,
            onValueChange = onQueryChange,
            placeholder = "搜索分类、照片或笔记",
            colors = colors
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            NoteChip(
                text = "全部",
                selected = filterCategoryId == null,
                onClick = { onFilterChange(null) },
                colors = colors
            )
            chips.forEach { folder ->
                NoteChip(
                    text = folder.category.name,
                    selected = filterCategoryId == folder.category.id,
                    onClick = { onFilterChange(folder.category.id) },
                    colors = colors
                )
            }
            NoteChip(
                text = "新建分类",
                selected = false,
                onClick = onCreateCategory,
                colors = colors,
                leadingIcon = Icons.Outlined.Add
            )
        }
        Spacer(Modifier.height(16.dp))
        NoteSectionHeader(
            title = "我的学习文件夹",
            subtitle = "按课程整理，随时回看课堂重点",
            colors = colors,
            action = {
                TextButton(onClick = onCreateCategory) {
                    Text("＋ 创建", color = colors.accentText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        )
        Spacer(Modifier.height(12.dp))

        if (!ready) {
            Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
        } else if (showEmpty) {
            NoteEmptyState(
                icon = Icons.Outlined.StickyNote2,
                title = "按课程创建文件夹，整理课堂资料",
                message = "拍下 PPT、板书或重点内容，按课程放进文件夹；之后也可以随时补充分类。",
                colors = colors,
                primaryLabel = "创建分类",
                onPrimary = onCreateCategory,
                secondaryLabel = "立即拍摄",
                onSecondary = onCamera
            )
        } else {
            if (userCategoryEmpty) {
                NoteEmptyState(
                    icon = Icons.Outlined.StickyNote2,
                    title = "按课程创建文件夹，整理课堂资料",
                    message = "还没有自定义分类。先建一个「高等数学」试试，拍下的照片会自动进入未归类。",
                    colors = colors,
                    primaryLabel = "创建分类",
                    onPrimary = onCreateCategory,
                    secondaryLabel = "立即拍摄",
                    onSecondary = onCamera
                )
            }

            if (visibleFolders.isEmpty()) {
                NoteEmptyState(
                    icon = Icons.Outlined.FolderOpen,
                    title = "没有找到相关分类",
                    message = "试试其他关键词，或创建一个新的学习分类。",
                    colors = colors,
                    primaryLabel = "创建分类",
                    onPrimary = onCreateCategory,
                    secondaryLabel = "清除搜索",
                    onSecondary = { onQueryChange(""); onFilterChange(null) }
                )
            } else {
                FlipGridList(
                    items = visibleFolders.map { it.category.id },
                    isGrid = isGrid,
                    gridAspect = 0.98f,
                    listRowHeight = 86.dp
                ) { id, grid ->
                    val folder = visibleFolders.firstOrNull { it.category.id == id }
                    if (folder != null) {
                        FolderCard(
                            folder = folder,
                            isGrid = grid,
                            colors = colors,
                            onOpen = { onOpenCategory(folder.category.id) },
                            onManage = { onManageCategory(folder.category) }
                        )
                    }
                }
            }

            if (matchedPhotos.isNotEmpty()) {
                Spacer(Modifier.height(22.dp))
                NoteSectionHeader(
                    title = "匹配的照片",
                    subtitle = "共 ${matchedPhotos.size} 张",
                    colors = colors
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    matchedPhotos.take(12).forEach { photo ->
                        MatchPhotoItem(
                            photo = photo,
                            colors = colors,
                            onClick = { onOpenPhoto(photo.id) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderCard(
    folder: NoteFolder,
    isGrid: Boolean,
    colors: AppColors,
    onOpen: () -> Unit,
    onManage: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val covers = folder.covers
    // 在卡片根部加载三张缩略图：网格/列表切换只改布局，Bitmap 状态不重建。
    val cover0 = rememberNoteBitmap(covers.getOrNull(0), maxSize = 512)
    val cover1 = rememberNoteBitmap(covers.getOrNull(1), maxSize = 512)
    val cover2 = rememberNoteBitmap(covers.getOrNull(2), maxSize = 512)
    val coverBitmaps = listOf(cover0, cover1, cover2)
    NoteSurface(
        colors = colors,
        modifier = Modifier
            .fillMaxSize()
            .elasticPress(shape)
            .combinedClickable(onClick = onOpen, onLongClick = onManage),
        shape = shape
    ) {
        if (isGrid) {
            Column(Modifier.fillMaxSize().padding(11.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FolderIconBadge(folder.category, colors, 30.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = folder.category.name,
                            color = colors.textPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${folder.count} 张照片",
                            color = colors.textMuted,
                            fontSize = 10.sp
                        )
                    }
                    FolderOverflow(onManage, colors)
                }
                FolderCoverStack(coverBitmaps, folder.count, colors, Modifier.weight(1f))
                Spacer(Modifier.height(6.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.hairline))
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (folder.count == 0) "暂无内容" else "最近更新 ${formatNoteTime(folder.latestAt)}",
                        color = colors.textMuted,
                        fontSize = 9.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Outlined.ChevronRight,
                        contentDescription = "进入文件夹",
                        tint = colors.accentText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FolderIconBadge(folder.category, colors, 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = folder.category.name,
                        color = colors.textPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${folder.count} 张照片 · ${if (folder.count == 0) "暂无内容" else "更新于 ${formatNoteTime(folder.latestAt)}"}",
                        color = colors.textMuted,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
                FolderOverflow(onManage, colors)
                Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = "进入文件夹",
                    tint = colors.accentText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun FolderIconBadge(category: NoteCategory, colors: AppColors, size: androidx.compose.ui.unit.Dp) {
    val tint = noteColor(category.colorValue)
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.32f))
            .background(tint.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = noteIconFor(category.iconKey),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}

@Composable
private fun FolderOverflow(onManage: () -> Unit, colors: AppColors) {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .semantics { contentDescription = "分类管理" }
            .clickable(onClick = onManage),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Outlined.MoreHoriz,
            contentDescription = null,
            tint = colors.textMuted,
            modifier = Modifier.size(17.dp)
        )
    }
}

@Composable
private fun FolderCoverStack(
    bitmaps: List<android.graphics.Bitmap?>,
    count: Int,
    colors: AppColors,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (bitmaps.all { it == null }) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.Image,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(22.dp)
                )
                Text("暂无照片", color = colors.textMuted, fontSize = 10.sp)
            }
        } else {
            // 三张缩略图错位叠放：形成“文件夹里装着多张照片”的视觉暗示。
            val rotations = listOf(-9f, 8f, 0f)
            val offsets = listOf(-22.dp, 20.dp, 0.dp)
            val verticalOffsets = listOf(5.dp, 4.dp, (-3).dp)
            val order = listOf(0, 1, 2)
            order.forEach { index ->
                val bitmap = bitmaps.getOrNull(index)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .fillMaxHeight(0.78f)
                            .offset(x = offsets[index], y = verticalOffsets[index])
                            .rotate(rotations[index])
                            .clip(RoundedCornerShape(9.dp))
                            .background(colors.surfaceSunken),
                    )
                }
            }
            if (count == 0) {
                Text("暂无照片", color = colors.textMuted, fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun MatchPhotoItem(photo: NotePhoto, colors: AppColors, onClick: () -> Unit) {
    val bitmap = rememberNoteBitmap(photo.previewRelativePath ?: photo.originalRelativePath, maxSize = 512)
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .widthIn(min = 108.dp, max = 128.dp)
            .elasticPress(shape)
            .clip(shape)
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(7.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(66.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.surfaceSunken),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Outlined.Image, null, tint = colors.textMuted, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            photo.title,
            color = colors.textPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(formatNoteTime(photo.capturedAt), color = colors.textMuted, fontSize = 9.sp)
    }
}

// ── 分类详情 ──────────────────────────────────────────────────────────────

@Composable
private fun FolderPage(
    folder: NoteFolder?,
    query: String,
    onQueryChange: (String) -> Unit,
    isGrid: Boolean,
    colors: AppColors,
    contentPadding: PaddingValues,
    selection: Set<String>,
    ready: Boolean,
    onBack: () -> Unit,
    onToggleLayout: () -> Unit,
    onManageCategory: () -> Unit,
    onOpenPhoto: (String) -> Unit,
    onLongPressPhoto: (String) -> Unit,
    onClearSelection: () -> Unit,
    onMoveSelection: () -> Unit,
    onDeleteSelection: () -> Unit,
    onExportSelection: () -> Unit,
    onAddPhoto: () -> Unit,
    onImport: () -> Unit
) {
    if (folder == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("分类不存在或已删除", color = colors.textSecondary)
                TextButton(onClick = onBack) { Text("返回", color = colors.accentText) }
            }
        }
        return
    }
    val scroll = rememberScrollState()
    val photos = remember(folder.photos, query) { filterNotePhotos(folder.photos, query) }
    val selectedMode = selection.isNotEmpty()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(
                start = 16.dp,
                end = 16.dp,
                top = contentPadding.calculateTopPadding() + 8.dp,
                bottom = contentPadding.calculateBottomPadding() + 108.dp
            )
    ) {
        if (selectedMode) {
            Row(
                modifier = Modifier.fillMaxWidth().height(40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoteIconButton(
                    icon = Icons.Outlined.Close,
                    description = "退出多选",
                    onClick = onClearSelection,
                    colors = colors
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "已选 ${selection.size} 张",
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                NoteIconButton(Icons.Outlined.DriveFileMove, "移动到分类", onMoveSelection, colors)
                Spacer(Modifier.width(4.dp))
                NoteIconButton(Icons.Outlined.Upload, "导出所选", onExportSelection, colors)
                Spacer(Modifier.width(4.dp))
                NoteIconButton(Icons.Outlined.Delete, "删除所选", onDeleteSelection, colors)
            }
            Spacer(Modifier.height(8.dp))
        } else {
            NotePageHeader(
                title = folder.category.name,
                subtitle = "${folder.count} 张课堂照片",
                colors = colors,
                onBack = onBack,
                actions = {
                    NoteLayoutSwitch(isGrid = isGrid, onToggle = onToggleLayout, colors = colors)
                    Spacer(Modifier.width(6.dp))
                    NoteIconButton(
                        icon = Icons.Outlined.MoreHoriz,
                        description = "分类管理",
                        onClick = onManageCategory,
                        colors = colors
                    )
                }
            )
            Spacer(Modifier.height(12.dp))
            MorphingSearchBar(
                value = query,
                onValueChange = onQueryChange,
                placeholder = "搜索本分类下的照片或笔记",
                colors = colors
            )
            Spacer(Modifier.height(14.dp))
            NoteSectionHeader(
                title = "全部照片",
                subtitle = "共 ${photos.size} 张 · 按拍摄时间倒序",
                colors = colors,
                action = {
                    Row {
                        TextButton(onClick = onImport) {
                            Text("导入", color = colors.accentText, fontSize = 12.sp)
                        }
                        TextButton(onClick = onAddPhoto) {
                            Text("＋ 拍摄", color = colors.accentText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            )
            Spacer(Modifier.height(10.dp))
        }

        if (!ready) {
            Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.accent)
            }
        } else if (folder.photos.isEmpty()) {
            NoteEmptyState(
                icon = Icons.Outlined.CameraAlt,
                title = "这个文件夹还没有照片",
                message = "拍下课堂 PPT、板书或知识重点，放进这个文件夹。也可以从相册导入已有照片。",
                colors = colors,
                primaryLabel = "立即拍摄",
                onPrimary = onAddPhoto,
                secondaryLabel = "从相册导入",
                onSecondary = onImport
            )
        } else if (photos.isEmpty()) {
            NoteEmptyState(
                icon = Icons.Outlined.Image,
                title = "没有找到相关照片",
                message = "试试其他关键词，或清除搜索查看全部照片。",
                colors = colors,
                primaryLabel = "清除搜索",
                onPrimary = { onQueryChange("") }
            )
        } else {
            FlipGridList(
                items = photos.map { it.id },
                isGrid = isGrid,
                gridAspect = 0.94f,
                listRowHeight = 94.dp
            ) { id, grid ->
                val photo = photos.firstOrNull { it.id == id }
                if (photo != null) {
                    PhotoCard(
                        photo = photo,
                        isGrid = grid,
                        colors = colors,
                        selected = id in selection,
                        selectionMode = selectedMode,
                        onOpen = { onOpenPhoto(id) },
                        onLongPress = { onLongPressPhoto(id) }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "长按照片可多选，批量移动、导出或删除",
                color = colors.textMuted,
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// ── 可收起的拍摄入口 ──────────────────────────────────────────────────────

@Composable
private fun NoteCaptureDock(
    colors: AppColors,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onCamera: () -> Unit,
    onImport: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding + 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                (fadeIn(tween(160)) + scaleIn(tween(200), initialScale = 0.94f)) togetherWith
                    (fadeOut(tween(120)) + scaleOut(tween(140), targetScale = 0.94f))
            },
            label = "noteCaptureDock"
        ) { open ->
            NoteSurface(
                colors = colors,
                shape = shape,
                modifier = Modifier.animateContentSize(),
                elevated = true
            ) {
                if (open) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DockAction(
                            icon = Icons.Outlined.CameraAlt,
                            label = "拍照",
                            colors = colors,
                            primary = true,
                            onClick = {
                                expanded = false
                                onCamera()
                            }
                        )
                        DockAction(
                            icon = Icons.Outlined.PhotoLibrary,
                            label = "相册导入",
                            colors = colors,
                            primary = false,
                            onClick = {
                                expanded = false
                                onImport()
                            }
                        )
                        NoteIconButton(
                            icon = Icons.Outlined.KeyboardArrowDown,
                            description = "收起拍摄入口",
                            onClick = { expanded = false },
                            colors = colors,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .elasticPress(RoundedCornerShape(22.dp))
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { expanded = true }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = colors.accentOn,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(colors.accent)
                                .padding(5.dp)
                        )
                        Column {
                            Text("拍摄", color = colors.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text("拍照 / 相册导入", color = colors.textMuted, fontSize = 9.sp)
                        }
                        Icon(
                            Icons.Outlined.KeyboardArrowUp,
                            contentDescription = "展开拍摄入口",
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DockAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    colors: AppColors,
    primary: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .elasticPress(shape)
            .clip(shape)
            .background(if (primary) colors.accent else colors.surfaceSunken)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (primary) colors.accentOn else colors.accentText,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = label,
            color = if (primary) colors.accentOn else colors.textSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
