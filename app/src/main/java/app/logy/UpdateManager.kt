package app.logy

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val version: String, val downloadUrl: String, val releaseUrl: String)

object UpdateManager {
    private const val LATEST_RELEASE = "https://api.github.com/repos/arulmr/logy/releases/latest"

    suspend fun check(context: Context): UpdateInfo? = withContext(Dispatchers.IO) {
        val connection = open(LATEST_RELEASE)
        val json = connection.inputStream.bufferedReader().use { JSONObject(it.readText()) }
        val tag = json.getString("tag_name").removePrefix("v")
        val current = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"
        val assets = json.getJSONArray("assets")
        var downloadUrl: String? = null
        for (index in 0 until assets.length()) {
            val asset = assets.getJSONObject(index)
            if (asset.getString("name").endsWith(".apk", true)) {
                downloadUrl = asset.getString("browser_download_url")
                break
            }
        }
        if (downloadUrl != null && isNewer(tag, current)) UpdateInfo(tag, downloadUrl, json.getString("html_url")) else null
    }

    suspend fun download(context: Context, update: UpdateInfo): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val destination = File(directory, "Logy-v${update.version}.apk")
        open(update.downloadUrl).inputStream.use { input -> destination.outputStream().use(input::copyTo) }
        require(destination.length() > 0) { "GitHub returned an empty APK." }
        destination
    }

    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun open(url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 15_000
        readTimeout = 30_000
        instanceFollowRedirects = true
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "Logy-Android")
        check(responseCode in 200..299) { "GitHub update check failed ($responseCode)." }
    }

    internal fun isNewer(candidate: String, current: String): Boolean {
        val left = candidate.split('.').map { it.toIntOrNull() ?: 0 }
        val right = current.split('.').map { it.toIntOrNull() ?: 0 }
        for (index in 0 until maxOf(left.size, right.size)) {
            val comparison = (left.getOrElse(index) { 0 }).compareTo(right.getOrElse(index) { 0 })
            if (comparison != 0) return comparison > 0
        }
        return false
    }
}
