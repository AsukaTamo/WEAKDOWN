package com.example.courseapp.ui.components

import android.graphics.LinearGradient
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Shader
import android.os.Build
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import io.github.itshedimisawi.vitro.GlassBackdrop

/**
 * 在页面**底部**边缘叠一层真正的高斯模糊，并用竖向渐变遮罩淡出。
 *
 * 顶部不做模糊：状态栏/系统 tag 栏本身有自己的底色，叠加模糊会与其产生色差冲突。
 * 只把 backdrop 底部的一条带状区域重录到小图层再模糊，避免整屏模糊的开销。
 * 需要 Android 12（API 31）及以上，低版本直接跳过（页面保持清晰）。
 */
fun Modifier.edgeBlur(
    backdrop: GlassBackdrop,
    radius: Dp = 18.dp,
    edgeHeight: Dp = 110.dp,
): Modifier = this then EdgeBlurElement(backdrop, radius, edgeHeight)

private data class EdgeBlurElement(
    private val backdrop: GlassBackdrop,
    private val radius: Dp,
    private val edgeHeight: Dp,
) : ModifierNodeElement<EdgeBlurNode>() {

    override fun create(): EdgeBlurNode = EdgeBlurNode(backdrop, radius, edgeHeight)

    override fun update(node: EdgeBlurNode) {
        node.backdrop = backdrop
        node.radius = radius
        node.edgeHeight = edgeHeight
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "edgeBlur"
        properties["radius"] = radius
        properties["edgeHeight"] = edgeHeight
    }

    override fun equals(other: Any?): Boolean =
        other is EdgeBlurElement &&
            other.backdrop === backdrop &&
            other.radius == radius &&
            other.edgeHeight == edgeHeight

    override fun hashCode(): Int {
        var result = backdrop.hashCode()
        result = 31 * result + radius.hashCode()
        result = 31 * result + edgeHeight.hashCode()
        return result
    }
}

private class EdgeBlurNode(
    var backdrop: GlassBackdrop,
    var radius: Dp,
    var edgeHeight: Dp,
) : Modifier.Node(), DrawModifierNode {

    private var bottomLayer: GraphicsLayer? = null
    private val maskPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    override fun ContentDrawScope.draw() {
        drawContent()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        if (backdrop.coordinates?.isAttached != true) return

        val pageWidth = size.width
        val pageHeight = size.height
        if (pageWidth <= 0f || pageHeight <= 0f) return

        val band = with(density) { edgeHeight.toPx() }.coerceIn(1f, pageHeight / 2f)
        val radiusPx = with(density) { radius.toPx() }
        val context = requireGraphicsContext()

        val bottom = bottomLayer ?: context.createGraphicsLayer().also { bottomLayer = it }
        bottom.record(this, layoutDirection, IntSize(pageWidth.toInt(), band.toInt())) {
            translate(0f, -(pageHeight - band)) { drawLayer(backdrop.layer) }
        }
        bottom.renderEffect = BlurEffect(radiusPx, radiusPx, TileMode.Clamp)

        val y = pageHeight - band
        drawIntoCanvas { canvas ->
            canvas.saveLayer(Rect(0f, y, pageWidth, pageHeight), Paint())
            translate(0f, y) { drawLayer(bottom) }
            // 用原生 DstIn + 竖向 LinearGradient 做遮罩：越靠屏幕底部越模糊，向上渐隐。
            maskPaint.shader = LinearGradient(
                0f,
                y,
                0f,
                pageHeight,
                intArrayOf(android.graphics.Color.TRANSPARENT, android.graphics.Color.BLACK),
                null,
                Shader.TileMode.CLAMP
            )
            canvas.nativeCanvas.drawRect(0f, y, pageWidth, pageHeight, maskPaint)
            canvas.restore()
        }
    }

    override fun onDetach() {
        val context = requireGraphicsContext()
        bottomLayer?.let(context::releaseGraphicsLayer)
        bottomLayer = null
    }
}
