package com.example.courseapp.data.storage

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.courseapp.data.model.NoteAnnotationMapping
import com.example.courseapp.data.model.NoteDraft
import com.example.courseapp.data.model.NoteEditorDocument
import com.example.courseapp.data.model.NotePhoto
import com.example.courseapp.data.model.NoteStrokeTool
import com.example.courseapp.data.model.NoteTextLayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max
import kotlin.math.roundToInt

/** 图片无法解码时给 UI 的稳定错误类型（格式不支持 / 文件损坏）。 */
class UnsupportedImageException(message: String) : Exception(message)

data class ImageInfo(
    val width: Int,
    val height: Int,
    val rotation: Int,
    val mimeType: String
) {
    /** 摆正后用于显示/导出的尺寸。 */
    val uprightWidth: Int get() = if (rotation % 180 == 0) width else height
    val uprightHeight: Int get() = if (rotation % 180 == 0) height else width
}

data class RenderedImage(val file: File, val width: Int, val height: Int)

/**
 * 随手记图片文件管理。所有路径相对 `filesDir/notebook`，数据库只保存相对路径。
 *
 * - 原图与编辑合成图分离，编辑永远不写回原图；
 * - 缩略图可随时重建，不作为唯一原始数据；
 * - 文件先写成功，再由 Repository 提交数据库。
 */
@Singleton
class NoteImageStore @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notebookDir: File get() = File(context.filesDir, NOTEBOOK_DIR).also { it.mkdirs() }

    fun originalsDir(): File = dir("originals")
    fun previewsDir(): File = dir("previews")
    fun exportsDir(): File = dir("exports")
    fun tempDir(): File = dir("tmp")

    private fun dir(name: String): File =
        File(notebookDir, name).also { if (!it.exists()) it.mkdirs() }

    fun resolve(relativePath: String?): File? {
        if (relativePath.isNullOrBlank()) return null
        val clean = relativePath.replace('\\', '/').trimStart('/')
        if (clean.startsWith("..")) return null
        return File(notebookDir, clean)
    }

    fun relativeOf(file: File): String =
        file.relativeTo(notebookDir).path.replace('\\', '/')

    fun newTempFile(prefix: String, extension: String = "jpg"): File =
        File(tempDir(), "${prefix}_${UUID.randomUUID()}.$extension")

    /** 从系统 Photo Picker 的 URI 复制到应用私有临时目录，原字节保持不变。 */
    suspend fun importFromUri(uri: Uri, id: String): NoteDraft = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val extension = extensionFor(mime, uri.toString())
        val target = newTempFile("import_$id", extension)
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            } ?: throw UnsupportedImageException("无法读取所选图片，请重新选择")
            val info = readInfo(target, mime)
            NoteDraft(
                id = id,
                sourcePath = target.absolutePath,
                capturedAt = System.currentTimeMillis(),
                width = info.uprightWidth,
                height = info.uprightHeight,
                rotation = info.rotation,
                mimeType = info.mimeType
            )
        } catch (error: Exception) {
            target.delete()
            throw error
        }
    }

    /** CameraX 已把文件写进私有 tmp，这里只需要读取尺寸与 EXIF 方向。 */
    suspend fun draftFromCapturedFile(file: File, capturedAt: Long): NoteDraft =
        withContext(Dispatchers.IO) {
            val info = readInfo(file, "image/jpeg")
            NoteDraft(
                id = UUID.randomUUID().toString(),
                sourcePath = file.absolutePath,
                capturedAt = capturedAt,
                width = info.uprightWidth,
                height = info.uprightHeight,
                rotation = info.rotation,
                mimeType = info.mimeType
            )
        }

    suspend fun readInfo(file: File, fallbackMime: String = "image/jpeg"): ImageInfo =
        withContext(Dispatchers.IO) {
            if (!file.exists() || file.length() == 0L) {
                throw UnsupportedImageException("图片文件为空或不存在，请重新选择")
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                throw UnsupportedImageException("不支持的图片格式，请选择 JPEG、PNG 或 WebP 图片")
            }
            val rotation = runCatching { readExifRotation(file) }.getOrDefault(0)
            val mime = when (file.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                "heic", "heif" -> "image/heic"
                "jpg", "jpeg" -> "image/jpeg"
                else -> fallbackMime
            }
            ImageInfo(bounds.outWidth, bounds.outHeight, rotation, mime)
        }

    private fun readExifRotation(file: File): Int {
        val exif = ExifInterface(file.absolutePath)
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    /** 解码并摆正到显示方向；只按需要采样，绝不在主线程调用。 */
    suspend fun decodeUpright(
        file: File,
        maxDimension: Int,
        rotation: Int
    ): Bitmap = withContext(Dispatchers.IO) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw UnsupportedImageException("图片无法解码，可能已损坏")
        }
        var sample = 1
        var longest = max(bounds.outWidth, bounds.outHeight)
        while (longest / sample > maxDimension) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options)
            ?: throw UnsupportedImageException("图片无法解码，可能已损坏")
        if (rotation % 360 == 0) {
            decoded
        } else {
            rotateBitmap(decoded, rotation)
        }
    }

    private fun rotateBitmap(source: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
        if (rotated !== source) source.recycle()
        return rotated
    }

    /** 把草稿文件移进 originals；返回数据库使用的相对路径。 */
    suspend fun adoptIntoOriginals(draft: NoteDraft, photoId: String): String =
        withContext(Dispatchers.IO) {
            val source = File(draft.sourcePath)
            if (!source.exists()) throw UnsupportedImageException("原图文件已丢失，请重新拍摄或导入")
            val extension = source.extension.ifBlank { extensionFor(draft.mimeType, "") }
            val target = File(originalsDir(), "$photoId.$extension")
            if (target.exists()) target.delete()
            if (!source.renameTo(target)) {
                source.copyTo(target, overwrite = true)
                source.delete()
            }
            relativeOf(target)
        }

    suspend fun writePreview(sourceFile: File, rotation: Int, photoId: String): String? =
        withContext(Dispatchers.IO) {
            runCatching {
                val bitmap = decodeUpright(sourceFile, PREVIEW_MAX_DIMENSION, rotation)
                val target = File(previewsDir(), "$photoId.webp")
                FileOutputStream(target).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.WEBP, 88, out)
                }
                bitmap.recycle()
                relativeOf(target)
            }.getOrNull()
        }

    /**
     * 把编辑文档合成为一张新图片。
     *
     * 顺序：原图（已摆正）→ 旋转 → 裁剪 → 按原图归一化坐标绘制文字/笔迹。
     * 输出长边限制在 [RENDER_MAX_DIMENSION]，避免大图编辑 OOM。
     */
    suspend fun renderDocument(
        sourceFile: File,
        document: NoteEditorDocument,
        destination: File
    ): RenderedImage = withContext(Dispatchers.IO) {
        val info = readInfo(sourceFile)
        var bitmap = decodeUpright(sourceFile, RENDER_MAX_DIMENSION, info.rotation)
        if (document.normalizedRotation != 0) {
            bitmap = rotateBitmap(bitmap, document.normalizedRotation)
        }
        val crop = document.crop.clamp()
        val left = (crop.left * bitmap.width).roundToInt().coerceIn(0, bitmap.width - 1)
        val top = (crop.top * bitmap.height).roundToInt().coerceIn(0, bitmap.height - 1)
        val right = (crop.right * bitmap.width).roundToInt().coerceIn(left + 1, bitmap.width)
        val bottom = (crop.bottom * bitmap.height).roundToInt().coerceIn(top + 1, bitmap.height)
        val outWidth = right - left
        val outHeight = bottom - top

        val output = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(output)
            canvas.drawARGB(255, 255, 255, 255)
            canvas.save()
            canvas.translate(-left.toFloat(), -top.toFloat())
            canvas.drawBitmap(bitmap, 0f, 0f, null)
            canvas.restore()

            val scale = NoteAnnotationMapping.sizeFractionScale(
                document,
                info.uprightWidth,
                info.uprightHeight
            )
            // 标注尺寸以“原图宽度”为绝对基准；导出图可能被裁剪/采样，这里换算到输出像素。
            val rotatedWidth = if (document.normalizedRotation % 180 == 0) {
                info.uprightWidth
            } else {
                info.uprightHeight
            }.coerceAtLeast(1)
            val renderScale = outWidth.toFloat() / (crop.width * rotatedWidth)
            val annotationScale = info.uprightWidth.toFloat() * renderScale
            document.strokes.forEach { stroke ->
                drawStroke(canvas, stroke, document, outWidth, outHeight, annotationScale)
            }
            document.texts.forEach { layer ->
                drawText(canvas, layer, document, outWidth, outHeight, annotationScale)
            }

            destination.parentFile?.mkdirs()
            FileOutputStream(destination).use { out ->
                output.compress(Bitmap.CompressFormat.JPEG, 94, out)
            }
        } finally {
            output.recycle()
            bitmap.recycle()
        }
        RenderedImage(destination, outWidth, outHeight)
    }

    private fun drawStroke(
        canvas: Canvas,
        stroke: com.example.courseapp.data.model.NoteStroke,
        document: NoteEditorDocument,
        width: Int,
        height: Int,
        annotationScale: Float
    ) {
        if (stroke.points.isEmpty()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.color
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            strokeWidth = (stroke.width * annotationScale).coerceAtLeast(1f)
        }
        val mapped = stroke.points.map { NoteAnnotationMapping.toOutput(it, document) }
        when (stroke.tool) {
            NoteStrokeTool.PEN -> {
                if (mapped.size == 1) {
                    canvas.drawCircle(mapped[0].x * width, mapped[0].y * height, paint.strokeWidth / 2f, paint)
                    return
                }
                val path = android.graphics.Path()
                mapped.forEachIndexed { index, point ->
                    val x = point.x * width
                    val y = point.y * height
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                canvas.drawPath(path, paint)
            }

            NoteStrokeTool.CIRCLE -> {
                if (mapped.size < 2) return
                val rect = RectF(
                    minOf(mapped[0].x, mapped[1].x) * width,
                    minOf(mapped[0].y, mapped[1].y) * height,
                    maxOf(mapped[0].x, mapped[1].x) * width,
                    maxOf(mapped[0].y, mapped[1].y) * height
                )
                canvas.drawOval(rect, paint)
            }

            NoteStrokeTool.ARROW -> {
                if (mapped.size < 2) return
                val startX = mapped[0].x * width
                val startY = mapped[0].y * height
                val endX = mapped[1].x * width
                val endY = mapped[1].y * height
                canvas.drawLine(startX, startY, endX, endY, paint)
                val angle = Math.atan2((endY - startY).toDouble(), (endX - startX).toDouble())
                val head = (paint.strokeWidth * 4f).coerceAtLeast(12f)
                val left = angle + Math.PI / 6
                val right = angle - Math.PI / 6
                canvas.drawLine(
                    endX, endY,
                    endX - (head * Math.cos(left)).toFloat(),
                    endY - (head * Math.sin(left)).toFloat(),
                    paint
                )
                canvas.drawLine(
                    endX, endY,
                    endX - (head * Math.cos(right)).toFloat(),
                    endY - (head * Math.sin(right)).toFloat(),
                    paint
                )
            }
        }
    }

    private fun drawText(
        canvas: Canvas,
        layer: NoteTextLayer,
        document: NoteEditorDocument,
        width: Int,
        height: Int,
        annotationScale: Float
    ) {
        if (layer.text.isBlank()) return
        val point = NoteAnnotationMapping.toOutput(com.example.courseapp.data.model.NotePoint(layer.x, layer.y), document)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layer.color
            textSize = (layer.fontSize * annotationScale).coerceAtLeast(8f)
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            style = Paint.Style.FILL
        }
        canvas.save()
        canvas.translate(point.x * width, point.y * height)
        if (layer.rotation != 0f) canvas.rotate(layer.rotation)
        // 与 Compose 的 drawText(topLeft=…) 对齐：锚点按“文字左上角”解释。
        val baseline = -paint.fontMetrics.ascent
        canvas.drawText(layer.text, 0f, baseline, paint)
        canvas.restore()
    }

    /** 写入系统相册。Android 10+ 使用 scoped storage，不申请存储权限。 */
    suspend fun exportToMediaStore(file: File, displayName: String, mimeType: String): Uri? =
        withContext(Dispatchers.IO) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return@withContext null
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/随手记")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: return@withContext null
            try {
                resolver.openOutputStream(uri)?.use { output ->
                    file.inputStream().use { input -> input.copyTo(output) }
                } ?: throw IllegalStateException("无法写入系统相册")
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
                uri
            } catch (error: Exception) {
                runCatching { resolver.delete(uri, null, null) }
                throw error
            }
        }

    /** 分享私有图片时使用 FileProvider content URI。 */
    fun shareUri(photo: NotePhoto): Uri? {
        val file = resolve(photo.displayRelativePath) ?: return null
        if (!file.exists()) return null
        return runCatching {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        }.getOrNull()
    }

    suspend fun deletePhotoFiles(photo: NotePhoto) = withContext(Dispatchers.IO) {
        listOf(photo.originalRelativePath, photo.editedRelativePath, photo.previewRelativePath)
            .filterNotNull()
            .distinct()
            .forEach { relative -> resolve(relative)?.delete() }
    }

    suspend fun deleteTemp(file: File?) = withContext(Dispatchers.IO) {
        if (file != null && file.parentFile == tempDir()) file.delete()
    }

    /** 清理超过一天仍未归档的临时文件（中途退出、导入失败等）。 */
    suspend fun cleanupStaleTemp() = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        tempDir().listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) file.delete()
        }
    }

    private fun extensionFor(mime: String?, fallback: String): String {
        return when {
            mime?.contains("png", true) == true -> "png"
            mime?.contains("webp", true) == true -> "webp"
            mime?.contains("heic", true) == true || mime?.contains("heif", true) == true -> "heic"
            mime?.contains("jpeg", true) == true -> "jpg"
            fallback.endsWith(".png", true) -> "png"
            fallback.endsWith(".webp", true) -> "webp"
            else -> "jpg"
        }
    }

    companion object {
        private const val NOTEBOOK_DIR = "notebook"
        const val PREVIEW_MAX_DIMENSION = 512
        const val RENDER_MAX_DIMENSION = 2560

        /** 生成文件名时去掉会破坏 MediaStore 的字符。 */
        fun safeExportName(title: String, extension: String = "jpg"): String {
            val base = title.trim().ifBlank { "随手记" }
                .replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), "_")
                .take(40)
            return "$base.$extension"
        }
    }
}
