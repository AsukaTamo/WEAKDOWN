package com.example.courseapp.ui.notes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.LruCache
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max

/**
 * 随手记缩略图/大图的内存缓存。
 *
 * 网格与列表使用**同一个解码尺寸**（缩略图 512、详情 2048），因此布局切换时
 * 不会触发新的磁盘解码；即使节点被重建，[peek] 也能同步拿回同一张 Bitmap。
 * 缓存 key 包含文件修改时间，重新编辑后不会读到旧图。
 */
object NoteImageCache {

    /** 编辑/归档后自增；图片 Composable 把它作为 key，强制重新读取最新文件。 */
    var generation by androidx.compose.runtime.mutableIntStateOf(0)
        private set

    fun invalidate() {
        generation++
    }

    private val memoryCache: LruCache<String, Bitmap> = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).coerceAtMost(96L * 1024 * 1024).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun fileFor(context: Context, relativePath: String): File =
        File(File(context.filesDir, "notebook"), relativePath)

    private fun key(file: File, maxSize: Int): String =
        "${file.absolutePath}#${file.lastModified()}#${file.length()}#$maxSize"

    fun peek(file: File, maxSize: Int): Bitmap? =
        runCatching { memoryCache.get(key(file, maxSize)) }.getOrNull()

    suspend fun load(file: File, rotation: Int, maxSize: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        val cacheKey = key(file, maxSize)
        memoryCache.get(cacheKey)?.let { return@withContext it }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext null
        var sample = 1
        var longest = max(bounds.outWidth, bounds.outHeight)
        while (longest / sample > maxSize) sample *= 2
        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        var bitmap = runCatching { BitmapFactory.decodeFile(file.absolutePath, options) }.getOrNull()
            ?: return@withContext null
        if (rotation % 360 != 0) {
            val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            if (rotated !== bitmap) bitmap.recycle()
            bitmap = rotated
        }
        memoryCache.put(cacheKey, bitmap)
        bitmap
    }
}

/**
 * 读取一张随手记图片。初始化时先查内存缓存，命中则当帧就有图，不会闪白；
 * 未命中才进 IO 解码。网格/列表切换不会改变这里的参数，因此不会重新加载。
 */
@Composable
fun rememberNoteBitmap(
    relativePath: String?,
    rotation: Int = 0,
    maxSize: Int = 512
): Bitmap? {
    val context = LocalContext.current
    val file = remember(relativePath, context) {
        relativePath?.let { NoteImageCache.fileFor(context, it) }
    }
    return rememberNoteFileBitmap(file, rotation, maxSize)
}

/** 草稿/编辑器的临时文件同样走缓存，避免页面切换重复解码。 */
@Composable
fun rememberNoteFileBitmap(
    file: File?,
    rotation: Int = 0,
    maxSize: Int = 512
): Bitmap? {
    val generation = NoteImageCache.generation
    val initial = remember(file, rotation, maxSize, generation) {
        file?.let { NoteImageCache.peek(it, maxSize) }
    }
    var bitmap by remember(file, rotation, maxSize, generation) { mutableStateOf(initial) }
    LaunchedEffect(file, rotation, maxSize, generation) {
        if (file == null) {
            bitmap = null
        } else {
            val cached = NoteImageCache.peek(file, maxSize)
            if (cached != null) {
                bitmap = cached
            } else {
                bitmap = NoteImageCache.load(file, rotation, maxSize)
            }
        }
    }
    return bitmap
}

/** 把 Bitmap 包成 ImageBitmap 并跟随 Bitmap 缓存。 */
@Composable
fun rememberNoteImageBitmap(
    relativePath: String?,
    rotation: Int = 0,
    maxSize: Int = 512
) = rememberNoteBitmap(relativePath, rotation, maxSize)?.let { bitmap ->
    remember(bitmap) { bitmap.asImageBitmap() }
}
