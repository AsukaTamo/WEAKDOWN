package com.example.courseapp.ui.import

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.courseapp.R

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

        webView.webChromeClient = WebChromeClient()

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

            webView.evaluateJavascript(
                "(function() { return document.documentElement.outerHTML; })()"
            ) { html ->
                // evaluateJavascript returns JSON-encoded string, strip surrounding quotes
                val cleanedHtml = html?.removeSurrounding("\"")
                    ?.replace("\\u003C", "<")
                    ?.replace("\\u003E", ">")
                    ?.replace("\\\"", "\"")
                    ?.replace("\\n", "\n")
                    ?.replace("\\t", "\t")
                    ?: ""

                if (cleanedHtml.length < 100) {
                    Toast.makeText(this, "页面内容提取失败，请确保页面已加载完成", Toast.LENGTH_SHORT).show()
                    btnImport.isEnabled = true
                    btnImport.text = "导入到课表"
                    return@evaluateJavascript
                }

                val resultIntent = Intent().apply {
                    putExtra("html", cleanedHtml)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
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
