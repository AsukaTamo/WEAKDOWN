package com.example.courseapp.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.CourseType
import com.example.courseapp.ui.theme.ContentPalette
import com.example.courseapp.ui.theme.contentPalette
import io.github.itshedimisawi.vitro.Glass
import io.github.itshedimisawi.vitro.GlassBackdrop
import io.github.itshedimisawi.vitro.glassEffect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val QuickAddSheetCorner = 28.dp
private const val QuickAddDismissVelocityDp = 1100f
private const val QuickAddDismissFraction = 0.28f

private val QuickAddDays = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/**
 * 首页长按空白格拖选后传入的添加草稿：
 * 星期与节次范围由手势决定，[week] 是打开面板时正在浏览的周，用来做默认周次。
 */
data class QuickAddDraft(
    val dayOfWeek: Int,
    val startSlot: Int,
    val slotCount: Int,
    val week: Int,
    val semesterId: String
) {
    val endSlot: Int get() = startSlot + slotCount - 1
}

/**
 * 课程快捷添加面板。
 *
 * 与课程详情抽屉共用视觉语言：液态玻璃底 + 顶部圆角 + 弹簧入场，
 * 遮罩透明度、拖拽手感也与详情抽屉一致；松手后已经选好的星期/节次以只读摘要呈现。
 */
@Composable
fun QuickAddCourseSheet(
    draft: QuickAddDraft?,
    isDarkMode: Boolean,
    timeSlotLabels: List<String>,
    backdrop: GlassBackdrop,
    glass: Glass,
    onDismiss: () -> Unit,
    onCheckConflicts: suspend (Course) -> List<Course>,
    onSave: (Course, Boolean) -> Unit
) {
    if (draft == null) return

    val palette = contentPalette(isDarkMode)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(topStart = QuickAddSheetCorner, topEnd = QuickAddSheetCorner)

    var name by remember(draft) { mutableStateOf("") }
    var teacher by remember(draft) { mutableStateOf("") }
    var location by remember(draft) { mutableStateOf("") }
    var weekRange by remember(draft) { mutableStateOf("${draft.week}周") }
    var type by remember(draft) { mutableStateOf(CourseType.REQUIRED) }
    var nameError by remember(draft) { mutableStateOf(false) }
    var saving by remember(draft) { mutableStateOf(false) }
    var pendingConflict by remember(draft) { mutableStateOf<Pair<Course, List<Course>>?>(null) }

    var sheetHeightPx by remember(draft) { mutableFloatStateOf(0f) }
    // 初始值放到屏幕外：首帧测量完成前不会在最终位置闪一下
    var offsetY by remember(draft) { mutableFloatStateOf(Float.MAX_VALUE / 8f) }
    var dismissing by remember(draft) { mutableStateOf(false) }
    val focusRequester = remember(draft) { FocusRequester() }
    val scrollState = rememberScrollState()

    fun buildCourse(): Course = Course(
        name = name.trim(),
        teacher = teacher.trim(),
        location = location.trim(),
        dayOfWeek = draft.dayOfWeek,
        startSlot = draft.startSlot,
        slotCount = draft.slotCount,
        type = type,
        weekRange = weekRange.trim().ifEmpty { "${draft.week}周" },
        semester = draft.semesterId
    )

    suspend fun playDismiss() {
        if (dismissing) return
        dismissing = true
        val fallbackPx = with(density) { 720.dp.toPx() }
        val target = (if (sheetHeightPx > 0f) sheetHeightPx else fallbackPx) +
            with(density) { 48.dp.toPx() }
        animate(
            initialValue = offsetY,
            targetValue = target,
            animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
        ) { value, _ -> offsetY = value }
        onDismiss()
    }

    fun submit() {
        if (name.isBlank()) {
            nameError = true
            focusRequester.requestFocus()
            return
        }
        nameError = false
        val course = buildCourse()
        scope.launch {
            saving = true
            val conflicts = onCheckConflicts(course)
            saving = false
            if (conflicts.isEmpty()) {
                onSave(course, false)
                playDismiss()
            } else {
                pendingConflict = course to conflicts
            }
        }
    }

    BackHandler { scope.launch { playDismiss() } }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        val containerPx = constraints.maxHeight.toFloat()
        val containerDp = with(density) { containerPx.toDp() }
        val maxSheetHeight = minOf(containerDp * 0.94f, 760.dp)
        val dismissPx = with(density) { QuickAddDismissVelocityDp.dp.toPx() }

        // 测量到真实高度后，从屏幕下方弹上来；与详情抽屉同一档高阻尼 spring。
        // key 用 draft 而不是高度：键盘弹出会改变容器/面板高度，不能把入场动画打断在半路。
        LaunchedEffect(draft) {
            val measured = snapshotFlow { sheetHeightPx }.first { it > 0f }
            offsetY = measured + with(density) { 48.dp.toPx() }
            animate(
                initialValue = offsetY,
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = 340f,
                    visibilityThreshold = 1f
                )
            ) { value, _ -> offsetY = value }
        }

        // 入场开始后再聚焦名称：输入法不会和弹入动画抢同一帧
        LaunchedEffect(draft) {
            kotlinx.coroutines.delay(260)
            focusRequester.requestFocus()
        }

        val dragState = rememberDraggableState { delta ->
            if (!dismissing) offsetY = (offsetY + delta).coerceAtLeast(0f)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { scope.launch { playDismiss() } }
        ) {
            // 只负责视觉的遮罩：alpha 在 draw 阶段读 offsetY，拖动时不需要重组
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val progress = if (sheetHeightPx > 0f) {
                            (1f - offsetY / sheetHeightPx).coerceIn(0f, 1f)
                        } else {
                            0f
                        }
                        alpha = 0.34f * progress
                    }
                    .background(Color.Black)
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
                    .onSizeChanged { sheetHeightPx = it.height.toFloat() }
                    .offset { IntOffset(0, offsetY.roundToInt().coerceAtLeast(0)) }
                    .clip(shape)
                    .glassEffect(backdrop = backdrop, glass = glass, shape = shape)
                    // 玻璃负责模糊/折射，留 12% 半透明表面色保证表单可读
                    .background(palette.surface.copy(alpha = 0.90f))
                    // 吸收面板空白处的点击：否则会穿透到遮罩，把正在填的表单一键关掉
                    .pointerInput(Unit) { detectTapGestures { } }
                    .draggable(
                        state = dragState,
                        orientation = Orientation.Vertical,
                        enabled = !dismissing,
                        onDragStopped = { velocity ->
                            if (dismissing) return@draggable
                            if (offsetY > sheetHeightPx * QuickAddDismissFraction ||
                                velocity > dismissPx
                            ) {
                                scope.launch { playDismiss() }
                            } else {
                                scope.launch {
                                    animate(
                                        initialValue = offsetY,
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = 0.86f,
                                            stiffness = 380f,
                                            visibilityThreshold = 1f
                                        )
                                    ) { value, _ -> offsetY = value }
                                }
                            }
                        }
                    )
            ) {
                // ── 头部 ──
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp)
                        .align(Alignment.CenterHorizontally)
                        .width(38.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(palette.muted.copy(alpha = 0.35f))
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 10.dp, top = 14.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "添加课程",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = palette.title
                        )
                        Text(
                            text = "已选好时间，填完即可保存",
                            fontSize = 12.sp,
                            color = palette.muted,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    IconButton(
                        onClick = { scope.launch { playDismiss() } },
                        modifier = Modifier
                            .size(40.dp)
                            .elasticPress(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭",
                            tint = palette.muted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(color = palette.divider, thickness = 0.8.dp)

                // ── 表单 ──
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(horizontal = 20.dp)
                        .padding(top = 14.dp, bottom = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val startTime = timeSlotLabels.getOrElse(draft.startSlot * 2) { "--:--" }
                    val endTime = timeSlotLabels.getOrElse(draft.endSlot * 2 + 1) { "--:--" }
                    val periodLabel = if (draft.slotCount > 1) {
                        "第${draft.startSlot + 1}–${draft.endSlot + 1}节"
                    } else {
                        "第${draft.startSlot + 1}节"
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(palette.accentSoft)
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Event,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${QuickAddDays.getOrElse(draft.dayOfWeek) { "" }} · $periodLabel",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.title
                            )
                            Text(
                                text = "$startTime – $endTime · 默认第${draft.week}周",
                                fontSize = 11.sp,
                                color = palette.muted,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }

                    QuickAddTextField(
                        label = "课程名称 *",
                        value = name,
                        onValueChange = {
                            name = it
                            if (nameError && it.isNotBlank()) nameError = false
                        },
                        placeholder = "请输入课程名称",
                        palette = palette,
                        focusRequester = focusRequester,
                        isError = nameError
                    )
                    if (nameError) {
                        Text(
                            text = "课程名称不能为空",
                            fontSize = 11.sp,
                            color = palette.destructive
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        QuickAddTextField(
                            label = "授课教师",
                            value = teacher,
                            onValueChange = { teacher = it },
                            placeholder = "教师姓名",
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                        QuickAddTextField(
                            label = "上课地点",
                            value = location,
                            onValueChange = { location = it },
                            placeholder = "教室",
                            palette = palette,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Column {
                        Text(
                            text = "课程类型",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = palette.muted
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CourseType.entries.forEach { option ->
                                val selected = option == type
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 40.dp)
                                        .elasticPress(RoundedCornerShape(10.dp))
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (selected) palette.accent else palette.inputSurface)
                                        .clickable { type = option }
                                        .padding(horizontal = 2.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = option.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selected) palette.onAccent else palette.title,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    QuickAddTextField(
                        label = "周次范围",
                        value = weekRange,
                        onValueChange = { weekRange = it },
                        placeholder = "如 1-18周 / 1-5,7-9周",
                        palette = palette
                    )
                }

                // ── 底部按钮 ──
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { scope.launch { playDismiss() } },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .elasticPress(RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, palette.surfaceBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.body)
                    ) {
                        Text("取消", fontSize = 14.sp)
                    }
                    Button(
                        onClick = { submit() },
                        enabled = !saving,
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .elasticPress(RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                    ) {
                        if (saving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = palette.onAccent
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("保存课程", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        pendingConflict?.let { (course, conflicts) ->
            AlertDialog(
                onDismissRequest = { pendingConflict = null },
                title = { Text("时间冲突") },
                text = {
                    Text("目标时段与以下课程冲突：${conflicts.joinToString { it.name }}。是否替换？")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            pendingConflict = null
                            onSave(course, true)
                            scope.launch { playDismiss() }
                        },
                        modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                    ) {
                        Text("替换")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { pendingConflict = null },
                        modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                    ) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
private fun QuickAddTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    palette: ContentPalette,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    isError: Boolean = false
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = palette.muted,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier),
            placeholder = {
                Text(
                    text = placeholder,
                    color = palette.muted.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            },
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isError) palette.destructive else palette.accent,
                unfocusedBorderColor = if (isError) palette.destructive else palette.surfaceBorder,
                focusedContainerColor = palette.inputSurface,
                unfocusedContainerColor = palette.inputSurface,
                focusedTextColor = palette.title,
                unfocusedTextColor = palette.title
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )
    }
}
