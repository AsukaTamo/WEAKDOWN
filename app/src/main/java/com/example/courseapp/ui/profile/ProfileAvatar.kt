package com.example.courseapp.ui.profile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * 自定义头像的本地存储。
 *
 * 选择图片后先压到 512px 的 JPEG 放进 cache（随时可丢），点「保存」时才搬到
 * `filesDir/avatars/`；取消编辑只删缓存，不会动正在用的头像。
 * 每次保存生成新的文件名，Compose 用路径做 remember key 才能在换头像后重新解码；
 * 旧文件由 ViewModel 在保存新路径时删除。
 */
object ProfileAvatarStore {
    private const val MAX_AVATAR_PX = 512
    private const val PENDING_DIR = "avatar_pending"
    private const val AVATAR_DIR = "avatars"
    private const val AVATAR_PREFIX = "avatar_"

    fun createPending(context: Context, uri: Uri): String? = runCatching {
        val bitmap = decodeFromUri(context, uri, MAX_AVATAR_PX) ?: return null
        val dir = File(context.cacheDir, PENDING_DIR).apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "pending_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        bitmap.recycle()
        file.absolutePath
    }.getOrNull()

    fun commit(context: Context, pendingPath: String): String? = runCatching {
        val pending = File(pendingPath)
        if (!pending.exists()) return null
        val dir = File(context.filesDir, AVATAR_DIR).apply { mkdirs() }
        val target = File(dir, "$AVATAR_PREFIX${System.currentTimeMillis()}.jpg")
        pending.copyTo(target, overwrite = true)
        pending.delete()
        target.absolutePath
    }.getOrNull()

    fun discardPending(path: String?) {
        if (path.isNullOrEmpty()) return
        runCatching { File(path).delete() }
    }

    fun delete(path: String?) {
        if (path.isNullOrEmpty()) return
        runCatching { File(path).delete() }
    }

    /** 解码本地头像给 UI 用；始终按最大边采样，不会把整张原图读进内存。 */
    fun decodeFile(path: String?, maxPx: Int = MAX_AVATAR_PX): Bitmap? {
        if (path.isNullOrEmpty()) return null
        val file = File(path)
        if (!file.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxPx)
            }
            BitmapFactory.decodeFile(path, options)
        }.getOrNull()
    }

    private fun decodeFromUri(context: Context, uri: Uri, maxPx: Int): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(bounds.outWidth, bounds.outHeight, maxPx)
        }
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        val scale = maxPx.toFloat() / max(decoded.width, decoded.height)
        if (scale >= 1f) return decoded
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            (decoded.width * scale).toInt().coerceAtLeast(1),
            (decoded.height * scale).toInt().coerceAtLeast(1),
            true
        )
        if (scaled !== decoded) decoded.recycle()
        return scaled
    }

    private fun sampleSize(width: Int, height: Int, maxPx: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        while (max(width, height) / sample > maxPx * 2) sample *= 2
        return sample
    }
}
