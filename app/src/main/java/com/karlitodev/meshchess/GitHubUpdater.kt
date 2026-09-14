package com.karlitodev.meshchess

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class GitHubUpdater(private val context: Context, private val token: String? = null) {
    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val GITHUB_REPO = "Badley08/MeshChess"

    fun checkForUpdates() {
        Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
        scope.launch {
            try {
                val reqBuilder = Request.Builder()
                    .url("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                if (!token.isNullOrBlank()) {
                    reqBuilder.addHeader("Authorization", "Bearer ${token.trim()}")
                }

                client.newCall(reqBuilder.build()).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("Updater", "Failed to check updates: ${response.code}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Update check failed (${response.code}). Check Token/Repo access.", Toast.LENGTH_LONG).show()
                        }
                        return@use
                    }
                    val responseData = response.body?.string()
                    val json = JSONObject(responseData ?: "")
                    val assets = json.optJSONArray("assets")
                    if (assets != null && assets.length() > 0) {
                        val assetObj = assets.getJSONObject(0)
                        val assetApiUrl = assetObj.optString("url")
                        val browserUrl = assetObj.optString("browser_download_url")
                        downloadAndInstallUpdate(assetApiUrl, browserUrl)
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "No APK asset found in latest release.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("Updater", "Error checking for updates", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error checking updates: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun downloadAndInstallUpdate(assetApiUrl: String, browserUrl: String) {
        val downloadUri = if (!token.isNullOrBlank() && assetApiUrl.isNotBlank()) Uri.parse(assetApiUrl) else Uri.parse(browserUrl)
        val request = DownloadManager.Request(downloadUri)
            .setTitle("MeshChess Update")
            .setDescription("Downloading latest version")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MeshChess_update.apk")

        if (!token.isNullOrBlank()) {
            request.addRequestHeader("Authorization", "Bearer ${token.trim()}")
            request.addRequestHeader("Accept", "application/octet-stream")
        }

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        val onComplete = object : BroadcastReceiver() {
            override fun onReceive(ctxt: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (downloadId == id) {
                    val installIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(manager.getUriForDownloadedFile(downloadId), "application/vnd.android.package-archive")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    }
                    context.startActivity(installIntent)
                    context.unregisterReceiver(this)
                }
            }
        }
        context.registerReceiver(onComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED)
    }
}
