package com.example.courseapp.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Matrix
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * 液态玻璃（Liquid Glass）材质参数。
 * 这些就是着色器暴露的 uniform，改这里即可调材质。
 */
data class LiquidGlassSpec(
    /** 玻璃最厚处的“厚度”（归一化高度，0.02~0.08 之间比较自然） */
    val thickness: Float = 0.075f,
    /** 倒角过渡带宽度（归一化，越大边缘越圆润） */
    val normalTransition: Float = 0.22f,
    /** 虚拟背景平面离玻璃的距离，决定折射位移量 */
    val backgroundDistance: Float = 1.15f,
    /** 折射率，玻璃取 1.46 */
    val ior: Float = 1.46f,
    /** 色散强度，只在内外缘生效 */
    val dispersion: Float = 0.035f,
    /** 感知粗糙度，GGX 用 */
    val roughness: Float = 0.32f,
    /** 圆角半径 */
    val cornerRadius: Dp = 26.dp,
    /** 白色染色强度 */
    val tint: Color = Color.White.copy(alpha = 0.05f),
    /** 外部柔和阴影强度 */
    val shadow: Float = 0.30f,
    /** 边缘亮线强度：深色模式要压低，否则会有一条突兀的白边 */
    val edgeGlow: Float = 0.22f,
    /** 镜面反射强度 */
    val specular: Float = 2.4f,
    /** 调试：只输出采样到的背景，用于验证纹理绑定是否正常 */
    val debugSample: Boolean = false
)

private const val LIQUID_GLASS_AGSL = """
uniform shader uSharp;      // 清晰背景（屏幕尺寸位图）
uniform shader uBlur;       // 模糊背景（低分辨率位图）
uniform float2 uSize;       // 组件尺寸 px
uniform float2 uResolution; // 屏幕尺寸 px
uniform float2 uOrigin;     // 组件左上角在屏幕中的位置 px
uniform float uCorner;      // 圆角半径 px
uniform float uThickness;
uniform float uNormalTransition;
uniform float uBackgroundDistance;
uniform float uIor;
uniform float uDispersion;
uniform float uRoughness;
uniform float4 uTint;
uniform float uShadow;
uniform float uEdgeGlow;
uniform float uSpecular;
uniform float uTime;
uniform float uDebugSample;

const float PI = 3.14159265;

float quintic(float t) { return t * t * t * (t * (t * 6.0 - 15.0) + 10.0); }

// 手写折射：AGSL 对 refract 的支持因版本而异，这里给出稳定实现
vec3 refractDir(vec3 I, vec3 N, float eta) {
    float cosi = clamp(dot(N, I), -1.0, 1.0);
    float k = 1.0 - eta * eta * (1.0 - cosi * cosi);
    if (k < 0.0) {
        return vec3(0.0, 0.0, 0.0);
    }
    return eta * I - (eta * cosi + sqrt(k)) * N;
}

// 圆角矩形 SDF（p 为 aspect 校正后的中心坐标系）
float sdRoundBox(vec2 p, vec2 halfSize, float r) {
    vec2 q = abs(p) - halfSize + r;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r;
}

float heightAt(vec2 p, vec2 halfSize, float r, float bevel) {
    float sdf = sdRoundBox(p, halfSize, r);
    float interior = max(-sdf, 0.0);
    float t = clamp(interior / bevel, 0.0, 1.0);
    return uThickness * quintic(t);
}

// GGX 法线分布
float D_GGX(float NoH, float a) {
    float a2 = a * a;
    float d = NoH * NoH * (a2 - 1.0) + 1.0;
    return a2 / max(PI * d * d, 1e-5);
}

// Smith height-correlated visibility
float V_Smith(float NoV, float NoL, float a) {
    float a2 = a * a;
    float gv = NoL * sqrt(NoV * NoV * (1.0 - a2) + a2);
    float gl = NoV * sqrt(NoL * NoL * (1.0 - a2) + a2);
    return 0.5 / max(gv + gl, 1e-5);
}

float F_Schlick(float u, float f0) {
    return f0 + (1.0 - f0) * pow(1.0 - u, 5.0);
}

vec4 sampleBackground(vec2 screenPx) {
    return uBlur.eval(screenPx);
}

vec4 sampleBackgroundSharp(vec2 screenPx) {
    return uSharp.eval(screenPx);
}

// 单条光线的完整光路：入射 -> 玻璃内 -> 出射 -> 打到背景平面
vec2 traceRay(vec2 p, float height, vec3 normal, float ior) {
    vec3 incident = vec3(0.0, 0.0, -1.0);
    vec3 insideRay = refractDir(incident, normal, 1.0 / ior);
    if (dot(insideRay, insideRay) < 1e-5) {
        insideRay = vec3(0.0, 0.0, -1.0);
    }
    float path = height / max(-insideRay.z, 0.025);
    vec2 backUV = p + insideRay.xy * path;

    // 背面是平面，直接沿 -z 出射，再与背景平面求交
    vec3 exitDir = refractDir(vec3(0.0, 0.0, -1.0), vec3(0.0, 0.0, 1.0), ior);
    if (dot(exitDir, exitDir) < 1e-5) {
        exitDir = vec3(0.0, 0.0, -1.0);
    }
    vec2 travel = exitDir.xy / max(-exitDir.z, 0.05) * uBackgroundDistance;
    return backUV + travel;
}

half4 main(float2 fragCoord) {
    vec2 size = uSize;
    // 调试通道：原样输出采样结果，用来判断纹理绑定/坐标是否正常
    if (uDebugSample > 0.5) {
        return half4(uSharp.eval(fragCoord).rgb, 1.0);
    }
    float aspect = size.x / max(size.y, 1.0);
    vec2 uv = fragCoord / size;

    // ── 01 坐标与 SDF ──
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0) * 2.0;
    vec2 halfSize = vec2(aspect, 1.0);
    float r = uCorner / max(size.y, 1.0) * 2.0;
    float sdf = sdRoundBox(p, halfSize - r, r);

    // 抗锯齿蒙版本（低频，用于 alpha）与光滑版本（用于高度/法线）
    float aa = (2.0 / max(size.y, 1.0)) * 1.5;
    float mask = smoothstep(aa, -aa, sdf);
    if (mask <= 0.001) {
        return half4(0.0);
    }
    float glassSdf = sdf;

    // ── 02 高度场与法线（四方向差分）──
    float bevel = max(uNormalTransition * 0.82, 1e-3);
    float interior = max(-glassSdf, 0.0);
    float height = heightAt(p, halfSize - r, r, bevel);

    float step = clamp(uNormalTransition * 0.13, 0.003, 0.012);
    float hL = heightAt(p - vec2(step, 0.0), halfSize - r, r, bevel);
    float hR = heightAt(p + vec2(step, 0.0), halfSize - r, r, bevel);
    float hD = heightAt(p - vec2(0.0, step), halfSize - r, r, bevel);
    float hU = heightAt(p + vec2(0.0, step), halfSize - r, r, bevel);
    vec2 gradient = vec2(hR - hL, hU - hD) / (2.0 * step);
    vec3 normal = normalize(vec3(-gradient, 1.0));

    // ── 03 三路折射采样 ──
    vec2 innerUV = traceRay(p, height, normal, uIor - uDispersion);
    vec2 greenUV = traceRay(p, height, normal, uIor);
    vec2 outerUV = traceRay(p, height, normal, uIor + uDispersion);

    float bevelT = clamp(interior / bevel, 0.0, 1.0);
    // 内缘/外缘权重只在倒角带内出现
    float rim = 1.0 - smoothstep(0.0, 1.0, bevelT);
    float rimDir = length(gradient) > 1e-4 ? 1.0 : 0.0;
    vec2 dir = rimDir > 0.5 ? normalize(gradient + 1e-6) : vec2(0.0);

    // Face：略微放大，保持中心清晰
    vec2 faceP = p * 0.975;
    vec2 faceUV = faceP * 0.5 / vec2(aspect, 1.0) + 0.5;
    vec2 facePx = faceUV * size;

    vec2 innerPx = (greenUV * 0.5 / vec2(aspect, 1.0) + 0.5) * size - dir * bevel * size.y * 0.55;
    vec2 outerPx = (outerUV * 0.5 / vec2(aspect, 1.0) + 0.5) * size + dir * bevel * size.y * 0.72;

    vec3 faceCol = sampleBackgroundSharp(facePx).rgb;
    vec4 innerR = uSharp.eval(innerPx + dir * 1.5);
    vec4 innerG = uSharp.eval(innerPx);
    vec4 innerB = uSharp.eval(innerPx - dir * 1.5);
    vec3 innerCol = vec3(innerR.r, innerG.g, innerB.b);

    vec4 outerR = uBlur.eval(outerPx + dir * 2.5);
    vec4 outerG = uBlur.eval(outerPx);
    vec4 outerB = uBlur.eval(outerPx - dir * 2.5);
    vec3 outerCol = vec3(outerR.r, outerG.g, outerB.b);

    float faceWeight = smoothstep(0.35, 1.0, bevelT);
    float innerWeight = rim * (1.0 - smoothstep(0.55, 1.0, bevelT));
    float outerWeight = rim * (1.0 - bevelT);
    float wSum = max(faceWeight + innerWeight + outerWeight, 1e-4);
    vec3 refracted = (faceCol * faceWeight + innerCol * innerWeight + outerCol * outerWeight) / wSum;

    // ── 04 阴影 + 亮边 + 染色 ──
    // 贴边内阴影：让玻璃看起来有厚度，边缘压暗一圈
    float edgeBand = 1.0 - smoothstep(0.0, bevel * 2.0, interior);
    float innerShade = mix(1.0 - uShadow * 0.55, 1.0, smoothstep(0.0, bevel * 2.6, interior));
    vec3 col = refracted * innerShade;
    // 亮边：模拟玻璃厚度的折射亮线，越靠边越亮
    col += vec3(0.22, 0.23, 0.25) * uEdgeGlow * pow(edgeBand, 2.6) * 4.55;
    // 倒角外缘一点冷色溢出（色散在视觉上的落点）
    col += vec3(-0.02, 0.01, 0.05) * pow(edgeBand, 3.0) * (uDispersion * 12.0);
    col = mix(col, uTint.rgb, uTint.a);

    // ── 05 GGX + 四点掠射光 ──
    float f0 = pow((uIor - 1.0) / (uIor + 1.0), 2.0);
    float rough = clamp(uRoughness, 0.05, 0.9);
    vec3 V = vec3(0.0, 0.0, 1.0);
    vec3 N = normal;
    float NoV = max(dot(N, V), 1e-4);
    // 高光只允许出现在倒角外半段
    float bevelSupport = rim * smoothstep(0.15, 0.85, 1.0 - bevelT + 0.15);

    vec3 spec = vec3(0.0);
    for (int i = 0; i < 4; i++) {
        float a = uTime * 0.05 + float(i) * PI * 0.5;
        float radius = 0.90 + 0.06 * sin(float(i) * 2.1);
        vec3 L = normalize(vec3(cos(a) * radius, sin(a) * radius, 0.28 + 0.14 * cos(float(i) * 1.7)));
        vec3 H = normalize(L + V);
        float NoL = max(dot(N, L), 1e-4);
        float NoH = max(dot(N, H), 1e-4);
        float VoH = max(dot(V, H), 1e-4);
        float d = D_GGX(NoH, rough * rough);
        float vis = V_Smith(NoV, NoL, rough * rough);
        float fres = F_Schlick(VoH, f0);
        float warm = 1.0 + 0.06 * cos(float(i) * 1.3);
        vec3 lightTint = vec3(warm, 1.0, 2.0 - warm);
        spec += lightTint * (d * vis * fres * NoL);
    }
    col += spec * bevelSupport * uSpecular;

    // 极淡的边缘菲涅尔，让轮廓有一圈柔光
    float rimLight = pow(1.0 - NoV, 3.0) * rim;
    col += vec3(rimLight * 0.18);

    return half4(col, mask);
}
"""


/**
 * 方案 C 第一步：把「整屏纹理」按屏幕坐标切片画进组件（Canvas 原生绘制，不经过 AGSL 输入）。
 * 只要切片与组件背后的画面完全对齐，就说明采集与坐标链路是对的，可以在它上面叠光学层。
 */
@Composable
fun Modifier.glassSlice(bitmap: Bitmap?): Modifier {
    if (bitmap == null) return this
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { origin = it.positionInWindow() }
        .drawBehind {
            // 整屏纹理按屏幕坐标平移后绘制，只保留组件矩形内的部分
            clipRect(
                left = 0f,
                top = 0f,
                right = size.width,
                bottom = size.height
            ) {
                translate(
                    left = -origin.x,
                    top = -origin.y
                ) {
                    drawImage(image = image)
                }
            }
        }
}

/**
 * 把液态玻璃画到当前组件的背景层。
 *
 * - API 33+ 且拿到壁纸时：走 AGSL 着色器（SDF + 高度场 + 双界面折射 + 色散 + GGX）
 * - 其它情况：完全不绘制，由调用方继续用原来的 surface/模糊玻璃兜底
 */
@Composable
fun Modifier.liquidGlass(
    wallpaper: Bitmap?,
    spec: LiquidGlassSpec = LiquidGlassSpec(),
    textureOrigin: Offset = Offset.Zero,
    enabled: Boolean = true
): Modifier {
    val density = LocalDensity.current
    if (!enabled || wallpaper == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        return this
    }
    // 注意：调用方必须传入**同一个 Bitmap 实例**并在原地更新像素，
    // 否则这里每次捕获都会重建模糊位图与 AGSL 程序（会导致全局卡顿）。
    val blurred = remember(wallpaper) { wallpaper.blurredCopy(10) }
    val program = remember(wallpaper, blurred) {
        runCatching { GlassProgram(wallpaper, blurred) }
            .onFailure { android.util.Log.w("LiquidGlass", "AGSL 初始化失败，回落普通玻璃", it) }
            .getOrNull()
    }
    if (program == null) return this

    val cornerPx = with(density) { spec.cornerRadius.toPx() }
    var origin by remember { mutableStateOf(Offset.Zero) }
    return this
        .onGloballyPositioned { origin = it.positionInWindow() }
        .drawBehind {
            program.draw(
                scope = this,
                origin = origin,
                textureOrigin = textureOrigin,
                cornerPx = cornerPx,
                spec = spec
            )
        }
}

/** 承载 RuntimeShader 与两张位图着色器。着色器只创建一次，uniform 每帧更新。 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class GlassProgram(sharp: Bitmap, blur: Bitmap) {
    private val shader = RuntimeShader(LIQUID_GLASS_AGSL)
    private val sharpShader: BitmapShader
    private val blurShader: BitmapShader
    private val screen = Size(sharp.width.toFloat(), sharp.height.toFloat())

    init {
        sharpShader = BitmapShader(sharp, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        blurShader = BitmapShader(blur, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        shader.setInputShader("uSharp", sharpShader)
        shader.setInputShader("uBlur", blurShader)
    }

    fun draw(
        scope: DrawScope,
        origin: Offset,
        textureOrigin: Offset,
        cornerPx: Float,
        spec: LiquidGlassSpec
    ) {
        val w = scope.size.width
        val h = scope.size.height
        if (w <= 0f || h <= 0f) return

        // 约定：Canvas 已平移到组件原点，着色器用**本地坐标**采样；
        // 由 BitmapShader 的矩阵把本地坐标映射到整屏纹理（local + origin）。
        val matrix = Matrix().apply { setTranslate(-origin.x, -origin.y) }
        sharpShader.setLocalMatrix(matrix)
        blurShader.setLocalMatrix(matrix)

        shader.setFloatUniform("uSize", w, h)
        shader.setFloatUniform("uResolution", screen.width, screen.height)
        shader.setFloatUniform("uOrigin", origin.x, origin.y)
        shader.setFloatUniform("uCorner", cornerPx)
        shader.setFloatUniform("uThickness", spec.thickness)
        shader.setFloatUniform("uNormalTransition", spec.normalTransition)
        shader.setFloatUniform("uBackgroundDistance", spec.backgroundDistance)
        shader.setFloatUniform("uIor", spec.ior)
        shader.setFloatUniform("uDispersion", spec.dispersion)
        shader.setFloatUniform("uRoughness", spec.roughness)
        shader.setFloatUniform(
            "uTint",
            spec.tint.red,
            spec.tint.green,
            spec.tint.blue,
            spec.tint.alpha
        )
        shader.setFloatUniform("uShadow", spec.shadow)
        shader.setFloatUniform("uEdgeGlow", spec.edgeGlow)
        shader.setFloatUniform("uSpecular", spec.specular)
        shader.setFloatUniform("uDebugSample", if (spec.debugSample) 1f else 0f)
        shader.setFloatUniform("uTime", (System.currentTimeMillis() % 100000L) / 1000f)

        scope.drawIntoCanvas { canvas ->
            val paint = android.graphics.Paint().apply {
                this.shader = this@GlassProgram.shader
                isAntiAlias = true
            }
            val native = canvas.nativeCanvas
            native.save()
            native.translate(origin.x, origin.y)
            native.drawRect(0f, 0f, w, h, paint)
            native.restore()
        }
    }
}

/** 生成低分辨率模糊位图（下采样 + 平滑放大，等价于廉价 mip）。 */
private fun Bitmap.blurredCopy(divisor: Int): Bitmap {
    val smallW = (width / divisor).coerceAtLeast(2)
    val smallH = (height / divisor).coerceAtLeast(2)
    val small = Bitmap.createScaledBitmap(this, smallW, smallH, true)
    val up = Bitmap.createScaledBitmap(small, width, height, true)
    if (small != up) small.recycle()
    return up
}

/** 供 Compose 侧把 ImageBitmap 转成着色器可用的屏幕尺寸位图。 */
fun androidx.compose.ui.graphics.ImageBitmap.toGlassBitmap(screenWidth: Int, screenHeight: Int): Bitmap {
    val src = asAndroidBitmap()
    return if (src.width == screenWidth && src.height == screenHeight) {
        src
    } else {
        Bitmap.createScaledBitmap(src, screenWidth, screenHeight, true)
    }
}

/** 兜底：把纯色背景画成一张位图，保证没有壁纸时着色器也有输入。 */
fun solidGlassBitmap(color: Color, width: Int, height: Int): Bitmap {
    val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    bmp.eraseColor(color.toArgb())
    return bmp
}
