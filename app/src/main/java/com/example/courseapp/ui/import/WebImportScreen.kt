package com.example.courseapp.ui.import

import android.annotation.SuppressLint
import android.webkit.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.courseapp.ui.theme.*

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebImportScreen(
    isDarkMode: Boolean = false,
    onBack: () -> Unit = {},
    onImportSuccess: (String) -> Unit = {},
    onParseHtml: (String) -> Unit = {}
) {
    var currentUrl by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showParseButton by remember { mutableStateOf(false) }

    val bgColor = if (isDarkMode) BgDark else Color.White
    val textColor = if (isDarkMode) Color.White else TextPrimary

    Column(modifier = Modifier.fillMaxSize().background(bgColor)) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(if (isDarkMode) CardDark else Color(0xFFF5F5F5))
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
            if (showParseButton) {
                TextButton(
                    onClick = {
                        webView?.evaluateJavascript(
                            "(function() { return document.documentElement.outerHTML; })()"
                        ) { html ->
                            if (html != null && html.length > 100) {
                                onParseHtml(html)
                            }
                        }
                    }
                ) {
                    Text("解析", color = Primary, fontWeight = FontWeight.SemiBold)
                }
            }
            IconButton(
                onClick = { webView?.reload() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Refresh, "刷新", tint = textColor, modifier = Modifier.size(20.dp))
            }
        }

        // URL bar
        if (currentUrl.isNotEmpty()) {
            Text(
                text = currentUrl,
                fontSize = 11.sp,
                color = if (isDarkMode) Color(0xFF8E93A6) else TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                maxLines = 1
            )
        }

        // Loading indicator
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = Primary
            )
        }

        // WebView
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = true
                    settings.userAgentString = "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36"

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            currentUrl = url ?: ""
                            isLoading = false

                            // Auto-detect course table page
                            url?.let {
                                if (it.contains("kebiao") || it.contains("schedule") || it.contains("xskb") || it.contains("course")) {
                                    showParseButton = true
                                    // Auto-parse
                                    view?.evaluateJavascript(
                                        "(function() { return document.documentElement.outerHTML; })()"
                                    ) { html ->
                                        if (html != null && html.length > 100) {
                                            onParseHtml(html)
                                        }
                                    }
                                }
                            }
                        }

                        override fun shouldOverrideUrlLoading(
                            view: WebView?, request: WebResourceRequest?
                        ): Boolean = false
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            isLoading = newProgress < 100
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { webView = it }
        )
    }
}
