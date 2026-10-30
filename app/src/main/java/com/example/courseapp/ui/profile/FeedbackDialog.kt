package com.example.courseapp.ui.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.theme.contentPalette

/** 反馈统一寄到这个邮箱，改地址只改这里。 */
const val FEEDBACK_EMAIL = "lingfangod666@gmail.com"

/** 关于 / 支持与分享里展示的仓库地址。 */
const val PROJECT_REPO_URL = "https://github.com/AsukaTamo/WEAKDOWN"

internal enum class FeedbackKind(val label: String) {
    Suggestion("功能建议"),
    Bug("问题反馈"),
    Other("其他")
}

internal fun feedbackSubject(kind: FeedbackKind): String = "WEAKDOWN 反馈 · ${kind.label}"

/**
 * 「帮助与反馈」的编辑界面。
 *
 * 应用本身没有后端，所以这里不做静默上传：写好后生成一封已经填好
 * 收件人、标题和正文的邮件，交给系统邮件应用，由用户自己点发送。
 * 设备上没有邮件应用时退化成「复制到剪贴板 + 提示收件地址」。
 */
@Composable
fun FeedbackDialog(
    isDarkMode: Boolean,
    appVersion: String,
    onDismiss: () -> Unit,
    onSendResult: (message: String, type: String) -> Unit
) {
    val palette = contentPalette(isDarkMode)
    val context = LocalContext.current
    var kind by remember { mutableStateOf(FeedbackKind.Suggestion) }
    var content by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var includeDeviceInfo by remember { mutableStateOf(true) }

    val deviceInfo = remember { deviceSummary() }
    val canSend = content.trim().length >= 5

    val send = {
        val subject = feedbackSubject(kind)
        val body = buildFeedbackBody(
            kind = kind,
            content = content.trim(),
            contact = contact.trim(),
            appVersion = appVersion,
            deviceInfo = if (includeDeviceInfo) deviceInfo else null
        )
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:${Uri.encode(FEEDBACK_EMAIL)}")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        // manifest 里声明了 mailto 的 <queries>，这里才查得到有没有装邮件应用（Android 11+ 包可见性）。
        val hasMailApp = intent.resolveActivity(context.packageManager) != null
        if (!hasMailApp) {
            copyToClipboard(context, body)
            onSendResult("没找到邮件应用，内容已复制，可发到 $FEEDBACK_EMAIL", "info")
        } else {
            // 用选择器而不是直接跳到默认应用：设备上常常同时装了 QQ 邮箱、网易邮箱和系统邮件。
            val opened = runCatching {
                context.startActivity(Intent.createChooser(intent, "选择邮件应用"))
            }.isSuccess
            if (opened) {
                onSendResult("已打开邮件应用，点发送即可寄出", "success")
            } else {
                copyToClipboard(context, body)
                onSendResult("没打开邮件应用，内容已复制，可发到 $FEEDBACK_EMAIL", "info")
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp)
                    .border(1.dp, palette.surfaceBorder, RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                color = palette.surfaceAlt
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "帮助与反馈",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.title
                            )
                            Text(
                                text = "写好后会生成一封发给开发者的邮件",
                                fontSize = 11.5.sp,
                                color = palette.muted
                            )
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(36.dp)
                                .elasticPress(CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "关闭",
                                tint = palette.muted,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FeedbackKind.entries.forEach { item ->
                            FilterChip(
                                selected = kind == item,
                                onClick = { kind = item },
                                label = {
                                    Text(item.label, fontSize = 12.5.sp)
                                },
                                shape = RoundedCornerShape(11.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = palette.inputSurface,
                                    labelColor = palette.body,
                                    selectedContainerColor = palette.accent,
                                    selectedLabelColor = palette.onAccent
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = kind == item,
                                    borderColor = palette.surfaceBorder,
                                    selectedBorderColor = Color.Transparent
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it.take(2000) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 132.dp),
                        placeholder = {
                            Text(
                                text = "说说遇到的问题或想要的功能，越具体越好",
                                fontSize = 13.sp,
                                color = palette.muted
                            )
                        },
                        textStyle = TextStyle(fontSize = 13.5.sp),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default)
                    )
                    Text(
                        text = "${content.length}/2000",
                        fontSize = 10.sp,
                        color = palette.muted,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(top = 3.dp, end = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = contact,
                        onValueChange = { contact = it.take(80) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("联系方式（选填）", fontSize = 12.sp) },
                        placeholder = { Text("邮箱 / QQ，方便回复你", fontSize = 12.sp) },
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 13.5.sp),
                        shape = RoundedCornerShape(14.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Done
                        )
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = includeDeviceInfo,
                            onCheckedChange = { includeDeviceInfo = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = palette.accent,
                                uncheckedColor = palette.muted,
                                checkmarkColor = palette.onAccent
                            )
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "附带设备与版本信息",
                                fontSize = 12.5.sp,
                                color = palette.title
                            )
                            Text(
                                text = "$appVersion · $deviceInfo",
                                fontSize = 10.5.sp,
                                color = palette.muted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "邮件收件人：$FEEDBACK_EMAIL",
                        fontSize = 10.5.sp,
                        color = palette.muted,
                        modifier = Modifier.padding(start = 2.dp, bottom = 14.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1f)
                                .elasticPress(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("取消") }
                        Button(
                            onClick = send,
                            enabled = canSend,
                            modifier = Modifier
                                .weight(1f)
                                .elasticPress(
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = canSend
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("发送邮件", fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

internal fun buildFeedbackBody(
    kind: FeedbackKind,
    content: String,
    contact: String,
    appVersion: String,
    deviceInfo: String?
): String = buildString {
    appendLine("【${kind.label}】")
    appendLine()
    appendLine(content)
    if (contact.isNotEmpty()) {
        appendLine()
        appendLine("联系方式：$contact")
    }
    if (deviceInfo != null) {
        appendLine()
        appendLine("—— 以下信息由 WEAKDOWN 自动附带 ——")
        appendLine("版本：$appVersion")
        appendLine("设备：$deviceInfo")
    }
    appendLine()
    append("发自我的 WEAKDOWN")
}

private fun deviceSummary(): String {
    val model = listOf(Build.MANUFACTURER, Build.MODEL)
        .filter { it.isNotBlank() }
        .joinToString(" ")
    val os = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
    return if (model.isBlank()) os else "$model · $os"
}

private fun copyToClipboard(context: Context, text: String) {
    runCatching {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        manager.setPrimaryClip(ClipData.newPlainText("WEAKDOWN 反馈", text))
    }
}

/** 用系统浏览器/应用打开外部链接；没有可用应用时静默失败。 */
fun openExternalUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}
