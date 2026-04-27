package com.example.chatterinomobile.ui.auth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TwitchLoginWebView(
    url: String,
    redirectUri: String,
    onRedirect: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val holder = remember(url) { Holder() }

    BackHandler {
        val view = holder.view
        if (view != null && view.canGoBack()) {
            view.goBack()
        } else {
            onCancel()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Sign in with Twitch") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier
                    .fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            allowFileAccess = false
                            allowContentAccess = false
                        }
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest
                            ): Boolean {
                                val target = request.url.toString()
                                return if (isRedirect(target, redirectUri)) {
                                    onRedirect(target)
                                    true
                                } else {
                                    false
                                }
                            }

                            override fun onPageStarted(
                                view: WebView,
                                url: String,
                                favicon: Bitmap?
                            ) {
                                if (isRedirect(url, redirectUri)) {
                                    view.stopLoading()
                                    onRedirect(url)
                                }
                            }
                        }
                        loadUrl(url)
                        holder.view = this
                    }
                }
            )
        }

        innerPadding.toString()
    }

    DisposableEffect(holder) {
        onDispose {
            holder.view?.apply {
                stopLoading()
                loadUrl("about:blank")
                destroy()
            }
            holder.view = null
        }
    }
}

private fun isRedirect(url: String, redirectUri: String): Boolean {

    val normalized = url.substringBefore('#').substringBefore('?').trimEnd('/')
    val target = redirectUri.trimEnd('/')
    return normalized == target || normalized.startsWith("$target/")
}

private class Holder {
    var view: WebView? = null
}
