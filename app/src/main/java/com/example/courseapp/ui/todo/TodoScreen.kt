package com.example.courseapp.ui.todo

import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.components.contentEdgeBlur
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.courseapp.data.model.*
import com.example.courseapp.ui.profile.GlassSurface
import com.example.courseapp.ui.theme.appColors
import com.example.courseapp.viewmodel.TodoViewModel
import io.github.itshedimisawi.vitro.GlassBackdrop
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil

@Composable
fun TodoScreen(isDarkMode: Boolean, backdrop: GlassBackdrop, contentPadding: PaddingValues,
    viewModel: TodoViewModel = hiltViewModel(),
    onContextRequest: (Todo, Rect, Boolean) -> Unit = { _, _, _ -> }) {
    val todos by viewModel.todos.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val colors = appColors(isDarkMode)
    var completed by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf<String?>(null) }
    val todoSaver = listSaver<Todo?, Any>(save = { if(it == null) emptyList() else listOf(it.id,it.title,it.createdAt,it.dueAt ?: -1L,it.notes,it.tags,it.completedAt ?: -1L,it.imagePath ?: "",it.isPinned) },
        restore = { if(it.isEmpty()) null else Todo(it[0] as String,it[1] as String,it[2] as Long,(it[3] as Long).takeUnless { v -> v == -1L },it[4] as String,it[5] as String,(it[6] as Long).takeUnless { v -> v == -1L },(it[7] as String).ifBlank { null },it.getOrNull(8) as? Boolean ?: false) })
    var draft by rememberSaveable(stateSaver = todoSaver) { mutableStateOf<Todo?>(null) }
    var sheetRequested by rememberSaveable { mutableStateOf(false) }
    var pendingInsert by remember { mutableStateOf<String?>(null) }
    var highlighted by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<Todo?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val pendingList = rememberLazyListState()
    val completedList = rememberLazyListState()
    val list = if (completed) completedList else pendingList
    val density = LocalDensity.current
    val contextMenuTop = WindowInsets.statusBars.getTop(density) + with(density) {
        (16.dp + 52.dp * density.fontScale.coerceAtLeast(1f) * 2 + 16.dp + 8.dp).toPx()
    }
    fun openContext(todo: Todo, bounds: Rect, isExpanded: Boolean) {
        scope.launch {
            // Leave room above a card near the viewport edge instead of covering it with the menu.
            val shift = (bounds.top - contextMenuTop).coerceAtMost(0f)
            val consumed = if (shift < 0f) list.animateScrollBy(shift, tween(180)) else 0f
            onContextRequest(todo, bounds.translate(0f, -consumed), isExpanded)
        }
    }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
    LaunchedEffect(highlighted) { if (highlighted != null) { delay(1800); highlighted = null } }
    LaunchedEffect(error) { error?.let { if(snack.showSnackbar(it, "重试") == SnackbarResult.ActionPerformed) viewModel.retry(); viewModel.clearError() } }
    BackHandler(draft != null || expanded != null) { if (draft != null) draft = null else expanded = null }
    fun add() { sheetRequested = true }
    val displayedTodos = todos.filter { it.id != pendingInsert }
    Box(Modifier.fillMaxSize()) {
        val reduced = !android.animation.ValueAnimator.areAnimatorsEnabled()
        AnimatedContent(targetState = completed, transitionSpec = {
            val spec = if (reduced) snap<IntOffset>() else spring(dampingRatio = .85f, stiffness = Spring.StiffnessMediumLow)
            val direction = if (targetState) 1 else -1
            (slideInHorizontally(spec) { it * direction } + fadeIn(tween(if (reduced) 0 else 180))) togetherWith
                (slideOutHorizontally(spec) { -it * direction } + fadeOut(tween(if (reduced) 0 else 180)))
        }, label = "todoTabs") { showingCompleted ->
        val visible = if (showingCompleted) sortCompletedTodos(displayedTodos)
            else displayedTodos.filter { it.completedAt == null }
        LazyColumn(state = if (showingCompleted) completedList else pendingList, modifier = Modifier.fillMaxSize().contentEdgeBlur(contentPadding.calculateBottomPadding()),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp,
                top = contentPadding.calculateTopPadding() + 12.dp,
                bottom = contentPadding.calculateBottomPadding() + 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "tabs") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(false to "待办", true to "已完成").forEach { (done, label) ->
                        FilterChip(modifier = Modifier.elasticPress(RoundedCornerShape(20.dp)), selected = showingCompleted == done, onClick = { completed = done; expanded = null },
                            label = { Text("$label ${displayedTodos.count { (it.completedAt != null) == done }}") },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = colors.surface.copy(alpha = .3f), labelColor = colors.textSecondary,
                                selectedContainerColor = colors.accentSoft, selectedLabelColor = colors.accentText))
                    }
                }
            }
            if (visible.isEmpty()) item(key = "empty") {
                Column(Modifier.fillMaxWidth().padding(vertical = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (showingCompleted) "还没有已完成的待办" else "还没有待办", color = colors.textSecondary)
                    if (!showingCompleted) TextButton(modifier = Modifier.elasticPress(CircleShape), onClick = { add() }) { Text("添加待办") }
                }
            }
            val groups = if (showingCompleted) listOf("" to visible) else groupTodos(displayedTodos, now).map { it.key.label to it.value }
            groups.forEach { (label, entries) ->
                if (label.isNotEmpty()) item(key = "group:$label") {
                    Text(label, color = colors.textSecondary, style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp))
                }
                items(entries, key = { it.id }) { todo ->
                    Box(Modifier.animateItem()) {
                    if (draft?.id == todo.id) {
                        key(todo.id) { TodoEditor(draft!!, isDarkMode, backdrop, error=error,
                            onCancel = { draft = null }, onDelete = { deleteTarget = todo },
                            onSave = { value -> viewModel.save(value) { draft = null; highlighted = value.id } }) }
                    } else TodoCard(todo, expanded == todo.id, highlighted == todo.id, now, isDarkMode, backdrop,
                        onExpand = { expanded = if (expanded == todo.id) null else todo.id },
                        onEdit = { draft = todo },
                        onLongPress = { bounds -> openContext(todo, bounds, expanded == todo.id) },
                        onComplete = {
                            viewModel.setCompleted(todo, todo.completedAt == null) {
                            scope.launch {
                                if (snack.showSnackbar(if (todo.completedAt == null) "已完成" else "已恢复待办", "撤销", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed)
                                    viewModel.setCompleted(todo, todo.completedAt != null)
                            }
                            }
                        })
                    }
                }
            }
        }
        }
        TodoFabSheet(isDarkMode, backdrop, contentPadding, sheetRequested, editing = draft != null,
            onRequestChange = { sheetRequested = it }, saveError = error, existingTodoIds = todos.map { it.id }.toSet(),
            onPending = { pendingInsert = it },
            onSave = { value, success -> viewModel.save(value, success) },
            onReveal = { value -> pendingInsert = null; completed = false; highlighted = value.id })
        SnackbarHost(snack, Modifier.align(Alignment.BottomCenter).padding(bottom = contentPadding.calculateBottomPadding() + 80.dp)) { data ->
            Snackbar(containerColor = colors.surfaceRaised, contentColor = colors.textPrimary,
                action = {
                    data.visuals.actionLabel?.let { label ->
                        TextButton(modifier = Modifier.elasticPress(CircleShape), onClick = { data.performAction() }) {
                            Text(label, color = colors.accentText)
                        }
                    }
                }) { Text(data.visuals.message, color = colors.textPrimary) }
        }
    }
    deleteTarget?.let { target -> AlertDialog(onDismissRequest = { deleteTarget = null }, title = { Text("删除待办？") },
        text = { Text(target.title) }, confirmButton = { TextButton(modifier = Modifier.elasticPress(CircleShape), onClick = { viewModel.delete(target) { deleteTarget = null; draft = null } }) { Text("删除") } },
        dismissButton = { TextButton(modifier = Modifier.elasticPress(CircleShape), onClick = { deleteTarget = null }) { Text("取消") } }) }
}

private fun deadline(todo: Todo, now: Long): String {
    todo.completedAt?.let { return "已完成 ${completedTodoDays(it, now)} 天" }
    val due = todo.dueAt ?: return "未设置截止时间"
    val difference = due - now
    return when {
        difference < 0 -> "已逾期 ${ceil(abs(difference) / 86_400_000.0).toLong()} 天"
        difference == 0L -> "已到截止时间"
        difference < 3_600_000 -> "还剩 ${ceil(difference / 60_000.0).toLong()} 分钟"
        difference < 86_400_000 -> "还剩 ${ceil(difference / 3_600_000.0).toLong()} 小时"
        else -> "还剩 ${ceil(difference / 86_400_000.0).toLong()} 天"
    }
}
private fun dateLabel(time: Long) = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TodoCard(todo: Todo, expanded: Boolean, highlighted: Boolean, now: Long, dark: Boolean, backdrop: GlassBackdrop,
    onExpand: () -> Unit, onEdit: () -> Unit, onLongPress: (Rect) -> Unit, onComplete: () -> Unit,
    modifier: Modifier = Modifier, interactive: Boolean = true) {
    val colors = appColors(dark)
    val reduced = !android.animation.ValueAnimator.areAnimatorsEnabled()
    val p by animateFloatAsState(if (expanded) 1f else 0f, tween(if (reduced) 0 else 420, easing = CubicBezierEasing(.4f,0f,.2f,1f)), label = "todoExpand")
    var cardBounds by remember { mutableStateOf(Rect.Zero) }
    val factor = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val bring = remember { BringIntoViewRequester() }
    var fullNotes by remember(todo.id) { mutableStateOf(false) }
    LaunchedEffect(expanded) { if (expanded && interactive) { delay(if (reduced) 0 else 430); bring.bringIntoView() } }
    fun layer(start: Float, end: Float) = ((p - start)/(end-start)).coerceIn(0f,1f)
    GlassSurface(backdrop, dark, modifier.fillMaxWidth().height(((88 + 180*p)*factor).dp).bringIntoViewRequester(bring)
        .onGloballyPositioned { cardBounds = it.boundsInRoot() }
        .combinedClickable(enabled = interactive, onClick = {}, onLongClickLabel = "待办选项", onLongClick = { onLongPress(cardBounds) })
        .then(if(highlighted) Modifier.border(2.dp, colors.accent, RoundedCornerShape(24.dp)) else Modifier),
        shape = RoundedCornerShape(24.dp), corner = 24.dp) {
        Column {
            Row(Modifier.fillMaxWidth().height((88*factor).dp).elasticPress(RoundedCornerShape(24.dp), enabled = interactive).semantics { stateDescription = if (expanded) "已展开" else "已收起" }.combinedClickable(enabled = interactive, onClick = onExpand, onLongClickLabel = "待办选项", onLongClick = { onLongPress(cardBounds) }).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(modifier = Modifier.elasticPress(CircleShape, enabled = interactive), enabled = interactive, onClick = onComplete) { Icon(if(todo.completedAt != null) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    if(todo.completedAt != null) "恢复待办" else "完成待办", tint = colors.accent) }
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isTodoPinned(todo)) Icon(Icons.Default.PushPin, "已置顶", Modifier.padding(end = 4.dp).size(14.dp), tint = colors.accentText)
                        Text(todo.title, color = colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    }
                    Text(deadline(todo, now), color = if(todo.completedAt == null && todo.dueAt != null && todo.dueAt <= now) colors.danger else colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                }
                Icon(Icons.Default.ExpandMore, null, tint = colors.textMuted, modifier = Modifier.padding(12.dp).graphicsLayer { rotationZ = p*180 })
            }
            if(p > 0f) Column(Modifier.fillMaxWidth().height((76*factor).dp).padding(horizontal = 20.dp).graphicsLayer { alpha = layer(.15f,.45f) }) {
                if (todo.dueAt != null) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("创建", style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
                        Icon(Icons.Default.Flag, "截止", Modifier.size(16.dp), tint = colors.accent)
                    }
                    LinearProgressIndicator(progress = { (todoTimeProgress(todo, now) ?: 0f)*layer(.15f,.45f) }, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), color = colors.accent, trackColor = colors.accentSoft)
                    Text("${deadline(todo, now)} · ${dateLabel(todo.dueAt)} 截止", color = colors.textSecondary, style = MaterialTheme.typography.labelMedium)
                } else Text("未设置截止时间", color = colors.textMuted)
            }
            if(p > 0f) Column(Modifier.fillMaxWidth().height((104*factor).dp).padding(horizontal = 20.dp)) {
                Box(Modifier.weight(1f).fillMaxWidth().graphicsLayer { alpha = layer(.4f,.7f) }.elasticPress(RoundedCornerShape(8.dp), enabled = interactive).combinedClickable(enabled = interactive, onClick = { fullNotes = !fullNotes }, onLongClickLabel = "待办选项", onLongClick = { onLongPress(cardBounds) })) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        todo.imagePath?.let { path ->
                            val bitmap by produceState<android.graphics.Bitmap?>(null,path) {
                                value = withContext(Dispatchers.IO) {
                                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds=true }
                                    BitmapFactory.decodeFile(path,bounds)
                                    val options = BitmapFactory.Options().apply { inSampleSize=(maxOf(bounds.outWidth,bounds.outHeight)/160).coerceAtLeast(1) }
                                    BitmapFactory.decodeFile(path,options)
                                }
                            }
                            bitmap?.let { Image(it.asImageBitmap(),"待办附件",Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),contentScale=androidx.compose.ui.layout.ContentScale.Crop) }
                        }
                        Text(todo.notes.ifBlank { "暂无备注" }, color = colors.textSecondary, style = MaterialTheme.typography.bodySmall,
                            maxLines = if(fullNotes) Int.MAX_VALUE else 2, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).then(if(fullNotes) Modifier.verticalScroll(rememberScrollState()) else Modifier))
                    }
                }
                Row(Modifier.heightIn(min=48.dp).graphicsLayer { alpha = layer(.65f,.95f) }, verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        parseTodoTags(todo.tags).forEach { TodoTagChip(it, dark) }
                    }
                    TextButton(modifier = Modifier.elasticPress(CircleShape, enabled = expanded && interactive), onClick = onEdit, enabled = expanded && interactive) { Text("编辑", color=colors.accentText) }
                }
            }
        }
    }
}

@Composable
private fun TodoEditor(initial: Todo, dark: Boolean, backdrop: GlassBackdrop, error: String?,
    onCancel: () -> Unit, onDelete: () -> Unit, onSave: (Todo) -> Unit) {
    var saving by remember(initial.id) { mutableStateOf(false) }
    LaunchedEffect(error) { if(error != null) saving = false }
    BackHandler(saving) { /* A pending write must finish before this editor can be dismissed. */ }
    GlassSurface(backdrop, dark, Modifier.fillMaxWidth(), shape=RoundedCornerShape(24.dp),corner=24.dp,contentPadding=PaddingValues(16.dp)) {
        TodoManualFields(initial, dark, progress = 1f, editing = true, enabled = !saving,
            onCancel = onCancel, onDelete = onDelete, onSave = { value ->
                if(!saving) { saving = true; onSave(value) }
            })
    }
}
