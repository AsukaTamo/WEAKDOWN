package com.example.courseapp.ui.import

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import com.example.courseapp.R

private const val TAG = "WebImport"

class WebImportActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var btnToggleUA: Button
    private var isDesktopMode = true

    private val desktopUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
    private val mobileUA = "Mozilla/5.0 (Linux; Android 13; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Mobile Safari/537.36"

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_import)

        webView = findViewById(R.id.webView)
        val btnImport = findViewById<Button>(R.id.btnImport)
        btnToggleUA = findViewById(R.id.btnToggleUA)

        // 部分学校（武理 jwapp）的教务只有移动端页面才给课表表格，由调用方指定首屏 UA。
        isDesktopMode = !intent.getBooleanExtra("mobile_ua", false)

        // WebView settings
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            useWideViewPort = isDesktopMode
            loadWithOverviewMode = isDesktopMode
            
            // Zoom settings
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = true
            allowContentAccess = true
            allowFileAccess = true
            setGeolocationEnabled(true)
            
            // Enable pinch-to-zoom gestures
            javaScriptCanOpenWindowsAutomatically = false
            
            // Text zoom percentage
            textZoom = 100
            
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // 桌面 UA 让站点返回完整 PC 版布局；移动端 Web 教务（武理）则直接用手机 UA
            userAgentString = if (isDesktopMode) desktopUA else mobileUA
        }
        btnToggleUA.text = if (isDesktopMode) "手机版" else "电脑版"

        // 统一身份认证（CAS）会跨域跳转几次，第三方 Cookie 不放行的话登录态带不到教务系统。
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        // Keep navigation in-app
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                return false // Let WebView handle all URLs
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(cm: ConsoleMessage?): Boolean {
                cm?.let { Log.d(TAG, "JS: ${it.message()}") }
                return true
            }
        }

        // 返回键优先回退 WebView 历史，没有历史时才退出导入页
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        // Load school URL
        val schoolUrl = intent.getStringExtra("school_url") ?: ""
        if (schoolUrl.isNotEmpty()) {
            webView.loadUrl(schoolUrl)
        } else {
            Toast.makeText(this, "未指定学校网址", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Toggle UA button: switch between mobile and desktop mode
        btnToggleUA.setOnClickListener {
            isDesktopMode = !isDesktopMode
            
            if (isDesktopMode) {
                webView.settings.userAgentString = desktopUA
                btnToggleUA.text = "手机版"
                webView.settings.useWideViewPort = true
                webView.settings.loadWithOverviewMode = true
            } else {
                webView.settings.userAgentString = mobileUA
                btnToggleUA.text = "电脑版"
                webView.settings.useWideViewPort = false
                webView.settings.loadWithOverviewMode = false
            }
            
            // Reload current page with new UA
            webView.reload()
        }

        // Import button: extract HTML and return to caller
        btnImport.setOnClickListener {
            btnImport.isEnabled = false
            btnImport.text = "正在提取..."

            // Use JSON array wrapper so JSONArray can decode all escape sequences
            webView.evaluateJavascript(EXTRACT_HTML_SCRIPT) { json ->
                try {
                    val jsonArray = org.json.JSONArray(json)
                    val html = jsonArray.getString(0) ?: ""

                    Log.d(TAG, "Extracted HTML length: ${html.length}")
                    // Log a snippet around <table> to verify content
                    val tableIdx = html.indexOf("<table")
                    if (tableIdx >= 0) {
                        Log.d(TAG, "Found <table> at index $tableIdx")
                        Log.d(TAG, "Table snippet: ${html.substring(tableIdx, (tableIdx + 500).coerceAtMost(html.length))}")
                    } else {
                        Log.w(TAG, "No <table> found in HTML!")
                        // Log first 500 chars for debugging
                        Log.d(TAG, "HTML start: ${html.take(500)}")
                    }

                    if (html.length < 100) {
                        Toast.makeText(this, "页面内容提取失败，请确保页面已加载完成", Toast.LENGTH_SHORT).show()
                        btnImport.isEnabled = true
                        btnImport.text = "导入到课表"
                        return@evaluateJavascript
                    }

                    val resultIntent = Intent().apply {
                        putExtra("html", html)
                    }
                    setResult(RESULT_OK, resultIntent)
                    finish()
                } catch (e: Exception) {
                    Log.e(TAG, "HTML extraction error", e)
                    Toast.makeText(this, "数据提取出错: ${e.message}", Toast.LENGTH_SHORT).show()
                    btnImport.isEnabled = true
                    btnImport.text = "导入到课表"
                }
            }
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    private companion object {
        /**
         * 抓当前页面的 HTML，顺带把同源 iframe 的 body 拼进同一个字符串。
         *
         * 武理 jwapp 的「我的课表」是嵌在首页里的 iframe：只取顶层
         * `document.documentElement.outerHTML` 拿到的是 React 空壳，课表表格根本不在里面。
         * 同源 iframe 可以直接读 contentDocument（跨域会抛异常，这里跳过）；
         * 当前教务都只嵌一层，所以只拼一层 body，不做递归。
         */
        const val EXTRACT_HTML_SCRIPT = """
            (function() {
              function collect(doc) {
                var html = doc.documentElement ? doc.documentElement.outerHTML : '';
                var frames = doc.querySelectorAll('iframe, frame');
                for (var i = 0; i < frames.length; i++) {
                  try {
                    var sub = frames[i].contentDocument;
                    if (sub && sub.body) {
                      html += '<div data-import-frame="' + i + '">' + sub.body.innerHTML + '</div>';
                    }
                  } catch (e) {
                    // 跨域 iframe 读不到 contentDocument，忽略即可
                  }
                }
                return html;
              }
              return [collect(document)];
            })()
        """
    }
}
