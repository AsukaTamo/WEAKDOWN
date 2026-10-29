package com.example.courseapp.ui.profile

import android.content.Intent
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.courseapp.data.model.Schools
import com.example.courseapp.data.model.Semester
import com.example.courseapp.data.model.UserProfile
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.theme.contentPalette
import kotlin.math.roundToInt

/**
 * 个人信息编辑。当前阶段不做账号体系，直接本地编辑、保存后立刻刷新「我的」页与背景学校。
 * 用 Dialog 而不是页面内浮层，编辑时不会被顶栏/底栏盖住。
 */
@Composable
fun PersonalInfoDialog(
    profile: UserProfile,
    isDarkMode: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, schoolId: String, major: String, grade: String, avatarUri: String) -> Unit
) {
    val palette = contentPalette(isDarkMode)
    val context = LocalContext.current
    var name by remember { mutableStateOf(profile.name) }
    var schoolId by remember { mutableStateOf(profile.schoolId) }
    var major by remember { mutableStateOf(profile.major) }
    var grade by remember { mutableStateOf(profile.grade) }
    var schoolMenuOpen by remember { mutableStateOf(false) }
    var pendingAvatar by remember { mutableStateOf<String?>(null) }
    var avatarRemoved by remember { mutableStateOf(false) }
    val school = remember(schoolId) { Schools.byId(schoolId) }
    val dialogSurface = if (isDarkMode) Color(0xFF17171E) else Color(0xFFFFFCF2)

    val avatarPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val pending = ProfileAvatarStore.createPending(context, uri)
            if (pending != null) {
                ProfileAvatarStore.discardPending(pendingAvatar)
                pendingAvatar = pending
                avatarRemoved = false
            }
        }
    }

    val activeAvatarPath = when {
        pendingAvatar != null -> pendingAvatar
        avatarRemoved -> null
        else -> profile.avatarUri.ifEmpty { null }
    }
    val avatarBitmap = remember(activeAvatarPath) {
        ProfileAvatarStore.decodeFile(activeAvatarPath, 256)
    }
    val avatarInitial = name.trim().firstOrNull()?.toString() ?: profile.avatarText
    val dismissWithCleanup = {
        ProfileAvatarStore.discardPending(pendingAvatar)
        onDismiss()
    }

    Dialog(
        onDismissRequest = dismissWithCleanup,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .border(1.dp, palette.surfaceBorder, RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                color = dialogSurface
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
                        Text(
                            text = "个人信息",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.title,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = dismissWithCleanup,
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

                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(78.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .elasticPress(CircleShape)
                                .clip(CircleShape)
                                .background(
                                    if (isDarkMode) Color(0xFF232B39) else Color(0xFFFFF1D2)
                                )
                                .clickable {
                                    avatarPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (avatarBitmap != null) {
                                Image(
                                    bitmap = avatarBitmap.asImageBitmap(),
                                    contentDescription = "头像",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = avatarInitial,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = palette.accent
                                )
                            }
                        }
                        // 相机角标（不参与圆形裁切，压在头像右下角）
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(palette.accent)
                                .border(2.dp, dialogSurface, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "更换头像",
                                tint = palette.onAccent,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }

                    if (avatarBitmap != null) {
                        TextButton(
                            onClick = {
                                ProfileAvatarStore.discardPending(pendingAvatar)
                                pendingAvatar = null
                                avatarRemoved = true
                            },
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .elasticPress(ButtonDefaults.textShape)
                        ) {
                            Text(
                                text = "移除头像",
                                fontSize = 11.sp,
                                color = palette.muted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("姓名") },
                        placeholder = { Text("张同学") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box {
                        OutlinedTextField(
                            value = school.name,
                            onValueChange = {},
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("学校") },
                            readOnly = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = if (schoolMenuOpen) {
                                        Icons.Default.KeyboardArrowUp
                                    } else {
                                        Icons.Default.KeyboardArrowDown
                                    },
                                    contentDescription = "选择学校",
                                    tint = palette.muted,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            shape = RoundedCornerShape(12.dp)
                        )
                        // 透明覆盖层：整块输入框都能唤起学校列表
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { schoolMenuOpen = true }
                        )
                        DropdownMenu(
                            expanded = schoolMenuOpen,
                            onDismissRequest = { schoolMenuOpen = false },
                            modifier = Modifier.background(if (isDarkMode) Color(0xFF17171E) else Color(0xFFFFFCF2))
                        ) {
                            Schools.All.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = item.name,
                                            fontSize = 14.sp,
                                            color = palette.title,
                                            fontWeight = if (item.id == schoolId) {
                                                FontWeight.SemiBold
                                            } else {
                                                FontWeight.Normal
                                            }
                                        )
                                    },
                                    modifier = Modifier.elasticPress(RoundedCornerShape(8.dp)),
                                    onClick = {
                                        schoolId = item.id
                                        schoolMenuOpen = false
                                    },
                                    trailingIcon = {
                                        if (item.id == schoolId) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = palette.accent,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = major,
                        onValueChange = { major = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("专业") },
                        placeholder = { Text("计算机科学与技术") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = grade,
                        onValueChange = { grade = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("年级") },
                        placeholder = { Text("2023") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = dismissWithCleanup,
                            modifier = Modifier
                                .weight(1f)
                                .elasticPress(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) { Text("取消") }
                        Button(
                            onClick = {
                                val committed = pendingAvatar?.let {
                                    ProfileAvatarStore.commit(context, it)
                                }
                                val nextAvatar = when {
                                    pendingAvatar != null -> committed ?: profile.avatarUri
                                    avatarRemoved -> ""
                                    else -> profile.avatarUri
                                }
                                ProfileAvatarStore.discardPending(pendingAvatar)
                                onSave(name, schoolId, major, grade, nextAvatar)
                            },
                            enabled = name.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .elasticPress(
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = name.isNotBlank()
                                ),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                        ) { Text("保存") }
                    }
                }
            }
        }
    }
}

/**
 * 课表背景详情页。原来的「选择图片 / 遮罩 / 辅助线 / 清除」全部收在这里，
 * 主页面只留一个快捷入口，避免「我的」页被一条很长的背景设置撑开。
 */
@Composable
fun BackgroundSettingsDialog(
    semester: Semester?,
    isDarkMode: Boolean,
    showScheduleGuides: Boolean,
    onScheduleGuidesChange: (Boolean) -> Unit,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onScrimCommit: (Float) -> Unit,
    onCreateSemester: () -> Unit,
    onDismiss: () -> Unit
) {
    val palette = contentPalette(isDarkMode)
    var scrim by remember(semester?.id, semester?.scrimAlpha) {
        mutableFloatStateOf(semester?.scrimAlpha ?: 0.4f)
    }
    val hasImage = !semester?.backgroundUri.isNullOrEmpty()
    val preview = remember(semester?.backgroundUri) {
        val uri = semester?.backgroundUri
        if (uri.isNullOrEmpty()) {
            null
        } else {
            BitmapFactory.decodeFile(uri)?.asImageBitmap()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = palette.canvasStart
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                        .height(48.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(44.dp)
                            .elasticPress(CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = palette.title,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "课表背景",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.title
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                    ) {
                        Text("完成", color = palette.accent, fontSize = 13.sp)
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(188.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(palette.inputSurface)
                            .border(1.dp, palette.surfaceBorder, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (preview != null) {
                            Image(
                                bitmap = preview,
                                contentDescription = "背景预览",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Outlined.Wallpaper,
                                    contentDescription = null,
                                    tint = palette.muted,
                                    modifier = Modifier.size(30.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (semester == null) "还没有学期" else "还没有设置背景",
                                    fontSize = 12.5.sp,
                                    color = palette.muted
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "选一张图，让课表更有你的样子",
                                    fontSize = 10.5.sp,
                                    color = palette.muted.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }

                    if (semester == null) {
                        Button(
                            onClick = onCreateSemester,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .elasticPress(RoundedCornerShape(14.dp)),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("先创建一个学期")
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onPickImage,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .elasticPress(RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                            ) {
                                Text(if (hasImage) "更换图片" else "选择图片", fontSize = 13.sp)
                            }
                            if (hasImage) {
                                OutlinedButton(
                                    onClick = onClearImage,
                                    modifier = Modifier
                                        .height(44.dp)
                                        .elasticPress(RoundedCornerShape(14.dp)),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = palette.destructive
                                    )
                                ) {
                                    Text("清除", fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(palette.surface)
                                .border(1.dp, palette.surfaceBorder, RoundedCornerShape(18.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "背景遮罩",
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = palette.title
                                    )
                                    Text(
                                        text = "数值越高，课程文字越清晰",
                                        fontSize = 10.5.sp,
                                        color = palette.muted
                                    )
                                }
                                Text(
                                    text = "${(scrim * 100).roundToInt()}%",
                                    fontSize = 12.sp,
                                    color = palette.accent,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Slider(
                                value = scrim,
                                onValueChange = { scrim = it },
                                onValueChangeFinished = { onScrimCommit(scrim) },
                                valueRange = 0f..0.9f,
                                colors = SliderDefaults.colors(
                                    thumbColor = palette.accent,
                                    activeTrackColor = palette.accent,
                                    inactiveTrackColor = palette.accentSoft
                                )
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .elasticPress(RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                                .background(palette.surface)
                                .border(1.dp, palette.surfaceBorder, RoundedCornerShape(18.dp))
                                .clickable { onScheduleGuidesChange(!showScheduleGuides) }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.GridOn,
                                contentDescription = null,
                                tint = palette.muted,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "课表辅助线",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = palette.title
                                )
                                Text(
                                    text = "控制课表横向分割线显示",
                                    fontSize = 10.5.sp,
                                    color = palette.muted
                                )
                            }
                            Switch(
                                checked = showScheduleGuides,
                                onCheckedChange = null
                            )
                        }
                    }

                    Text(
                        text = "背景与遮罩只作用于首页课表；「我的」页始终使用学校水印。",
                        fontSize = 10.5.sp,
                        color = palette.muted,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SupportDialog(
    isDarkMode: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("支持 CourseApp", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("如果它帮到了你，欢迎分享给同学。", fontSize = 13.sp)
                Text(
                    "也可以到「帮助与反馈」留下建议，每一条都会看。",
                    fontSize = 12.sp,
                    color = contentPalette(isDarkMode).muted
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(
                            Intent.EXTRA_TEXT,
                            "推荐一个干净的课表 App：CourseApp，支持教务系统导入、液态玻璃课表。"
                        )
                    }
                    runCatching {
                        context.startActivity(Intent.createChooser(send, "分享 CourseApp"))
                    }
                    onDismiss()
                },
                modifier = Modifier.elasticPress(ButtonDefaults.textShape)
            ) { Text("分享给同学") }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.elasticPress(ButtonDefaults.textShape)
            ) { Text("关闭") }
        }
    )
}

@Composable
fun AboutDialog(
    version: String,
    isDarkMode: Boolean,
    onDismiss: () -> Unit
) {
    val palette = contentPalette(isDarkMode)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于应用", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("CourseApp  $version", fontSize = 13.sp, color = palette.title)
                Text("面向大学生的课程表应用", fontSize = 12.sp, color = palette.muted)
                Text("课表展示 · 教务导入 · 学期与作息管理", fontSize = 12.sp, color = palette.muted)
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.elasticPress(ButtonDefaults.textShape)
            ) { Text("知道了") }
        }
    )
}
