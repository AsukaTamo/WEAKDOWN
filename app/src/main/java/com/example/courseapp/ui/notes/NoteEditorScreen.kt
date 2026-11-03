package com.example.courseapp.ui.notes

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Redo
import androidx.compose.material.icons.outlined.Rotate90DegreesCcw
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.data.model.NormalizedRect
import com.example.courseapp.data.model.NoteAnnotationMapping
import com.example.courseapp.data.model.NoteEditorDocument
import com.example.courseapp.data.model.NotePoint
import com.example.courseapp.data.model.NoteStroke
import com.example.courseapp.data.model.NoteStrokeTool
import com.example.courseapp.data.model.NoteTextLayer
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.theme.AppColors
import com.example.courseapp.ui.theme.appColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class EditorTool { CROP, TEXT, PEN }

internal enum class CropHandle { NONE, MOVE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

private val EditorColors = listOf(
    0xFFE43D35.toInt(),
    0xFF276EC1.toInt(),
    0xFFF2B827.toInt(),
    0xFF249D68.toInt(),
    0xFF26384A.toInt()
)

/**
 * 非破坏性编辑器：
 * - 原图只读，编辑数据是 [NoteEditorDocument]（裁剪、旋转、文字层、笔迹层）；
 * - 撤销/重做保存的是文档快照，不是整张 Bitmap；
 * - 手势坐标先映射到原图归一化坐标，导出时再按输出分辨率映射，避免标注错位。
 */
@Composable
fun NoteEditorScreen(
    isDarkMode: Boolean,
    sourceFile: File,
    sourceRotation: Int,
    initialDocument: NoteEditorDocument,
    onCancel: () -> Unit,
    onDone: (NoteEditorDocument) -> Unit
) {
    val colors = appColors(isDarkMode)
    val density = LocalDensity.current
    val base = rememberNoteFileBitmap(sourceFile, sourceRotation, maxSize = 1600)
    var doc by remember { mutableStateOf(initialDocument) }
    var activeTool by remember { mutableStateOf(EditorTool.PEN) }
    var penTool by remember { mutableStateOf(NoteStrokeTool.PEN) }
    var brushColor by remember { mutableStateOf(EditorColors.first()) }
    var brushWidth by remember { mutableFloatStateOf(0.004f) }
    var fontSize by remember { mutableFloatStateOf(0.052f) }
    var selectedTextId by remember { mutableStateOf<String?>(null) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var draftCrop by remember { mutableStateOf(NormalizedRect.Full) }
    var cropRatio by remember { mutableStateOf<Float?>(null) }
    var draftCropActive by remember { mutableStateOf(false) }
    var cropHandle by remember { mutableStateOf(CropHandle.NONE) }
    var activeStrokeId by remember { mutableStateOf<String?>(null) }
    var gestureBaseline by remember { mutableStateOf<NoteEditorDocument?>(null) }
    var textDragBaseline by remember { mutableStateOf<NotePoint?>(null) }
    var textDragTotal by remember { mutableStateOf(Offset.Zero) }
    var fontSizeHistoryBase by remember { mutableStateOf<NoteEditorDocument?>(null) }

    val undoStack = remember { mutableStateListOf<NoteEditorDocument>() }
    val redoStack = remember { mutableStateListOf<NoteEditorDocument>() }

    fun pushHistory(before: NoteEditorDocument = doc) {
        undoStack.add(before)
        if (undoStack.size > 40) undoStack.removeAt(0)
        redoStack.clear()
    }

    fun beginGesture() {
        gestureBaseline = doc
        pushHistory(doc)
    }

    fun endGesture() {
        val baseline = gestureBaseline
        if (baseline != null && baseline == doc && undoStack.isNotEmpty()) {
            undoStack.removeAt(undoStack.lastIndex)
        }
        gestureBaseline = null
    }

    fun undo() {
        if (undoStack.isEmpty()) return
        redoStack.add(doc)
        doc = undoStack.removeAt(undoStack.lastIndex)
        draftCropActive = false
        selectedTextId = null
    }

    fun redo() {
        if (redoStack.isEmpty()) return
        undoStack.add(doc)
        doc = redoStack.removeAt(redoStack.lastIndex)
        draftCropActive = false
        selectedTextId = null
    }

    fun applyDraftCrop() {
        if (!draftCropActive || draftCrop.isFull) {
            draftCropActive = false
            draftCrop = NormalizedRect.Full
            return
        }
        pushHistory()
        doc = doc.withCrop(draftCrop)
        draftCrop = NormalizedRect.Full
        draftCropActive = false
        cropRatio = null
    }

    val display by produceState<Bitmap?>(null, base, doc.crop, doc.rotation) {
        val source = base
        if (source == null) {
            value = null
            return@produceState
        }
        value = withContext(Dispatchers.Default) {
            renderEditorDisplay(source, doc.rotation, doc.crop)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.canvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // 顶部：取消 / 标题 / 完成
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = {
                if (doc == initialDocument && !draftCropActive) onCancel() else showCancelDialog = true
            }) {
                Text("取消", color = colors.textSecondary, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Text(
                "编辑照片",
                color = colors.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.weight(1f))
            NotePrimaryButton(
                text = "完成",
                onClick = {
                    val finalDoc = if (draftCropActive && !draftCrop.isFull) doc.withCrop(draftCrop) else doc
                    onDone(finalDoc)
                },
                colors = colors,
                enabled = display != null
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surfaceSunken)
        ) {
            val stageWidth = with(density) { maxWidth.toPx() }
            val stageHeight = with(density) { maxHeight.toPx() }
            val bitmap = display
            if (bitmap == null || base == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else {
                val scale = min(
                    stageWidth / bitmap.width.coerceAtLeast(1),
                    stageHeight / bitmap.height.coerceAtLeast(1)
                )
                val imageWidth = bitmap.width * scale
                val imageHeight = bitmap.height * scale
                val imageRect = Rect(
                    offset = Offset(
                        (stageWidth - imageWidth) / 2f,
                        (stageHeight - imageHeight) / 2f
                    ),
                    size = Size(imageWidth, imageHeight)
                )
                val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                val measurer = rememberTextMeasurer()
                val originalWidth = base.width.toFloat()
                val originalHeight = base.height.toFloat()
                val rotatedBaseWidth = if (doc.rotation % 180 == 0) originalWidth else originalHeight
                val annotationScale = originalWidth * imageRect.width /
                    (doc.crop.width * rotatedBaseWidth.coerceAtLeast(1f))

                fun toOriginal(position: Offset): NotePoint {
                    val output = NotePoint(
                        x = ((position.x - imageRect.left) / imageRect.width).coerceIn(0f, 1f),
                        y = ((position.y - imageRect.top) / imageRect.height).coerceIn(0f, 1f)
                    )
                    return NoteAnnotationMapping.fromOutput(output, doc)
                }

                fun textRect(layer: NoteTextLayer): Rect {
                    val anchor = NoteAnnotationMapping.toOutput(NotePoint(layer.x, layer.y), doc)
                    val fontPx = layer.fontSize * annotationScale
                    val widthPx = fontPx * layer.text.length.coerceAtLeast(1) * 0.64f
                    return Rect(
                        offset = Offset(
                            imageRect.left + anchor.x * imageRect.width,
                            imageRect.top + anchor.y * imageRect.height
                        ),
                        size = Size(widthPx.coerceAtLeast(fontPx), fontPx * 1.35f)
                    )
                }

                fun hitText(position: Offset): NoteTextLayer? = doc.texts.lastOrNull { layer ->
                    textRect(layer).inflate(with(density) { 10.dp.toPx() }).contains(position)
                }

                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(activeTool, imageRect, doc.crop, doc.rotation, penTool, brushColor, brushWidth) {
                            when (activeTool) {
                                EditorTool.CROP -> detectDragGestures(
                                    onDragStart = { offset ->
                                        cropHandle = cropHitTest(offset, imageRect, draftCrop)
                                        if (cropHandle != CropHandle.NONE) {
                                            draftCropActive = true
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        if (cropHandle != CropHandle.NONE) {
                                            change.consume()
                                            draftCrop = updateDraftCrop(
                                                current = draftCrop,
                                                handle = cropHandle,
                                                dx = dragAmount.x / imageRect.width,
                                                dy = dragAmount.y / imageRect.height,
                                                ratio = cropRatio,
                                                displayAspect = imageRect.width / imageRect.height
                                            )
                                        }
                                    },
                                    onDragEnd = { cropHandle = CropHandle.NONE },
                                    onDragCancel = { cropHandle = CropHandle.NONE }
                                )

                                EditorTool.PEN -> detectDragGestures(
                                    onDragStart = { offset ->
                                        val point = toOriginal(offset)
                                        beginGesture()
                                        val stroke = NoteStroke(
                                            points = if (penTool == NoteStrokeTool.PEN) {
                                                listOf(point)
                                            } else {
                                                listOf(point, point)
                                            },
                                            color = brushColor,
                                            width = brushWidth,
                                            tool = penTool
                                        )
                                        activeStrokeId = stroke.id
                                        doc = doc.copy(strokes = doc.strokes + stroke)
                                    },
                                    onDrag = { change, _ ->
                                        val id = activeStrokeId ?: return@detectDragGestures
                                        change.consume()
                                        val point = toOriginal(change.position)
                                        doc = doc.copy(
                                            strokes = doc.strokes.map { stroke ->
                                                if (stroke.id != id) stroke
                                                else if (stroke.tool == NoteStrokeTool.PEN) {
                                                    val last = stroke.points.lastOrNull()
                                                    if (last != null && hypot(
                                                            (point.x - last.x) * imageRect.width,
                                                            (point.y - last.y) * imageRect.height
                                                        ) < 1.2f
                                                    ) {
                                                        stroke
                                                    } else {
                                                        stroke.copy(points = stroke.points + point)
                                                    }
                                                } else {
                                                    stroke.copy(points = listOf(stroke.points.first(), point))
                                                }
                                            }
                                        )
                                    },
                                    onDragEnd = {
                                        endGesture()
                                        activeStrokeId = null
                                    },
                                    onDragCancel = {
                                        endGesture()
                                        activeStrokeId = null
                                    }
                                )

                                EditorTool.TEXT -> detectDragGestures(
                                    onDragStart = { offset ->
                                        val hit = hitText(offset)
                                        if (hit != null) {
                                            selectedTextId = hit.id
                                            beginGesture()
                                            textDragBaseline = NotePoint(hit.x, hit.y)
                                            textDragTotal = Offset.Zero
                                        } else {
                                            textDragBaseline = null
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        val baseline = textDragBaseline ?: return@detectDragGestures
                                        val id = selectedTextId ?: return@detectDragGestures
                                        change.consume()
                                        textDragTotal += dragAmount
                                        val output = NoteAnnotationMapping.toOutput(baseline, doc)
                                        val moved = NotePoint(
                                            x = output.x + textDragTotal.x / imageRect.width,
                                            y = output.y + textDragTotal.y / imageRect.height
                                        )
                                        val original = NoteAnnotationMapping.fromOutput(moved, doc)
                                        doc = doc.copy(
                                            texts = doc.texts.map { layer ->
                                                if (layer.id == id) layer.copy(
                                                    x = original.x.coerceIn(0f, 1f),
                                                    y = original.y.coerceIn(0f, 1f)
                                                ) else layer
                                            }
                                        )
                                    },
                                    onDragEnd = {
                                        endGesture()
                                        textDragBaseline = null
                                    },
                                    onDragCancel = {
                                        endGesture()
                                        textDragBaseline = null
                                    }
                                )
                            }
                        }
                        .pointerInput(activeTool, imageRect, doc.crop, doc.rotation) {
                            if (activeTool == EditorTool.TEXT) {
                                detectTapGestures { offset ->
                                    selectedTextId = hitText(offset)?.id
                                }
                            }
                        }
                ) {
                    drawImage(
                        image = imageBitmap,
                        srcOffset = IntOffset.Zero,
                        srcSize = IntSize(bitmap.width, bitmap.height),
                        dstOffset = IntOffset(
                            imageRect.left.roundToInt(),
                            imageRect.top.roundToInt()
                        ),
                        dstSize = IntSize(
                            imageRect.width.roundToInt(),
                            imageRect.height.roundToInt()
                        )
                    )

                    // 笔迹：存原图坐标，绘制时映射到当前旋转/裁剪后的输出坐标。
                    doc.strokes.forEach { stroke ->
                        val mapped = stroke.points.map { point ->
                            val output = NoteAnnotationMapping.toOutput(point, doc)
                            Offset(
                                imageRect.left + output.x * imageRect.width,
                                imageRect.top + output.y * imageRect.height
                            )
                        }
                        val strokeWidth = (stroke.width * annotationScale).coerceAtLeast(1.2f)
                        val color = Color(stroke.color)
                        when (stroke.tool) {
                            NoteStrokeTool.PEN -> {
                                if (mapped.size == 1) {
                                    drawCircle(color, strokeWidth / 2f, mapped.first())
                                } else {
                                    val path = Path().apply {
                                        moveTo(mapped.first().x, mapped.first().y)
                                        mapped.drop(1).forEach { lineTo(it.x, it.y) }
                                    }
                                    drawPath(
                                        path,
                                        color,
                                        style = Stroke(strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }

                            NoteStrokeTool.CIRCLE -> {
                                if (mapped.size >= 2) {
                                    val topLeft = Offset(
                                        min(mapped[0].x, mapped[1].x),
                                        min(mapped[0].y, mapped[1].y)
                                    )
                                    val size = Size(
                                        abs(mapped[1].x - mapped[0].x),
                                        abs(mapped[1].y - mapped[0].y)
                                    )
                                    drawOval(color, topLeft, size, style = Stroke(strokeWidth))
                                }
                            }

                            NoteStrokeTool.ARROW -> {
                                if (mapped.size >= 2) {
                                    val start = mapped[0]
                                    val end = mapped[1]
                                    drawLine(color, start, end, strokeWidth, cap = StrokeCap.Round)
                                    val angle = atan2(end.y - start.y, end.x - start.x)
                                    val head = max(strokeWidth * 4.5f, 14f)
                                    listOf(angle + PI.toFloat() / 6f, angle - PI.toFloat() / 6f).forEach { a ->
                                        drawLine(
                                            color,
                                            end,
                                            Offset(end.x - head * cos(a), end.y - head * sin(a)),
                                            strokeWidth,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 文字：锚点为左上角，保持正立。
                    doc.texts.forEach { layer ->
                        val output = NoteAnnotationMapping.toOutput(NotePoint(layer.x, layer.y), doc)
                        val anchor = Offset(
                            imageRect.left + output.x * imageRect.width,
                            imageRect.top + output.y * imageRect.height
                        )
                        val fontPx = (layer.fontSize * annotationScale).coerceAtLeast(8f)
                        withTransform({
                            translate(anchor.x, anchor.y)
                            if (layer.rotation != 0f) rotate(layer.rotation, Offset.Zero)
                        }) {
                            drawText(
                                textMeasurer = measurer,
                                text = layer.text,
                                topLeft = Offset.Zero,
                                style = TextStyle(
                                    color = Color(layer.color),
                                    fontSize = fontPx.toSp(),
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // 文字选中框
                    val selected = selectedTextId?.let { id -> doc.texts.firstOrNull { it.id == id } }
                    if (selected != null && activeTool == EditorTool.TEXT) {
                        val rect = textRect(selected).inflate(6.dp.toPx())
                        drawRect(
                            color = colors.accent,
                            topLeft = rect.topLeft,
                            size = rect.size,
                            style = Stroke(1.5.dp.toPx())
                        )
                    }

                    // 裁剪遮罩
                    if (activeTool == EditorTool.CROP) {
                        val crop = draftCrop
                        val l = imageRect.left + crop.left * imageRect.width
                        val t = imageRect.top + crop.top * imageRect.height
                        val r = imageRect.left + crop.right * imageRect.width
                        val b = imageRect.top + crop.bottom * imageRect.height
                        val dim = Color.Black.copy(alpha = 0.48f)
                        drawRect(dim, imageRect.topLeft, Size(imageRect.width, t - imageRect.top))
                        drawRect(dim, Offset(imageRect.left, b), Size(imageRect.width, imageRect.bottom - b))
                        drawRect(dim, Offset(imageRect.left, t), Size(l - imageRect.left, b - t))
                        drawRect(dim, Offset(r, t), Size(imageRect.right - r, b - t))
                        drawRect(
                            color = colors.accent,
                            topLeft = Offset(l, t),
                            size = Size(r - l, b - t),
                            style = Stroke(2.dp.toPx())
                        )
                        val thirdW = (r - l) / 3f
                        val thirdH = (b - t) / 3f
                        val grid = Color.White.copy(alpha = 0.45f)
                        repeat(2) { index ->
                            drawLine(
                                grid,
                                Offset(l + thirdW * (index + 1), t),
                                Offset(l + thirdW * (index + 1), b),
                                0.8.dp.toPx()
                            )
                            drawLine(
                                grid,
                                Offset(l, t + thirdH * (index + 1)),
                                Offset(r, t + thirdH * (index + 1)),
                                0.8.dp.toPx()
                            )
                        }
                        listOf(Offset(l, t), Offset(r, t), Offset(l, b), Offset(r, b)).forEach { corner ->
                            drawCircle(Color.White, 8.dp.toPx(), corner)
                            drawCircle(colors.accent, 8.dp.toPx(), corner, style = Stroke(2.5.dp.toPx()))
                        }
                    }
                }
            }
        }

        // 工具选项
        val optionsModifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 9.dp)
        when (activeTool) {
            EditorTool.CROP -> Row(
                modifier = optionsModifier,
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                listOf(
                    null to "自由",
                    1f to "1:1",
                    4f / 3f to "4:3",
                    16f / 9f to "16:9"
                ).forEach { (ratio, label) ->
                    NoteChip(
                        text = label,
                        selected = cropRatio == ratio,
                        colors = colors,
                        onClick = {
                            cropRatio = ratio
                            if (ratio != null) {
                                val displayAspect = if (display != null) {
                                    display!!.width.toFloat() / display!!.height
                                } else 1f
                                draftCrop = fitCropToRatio(draftCrop, ratio, displayAspect)
                            }
                            draftCropActive = true
                        }
                    )
                }
                NoteIconButton(
                    icon = Icons.Outlined.Rotate90DegreesCcw,
                    description = "旋转 90 度",
                    onClick = {
                        pushHistory()
                        doc = doc.rotatedClockwise()
                        draftCropActive = false
                        draftCrop = NormalizedRect.Full
                        cropRatio = null
                    },
                    colors = colors
                )
                NoteChip(
                    text = "应用裁剪",
                    selected = true,
                    colors = colors,
                    onClick = { applyDraftCrop() }
                )
                NoteChip(
                    text = "取消",
                    selected = false,
                    colors = colors,
                    onClick = {
                        draftCropActive = false
                        draftCrop = NormalizedRect.Full
                        cropRatio = null
                    }
                )
            }

            EditorTool.PEN -> Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = optionsModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    NoteChip(
                        text = "画笔",
                        selected = penTool == NoteStrokeTool.PEN,
                        colors = colors,
                        leadingIcon = Icons.Outlined.Brush,
                        onClick = { penTool = NoteStrokeTool.PEN }
                    )
                    NoteChip(
                        text = "圈画",
                        selected = penTool == NoteStrokeTool.CIRCLE,
                        colors = colors,
                        leadingIcon = Icons.Outlined.RadioButtonUnchecked,
                        onClick = { penTool = NoteStrokeTool.CIRCLE }
                    )
                    NoteChip(
                        text = "箭头",
                        selected = penTool == NoteStrokeTool.ARROW,
                        colors = colors,
                        leadingIcon = Icons.Outlined.ArrowForward,
                        onClick = { penTool = NoteStrokeTool.ARROW }
                    )
                    Spacer(Modifier.width(4.dp))
                    EditorColorDots(colors, brushColor) { brushColor = it }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("笔触", color = colors.textMuted, fontSize = 10.sp)
                    Slider(
                        value = brushWidth,
                        onValueChange = { brushWidth = it },
                        valueRange = 0.002f..0.016f,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.surfaceSunken
                        )
                    )
                }
            }

            EditorTool.TEXT -> Column(Modifier.fillMaxWidth()) {
                Row(
                    modifier = optionsModifier,
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    NoteChip(
                        text = "添加文字",
                        selected = false,
                        colors = colors,
                        leadingIcon = Icons.Outlined.Add,
                        onClick = { showTextDialog = true }
                    )
                    EditorColorDots(colors, brushColor) { color ->
                        brushColor = color
                        val id = selectedTextId
                        if (id != null) {
                            pushHistory()
                            doc = doc.copy(
                                texts = doc.texts.map {
                                    if (it.id == id) it.copy(color = color) else it
                                }
                            )
                        }
                    }
                    val selectedLayer = doc.texts.firstOrNull { it.id == selectedTextId }
                    if (selectedLayer != null) {
                        NoteChip(
                            text = "删除文字",
                            selected = false,
                            colors = colors,
                            leadingIcon = Icons.Outlined.Delete,
                            onClick = {
                                pushHistory()
                                doc = doc.copy(texts = doc.texts.filterNot { it.id == selectedLayer.id })
                                selectedTextId = null
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("字号", color = colors.textMuted, fontSize = 10.sp)
                    Slider(
                        value = fontSize,
                        onValueChange = { newValue ->
                            fontSize = newValue
                            val id = selectedTextId
                            if (id != null) {
                                if (fontSizeHistoryBase == null) fontSizeHistoryBase = doc
                                doc = doc.copy(
                                    texts = doc.texts.map {
                                        if (it.id == id) it.copy(fontSize = newValue) else it
                                    }
                                )
                            }
                        },
                        onValueChangeFinished = {
                            val base = fontSizeHistoryBase
                            if (base != null && base != doc) {
                                undoStack.add(base)
                                if (undoStack.size > 40) undoStack.removeAt(0)
                                redoStack.clear()
                            }
                            fontSizeHistoryBase = null
                        },
                        valueRange = 0.025f..0.12f,
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accent,
                            activeTrackColor = colors.accent,
                            inactiveTrackColor = colors.surfaceSunken
                        )
                    )
                }
            }
        }

        // 工具条
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EditorToolButton(
                icon = Icons.Outlined.Crop,
                label = "裁剪",
                selected = activeTool == EditorTool.CROP,
                enabled = true,
                colors = colors,
                modifier = Modifier.weight(1f)
            ) {
                if (activeTool == EditorTool.CROP) {
                    applyDraftCrop()
                    activeTool = EditorTool.PEN
                } else {
                    draftCrop = NormalizedRect.Full
                    draftCropActive = false
                    cropRatio = null
                    activeTool = EditorTool.CROP
                }
            }
            EditorToolButton(
                icon = Icons.Outlined.TextFields,
                label = "文字",
                selected = activeTool == EditorTool.TEXT,
                enabled = true,
                colors = colors,
                modifier = Modifier.weight(1f)
            ) {
                activeTool = EditorTool.TEXT
            }
            EditorToolButton(
                icon = Icons.Outlined.Brush,
                label = "画笔",
                selected = activeTool == EditorTool.PEN,
                enabled = true,
                colors = colors,
                modifier = Modifier.weight(1f)
            ) {
                activeTool = EditorTool.PEN
            }
            EditorToolButton(
                icon = Icons.Outlined.Undo,
                label = "撤销",
                selected = false,
                enabled = undoStack.isNotEmpty(),
                colors = colors,
                modifier = Modifier.weight(1f)
            ) { undo() }
            EditorToolButton(
                icon = Icons.Outlined.Redo,
                label = "重做",
                selected = false,
                enabled = redoStack.isNotEmpty(),
                colors = colors,
                modifier = Modifier.weight(1f)
            ) { redo() }
        }
        Text(
            text = when (activeTool) {
                EditorTool.CROP -> "拖动边角调整裁剪范围；应用后仍可再次修改，原图不会被覆盖。"
                EditorTool.TEXT -> "点击「添加文字」，再拖动文字调整位置；字号只影响当前选中文字。"
                EditorTool.PEN -> "在照片上直接圈画重点；笔迹会保存为可撤销的图层。"
            },
            color = colors.textMuted,
            fontSize = 9.sp,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)
        )
        Spacer(Modifier.height(4.dp))
    }

    if (showTextDialog) {
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTextDialog = false },
            title = { Text("添加文字标注") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(60) },
                    label = { Text("文字内容") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (text.isNotBlank()) {
                        pushHistory()
                        val layer = NoteTextLayer(
                            text = text.trim(),
                            x = 0.5f,
                            y = 0.45f,
                            fontSize = fontSize,
                            color = brushColor
                        )
                        doc = doc.copy(texts = doc.texts + layer)
                        selectedTextId = layer.id
                        activeTool = EditorTool.TEXT
                    }
                    showTextDialog = false
                }) { Text("添加", color = colors.accentText) }
            },
            dismissButton = {
                TextButton(onClick = { showTextDialog = false }) { Text("取消") }
            },
            containerColor = colors.surfaceRaised
        )
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("放弃本次编辑？") },
            text = { Text("未保存的裁剪、文字和笔迹会丢失；已保存的原图不会被修改。") },
            confirmButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    onCancel()
                }) { Text("放弃", color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) { Text("继续编辑") }
            },
            containerColor = colors.surfaceRaised
        )
    }
}

@Composable
private fun EditorColorDots(colors: AppColors, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EditorColors.forEach { color ->
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceRaised)
                    .border(
                        width = if (color == selected) 2.dp else 0.dp,
                        color = if (color == selected) colors.accent else Color.Transparent,
                        shape = CircleShape
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(Color(color))
                    .clickable { onSelect(color) }
                    .semantics { contentDescription = "画笔颜色" }
            )
        }
    }
}

@Composable
private fun EditorToolButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    enabled: Boolean,
    colors: AppColors,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(13.dp)
    Column(
        modifier = modifier
            .elasticPress(shape, enabled = enabled)
            .clip(shape)
            .background(if (selected) colors.accentSoft else colors.surfaceSunken)
            .then(
                if (enabled) Modifier.clickable(onClick = onClick)
                else Modifier
            )
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = when {
                !enabled -> colors.textMuted.copy(alpha = 0.4f)
                selected -> colors.accentText
                else -> colors.textSecondary
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            color = when {
                !enabled -> colors.textMuted.copy(alpha = 0.4f)
                selected -> colors.accentText
                else -> colors.textMuted
            },
            fontSize = 9.sp
        )
    }
}

// ── 纯函数：渲染与裁剪几何 ────────────────────────────────────────────────

/** 旋转 + 裁剪出显示用 Bitmap；base 由缓存持有，不能回收。 */
internal fun renderEditorDisplay(
    base: Bitmap,
    rotation: Int,
    crop: NormalizedRect
): Bitmap {
    val normalized = ((rotation % 360) + 360) % 360
    val rotated = if (normalized == 0) {
        base
    } else {
        Bitmap.createBitmap(
            base,
            0,
            0,
            base.width,
            base.height,
            Matrix().apply { postRotate(normalized.toFloat()) },
            true
        )
    }
    val rect = crop.clamp()
    val left = (rect.left * rotated.width).roundToInt().coerceIn(0, rotated.width - 1)
    val top = (rect.top * rotated.height).roundToInt().coerceIn(0, rotated.height - 1)
    val right = (rect.right * rotated.width).roundToInt().coerceIn(left + 1, rotated.width)
    val bottom = (rect.bottom * rotated.height).roundToInt().coerceIn(top + 1, rotated.height)
    val width = right - left
    val height = bottom - top
    return if (left == 0 && top == 0 && width == rotated.width && height == rotated.height) {
        rotated
    } else {
        val cropped = Bitmap.createBitmap(rotated, left, top, width, height)
        if (rotated !== base && rotated !== cropped) rotated.recycle()
        cropped
    }
}

private fun cropHitTest(position: Offset, imageRect: Rect, crop: NormalizedRect): CropHandle {
    if (!imageRect.contains(position)) return CropHandle.NONE
    val l = imageRect.left + crop.left * imageRect.width
    val t = imageRect.top + crop.top * imageRect.height
    val r = imageRect.left + crop.right * imageRect.width
    val b = imageRect.top + crop.bottom * imageRect.height
    val radius = 56f
    return when {
        distance(position, Offset(l, t)) <= radius -> CropHandle.TOP_LEFT
        distance(position, Offset(r, t)) <= radius -> CropHandle.TOP_RIGHT
        distance(position, Offset(l, b)) <= radius -> CropHandle.BOTTOM_LEFT
        distance(position, Offset(r, b)) <= radius -> CropHandle.BOTTOM_RIGHT
        position.x in l..r && position.y in t..b -> CropHandle.MOVE
        else -> CropHandle.NONE
    }
}

private fun distance(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)

internal fun updateDraftCrop(
    current: NormalizedRect,
    handle: CropHandle?,
    dx: Float,
    dy: Float,
    ratio: Float?,
    displayAspect: Float
): NormalizedRect {
    if (handle == null || handle == CropHandle.NONE) return current
    val minSize = 0.08f
    var l = current.left
    var t = current.top
    var r = current.right
    var b = current.bottom
    when (handle) {
        CropHandle.MOVE -> {
            val width = r - l
            val height = b - t
            l = (l + dx).coerceIn(0f, 1f - width)
            t = (t + dy).coerceIn(0f, 1f - height)
            r = l + width
            b = t + height
        }

        CropHandle.TOP_LEFT -> {
            l = (l + dx).coerceIn(0f, r - minSize)
            t = (t + dy).coerceIn(0f, b - minSize)
        }

        CropHandle.TOP_RIGHT -> {
            r = (r + dx).coerceIn(l + minSize, 1f)
            t = (t + dy).coerceIn(0f, b - minSize)
        }

        CropHandle.BOTTOM_LEFT -> {
            l = (l + dx).coerceIn(0f, r - minSize)
            b = (b + dy).coerceIn(t + minSize, 1f)
        }

        CropHandle.BOTTOM_RIGHT -> {
            r = (r + dx).coerceIn(l + minSize, 1f)
            b = (b + dy).coerceIn(t + minSize, 1f)
        }

        CropHandle.NONE -> return current
    }
    var result = NormalizedRect(l, t, r, b).clamp()
    if (ratio != null && displayAspect > 0f) {
        result = applyCropRatio(result, ratio, displayAspect)
    }
    return result
}

/**
 * 将裁剪框调整为指定宽高比。
 * displayAspect = 当前显示区域的宽/高；ratio 是像素宽高比。
 * 在归一化坐标中，width / height = ratio / displayAspect。
 */
private fun applyCropRatio(rect: NormalizedRect, ratio: Float, displayAspect: Float): NormalizedRect {
    val targetNormalized = (ratio / displayAspect).coerceAtLeast(0.05f)
    val width = rect.width
    var height = width / targetNormalized
    var newWidth = width
    if (height > 1f) {
        height = 1f
        newWidth = height * targetNormalized
    }
    if (newWidth > 1f) {
        newWidth = 1f
        height = newWidth / targetNormalized
    }
    val centerX = (rect.left + rect.right) / 2f
    val centerY = (rect.top + rect.bottom) / 2f
    val left = (centerX - newWidth / 2f).coerceIn(0f, 1f - newWidth)
    val top = (centerY - height / 2f).coerceIn(0f, 1f - height)
    return NormalizedRect(left, top, left + newWidth, top + height).clamp()
}

private fun fitCropToRatio(
    current: NormalizedRect,
    ratio: Float,
    displayAspect: Float
): NormalizedRect = applyCropRatio(current, ratio, displayAspect)

private fun Rect.inflate(amount: Float): Rect = Rect(
    left - amount,
    top - amount,
    right + amount,
    bottom + amount
)
