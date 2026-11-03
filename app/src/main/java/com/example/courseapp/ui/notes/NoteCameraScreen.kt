package com.example.courseapp.ui.notes

import android.view.ViewGroup
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FlashOff
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.courseapp.data.model.NoteDraft
import com.example.courseapp.ui.theme.AppColors
import java.io.File

/**
 * CameraX 实时取景。生命周期绑定到当前 LifecycleOwner，销毁时解绑并释放；
 * 拍照串行化（capturing 守卫），快速连点不会产生重复文件或重复记录。
 */
@Composable
fun NoteCameraScreen(
    isDarkMode: Boolean,
    capturedCount: Int,
    latestDraft: NoteDraft?,
    onClose: () -> Unit,
    onImport: () -> Unit,
    onCaptured: (File) -> Unit,
    onOpenPreview: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var hasFlash by remember { mutableStateOf(false) }
    var hasFront by remember { mutableStateOf(false) }
    var hasBack by remember { mutableStateOf(true) }
    var capturing by remember { mutableStateOf(false) }
    var cameraReady by remember { mutableStateOf(false) }
    var previewStreaming by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retryTick by remember { mutableStateOf(0) }

    // 预览真正出帧后再允许快门：CameraX 绑定成功但首帧未到时拍照会得到全黑图片。
    DisposableEffect(previewView) {
        val view = previewView
        if (view == null) return@DisposableEffect onDispose { }
        val observer = androidx.lifecycle.Observer<PreviewView.StreamState> { state ->
            previewStreaming = state == PreviewView.StreamState.STREAMING
        }
        view.previewStreamState.observeForever(observer)
        onDispose {
            view.previewStreamState.removeObserver(observer)
            previewStreaming = false
        }
    }

    DisposableEffect(lifecycleOwner, lensFacing, previewView, retryTick) {
        val view = previewView
        if (view == null) return@DisposableEffect onDispose { }
        var disposed = false
        var provider: ProcessCameraProvider? = null
        val future = ProcessCameraProvider.getInstance(context)
        val listener = Runnable {
            if (disposed) return@Runnable
            try {
                val cameraProvider = future.get()
                provider = cameraProvider
                cameraProvider.unbindAll()
                val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                if (!cameraProvider.hasCamera(selector)) {
                    error = if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
                        "设备没有前置摄像头，可切换到后置或从相册导入"
                    } else {
                        "设备没有可用摄像头，请从相册导入照片"
                    }
                    return@Runnable
                }
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(view.surfaceProvider)
                }
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setFlashMode(flashMode)
                    .build()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    selector,
                    preview,
                    capture
                )
                imageCapture = capture
                hasFlash = camera.cameraInfo.hasFlashUnit()
                hasFront = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
                hasBack = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
                cameraReady = true
                error = null
            } catch (throwable: Throwable) {
                cameraReady = false
                error = "相机初始化失败：${throwable.message ?: "未知错误"}"
            }
        }
        future.addListener(listener, ContextCompat.getMainExecutor(context))
        onDispose {
            disposed = true
            provider?.unbindAll()
            provider = null
            imageCapture = null
            cameraReady = false
        }
    }

    LaunchedEffect(flashMode) {
        imageCapture?.flashMode = flashMode
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF0C1116))) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                    previewView = this
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 顶部/底部渐变，保证按钮在任何画面下都清晰
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.55f),
                        0.28f to Color.Transparent,
                        0.62f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.78f)
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CameraRoundButton(Icons.Outlined.Close, "关闭相机", onClose)
                Spacer(Modifier.weight(1f))
                Text(
                    "课堂拍摄",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                CameraRoundButton(
                    icon = Icons.Outlined.Cached,
                    description = "切换前后摄像头",
                    enabled = hasFront && hasBack,
                    onClick = {
                        lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                            CameraSelector.LENS_FACING_FRONT
                        } else {
                            CameraSelector.LENS_FACING_BACK
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                CameraRoundButton(
                    icon = if (flashMode == ImageCapture.FLASH_MODE_ON) Icons.Outlined.FlashOn else Icons.Outlined.FlashOff,
                    description = if (flashMode == ImageCapture.FLASH_MODE_ON) "关闭闪光灯" else "打开闪光灯",
                    enabled = hasFlash,
                    onClick = {
                        flashMode = if (flashMode == ImageCapture.FLASH_MODE_ON) {
                            ImageCapture.FLASH_MODE_OFF
                        } else {
                            ImageCapture.FLASH_MODE_ON
                        }
                    }
                )
            }

            Spacer(Modifier.weight(1f))

            if (error != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.62f))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = error ?: "",
                        color = Color.White,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        CameraTextButton("重试", onClick = { retryTick++ })
                        CameraTextButton("从相册导入", onClick = onImport)
                    }
                }
            } else {
                Text(
                    text = if (cameraReady && previewStreaming) {
                        "将 PPT 或板书置于取景框内"
                    } else {
                        "正在启动相机…"
                    },
                    color = Color.White.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 相册导入 & 已拍数量
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.14f))
                            .clickable(onClick = onImport),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Outlined.PhotoLibrary,
                            contentDescription = "从相册导入",
                            tint = Color.White,
                            modifier = Modifier.size(23.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (capturedCount > 0) "已拍 $capturedCount 张" else "相册",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 9.sp
                    )
                }

                // 快门
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.22f))
                        .padding(5.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable(enabled = cameraReady && previewStreaming && !capturing) {
                            if (capturing || !previewStreaming) return@clickable
                            val capture = imageCapture ?: return@clickable
                            capturing = true
                            error = null
                            val dir = File(context.filesDir, "notebook/tmp").also { it.mkdirs() }
                            val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
                            val options = ImageCapture.OutputFileOptions.Builder(file).build()
                            capture.takePicture(
                                options,
                                ContextCompat.getMainExecutor(context),
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        capturing = false
                                        onCaptured(file)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        capturing = false
                                        file.delete()
                                        error = "拍摄失败：${exception.message ?: "请重试"}"
                                    }
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (capturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = Color(0xFF2F68B5),
                            strokeWidth = 3.dp
                        )
                    } else if (!previewStreaming) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = Color(0xFF2F68B5),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Box(
                            Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF7F9FB))
                        )
                    }
                }

                // 镜头切换 / 待保存预览
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (capturedCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .clickable(onClick = onOpenPreview),
                            contentAlignment = Alignment.Center
                        ) {
                            val thumb = rememberNoteFileBitmap(
                                latestDraft?.let { File(it.sourcePath) },
                                latestDraft?.rotation ?: 0,
                                maxSize = 256
                            )
                            if (thumb != null) {
                                Image(
                                    bitmap = thumb.asImageBitmap(),
                                    contentDescription = "查看待保存照片",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.CameraAlt,
                                    contentDescription = "查看待保存照片",
                                    tint = Color.White,
                                    modifier = Modifier.size(23.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("预览", color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp)
                    } else {
                        CameraRoundButton(
                            icon = Icons.Outlined.Cached,
                            description = "切换前后摄像头",
                            enabled = hasFront && hasBack,
                            onClick = {
                                lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                    CameraSelector.LENS_FACING_FRONT
                                } else {
                                    CameraSelector.LENS_FACING_BACK
                                }
                            }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text("切换", color = Color.White.copy(alpha = 0.75f), fontSize = 9.sp)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "可以连续拍摄多张，之后统一预览、编辑和归档",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun CameraRoundButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    description: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (enabled) 0.16f else 0.06f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = description,
            tint = Color.White.copy(alpha = if (enabled) 1f else 0.35f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun CameraTextButton(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp)
    ) {
        Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ── 拍摄预览 ──────────────────────────────────────────────────────────────

@Composable
fun NoteCapturePreviewScreen(
    draft: NoteDraft,
    drafts: List<NoteDraft>,
    colors: AppColors,
    onClose: () -> Unit,
    onContinueShooting: () -> Unit,
    onSelectDraft: (NoteDraft) -> Unit,
    onRetake: () -> Unit,
    onEdit: () -> Unit,
    onSkipEdit: () -> Unit,
    onBatchSave: () -> Unit
) {
    val image = rememberNoteFileBitmap(File(draft.sourcePath), draft.rotation, maxSize = 2048)
    Box(Modifier.fillMaxSize().background(Color(0xFF0C1116))) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CameraRoundButton(Icons.Outlined.Close, "关闭预览", onClose)
                Spacer(Modifier.weight(1f))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("拍摄预览", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    if (drafts.size > 1) {
                        Text("第 ${drafts.indexOfFirst { it.id == draft.id } + 1} / ${drafts.size} 张", color = Color.White.copy(alpha = 0.65f), fontSize = 10.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (drafts.size > 1) {
                        Text(
                            "${drafts.size}",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    CameraRoundButton(
                        icon = Icons.Outlined.CameraAlt,
                        description = "继续拍摄",
                        onClick = onContinueShooting
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (image != null) {
                    Image(
                        bitmap = image.asImageBitmap(),
                        contentDescription = "刚拍摄的照片",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            if (drafts.size > 1) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    drafts.take(8).forEach { item ->
                        val thumb = rememberNoteFileBitmap(File(item.sourcePath), item.rotation, maxSize = 256)
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.12f))
                                .then(
                                    if (item.id == draft.id) {
                                        Modifier.border(
                                            2.dp,
                                            Color(0xFF2F68B5),
                                            RoundedCornerShape(10.dp)
                                        )
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { onSelectDraft(item) }
                        ) {
                            if (thumb != null) {
                                Image(
                                    bitmap = thumb.asImageBitmap(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                CameraOutlineButton("重拍", onRetake, Modifier.weight(1f))
                CameraOutlineButton("直接归档", onSkipEdit, Modifier.weight(1f))
                CameraSolidButton("编辑", onEdit, Modifier.weight(1f))
            }
            if (drafts.size > 1) {
                Spacer(Modifier.height(9.dp))
                CameraSolidButton(
                    "批量保存 ${drafts.size} 张",
                    onBatchSave,
                    Modifier.fillMaxWidth(),
                    primary = false
                )
            }
        }
    }
}

@Composable
private fun CameraOutlineButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .height(46.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CameraSolidButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(15.dp))
            .background(if (primary) Color(0xFF2F68B5) else Color.White.copy(alpha = 0.18f))
            .clickable(onClick = onClick)
            .height(46.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
