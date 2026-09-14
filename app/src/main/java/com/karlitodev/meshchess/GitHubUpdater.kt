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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class GitHubUpdater(private val context: Context) {
    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val GITHUB_REPO = "Badley08/MeshChess" // Assume this repo for now

    fun checkForUpdates() {
        Toast.makeText(context, "Checking for updates...", Toast.LENGTH_SHORT).show()
        scope.launch {
            try {
                val request = Request.Builder()
                    .url("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("Updater", "Failed to check updates")
                        return@use
                    }
                    val responseData = response.body?.string()
                    val json = JSONObject(responseData ?: "")
                    val assets = json.getJSONArray("assets")
                    if (assets.length() > 0) {
                        val apkUrl = assets.getJSONObject(0).getString("browser_download_url")
                        downloadAndInstallUpdate(apkUrl)
                    }
                }
            } catch (e: Exception) {
                Log.e("Updater", "Error checking for updates", e)
            }
        }
    }

    private fun downloadAndInstallUpdate(apkUrl: String) {
        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("MeshChess Update")
            .setDescription("Downloading latest version")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "MeshChess_update.apk")

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
