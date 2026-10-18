package com.example.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CompatibleApp
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebAppRunnerDialog(
    app: CompatibleApp,
    onDismiss: () -> Unit,
    onLogToTerminal: (String) -> Unit
) {
    var hasRendererCrashed by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = app.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Environnement WebApp HephaistOS • Sandbox Sécurisée",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fermer la WebApp",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // WebView Container or Fallback
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color(0xFF121316))
                ) {
                    if (hasRendererCrashed) {
                        // Safe fallback UI if the environment lacks renderer nodes
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Moteur WebApp en Mode Sécurité",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Le moteur Chromium de l'émulateur ne dispose pas d'accélération matérielle GPU (Mesa rendernode). Le composant a été sécurisé sans fermer l'application.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF9E9E9E),
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = { hasRendererCrashed = false }) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Recharger en mode logiciel")
                            }
                        }
                    } else {
                        val targetUrl = app.webAppUrl

                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { context ->
                                WebView(context).apply {
                                    // CRITICAL: Disable hardware acceleration to prevent Mesa rendernode crashes in cloud emulators
                                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.allowFileAccess = true
                                    settings.allowContentAccess = true
                                    settings.cacheMode = WebSettings.LOAD_NO_CACHE

                                    // Add JavaScript Bridge for HephaistOS
                                    addJavascriptInterface(
                                        object {
                                            @JavascriptInterface
                                            fun log(msg: String) {
                                                onLogToTerminal("[WEBAPP] $msg")
                                            }

                                            @JavascriptInterface
                                            fun executeCommand(cmd: String) {
                                                onLogToTerminal("[COMMANDE WEBAPP] $cmd")
                                            }
                                        },
                                        "HephaistosBridge"
                                    )

                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            onLogToTerminal("[WEBAPP] '${app.name}' chargée ($url)")
                                        }

                                        override fun onReceivedError(
                                            view: WebView?,
                                            request: WebResourceRequest?,
                                            error: WebResourceError?
                                        ) {
                                            super.onReceivedError(view, request, error)
                                            val errDesc = error?.description?.toString() ?: "Erreur inconnue"
                                            onLogToTerminal("[WEBAPP-AVERTISSEMENT] Erreur de chargement de ressource : $errDesc")
                                        }

                                        // CRITICAL: Prevent host application crash if renderer process fails
                                        override fun onRenderProcessGone(
                                            view: WebView?,
                                            detail: RenderProcessGoneDetail?
                                        ): Boolean {
                                            try {
                                                (view?.parent as? ViewGroup)?.removeView(view)
                                                view?.destroy()
                                            } catch (_: Exception) {}

                                            hasRendererCrashed = true
                                            onLogToTerminal("[WEBAPP] Processus de rendu Chromium intercepté et sécurisé avec succès.")
                                            return true // Prevent terminating the host app!
                                        }
                                    }

                                    // Check if file exists, else load embedded default template
                                    if (targetUrl != null && targetUrl.startsWith("file://")) {
                                        val filePath = targetUrl.removePrefix("file://")
                                        val f = File(filePath)
                                        if (f.exists()) {
                                            loadUrl(targetUrl)
                                        } else {
                                            val defaultHtml = """
                                                <!DOCTYPE html>
                                                <html>
                                                <head>
                                                    <meta charset="utf-8">
                                                    <style>
                                                        body { background: #121418; color: #fff; font-family: sans-serif; padding: 20px; text-align: center; }
                                                        .box { background: #1c2026; padding: 20px; border-radius: 12px; margin-top: 20px; }
                                                        h2 { color: #ffb77d; margin-bottom: 8px; }
                                                        p { color: #8b949e; font-size: 14px; }
                                                        button { background: #e67e22; color: #fff; border: none; padding: 10px 20px; border-radius: 8px; font-weight: bold; cursor: pointer; margin-top: 10px; }
                                                    </style>
                                                </head>
                                                <body>
                                                    <div class="box">
                                                        <h2>${app.name}</h2>
                                                        <p>Environnement WebApp HephaistOS opérationnel.</p>
                                                        <p>Prêt pour les interactions Kitchen & Bridge.</p>
                                                        <button onclick="if(window.HephaistosBridge){ window.HephaistosBridge.log('Test HephaistosBridge OK !'); }">Tester le Bridge</button>
                                                    </div>
                                                </body>
                                                </html>
                                            """.trimIndent()
                                            loadDataWithBaseURL(null, defaultHtml, "text/html", "UTF-8", null)
                                        }
                                    } else {
                                        loadUrl(targetUrl ?: "about:blank")
                                    }
                                }
                            },
                            onRelease = { webView ->
                                try {
                                    webView.stopLoading()
                                    webView.destroy()
                                } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
    }
}
