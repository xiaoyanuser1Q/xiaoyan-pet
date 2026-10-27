package com.xiaoyan.pet.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * T0 网络: 检查更新多源回退 + APP 内下载安装 (v1.3.0, 方案 §1 + 圣经 §0/§6 R3)。
 *
 * 初始源序 [Gitee, eu.cc, pages.dev] (第三轮 R3 裁定, 与 §0 实测一致: 国内首启直接命中稳定源);
 * 成功源写 SharedPreferences, 下次优先 (海外/VPN 用户自然漂回 eu.cc);
 * 发布侧 version.json 仍指 eu.cc (发布源 ≠ 下载源, 不矛盾)。
 * 单源超时 8s, 首命中即用; 全失败 = 静默 (设置页「检查更新」显示"网络不可用")。
 *
 * 下载: 流式到 getExternalFilesDir (免存储权限) + 进度 + 断点续传 (Range) + SHA256 校验 (不一致删包)。
 * 安装: FileProvider + ACTION_VIEW; Android 8+ 需 REQUEST_INSTALL_PACKAGES, 未授权引导
 * Settings.ACTION_MANAGE_APP_INSTALL_PACKAGES (P20 Pro API 29 必触发一次)。
 */
object Updater {

    const val SRC_GITEE = "gitee"
    const val SRC_EUCC = "eu.cc"
    const val SRC_PAGES = "pages.dev"

    const val GITEE_REPO = "xiaoyanuser/xiaoyan-pet"
    const val GITEE_LATEST_API = "https://gitee.com/api/v5/repos/$GITEE_REPO/releases/latest"
    const val EUCC_VERSION_JSON = "https://xiaoyan-pet.xiaoyan-pan.eu.cc/version.json"
    const val PAGES_VERSION_JSON = "https://xiaoyan-pet-site.pages.dev/version.json"

    data class UpdateInfo(
        val version: String,
        val code: Int,
        val sha256: String,
        val force: Boolean,
        val note: String,
        val source: String,
        val apkUrl: String,
    )

    private val mainHandler = Handler(Looper.getMainLooper())

    /** 源序: 初始 [Gitee, eu.cc, pages.dev]; 上次成功源提到最前 (R3 自适应) */
    fun sourceOrder(repo: SaveRepository): List<String> {
        val base = listOf(SRC_GITEE, SRC_EUCC, SRC_PAGES)
        val good = repo.lastGoodSource()
        return if (good in base) listOf(good) + base.filter { it != good } else base
    }

    /**
     * 检查更新 (多源回退)。
     * @return (UpdateInfo | null, 至少一个源命中?) — 全失败 = (null, false); 命中且最新 = (null, true)
     */
    suspend fun checkUpdate(repo: SaveRepository, currentCode: Int): Pair<UpdateInfo?, Boolean> =
        withContext(Dispatchers.IO) {
            var anyHit = false
            for (src in sourceOrder(repo)) {
                val info = fetchSource(src) ?: continue
                anyHit = true
                repo.setLastGoodSource(src)
                if (info.code > currentCode) return@withContext info to true
                return@withContext null to true
            }
            null to anyHit
        }

    /** 某源是否命中过 (设置页「测试连接」多源状态用): 轻量探活 version.json */
    suspend fun probeSource(src: String): Boolean = withContext(Dispatchers.IO) {
        try {
            when (src) {
                SRC_GITEE -> {
                    val r = httpGet(GITEE_LATEST_API, null, null)
                    r.code == 200
                }
                SRC_EUCC -> httpGet(EUCC_VERSION_JSON, null, null).code == 200
                SRC_PAGES -> httpGet(PAGES_VERSION_JSON, null, null).code == 200
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun fetchSource(src: String): UpdateInfo? = try {
        when (src) {
            SRC_GITEE -> fetchGitee()
            SRC_EUCC -> fetchJson(EUCC_VERSION_JSON, null)
            SRC_PAGES -> fetchJson(PAGES_VERSION_JSON, "https://xiaoyan-pet-site.pages.dev")
            else -> null
        }
    } catch (e: Exception) {
        null
    }

    /** Gitee: releases/latest (匿名 API, 4G 无 VPN 未登录实测通道) → 取 version.json 资产 → 解析 */
    private fun fetchGitee(): UpdateInfo? {
        val latest = httpGet(GITEE_LATEST_API, null, null)
        if (latest.code != 200) return null
        val rel = JSONObject(latest.text)
        val assets = rel.optJSONArray("assets") ?: return null
        var versionJsonUrl: String? = null
        var apkUrl: String? = null
        var apkVersion = ""
        for (i in 0 until assets.length()) {
            val a = assets.getJSONObject(i)
            val name = a.optString("name", "")
            val url = firstNonNull(a.opt("browser_download_url"), a.opt("url"), a.opt("download_url")) as? String
            if (url.isNullOrBlank()) continue
            when {
                name.endsWith("version.json") -> versionJsonUrl = url
                name.endsWith(".apk") -> {
                    apkUrl = url
                    apkVersion = name.removePrefix("xiaoyan-pet-v").removeSuffix("-release.apk")
                }
            }
        }
        if (versionJsonUrl == null) return null   // Gitee release 未传 version.json 资产 (主人端待传) → 回落下一源
        val r = httpGet(versionJsonUrl, null, null)
        if (r.code != 200) return null
        val j = JSONObject(r.text)
        val sha = j.optString("sha256", "")
        val note = j.optString("note", "")
        val code = j.optInt("versionCode", 0)
        val version = j.optString("version", apkVersion)
        return UpdateInfo(version, code, sha, j.optBoolean("force_update", false), note, SRC_GITEE,
            apkUrl ?: "")
    }

    /** eu.cc / pages.dev: 直接 version.json; pages.dev 源下载 URL 重写为自身 host (发布侧内容指 eu.cc) */
    private fun fetchJson(vjUrl: String, overrideBase: String?): UpdateInfo? {
        val r = httpGet(vjUrl, null, null)
        if (r.code != 200) return null
        val j = JSONObject(r.text)
        val src = if (overrideBase != null) SRC_PAGES else SRC_EUCC
        val apkUrl = j.optString("url", "")
        val finalApk = if (overrideBase != null && apkUrl.isNotBlank()) {
            overrideBase + apkUrl.substringAfterLast("/download")
        } else apkUrl
        return UpdateInfo(
            j.optString("version", ""),
            j.optInt("versionCode", 0),
            j.optString("sha256", ""),
            j.optBoolean("force_update", false),
            j.optString("note", ""),
            src,
            finalApk,
        )
    }

    // ---------- 下载 (流式 + 进度 + 断点续传 + SHA256 校验) ----------

    /**
     * @return 校验通过的 APK 文件; 失败/校验不一致 = null (已删包)
     * @param onProgress 主线程回调 (0..1)
     */
    suspend fun download(
        ctx: Context,
        info: UpdateInfo,
        onProgress: (Double) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        if (info.apkUrl.isBlank()) return@withContext null
        val dir = ctx.getExternalFilesDir("updates") ?: return@withContext null
        val file = File(dir, "xiaoyan-pet-v${info.code}.apk")
        try {
            val existing = if (file.exists()) file.length() else 0L
            var conn = openConn(info.apkUrl, if (existing > 0) existing else 0)
            var append = existing > 0 && conn.responseCode == 206
            if (!append && conn.responseCode == 200) {
                // 服务器忽略了 Range → 从头
                conn.disconnect()
                conn = openConn(info.apkUrl, 0)
                append = false
            }
            if (conn.responseCode != 200 && conn.responseCode != 206) {
                conn.disconnect()
                return@withContext null
            }
            val total = conn.contentLengthLong
            val expectedTotal = if (append) total + existing else total
            FileOutputStream(file, append).use { out ->
                conn.inputStream.use { input ->
                    val buf = ByteArray(8192)
                    var done = if (append) existing else 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        out.write(buf, 0, n)
                        done += n
                        mainHandler.post {
                            onProgress(if (expectedTotal > 0) done.toDouble() / expectedTotal else 0.0)
                        }
                    }
                }
            }
            conn.disconnect()
        } catch (e: Exception) {
            return@withContext null
        }
        // SHA256 校验 (不一致删包并提示)
        if (info.sha256.isNotBlank() && !sha256Ok(file, info.sha256)) {
            file.delete()
            return@withContext null
        }
        file
    }

    fun sha256Of(file: File): String? = runCatching {
        MessageDigest.getInstance("SHA-256")
            .digest(file.readBytes())
            .joinToString("") { "%02x".format(it) }
    }.getOrNull()

    private fun sha256Ok(file: File, expected: String): Boolean =
        sha256Of(file)?.equals(expected, ignoreCase = true) ?: false

    private fun openConn(url: String, fromByte: Long): HttpURLConnection {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 15_000
        conn.readTimeout = 30_000
        conn.instanceFollowRedirects = true
        if (fromByte > 0) conn.setRequestProperty("Range", "bytes=$fromByte-")
        conn.requestProperty("User-Agent", "xiaoyan-pet/1.3")
        conn.connect()
        return conn
    }

    private data class Resp(val code: Int, val text: String)

    private fun httpGet(url: String, header: String?, value: String?): Resp {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8_000
        conn.readTimeout = 8_000
        conn.requestProperty("User-Agent", "Mozilla/5.0 (xiaoyan-pet updater)")
        if (header != null && value != null) conn.setRequestProperty(header, value)
        val code = try {
            conn.responseCode
        } catch (e: Exception) {
            conn.disconnect()
            throw e
        }
        val text = runCatching {
            if (code in 200..299) conn.inputStream.bufferedReader().use { it.readText() } else ""
        }.getOrDefault("")
        conn.disconnect()
        return Resp(code, text)
    }

    // ---------- 安装 (FileProvider + INSTALL_PACKAGES 授权引导) ----------

    fun canInstall(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            ctx.packageManager.canRequestPackageInstalls()
        else true

    fun requestInstall(ctx: Context, apkFile: File): Boolean {
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addCategory(Intent.CATEGORY_DEFAULT)
        return try {
            ctx.startActivity(Intent.createChooser(intent, "安装 ${apkFile.name}"))
            true
        } catch (e: Exception) {
            false
        }
    }

    /** 未授权 → 跳「安装未知应用」设置页 (API 29 P20 Pro 必触发一次) */
    fun openInstallPermission(ctx: Context) {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(android.provider.Settings.ACTION_MANAGE_APP_INSTALL_PACKAGES)
                .setData(Uri.parse("package:${ctx.packageName}"))
        } else {
            Intent(android.provider.Settings.ACTION_UNKNOWN_APP_SOURCES)
                .setData(Uri.parse("package:${ctx.packageName}"))
        }
        runCatching { ctx.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    private fun firstNonNull(a: Any?, b: Any?, c: Any?): Any? = a ?: b ?: c
}
