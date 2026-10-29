package com.example.courseapp.ui.import

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.courseapp.data.model.Schools
import com.example.courseapp.ui.components.elasticPress
import com.example.courseapp.ui.theme.*

private val schoolList = Schools.All

@Composable
fun WebImportScreen(
    isDarkMode: Boolean = false,
    showHeader: Boolean = true,
    onBack: () -> Unit = {},
    onParseHtml: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedSchool by remember { mutableStateOf(schoolList[0]) }
    var showSchoolDropdown by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }

    val webImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val html = result.data?.getStringExtra("html") ?: ""
            if (html.isNotEmpty()) {
                onParseHtml(html)
            }
        }
    }

    val palette = contentPalette(isDarkMode)
    val textColor = palette.title
    val subColor = palette.muted
    val cardColor = palette.surfaceAlt

    Column(modifier = Modifier.fillMaxSize().background(Color.Transparent)) {
        if (showHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .background(cardColor)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .elasticPress(CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = textColor, modifier = Modifier.size(20.dp))
                }
                Text(
                    text = "教务系统导入",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                // 作为浮层内容时要能在固定高度的卡片里滚动；独立页面则铺满剩余高度。
                .then(if (showHeader) Modifier.weight(1f) else Modifier.fillMaxSize())
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = palette.accent
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("选择学校教务系统", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor)
            Spacer(modifier = Modifier.height(8.dp))
            Text("在应用内打开学校官网，登录后进入课表页面即可导入", fontSize = 13.sp, color = subColor)

            Spacer(modifier = Modifier.height(32.dp))

            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .elasticPress(RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .background(cardColor)
                        .clickable { showSchoolDropdown = true }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("当前学校", fontSize = 11.sp, color = subColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(selectedSchool.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = textColor)
                    }
                    Icon(
                        if (showSchoolDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        null, tint = subColor, modifier = Modifier.size(24.dp)
                    )
                }

                DropdownMenu(
                    expanded = showSchoolDropdown,
                    onDismissRequest = { showSchoolDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.85f).background(cardColor)
                ) {
                    schoolList.forEach { school ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    school.name, fontSize = 14.sp, color = textColor,
                                    fontWeight = if (school == selectedSchool) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            modifier = Modifier.elasticPress(RoundedCornerShape(8.dp)),
                            onClick = { selectedSchool = school; showSchoolDropdown = false },
                            trailingIcon = {
                                if (school == selectedSchool) {
                                    Icon(Icons.Default.Check, null, tint = palette.accent, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(selectedSchool.portalUrl, fontSize = 12.sp, color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis)

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val intent = Intent(context, WebImportActivity::class.java).apply {
                        putExtra("school_url", selectedSchool.portalUrl)
                    }
                    webImportLauncher.launch(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .elasticPress(RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
            ) {
                Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("打开教务系统", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 使用说明收成一个小文字按钮，不再占一大块卡片；点击后弹出提示。
            TextButton(
                onClick = { showHelp = true },
                modifier = Modifier
                    .elasticPress(RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp)),
                colors = ButtonDefaults.textButtonColors(contentColor = palette.accent)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("使用说明", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("使用说明") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("1. 点击上方按钮打开学校官网", fontSize = 13.sp, color = subColor)
                    Text("2. 登录教务系统并进入课表页面", fontSize = 13.sp, color = subColor)
                    Text("3. 页面加载完成后，点击底部\"导入到课表\"按钮", fontSize = 13.sp, color = subColor)
                    Text("4. 确认课程信息后完成导入", fontSize = 13.sp, color = subColor)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showHelp = false },
                    modifier = Modifier.elasticPress(ButtonDefaults.textShape)
                ) {
                    Text("知道了")
                }
            }
        )
    }
}
