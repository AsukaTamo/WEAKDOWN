package com.example.courseapp.ui.import

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.courseapp.ui.theme.*

data class SchoolInfo(
    val name: String,
    val url: String
)

private val schoolList = listOf(
    SchoolInfo("中国地质大学(武汉)", "https://sfrz.cug.edu.cn/tpass/login?service=https%3A%2F%2Fi.cug.edu.cn%2Fweb%2F%3FCASLOGIN%3DCASLOGIN%23%2Flogin%3FredirectUrl%3D%252Fweb%252F%253FCASLOGIN%253DCASLOGIN%2523%252Fcurrent%252Fsys-portal"),
    SchoolInfo("武汉大学", "https://www.whu.edu.cn/"),
    SchoolInfo("华中科技大学", "https://www.hust.edu.cn/"),
    SchoolInfo("武汉理工大学", "https://www.whut.edu.cn/"),
    SchoolInfo("华中师范大学", "https://www.ccnu.edu.cn/"),
    SchoolInfo("中南财经政法大学", "https://www.zuel.edu.cn/")
)

@Composable
fun WebImportScreen(
    isDarkMode: Boolean = false,
    onBack: () -> Unit = {},
    onImportSuccess: (String) -> Unit = {},
    onParseHtml: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedSchool by remember { mutableStateOf(schoolList[0]) }
    var showSchoolDropdown by remember { mutableStateOf(false) }

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

    val bgColor = if (isDarkMode) BgDark else Color.White
    val textColor = if (isDarkMode) Color.White else TextPrimary
    val subColor = if (isDarkMode) Color(0xFFB0B0B0) else TextSecondary
    val cardColor = if (isDarkMode) CardDark else Color(0xFFF5F5F5)

    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(cardColor)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.ArrowBack, "返回", tint = textColor, modifier = Modifier.size(20.dp))
            }
            Text(
                text = "教务系统导入",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Primary
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
                            onClick = { selectedSchool = school; showSchoolDropdown = false },
                            trailingIcon = {
                                if (school == selectedSchool) {
                                    Icon(Icons.Default.Check, null, tint = Primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(selectedSchool.url, fontSize = 12.sp, color = subColor, maxLines = 1, overflow = TextOverflow.Ellipsis)

            Spacer(modifier = Modifier.height(40.dp))

            Button(
                onClick = {
                    val intent = Intent(context, WebImportActivity::class.java).apply {
                        putExtra("school_url", selectedSchool.url)
                    }
                    webImportLauncher.launch(intent)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.OpenInBrowser, null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("打开教务系统", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Primary.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("使用说明", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Primary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("1. 点击上方按钮打开学校官网", fontSize = 13.sp, color = subColor)
                    Text("2. 登录教务系统并进入课表页面", fontSize = 13.sp, color = subColor)
                    Text("3. 页面加载完成后，点击底部\"导入到课表\"按钮", fontSize = 13.sp, color = subColor)
                    Text("4. 确认课程信息后完成导入", fontSize = 13.sp, color = subColor)
                }
            }
        }
    }
}