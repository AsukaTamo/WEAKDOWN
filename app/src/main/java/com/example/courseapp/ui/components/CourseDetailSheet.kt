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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val SheetPeekHeight = 260.dp
private val SheetMaxHeightCap = 760.dp
private const val SheetMaxHeightFraction = 0.92f
private val SheetCorner = 28.dp

/** 阻尼系数越小，越界拖动时"越拉越沉"的感觉越明显。 */
private const val RubberBandCoefficient = 0.55f

/** 超过这个速度（dp/s）就按甩动方向吸附到下一档。 */
private const val FlingVelocityDp = 900f

/** 超过这个速度（dp/s）直接关闭。 */
private const val DismissVelocityDp = 1600f

private val Days = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

/**
 * 课程详情底部抽屉：
 * - 两档锚点：peek（露出 360dp）与 expanded（最高 92% 屏高 / 760dp）
 * - 拖动越界时做阻尼橡皮筋，不会硬停
 * - 松手按竖向速度决定吸附方向，速度不够则回最近锚点
 * - 快速下甩或在 peek 以下拖过阈值即关闭
 */
@Composable
fun CourseDetailSheet(
    course: Course?,
    isDarkMode: Boolean,
    timeSlotLabels: List<String>,
    backdrop: GlassBackdrop,
    glass: Glass,
    onDismiss: () -> Unit,
    onSave: (Course) -> Unit,
    onDelete: (Course) -> Unit,
    onCopy: (Course) -> Unit = {}
) {
    if (course == null) return

    val palette = contentPalette(isDarkMode)
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    var isEditing by remember(course.id) { mutableStateOf(false) }

    // 返回键先退出编辑态，再关闭抽屉，避免误按一下就把改动全丢了。
    BackHandler {
        if (isEditing) isEditing = false else onDismiss()
    }

    var editName by remember(course) { mutableStateOf(course.name) }
    var editTeacher by remember(course) { mutableStateOf(course.teacher) }
    var editLocation by remember(course) { mutableStateOf(course.location) }
    var editWeekRange by remember(course) { mutableStateOf(course.weekRange) }
    var editCredits by remember(course) { mutableStateOf(course.credits.toString()) }
    var editNotes by remember(course) { mutableStateOf(course.notes) }
    var editDay by remember(course) { mutableIntStateOf(course.dayOfWeek) }
    var editType by remember(course) { mutableStateOf(course.type) }
    var editStartSlot by remember(course) { mutableIntStateOf(course.startSlot) }
    var editSlotCount by remember(course) { mutableIntStateOf(course.slotCount) }

    var offsetY by remember(course.id) { mutableFloatStateOf(Float.MAX_VALUE / 8f) }
    var sheetHeightPx by remember(course.id) { mutableFloatStateOf(0f) }
    var entered by remember(course.id) { mutableStateOf(false) }
    var dismissing by remember(course.id) { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
    ) {
        val containerPx = constraints.maxHeight.toFloat()
        val containerDp = with(density) { containerPx.toDp() }
        val sheetMaxHeightDp = minOf(containerDp * SheetMaxHeightFraction, SheetMaxHeightCap)
        val contentMaxHeightDp = (sheetMaxHeightDp - 190.dp).coerceAtLeast(220.dp)
        val peekPx = with(density) { SheetPeekHeight.toPx() }
        val peekOffset = (sheetHeightPx - peekPx).coerceAtLeast(0f)
        val flingPx = with(density) { FlingVelocityDp.dp.toPx() }
        val dismissVelocityPx = with(density) { DismissVelocityDp.dp.toPx() }
        val dismissPastPeek = peekPx * 0.32f

        suspend fun animateTo(target: Float, initialVelocity: Float = 0f) {
            animate(
                initialValue = offsetY,
                targetValue = target,
                initialVelocity = initialVelocity,
                animationSpec = spring(
                    dampingRatio = 0.84f,
                    stiffness = 360f,
                    visibilityThreshold = 1f
                )
            ) { value, _ -> offsetY = value }
        }

        suspend fun dismissSheet() {
            if (dismissing) return
            dismissing = true
            animate(
                initialValue = offsetY,
                targetValue = sheetHeightPx + with(density) { 48.dp.toPx() },
                animationSpec = tween(durationMillis = 220, easing = FastOutLinearInEasing)
            ) { value, _ -> offsetY = value }
            onDismiss()
        }

        suspend fun settle(velocity: Float) {
            val draggedUnderPeek = offsetY > peekOffset + dismissPastPeek
            when {
                velocity > dismissVelocityPx -> dismissSheet()
                draggedUnderPeek && velocity > -flingPx * 0.25f -> dismissSheet()
                else -> {
                    val target = when {
                        velocity < -flingPx -> 0f
                        velocity > flingPx -> peekOffset
                        abs(offsetY) <= abs(offsetY - peekOffset) -> 0f
                        else -> peekOffset
                    }
                    animateTo(target, velocity)
                }
            }
        }

        // 统一处理入场与编辑模式切换：锚点目标必须用"当前"高度实时算，
        // 不能捕获组合期的旧 peekOffset（首帧高度还是 0，会算成 0 直接跳到展开）。
        LaunchedEffect(sheetHeightPx, isEditing) {
            if (sheetHeightPx <= 0f) return@LaunchedEffect
            val peekTarget = (sheetHeightPx - peekPx).coerceAtLeast(0f)
            if (!entered) {
                entered = true
                offsetY = sheetHeightPx
                animateTo(peekTarget)
            } else {
                // 退出编辑时保持展开（内容刚变短，回 peek 会显得莫名收起）
                animateTo(0f)
            }
        }

        fun withResistance(raw: Float): Float = when {
            // 已到最高档：硬顶。再往上抬会让抽屉离开屏幕底、露出桌面，所以不允许越界。
            raw < 0f -> 0f
            // 往下拖过 peek 后开始吃阻尼，越拉越沉，松手过阈值才关闭。
            raw > peekOffset -> peekOffset + rubberBand(raw - peekOffset, containerPx)
            else -> raw
        }

        val dragState = rememberDraggableState { delta ->
            if (!dismissing) offsetY = withResistance(offsetY + delta)
        }

        // 只有"完全展开/贴顶"这一个布尔位会变，用 derivedStateOf 让重组发生在翻越阈值那一刻
        val contentScrollEnabled by remember { derivedStateOf { offsetY <= 1f } }

        // 遮罩同时作为抽屉的父层：抽屉里的按钮/拖动先消费事件，不会穿透到遮罩。
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { scope.launch { dismissSheet() } }
        ) {
            // 只负责视觉的遮罩层，alpha 在 draw 阶段读 offsetY，避免每帧重组。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        val expansion = if (peekOffset > 1f) {
                            ((peekOffset - offsetY) / peekOffset).coerceIn(0f, 1f)
                        } else {
                            1f
                        }
                        alpha = 0.30f + 0.22f * expansion
                    }
                    .background(Color.Black)
            )

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .heightIn(max = sheetMaxHeightDp)
                .onSizeChanged { sheetHeightPx = it.height.toFloat() }
                .offset { IntOffset(0, offsetY.roundToInt()) }
                .clip(RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner))
                .glassEffect(
                    backdrop = backdrop,
                    glass = glass,
                    shape = RoundedCornerShape(topStart = SheetCorner, topEnd = SheetCorner)
                )
                // 玻璃负责模糊/折射，留 12% 的半透明表面色保证正文可读性
                .background(palette.surface.copy(alpha = 0.88f))
                .draggable(
                    state = dragState,
                    orientation = Orientation.Vertical,
                    enabled = !dismissing,
                    onDragStopped = { velocity -> settle(velocity) }
                )
        ) {
            SheetHeader(
                course = course,
                isEditing = isEditing,
                palette = palette,
                onToggleEdit = { isEditing = !isEditing }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = contentMaxHeightDp)
                    // 只有完全展开时才让内容自己滚；peek 状态下整块拖动都交给抽屉，
                    // 否则用户在详情区上拉时会变成"滚动一个本来就滚不动的列表"。
                    .verticalScroll(scrollState, enabled = contentScrollEnabled)
                    .padding(horizontal = 20.dp)
                    .padding(top = 14.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEditing) {
                    EditFields(
                        palette = palette,
                        editName = editName,
                        onNameChange = { editName = it },
                        editTeacher = editTeacher,
                        onTeacherChange = { editTeacher = it },
                        editLocation = editLocation,
                        onLocationChange = { editLocation = it },
                        editWeekRange = editWeekRange,
                        onWeekRangeChange = { editWeekRange = it },
                        editCredits = editCredits,
                        onCreditsChange = { editCredits = it },
                        editNotes = editNotes,
                        onNotesChange = { editNotes = it },
                        editDay = editDay,
                        onDayChange = { editDay = it },
                        editType = editType,
                        onTypeChange = { editType = it },
                        editStartSlot = editStartSlot,
                        onStartSlotChange = { editStartSlot = it },
                        editSlotCount = editSlotCount,
                        onSlotCountChange = { editSlotCount = it },
                        periodCount = (timeSlotLabels.size / 2).coerceAtLeast(1)
                    )
                } else {
                    DetailContent(course = course, palette = palette, timeSlotLabels = timeSlotLabels)
                }
            }

            if (isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { isEditing = false },
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
                        onClick = {
                            onSave(
                                course.copy(
                                    name = editName.trim().ifEmpty { course.name },
                                    teacher = editTeacher.trim(),
                                    location = editLocation.trim(),
                                    weekRange = editWeekRange.trim().ifEmpty { course.weekRange },
                                    credits = editCredits.toFloatOrNull() ?: 0f,
                                    notes = editNotes,
                                    dayOfWeek = editDay,
                                    type = editType,
                                    startSlot = editStartSlot,
                                    slotCount = editSlotCount
                                )
                            )
                        },
                        modifier = Modifier
                            .weight(1.4f)
                            .height(48.dp)
                            .elasticPress(RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("保存修改", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { onCopy(course) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .elasticPress(RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.45f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.accent)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("复制", fontSize = 14.sp)
                    }
                    OutlinedButton(
                        onClick = { onDelete(course) },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .elasticPress(RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, palette.destructive),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.destructive)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(17.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("删除", fontSize = 14.sp)
                    }
                }
            }
        }
        }
    }
}

/**
 * 阻尼函数：越界距离越大，实际位移增长越慢，并收敛在 [dimension] 之内。
 * 取自 iOS 等平台的 rubber-band 公式，手感和系统抽屉一致。
 */
private fun rubberBand(distance: Float, dimension: Float): Float {
    if (distance <= 0f || dimension <= 0f) return 0f
    return (1f - 1f / (distance * RubberBandCoefficient / dimension + 1f)) * dimension
}

@Composable
private fun SheetHeader(
    course: Course,
    isEditing: Boolean,
    palette: ContentPalette,
    onToggleEdit: () -> Unit
) {
    val accent = courseGradientColors(course, false).first

    Column(modifier = Modifier.fillMaxWidth()) {
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
                .padding(start = 20.dp, end = 10.dp, top = 14.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = course.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${Days.getOrElse(course.dayOfWeek) { "未知" }} · ${course.type.displayName}",
                    fontSize = 13.sp,
                    color = palette.muted,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            IconButton(
                onClick = onToggleEdit,
                modifier = Modifier
                    .size(40.dp)
                    .elasticPress(CircleShape)
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Close else Icons.Default.Edit,
                    contentDescription = if (isEditing) "退出编辑" else "编辑课程",
                    tint = palette.accent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(color = palette.divider, thickness = 0.8.dp)
    }
}

@Composable
private fun DetailContent(
    course: Course,
    palette: ContentPalette,
    timeSlotLabels: List<String>
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        DetailRow("授课教师", course.teacher.ifBlank { "未填写" }, palette)
        DetailRow("上课地点", course.location.ifBlank { "未填写" }, palette)
        DetailRow(
            "上课时间",
            "${Days.getOrElse(course.dayOfWeek) { "未知" }} ${timeSlotLabel(timeSlotLabels, course.startSlot, course.slotCount)}",
            palette
        )
        DetailRow("节次", "第${course.startSlot + 1}–${course.startSlot + course.slotCount}节", palette)
        DetailRow("周次范围", course.weekRange.ifBlank { "未填写" }, palette)
        DetailRow("课程类型", course.type.displayName, palette)
        if (course.credits > 0f) {
            DetailRow("学分", formatCredits(course.credits), palette)
        }
        if (course.notes.isNotBlank()) {
            DetailRow("备注", course.notes, palette)
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

private fun formatCredits(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else value.toString()

private fun timeSlotLabel(labels: List<String>, startSlot: Int, slotCount: Int): String {
    val start = labels.getOrElse(startSlot * 2) { "--:--" }
    val end = labels.getOrElse((startSlot + slotCount - 1) * 2 + 1) { "--:--" }
    return "$start - $end"
}

@Composable
private fun DetailRow(label: String, value: String, palette: ContentPalette) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = palette.muted,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = palette.title,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun EditFields(
    palette: ContentPalette,
    editName: String,
    onNameChange: (String) -> Unit,
    editTeacher: String,
    onTeacherChange: (String) -> Unit,
    editLocation: String,
    onLocationChange: (String) -> Unit,
    editWeekRange: String,
    onWeekRangeChange: (String) -> Unit,
    editCredits: String,
    onCreditsChange: (String) -> Unit,
    editNotes: String,
    onNotesChange: (String) -> Unit,
    editDay: Int,
    onDayChange: (Int) -> Unit,
    editType: CourseType,
    onTypeChange: (CourseType) -> Unit,
    editStartSlot: Int,
    onStartSlotChange: (Int) -> Unit,
    editSlotCount: Int,
    onSlotCountChange: (Int) -> Unit,
    periodCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditTextField("课程名称", editName, onNameChange, palette)
        EditTextField("授课教师", editTeacher, onTeacherChange, palette)
        EditTextField("上课地点", editLocation, onLocationChange, palette)
        EditTextField("周次范围（如 1-16周 / 1-5,7-9周）", editWeekRange, onWeekRangeChange, palette)
        EditTextField("学分", editCredits, onCreditsChange, palette)
        EditTextField("备注", editNotes, onNotesChange, palette)

        ChipRow(
            label = "上课星期",
            options = Days,
            selectedIndex = editDay,
            onSelected = onDayChange,
            palette = palette
        )
        ChipRow(
            label = "课程类型",
            options = CourseType.entries.map { it.displayName },
            selectedIndex = CourseType.entries.indexOf(editType),
            onSelected = { onTypeChange(CourseType.entries[it]) },
            palette = palette
        )
        ChipRow(
            label = "开始节次",
            options = (0 until periodCount).map { "第${it + 1}节" },
            selectedIndex = editStartSlot,
            onSelected = onStartSlotChange,
            palette = palette
        )
        ChipRow(
            label = "持续节数",
            options = (1..6).map { "${it}节" },
            selectedIndex = (editSlotCount - 1).coerceIn(0, 5),
            onSelected = { onSlotCountChange(it + 1) },
            palette = palette
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun EditTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    palette: ContentPalette
) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = palette.muted)
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(10.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.accent,
                unfocusedBorderColor = palette.surfaceBorder,
                focusedContainerColor = palette.inputSurface,
                unfocusedContainerColor = palette.inputSurface,
                focusedTextColor = palette.title,
                unfocusedTextColor = palette.title
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
        )
    }
}

@Composable
private fun ChipRow(
    label: String,
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    palette: ContentPalette
) {
    Column {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = palette.muted)
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEachIndexed { index, option ->
                val selected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .elasticPress(RoundedCornerShape(10.dp))
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) palette.accent else palette.inputSurface)
                        .clickable { onSelected(index) }
                        .padding(horizontal = 2.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        fontSize = if (options.size > 6) 11.sp else 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) Color.White else palette.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
