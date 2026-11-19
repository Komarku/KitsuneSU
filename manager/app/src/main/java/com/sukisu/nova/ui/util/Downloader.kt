package com.sukisu.nova.ui.util

import android.net.Uri
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import com.sukisu.nova.data.registry.ExpectedFile
import com.sukisu.nova.ksuApp
import com.sukisu.nova.ui.util.module.LatestVersionInfo
import okhttp3.Request

/**
 * @author weishu
 * @date 2023/6/22.
 */
suspend fun download(
    url: String,
    fileName: String,
    onDownloaded: (Uri) -> Unit = {},
    onDownloading: () -> Unit = {},
    onProgress: (Int) -> Unit = {},
    expected: ExpectedFile? = null,
    onFailed: (DownloadManager.DownloadState) -> Unit = {},
) {
    onDownloading()

    val downloadId = DownloadManager.enqueue(
        context = ksuApp,
        url = url,
        fileName = fileName,
        onCompleted = onDownloaded,
        expected = expected,
    )

    val finalState = DownloadManager.downloads
        .onEach { map -> map[downloadId]?.let { onProgress(it.progress) } }
        .first { map ->
            val status = map[downloadId]?.status
            status == DownloadManager.Status.COMPLETED ||
                status == DownloadManager.Status.FAILED
        }[downloadId]
    if (finalState?.status == DownloadManager.Status.FAILED) {
        withContext(Dispatchers.Main) { onFailed(finalState) }
    }
}

internal suspend fun isDownloadAvailable(uri: Uri): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        ksuApp.contentResolver.openFileDescriptor(uri, "r").use { it != null }
    }.getOrDefault(false)
}


fun checkNewVersion(): LatestVersionInfo = checkReleaseVersion("latest", isCi = false)

fun checkNewCiVersion(): LatestVersionInfo = checkReleaseVersion("tags/manager-ci", isCi = true)

private fun checkReleaseVersion(endpoint: String, isCi: Boolean): LatestVersionInfo {
    if (!isNetworkAvailable(ksuApp)) return LatestVersionInfo()
    val url = "https://api.github.com/repos/Komarku/KitsuneSU/releases/$endpoint"
    return runCatching {
        ksuApp.okhttpClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) {
                return LatestVersionInfo()
            }
            val json = org.json.JSONObject(response.body.string())
            val assets = json.optJSONArray("assets") ?: return LatestVersionInfo()
            val changelog = json.optString("body")
            val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: return LatestVersionInfo()
            val apkAssets = (0 until assets.length()).mapNotNull { assets.optJSONObject(it) }

            val versionPattern = Regex("""(?:KitsuneSU[_-])(?:v?)(.+?)[_-](\d+)(?:[_-](${Regex.escape(abi)}))?.*\.apk$""")
            for (asset in apkAssets) {
                val name = asset.optString("name")
                val match = versionPattern.matchEntire(name) ?: continue
                val code = match.groupValues[2].toLongOrNull() ?: continue
                return LatestVersionInfo(
                    code,
                    asset.optString("browser_download_url"),
                    changelog,
                )
            }
            LatestVersionInfo()
        }
    }.getOrDefault(LatestVersionInfo())
}
