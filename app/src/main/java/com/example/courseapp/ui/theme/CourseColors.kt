package com.example.courseapp.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.courseapp.data.model.Course
import com.example.courseapp.data.model.CourseType
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * 课程卡配色：6 套「液态玻璃微磨砂」色卡。
 *
 * 每套色卡给的是**一层带透明度的染色**，卡片自己再叠磨砂高光/倒角（见 [CourseCardStyle]），
 * 所以换主题不是把同一个色值变亮变暗，而是换一整套「浅色淡彩底 + 深色低透明染色」。
 *
 * - [lightFill] / [darkFill] 带 alpha，是铺在页面底色上的染色层；
 * - [lightInk] 是浅色模式的文字色，同时用来派生描边（降低 alpha）；
 * - [darkEdge] 是深色模式的 1px 描边/高光，去掉 alpha 后就是深色模式文字色；
 * - [defaultType] 是该色卡对应的课程类型：新建课程不选颜色时按类型自动取这一套。
 *
 * 色值迁移/派生全部用纯 Kotlin 实现（hex 解析 + HSV 换算），不依赖 android.graphics，
 * 这样 [parseStoredCourseColor] 可以在本地单元测试里直接验证。
 */
enum class CourseColorPreset(
    val storageKey: String,
    val displayName: String,
    val lightFill: Color,
    val lightInk: Color,
    val darkFill: Color,
    val darkEdge: Color,
    val defaultType: CourseType?
) {
    ICE("ice", "冰晶蓝", Color(0xD9E0ECFF), Color(0xFF1E40AF), Color(0x473B82F6), Color(0x7393C5FD), CourseType.REQUIRED),
    TARO("taro", "香芋紫", Color(0xD9EDE9FE), Color(0xFF5B21B6), Color(0x478B5CF6), Color(0x73C4B5FD), null),
    MINT("mint", "薄荷绿", Color(0xD9D1FAE5), Color(0xFF065F46), Color(0x4710B981), Color(0x736EE7B7), CourseType.LAB),
    AMBER("amber", "暖金橙", Color(0xE0FEF3C7), Color(0xFF92400E), Color(0x47F59E0B), Color(0x73FCD34D), CourseType.ELECTIVE),
    CORAL("coral", "珊瑚粉", Color(0xE0FFE4E6), Color(0xFF9F1239), Color(0x47F43F5E), Color(0x73FDA4AF), CourseType.PUBLIC_ELECTIVE),
    SLATE("slate", "冷灰蓝", Color(0xD9F1F5F9), Color(0xFF334155), Color(0x4094A3B8), Color(0x59E2E8F0), CourseType.CUSTOM);

    /** 深色模式正文色：描边色去掉透明度，保证同色系且和染色底拉得开。 */
    val darkInk: Color get() = darkEdge.copy(alpha = 1f)

    companion object {
        /** 持久化的色卡 key（如 "ice"），大小写不敏感；空串/未知值返回 null。 */
        fun fromStorageKey(value: String): CourseColorPreset? {
            val key = value.trim()
            if (key.isEmpty()) return null
            return entries.firstOrNull { it.storageKey.equals(key, ignoreCase = true) }
        }
    }
}

/** 深色卡片垫在染色下面的磨砂底：13% 透光，壁纸能透一点但不会把正文顶掉。 */
internal val DarkCourseCardBase = Color(0xE614141A)

/** 课程类型 → 默认色卡。找不到时落到冷灰蓝，保证永远有可用配色。 */
fun presetForType(type: CourseType): CourseColorPreset =
    CourseColorPreset.entries.firstOrNull { it.defaultType == type } ?: CourseColorPreset.SLATE

/**
 * 解析 `Course.customColor`：新数据存色卡 key，老数据存 "#RRGGBB"。
 * 老色值按色相迁移到最接近的新色卡（低饱和度归到冷灰蓝），避免旧课程突然变成灰卡。
 */
fun parseStoredCourseColor(raw: String): CourseColorPreset? {
    val value = raw.trim()
    if (value.isEmpty()) return null
    if (!value.startsWith("#")) return CourseColorPreset.fromStorageKey(value)
    val legacy = parseHexColor(value) ?: return null
    return legacyColorToPreset(legacy)
}

/** 课程显式指定的色卡；null = 跟随课程类型默认色。 */
fun Course.explicitColorPreset(): CourseColorPreset? = parseStoredCourseColor(customColor)

/** 课程最终生效的色卡：显式选择优先，否则按类型默认。 */
fun Course.colorPreset(): CourseColorPreset = explicitColorPreset() ?: presetForType(type)

/** 详情抽屉 / 管理页列表的小色条：用不透明的同色系强调色。 */
fun courseAccentColor(course: Course, isDarkMode: Boolean): Color {
    val preset = course.colorPreset()
    return if (isDarkMode) preset.darkInk else preset.lightInk
}

/**
 * 一张课程卡最终的绘制参数。
 *
 * 半透明染色 + 磨砂高光 + 1px 描边 + 内倒角，四层叠加才像一块「不太透的液态玻璃」：
 * 底色仍然能透出一点点壁纸，但文字对比度按 15% 左右的透光率保守设计。
 */
@Immutable
data class CourseCardStyle(
    val base: Color,
    val fillTop: Color,
    val fillBottom: Color,
    val ink: Color,
    val bodyInk: Color,
    val border: Color,
    val bevelTop: Color,
    val bevelBottom: Color,
    val shadowInk: Color,
    val glow: Color,
    val progressTrack: Color,
    val progress: Color
)

fun courseCardStyle(
    course: Course,
    isDarkMode: Boolean,
    hasWallpaper: Boolean
): CourseCardStyle {
    val preset = course.colorPreset()
    return if (isDarkMode) {
        darkCardStyle(preset)
    } else {
        lightCardStyle(preset, hasWallpaper)
    }
}

/** 浅色：淡彩底（85%~88% 不透明）+ 白高光，正文用同色系深色。 */
private fun lightCardStyle(preset: CourseColorPreset, hasWallpaper: Boolean): CourseCardStyle {
    val ink = preset.lightInk
    return CourseCardStyle(
        base = Color.Transparent,
        fillTop = shade(preset.lightFill, 0.02f),
        fillBottom = shade(preset.lightFill, -0.035f),
        ink = ink,
        bodyInk = ink.copy(alpha = 0.86f),
        border = ink.copy(alpha = if (hasWallpaper) 0.30f else 0.24f),
        bevelTop = Color.White.copy(alpha = 0.60f),
        bevelBottom = ink.copy(alpha = 0.14f),
        shadowInk = ink.copy(alpha = 0.55f),
        glow = ink,
        progressTrack = ink.copy(alpha = 0.14f),
        progress = ink
    )
}

/** 深色：先铺一层深色磨砂底（避免 28% 染色太透），再叠彩色染色。 */
private fun darkCardStyle(preset: CourseColorPreset): CourseCardStyle {
    val ink = preset.darkInk
    return CourseCardStyle(
        base = DarkCourseCardBase,
        fillTop = preset.darkFill,
        fillBottom = preset.darkFill.copy(alpha = preset.darkFill.alpha * 0.58f),
        ink = ink,
        bodyInk = ink.copy(alpha = 0.86f),
        border = preset.darkEdge,
        bevelTop = preset.darkEdge.copy(alpha = 0.65f),
        bevelBottom = Color.Black.copy(alpha = 0.30f),
        shadowInk = Color.Black.copy(alpha = 0.75f),
        glow = preset.darkEdge,
        progressTrack = Color.White.copy(alpha = 0.16f),
        progress = ink
    )
}

/** 在 HSV 明度上做小幅偏移，保留 alpha 与色相，避免同一色卡的上下两段像两种颜色。 */
private fun shade(color: Color, delta: Float): Color {
    val hsv = hsvOf(color)
    return hsvToColor(hsv[0], hsv[1], (hsv[2] + delta).coerceIn(0f, 1f)).copy(alpha = color.alpha)
}

private fun legacyColorToPreset(legacy: Color): CourseColorPreset {
    val hsv = hsvOf(legacy)
    // 几乎无饱和度的老色值（灰蓝）归到冷灰蓝；其它按色相找最近的色卡。
    if (hsv[1] < 0.12f) return CourseColorPreset.SLATE
    return CourseColorPreset.entries
        .filter { it != CourseColorPreset.SLATE }
        .minByOrNull { circularHueDistance(hsvOf(it.lightInk)[0], hsv[0]) }
        ?: CourseColorPreset.SLATE
}

private fun circularHueDistance(a: Float, b: Float): Float {
    val diff = abs(a - b) % 360f
    return if (diff > 180f) 360f - diff else diff
}

/** "#RRGGBB" / "#AARRGGBB" → Color；其它长度或非十六进制返回 null。 */
private fun parseHexColor(value: String): Color? {
    val hex = value.removePrefix("#")
    if (hex.length != 6 && hex.length != 8) return null
    val packed = hex.toLongOrNull(16) ?: return null
    return if (hex.length == 6) Color(0xFF000000L or packed) else Color(packed)
}

/** RGB → HSV（h: 0..360, s/v: 0..1）。纯 Kotlin 实现，单元测试可用。 */
private fun hsvOf(color: Color): FloatArray {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = max(r, max(g, b))
    val min = min(r, min(g, b))
    val delta = max - min
    val hue = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    return floatArrayOf(
        if (hue < 0f) hue + 360f else hue,
        if (max == 0f) 0f else delta / max,
        max
    )
}

/** HSV → Color（不透明，alpha 由调用方补齐）。 */
private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val c = v * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = v - c
    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}
