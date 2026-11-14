package com.remotecontrol.kiosk.ui

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.net.Uri
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun KioskWebView(url: String, modifier: Modifier = Modifier) {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }

    // Android WebView has no built-in file picker UI — the host app must
    // launch one itself and hand the result back via this callback, or
    // every <input type="file"> (e.g. the household photo upload) silently
    // does nothing when tapped.
    var pendingFileCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }
    val fileChooserLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val uris = if (result.resultCode == Activity.RESULT_OK) {
            WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        } else {
            null
        }
        pendingFileCallback?.onReceiveValue(uris)
        pendingFileCallback = null
    }

    BackHandler(enabled = true) {
        webView?.let { if (it.canGoBack()) it.goBack() }
    }

    fun retry() {
        loadFailed = false
        isRefreshing = true
        webView?.reload()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = ::retry,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    // Lets `chrome://inspect` on a USB-connected desktop Chrome attach
                    // full DevTools to this WebView — the fastest way to see what's
                    // actually failing on real hardware instead of guessing.
                    WebView.setWebContentsDebuggingEnabled(true)
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.useWideViewPort = true
                        settings.loadWithOverviewMode = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean = false // keep every navigation inside this WebView

                            override fun onPageFinished(view: WebView, finishedUrl: String) {
                                isRefreshing = false
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: WebResourceError,
                            ) {
                                Log.e(
                                    "KioskWebView",
                                    "Load error on ${request.url}: ${error.errorCode} ${error.description}",
                                )
                                if (request.isForMainFrame) loadFailed = true
                                isRefreshing = false
                            }

                            override fun onReceivedHttpError(
                                view: WebView,
                                request: WebResourceRequest,
                                errorResponse: WebResourceResponse,
                            ) {
                                Log.e(
                                    "KioskWebView",
                                    "HTTP ${errorResponse.statusCode} loading ${request.url}",
                                )
                                if (request.isForMainFrame) loadFailed = true
                                isRefreshing = false
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                                Log.println(
                                    when (message.messageLevel()) {
                                        ConsoleMessage.MessageLevel.ERROR -> Log.ERROR
                                        ConsoleMessage.MessageLevel.WARNING -> Log.WARN
                                        else -> Log.INFO
                                    },
                                    "KioskWebViewConsole",
                                    "${message.message()} (${message.sourceId()}:${message.lineNumber()})",
                                )
                                return true
                            }

                            override fun onShowFileChooser(
                                view: WebView,
                                filePathCallback: ValueCallback<Array<Uri>>,
                                fileChooserParams: FileChooserParams,
                            ): Boolean {
                                pendingFileCallback?.onReceiveValue(null)
                                pendingFileCallback = filePathCallback
                                return try {
                                    fileChooserLauncher.launch(fileChooserParams.createIntent())
                                    true
                                } catch (e: ActivityNotFoundException) {
                                    Log.e("KioskWebView", "No app available to handle file chooser", e)
                                    pendingFileCallback = null
                                    filePathCallback.onReceiveValue(null)
                                    true
                                }
                            }
                        }
                        webView = this
                        loadUrl(url)
                    }
                },
                update = { view ->
                    if (view.url != url) view.loadUrl(url)
                },
            )

            if (loadFailed) {
                NoConnectionScreen(onRetry = ::retry)
            }
        }
    }
}

@Composable
private fun NoConnectionScreen(onRetry: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Couldn't load the page", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(
                "Check the connection, then pull down or tap below to try again.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}
