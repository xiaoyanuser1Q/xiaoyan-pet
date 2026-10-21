package com.xiaoyan.pet.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

/** 账号注册/登录结果 */
sealed class AuthResult {
    /** token 为空 + needsEmailConfirm = 注册成功但需先收邮件确认 */
    data class Ok(
        val email: String,
        val token: String,
        val uid: String,
        val needsEmailConfirm: Boolean
    ) : AuthResult()

    data class Err(val message: String) : AuthResult()
}

/**
 * Supabase 纯 HTTP 客户端 (Auth + PostgREST)。
 * 不引入 supabase-kt SDK: 少一个依赖、零 R8 风险, 行为完全可控。
 */
class SupabaseApi(
    private val baseUrl: String = "https://wigjusjcgjajpnxfkxgy.supabase.co",
    private val anonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6IndpZ2p1c2pjZ2phanBueGZreGd5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODc4MTYzMDUsImV4cCI6MjEwMzM5MjMwNX0.8qPdeagIPkj1AKIYi_lDmEQaIspEuVL1RsEmV0gUK3I"
) {

    suspend fun signUp(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val body = JSONObject().put("email", email).put("password", password)
        val (code, text) = try {
            request("POST", "/auth/v1/signup", authHeaders(), body.toString())
        } catch (e: Exception) {
            return@withContext AuthResult.Err("网络异常：" + e.message)
        }
        when (code) {
            200 -> {
                val j = try { JSONObject(text) } catch (e: Exception) {
                    return@withContext AuthResult.Err("解析响应失败，请重试")
                }
                val token = j.optString("access_token", "")
                val uid = j.optJSONObject("user")?.optString("id", "") ?: ""
                if (token.isNotEmpty()) AuthResult.Ok(email, token, uid, false)
                else AuthResult.Ok(email, "", uid, true)
            }
            400 -> AuthResult.Err("注册失败：" + errDesc(text, "邮箱可能已注册"))
            else -> AuthResult.Err("注册失败 (HTTP $code)，请稍后再试")
        }
    }

    suspend fun signIn(email: String, password: String): AuthResult = withContext(Dispatchers.IO) {
        val body = JSONObject().put("email", email).put("password", password)
        val (code, text) = try {
            request("POST", "/auth/v1/token?grant_type=password", authHeaders(), body.toString())
        } catch (e: Exception) {
            return@withContext AuthResult.Err("网络异常：" + e.message)
        }
        when (code) {
            200 -> {
                val j = try { JSONObject(text) } catch (e: Exception) {
                    return@withContext AuthResult.Err("解析响应失败，请重试")
                }
                val token = j.optString("access_token", "")
                val uid = j.optJSONObject("user")?.optString("id", "") ?: ""
                if (token.isNotEmpty()) AuthResult.Ok(email, token, uid, false)
                else AuthResult.Err("登录失败：服务未返回会话，请重试")
            }
            400, 401, 422 -> AuthResult.Err("邮箱或密码不正确")
            else -> AuthResult.Err("登录失败 (HTTP $code)，请稍后再试")
        }
    }

    /** 云存档: 上传 (upsert) */
    suspend fun upsertSave(uid: String, token: String, dataJson: String): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().put("player_id", uid).put("data", dataJson)
        try {
            val (code, _) = request("POST", "/rest/v1/saves", saveHeaders(token, "resolution=merge-duplicates"), body.toString())
            code in 200..299
        } catch (e: Exception) {
            false
        }
    }

    /** 云存档: 下载, 无则空串, 401 抛 SessionExpired */
    suspend fun fetchSave(uid: String, token: String): String = withContext(Dispatchers.IO) {
        try {
            val (code, text) = request("GET", "/rest/v1/saves?player_id=eq.$uid&select=data", saveHeaders(token, null), null)
            when (code) {
                200 -> {
                    val arr = org.json.JSONArray(text)
                    if (arr.length() > 0) arr.getJSONObject(0).optString("data", "") else ""
                }
                401 -> throw SessionExpired()
                else -> ""
            }
        } catch (e: SessionExpired) {
            throw e
        } catch (e: Exception) {
            ""
        }
    }

    /** 云存档: 删除 */
    suspend fun deleteSave(uid: String, token: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val (code, _) = request("DELETE", "/rest/v1/saves?player_id=eq.$uid", saveHeaders(token, null), null)
            code in 200..299
        } catch (e: Exception) {
            false
        }
    }

    /** 玩家资料 upsert (display_name = 小桃名字) */
    suspend fun upsertPlayer(uid: String, token: String, displayName: String): Boolean = withContext(Dispatchers.IO) {
        val body = JSONObject().put("id", uid).put("display_name", displayName)
        try {
            val (code, _) = request("POST", "/rest/v1/players", saveHeaders(token, "resolution=merge-duplicates"), body.toString())
            code in 200..299
        } catch (e: Exception) {
            false
        }
    }

    /** 会话过期 (401) */
    class SessionExpired : RuntimeException("session_expired")

    // ---------- 内部工具 ----------

    private fun authHeaders(): Map<String, String> = mapOf(
        "apikey" to anonKey,
        "Content-Type" to "application/json",
        "Authorization" to "Bearer $anonKey"
    )

    private fun saveHeaders(token: String, prefer: String?): Map<String, String> {
        val m = HashMap<String, String>()
        m["apikey"] = anonKey
        m["Authorization"] = "Bearer $token"
        if (prefer != null) m["Prefer"] = prefer
        m["Content-Type"] = "application/json"
        return m
    }

    private fun errDesc(json: String, fallback: String): String = try {
        val j = JSONObject(json)
        val d = j.optString("error_description", j.optString("msg", ""))
        if (d.isNotEmpty()) d else fallback
    } catch (e: Exception) {
        fallback
    }

    private fun request(method: String, path: String, headers: Map<String, String>, body: String?): Pair<Int, String> {
        val conn = URL(baseUrl + path).openConnection() as HttpURLConnection
        conn.requestMethod = method
        conn.connectTimeout = 15000
        conn.readTimeout = 20000
        for ((k, v) in headers) conn.setRequestProperty(k, v)
        if (body != null) {
            conn.doOutput = true
            conn.outputStream.use { it.write(body.toByteArray()) }
        }
        val code = try {
            conn.responseCode
        } catch (e: Exception) {
            conn.disconnect()
            throw java.io.IOException("网络请求失败", e)
        }
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = runCatching {
            if (stream != null) stream.bufferedReader().use { it.readText() } else ""
        }.getOrDefault("")
        conn.disconnect()
        return code to text
    }

    /** 从 JWT 解析 uid (sub), 供没有 user 对象时的兜底 */
    fun uidFromJwt(token: String): String = runCatching {
        val payload = token.split(".").getOrNull(1) ?: return@runCatching ""
        val pad = payload + "=" repeat ((4 - payload.length % 4) % 4)
        String(Base64.getUrlDecoder().decode(pad), Charsets.UTF_8).let {
            JSONObject(it).optString("sub", "")
        }
    }.getOrDefault("")
}
