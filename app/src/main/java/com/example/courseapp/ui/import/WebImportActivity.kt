package com.example.courseapp.ui.import

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.courseapp.R

private const val TAG = "WebImport"

class WebImportActivity : ComponentActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_import)

        webView = findViewById(R.id.webView)
        val btnImport = findViewById<android.widget.Button>(R.id.btnImport)

        // WebView settings
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // Desktop UA so sites serve full PC layout
            userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36"
        }

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

        // Load school URL
        val schoolUrl = intent.getStringExtra("school_url") ?: ""
        if (schoolUrl.isNotEmpty()) {
            webView.loadUrl(schoolUrl)
        } else {
            Toast.makeText(this, "未指定学校网址", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Import button: extract HTML and return to caller
        btnImport.setOnClickListener {
            btnImport.isEnabled = false
            btnImport.text = "正在提取..."

            // Use JSON array wrapper so JSONArray can decode all escape sequences
            webView.evaluateJavascript(
                "(function() { return [document.documentElement.outerHTML]; })()"
            ) { json ->
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
