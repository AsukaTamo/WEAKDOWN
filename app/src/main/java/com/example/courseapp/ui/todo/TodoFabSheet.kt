package com.example.courseapp.ui.todo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import com.example.courseapp.data.model.parseTodoTags
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.example.courseapp.data.model.Todo
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.profile.GlassSurface
import com.example.courseapp.ui.theme.appColors
import io.github.itshedimisawi.vitro.GlassBackdrop
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// At 60 Hz four frames are approximately 67 ms. Drive each layer from the same reversible clock.
internal fun todoSheetLayer(clock: Float, index: Int): Float =
    ((clock.coerceIn(0f,1f) * 480f - index * (4000f / 60f)) / 140f).coerceIn(0f,1f)

internal fun todoSheetGeometry(clock: Float): Float =
    CubicBezierEasing(.2f,.9f,.22f,1f).transform(clock.coerceIn(0f,1f))

private data class SheetLayout(val width: Dp, val height: Dp, val top: Dp)

@Composable
internal fun TodoFabSheet(dark: Boolean, backdrop: GlassBackdrop, padding: PaddingValues, requested: Boolean, editing: Boolean,
    onRequestChange: (Boolean) -> Unit, saveError: String?, existingTodoIds: Set<String>, onPending: (String?) -> Unit,
    onSave: (Todo, () -> Unit) -> Unit, onReveal: (Todo) -> Unit) {
    val colors = appColors(dark)
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val density = LocalDensity.current
    val ime = WindowInsets.ime
    val progress = remember { Animatable(0f) }
    var draftId by rememberSaveable { mutableStateOf(java.util.UUID.randomUUID().toString()) }
    var lastOpenLayout by remember { mutableStateOf<SheetLayout?>(null) }
    var closingLayout by remember { mutableStateOf<SheetLayout?>(null) }
    var saving by remember { mutableStateOf(false) }
    val savedTodoSaver = listSaver<Todo?, Any>(
        save = { todo -> if(todo == null) emptyList() else listOf(todo.id,todo.title,todo.createdAt,todo.dueAt ?: -1L,todo.notes,todo.tags,todo.completedAt ?: -1L,todo.imagePath ?: "",todo.isPinned) },
        restore = { values -> if(values.isEmpty()) null else Todo(values[0] as String,values[1] as String,values[2] as Long,
            (values[3] as Long).takeUnless { it == -1L },values[4] as String,values[5] as String,
            (values[6] as Long).takeUnless { it == -1L },(values[7] as String).ifBlank { null },values.getOrNull(8) as? Boolean ?: false) })
    var saved by rememberSaveable(stateSaver=savedTodoSaver) { mutableStateOf<Todo?>(null) }
    val reduced = !android.animation.ValueAnimator.areAnimatorsEnabled()
    var collapsed by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(editing) { collapsed = editing }
    val slide by animateDpAsState(if (collapsed && !requested) 44.dp else 0.dp,
        if (reduced) snap() else spring(dampingRatio = .7f, stiffness = Spring.StiffnessMediumLow), label = "todoFabSlide")
    val isRequested by rememberUpdatedState(requested)
    fun close() { if (!saving) { closingLayout = lastOpenLayout; keyboard?.hide(); onRequestChange(false) } }
    BackHandler(requested || progress.value > 0f) { close() }
    LaunchedEffect(saveError) { if (saveError != null) { saving = false; onPending(null) } }
    LaunchedEffect(existingTodoIds, draftId) {
        // A save may complete after this page leaves composition, losing its old callback state.
        if (!saving && saved == null && draftId in existingTodoIds) {
            onPending(null)
            onRequestChange(false)
            draftId = java.util.UUID.randomUUID().toString()
        }
    }
    LaunchedEffect(requested) {
        if (requested) closingLayout = null
        if (!requested) {
            if(closingLayout == null) closingLayout = lastOpenLayout
            keyboard?.hide()
            // Frozen geometry lets IME dismissal and the reverse morph run together without reflow.
        }
        progress.animateTo(if(requested) 1f else 0f,
            tween(if(reduced) 0 else 480, easing=LinearEasing))
        if (!isRequested && progress.value == 0f) {
            focus.clearFocus()
            closingLayout = null
            saved?.let { onReveal(it); saved = null; draftId = java.util.UUID.randomUUID().toString() }
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val clock = progress.value
        val p = todoSheetGeometry(clock)
        val bottom = padding.calculateBottomPadding() + 16.dp
        val imeBottom = with(density) { ime.getBottom(this).toDp() }
        val openBottom = maxOf(bottom, imeBottom + 12.dp)
        val livePanelWidth = (maxWidth - 32.dp).coerceAtLeast(56.dp)
        val availableHeight = (maxHeight - openBottom - padding.calculateTopPadding() - 12.dp).coerceAtLeast(56.dp)
        val livePanelHeight = minOf(560.dp, availableHeight)
        val liveLayout = SheetLayout(livePanelWidth,livePanelHeight,maxHeight-openBottom-livePanelHeight)
        SideEffect { if(requested) lastOpenLayout = liveLayout }
        val layout = if(!requested) closingLayout ?: liveLayout else liveLayout
        val panelWidth = layout.width
        val panelHeight = layout.height
        val width = 56.dp + (panelWidth - 56.dp) * p
        val height = 56.dp + (panelHeight - 56.dp) * p
        val left = (maxWidth - 76.dp) + (16.dp - (maxWidth - 76.dp)) * p
        val yClosed = maxHeight - bottom - 56.dp
        val yOpen = layout.top
        val top = yClosed + (yOpen - yClosed) * p
        val corner = 28.dp + (24.dp - 28.dp) * p
        // The dismiss handler is an ancestor: Vitro's shared sibling hit testing must not
        // route a field tap to a separate scrim. When closed this parent has no input handler.
        val dismissInteraction = remember { MutableInteractionSource() }
        Box(Modifier.fillMaxSize().then(if(p > 0f || requested)
            Modifier.background(colors.glassScrim.copy(alpha=.15f*p))
                .clickable(interactionSource=dismissInteraction, indication=null) { close() }
            else Modifier)) {
        // One persistent glass surface is shared by the FAB and the expanded form.
        GlassSurface(backdrop, dark,
            Modifier.offset(left + slide,top).size(width,height)
                .pointerInput(requested, editing, collapsed) {
                    if (!requested && !editing && !collapsed) {
                        var drag = 0f
                        detectHorizontalDragGestures(onDragEnd = {
                            if (drag > 24.dp.toPx()) collapsed = true
                            drag = 0f
                        }, onHorizontalDrag = { change, amount -> drag += amount; change.consume() })
                    }
                }
                .then(if(requested) Modifier.clickable(interactionSource=remember { MutableInteractionSource() }, indication=null) {} else Modifier)
                .elasticPress(RoundedCornerShape(corner), enabled=p == 0f)
                .then(if(saved == null && (p == 0f || !requested)) Modifier.semantics { contentDescription=if (collapsed) "展开添加按钮" else "添加待办" }
                    .clickable(enabled = !editing, interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (collapsed) collapsed = false else onRequestChange(true)
                    } else Modifier),
            shape=RoundedCornerShape(corner),corner=corner) {
            Icon(if (collapsed && p == 0f) Icons.Default.ChevronLeft else Icons.Default.Add, null, tint=colors.accent,
                modifier=Modifier.align(if (collapsed && p == 0f) Alignment.CenterStart else Alignment.Center)
                    .then(if (collapsed && p == 0f) Modifier.padding(start = 4.dp) else Modifier)
                    .size(28.dp).graphicsLayer { alpha=1f-(p*5f).coerceIn(0f,1f); rotationZ=45f*p })
            // Keep fields composed across close/open so cancelling retains the unsaved draft.
            key(draftId) {
                Column(Modifier.wrapContentSize(Alignment.TopStart,unbounded=true).requiredSize(panelWidth,panelHeight).padding(16.dp)
                    .pointerInput(requested,saving,saved != null,p == 0f) {
                        if(p > 0f && (!requested || saving || saved != null)) awaitPointerEventScope {
                            var pressOrigin: androidx.compose.ui.geometry.Offset? = null
                            while(true) {
                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                event.changes.forEach { change ->
                                    if(change.pressed && !change.previousPressed) pressOrigin = change.position
                                    pressOrigin?.let { origin ->
                                        if((change.position-origin).getDistance() > viewConfiguration.touchSlop) pressOrigin = null
                                    }
                                    if(!change.pressed && change.previousPressed) {
                                        val reopen = pressOrigin != null && !requested && !saving && saved == null
                                        pressOrigin = null
                                        if(reopen) onRequestChange(true)
                                    }
                                    change.consume()
                                }
                            }
                        }
                    }
                    .then(if(!requested || p == 0f) Modifier.clearAndSetSemantics {} else Modifier)
                    .graphicsLayer { alpha=todoSheetLayer(clock,0) }
                    .verticalScroll(rememberScrollState())) {
                    TodoManualFields(Todo(id=draftId,title=""), dark, clock, false,
                        onCancel=::close, enabled = !saving && saved == null,
                        onSave={ todo ->
                            if(!saving && saved == null) {
                                saving=true
                                onPending(todo.id)
                                onSave(todo) {
                                    closingLayout = lastOpenLayout
                                    saved=todo; saving=false
                                    keyboard?.hide(); onRequestChange(false)
                                }
                            }
                        })
                }
            }
        }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TodoManualFields(initial: Todo, dark: Boolean, progress: Float, editing: Boolean,
    onCancel: () -> Unit, onDelete: () -> Unit = {}, enabled: Boolean = true, onSave: (Todo) -> Unit) {
    val colors = appColors(dark)
    var title by rememberSaveable(initial.id) { mutableStateOf(initial.title) }
    var notes by rememberSaveable(initial.id) { mutableStateOf(initial.notes) }
    var tags by rememberSaveable(initial.id) { mutableStateOf(parseTodoTags(initial.tags).joinToString(",")) }
    var customOpen by rememberSaveable(initial.id) { mutableStateOf(false) }
    var customInput by rememberSaveable(initial.id) { mutableStateOf("") }
    var due by rememberSaveable(initial.id) { mutableStateOf(initial.dueAt) }
    val context = LocalContext.current
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor=colors.textPrimary, unfocusedTextColor=colors.textPrimary, disabledTextColor=colors.textPrimary, disabledLabelColor=colors.textSecondary, disabledBorderColor=colors.outline, disabledContainerColor=colors.surface.copy(alpha=.3f),
        cursorColor=colors.accent, focusedLabelColor=colors.accentText, unfocusedLabelColor=colors.textSecondary,
        focusedBorderColor=colors.accent, unfocusedBorderColor=colors.outline,
        focusedContainerColor=colors.surface.copy(alpha=.3f),unfocusedContainerColor=colors.surface.copy(alpha=.3f))
    fun layer(index: Int) = Modifier.fillMaxWidth().graphicsLayer { alpha=todoSheetLayer(progress,index) }
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text(if(editing) "编辑待办" else "新增待办", color=colors.textPrimary,fontWeight=FontWeight.SemiBold,
            modifier=layer(0))
        OutlinedTextField(title,{title=it},label={Text("标题",color=colors.textSecondary)},
            modifier=layer(0),enabled=enabled,colors=fieldColors,maxLines=3)
        Row(layer(1),verticalAlignment=Alignment.CenterVertically) {
            TextButton(modifier=Modifier.weight(1f).elasticPress(CircleShape, enabled=enabled),enabled=enabled,onClick={
                val current=Instant.ofEpochMilli(due ?: System.currentTimeMillis()).atZone(ZoneId.systemDefault())
                DatePickerDialog(context,{ _,y,m,d -> TimePickerDialog(context,{ _,h,min ->
                    due=java.time.LocalDateTime.of(y,m+1,d,h,min).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                },current.hour,current.minute,true).show() },current.year,current.monthValue-1,current.dayOfMonth).show()
            }) { Text(due?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm")) } ?: "设置截止时间（可选）",color=colors.accentText) }
            if(due != null) IconButton(modifier=Modifier.elasticPress(CircleShape, enabled=enabled),enabled=enabled,onClick={due=null}) { Icon(Icons.Default.Close,"清除截止时间",tint=colors.textSecondary) }
        }
        OutlinedTextField(notes,{notes=it},label={Text("备注",color=colors.textSecondary)},modifier=layer(2),enabled=enabled,colors=fieldColors,minLines=2,maxLines=6)
        Column(layer(3),verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("标签（可多选）",color=colors.textSecondary,style=MaterialTheme.typography.labelMedium)
            val selected = parseTodoTags(tags)
            FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp),verticalArrangement=Arrangement.spacedBy(2.dp)) {
                TodoPresetTags.forEach { tag ->
                    TodoTagChip(tag,dark,tag in selected,enabled=enabled,onClick={
                        tags=(if(tag in selected) selected-tag else selected+tag).joinToString(",")
                    })
                }
                TodoTagChip("自定义",dark,customOpen,enabled=enabled,onClick={customOpen=!customOpen})
            }
            val customs=selected.filter { it !in TodoPresetTags }
            if(customs.isNotEmpty()) FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                customs.forEach { tag -> TodoTagChip(tag,dark,enabled=enabled,onClick={tags=(selected-tag).joinToString(",")}) }
            }
            if(customOpen) {
                OutlinedTextField(customInput,{customInput=it},label={Text("自定义标签",color=colors.textSecondary)},
                    supportingText={Text("多个标签可用逗号、分号或换行分隔",color=colors.textMuted)},
                    modifier=Modifier.fillMaxWidth(),enabled=enabled,colors=fieldColors,maxLines=3)
                TextButton(modifier=Modifier.elasticPress(CircleShape,enabled=enabled && customInput.isNotBlank()),
                    enabled=enabled && customInput.isNotBlank(),onClick={
                        tags=(selected+parseTodoTags(customInput).filter { it != "自定义" }).distinct().joinToString(",")
                        customInput=""
                    }) { Text("添加标签",color=colors.accentText) }
            }
        }
        Row(layer(4),horizontalArrangement=Arrangement.End) {
            if(editing) TextButton(modifier=Modifier.elasticPress(CircleShape, enabled=enabled),enabled=enabled,onClick=onDelete) { Text("删除",color=colors.danger) }
            TextButton(modifier=Modifier.elasticPress(CircleShape, enabled=enabled),enabled=enabled,onClick=onCancel) { Text("取消",color=colors.accentText) }
            Button(modifier=Modifier.elasticPress(CircleShape,enabled=enabled && title.isNotBlank()),
                enabled=enabled && title.isNotBlank(),colors=ButtonDefaults.buttonColors(containerColor=colors.accent,contentColor=colors.accentOn),
                onClick={
                    val pendingCustom = if(customOpen) parseTodoTags(customInput).filter { it != "自定义" } else emptyList()
                    val savedTags = (parseTodoTags(tags) + pendingCustom).distinct().joinToString(",")
                    onSave(initial.copy(title=title.trim(),notes=notes,tags=savedTags,dueAt=due))
                }) { Text("保存",color=if(enabled && title.isNotBlank()) colors.accentOn else colors.textMuted) }
        }
    }
}
