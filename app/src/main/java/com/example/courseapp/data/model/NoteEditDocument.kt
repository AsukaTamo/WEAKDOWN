package com.example.courseapp.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import java.util.UUID
import kotlin.math.abs

/**
 * 归一化矩形（0..1）。裁剪矩形相对“已经按 [NoteEditorDocument.rotation] 旋转后的原图”。
 * 保持值为 0..1，屏幕尺寸、导出分辨率变化时都能正确映射。
 */
data class NormalizedRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val width: Float get() = (right - left).coerceAtLeast(0.0001f)
    val height: Float get() = (bottom - top).coerceAtLeast(0.0001f)
    val isFull: Boolean
        get() = abs(left) < 0.0005f && abs(top) < 0.0005f &&
            abs(right - 1f) < 0.0005f && abs(bottom - 1f) < 0.0005f

    fun clamp(): NormalizedRect {
        val l = left.coerceIn(0f, 1f)
        val t = top.coerceIn(0f, 1f)
        val r = right.coerceIn(0f, 1f)
        val b = bottom.coerceIn(0f, 1f)
        return NormalizedRect(
            left = minOf(l, r),
            top = minOf(t, b),
            right = maxOf(l, r),
            bottom = maxOf(t, b)
        )
    }

    /** 把当前裁剪结果再裁一刀（子矩形相对于当前显示区域）。 */
    fun subRect(sub: NormalizedRect): NormalizedRect {
        val s = sub.clamp()
        return NormalizedRect(
            left = left + s.left * width,
            top = top + s.top * height,
            right = left + s.right * width,
            bottom = top + s.bottom * height
        ).clamp()
    }

    /** 图片顺时针旋转 90° 后，同一块内容在新坐标系中的矩形。 */
    fun rotatedClockwise(): NormalizedRect {
        val c = clamp()
        return NormalizedRect(
            left = 1f - c.bottom,
            top = c.left,
            right = 1f - c.top,
            bottom = c.right
        ).clamp()
    }

    companion object {
        val Full = NormalizedRect()
    }
}

/** 归一化坐标点：x 相对图片宽度，y 相对图片高度。 */
data class NotePoint(val x: Float, val y: Float)

enum class NoteStrokeTool { PEN, CIRCLE, ARROW }

/** 文字图层；坐标归一化到“原图（已按 EXIF 摆正）”。 */
data class NoteTextLayer(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    /** 字号占原图宽度的比例，导出时按实际输出宽度换算。 */
    val fontSize: Float = 0.05f,
    val color: Int = 0xFFE43D35.toInt(),
    val rotation: Float = 0f
)

/** 笔迹图层。PEN 保存完整路径点，CIRCLE/ARROW 使用首尾两点。 */
data class NoteStroke(
    val id: String = UUID.randomUUID().toString(),
    val points: List<NotePoint>,
    val color: Int = 0xFFE43D35.toInt(),
    /** 笔触宽度占原图宽度的比例。 */
    val width: Float = 0.004f,
    val tool: NoteStrokeTool = NoteStrokeTool.PEN
)

/**
 * 非破坏性编辑文档。原图不参与任何写操作，本对象是唯一可变的编辑结果。
 * 绘制顺序：旋转 → 裁剪 → 把“原图归一化坐标”映射成输出坐标 → 画笔迹/文字。
 */
data class NoteEditorDocument(
    val crop: NormalizedRect = NormalizedRect.Full,
    /** 用户旋转角度，只能是 0/90/180/270；EXIF 方向在解码时已单独处理。 */
    val rotation: Int = 0,
    val texts: List<NoteTextLayer> = emptyList(),
    val strokes: List<NoteStroke> = emptyList(),
    val version: Int = 1,
    val updatedAt: Long = 0L
) {
    val normalizedRotation: Int get() = ((rotation % 360) + 360) % 360
    val isIdentity: Boolean
        get() = crop.isFull && normalizedRotation == 0 && texts.isEmpty() && strokes.isEmpty()

    fun rotatedClockwise(): NoteEditorDocument = copy(
        crop = crop.rotatedClockwise(),
        rotation = (normalizedRotation + 90) % 360
    )

    fun withCrop(sub: NormalizedRect): NoteEditorDocument = copy(crop = crop.subRect(sub))

    fun withLayers(texts: List<NoteTextLayer>, strokes: List<NoteStroke>): NoteEditorDocument =
        copy(texts = texts, strokes = strokes)
}

/**
 * 归一化坐标映射工具。
 *
 * 原图 -> 旋转后的图 -> 裁剪后的输出。文字/笔迹以原图坐标为锚点，
 * 因此重新裁剪或旋转不会让标注“漂移”，只会跟着图片内容走。
 */
object NoteAnnotationMapping {

    fun rotatePoint(point: NotePoint, rotation: Int): NotePoint {
        val x = point.x.coerceIn(0f, 1f)
        val y = point.y.coerceIn(0f, 1f)
        return when (((rotation % 360) + 360) % 360) {
            90 -> NotePoint(1f - y, x)
            180 -> NotePoint(1f - x, 1f - y)
            270 -> NotePoint(y, 1f - x)
            else -> NotePoint(x, y)
        }
    }

    /** 原图坐标 -> 输出（旋转 + 裁剪后）的 0..1 坐标。可能落在 0..1 之外，调用方按需裁剪。 */
    fun toOutput(point: NotePoint, document: NoteEditorDocument): NotePoint {
        val rotated = rotatePoint(point, document.rotation)
        val crop = document.crop
        return NotePoint(
            x = (rotated.x - crop.left) / crop.width,
            y = (rotated.y - crop.top) / crop.height
        )
    }

    /** 输出坐标 -> 原图坐标（toOutput 的逆映射），用于把屏幕手势写回归一化标注。 */
    fun fromOutput(point: NotePoint, document: NoteEditorDocument): NotePoint {
        val crop = document.crop
        val rotated = NotePoint(
            x = crop.left + point.x * crop.width,
            y = crop.top + point.y * crop.height
        )
        val r = ((document.rotation % 360) + 360) % 360
        return when (r) {
            90 -> NotePoint(rotated.y, 1f - rotated.x)
            180 -> NotePoint(1f - rotated.x, 1f - rotated.y)
            270 -> NotePoint(1f - rotated.y, rotated.x)
            else -> rotated
        }
    }

    /**
     * 字号/线宽从“原图宽度的比例”换算到“输出宽度的比例”。
     * 旋转 90° 时原图宽变成输出高，但绘制仍以输出宽度为基准，所以要做这一步。
     */
    fun sizeFractionScale(document: NoteEditorDocument, originalWidth: Int, originalHeight: Int): Float {
        if (originalWidth <= 0 || originalHeight <= 0) return 1f
        val rotatedWidth = if (document.normalizedRotation % 180 == 0) originalWidth else originalHeight
        if (rotatedWidth <= 0) return 1f
        return originalWidth.toFloat() / rotatedWidth.toFloat()
    }
}

// ── 持久化 ────────────────────────────────────────────────────────────────

private val noteEditGson = Gson()

internal fun encodeTextLayers(layers: List<NoteTextLayer>): String = noteEditGson.toJson(layers)

internal fun decodeTextLayers(json: String?): List<NoteTextLayer> =
    runCatching {
        noteEditGson.fromJson(json, Array<NoteTextLayer>::class.java)?.toList().orEmpty()
    }.getOrDefault(emptyList())

internal fun encodeStrokes(strokes: List<NoteStroke>): String = noteEditGson.toJson(strokes)

internal fun decodeStrokes(json: String?): List<NoteStroke> =
    runCatching {
        noteEditGson.fromJson(json, Array<NoteStroke>::class.java)?.toList().orEmpty()
    }.getOrDefault(emptyList())

@Entity(tableName = "note_edit_documents")
data class NoteEditDocumentEntity(
    @PrimaryKey val photoId: String,
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val rotation: Int = 0,
    val textLayersJson: String = "[]",
    val strokesJson: String = "[]",
    val version: Int = 1,
    val updatedAt: Long = 0L
) {
    fun toDomain(): NoteEditorDocument = NoteEditorDocument(
        crop = NormalizedRect(cropLeft, cropTop, cropRight, cropBottom).clamp(),
        rotation = rotation,
        texts = decodeTextLayers(textLayersJson),
        strokes = decodeStrokes(strokesJson),
        version = version,
        updatedAt = updatedAt
    )
}

fun NoteEditorDocument.toEntity(photoId: String): NoteEditDocumentEntity = NoteEditDocumentEntity(
    photoId = photoId,
    cropLeft = crop.clamp().left,
    cropTop = crop.clamp().top,
    cropRight = crop.clamp().right,
    cropBottom = crop.clamp().bottom,
    rotation = normalizedRotation,
    textLayersJson = encodeTextLayers(texts),
    strokesJson = encodeStrokes(strokes),
    version = version,
    updatedAt = updatedAt
)
