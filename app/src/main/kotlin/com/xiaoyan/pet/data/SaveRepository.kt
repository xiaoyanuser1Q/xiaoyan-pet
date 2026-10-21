package com.xiaoyan.pet.data

import android.content.Context

/** 本地持久化: SharedPreferences + JSON (零额外依赖, 无 kapt/R8 风险) */
class SaveRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("xiaoyan_pet", Context.MODE_PRIVATE)

    // ---------- 存档 ----------
    fun loadState(): PetState? = prefs.getString(KEY_STATE, null)?.let { PetState.fromJson(it) }

    fun saveState(s: PetState) {
        prefs.edit().putString(KEY_STATE, s.toJson().toString()).apply()
    }

    fun clearState() {
        prefs.edit().remove(KEY_STATE).apply()
    }

    /** 读出来给用户复制的存档文本 */
    fun exportStateText(): String? = prefs.getString(KEY_STATE, null)

    // ---------- 音频设置 ----------
    fun sfxOn(): Boolean = prefs.getBoolean(KEY_SFX, true)
    fun setSfxOn(v: Boolean) = prefs.edit().putBoolean(KEY_SFX, v).apply()
    fun bgmOn(): Boolean = prefs.getBoolean(KEY_BGM, true)
    fun setBgmOn(v: Boolean) = prefs.edit().putBoolean(KEY_BGM, v).apply()

    // ---------- 云账号 (Supabase 会话) ----------
    fun cloudEmail(): String? = prefs.getString(KEY_CLOUD_EMAIL, null)
    fun cloudToken(): String? = prefs.getString(KEY_CLOUD_TOKEN, null)
    fun cloudUid(): String? = prefs.getString(KEY_CLOUD_UID, null)
    fun lastCloudSyncMs(): Long = prefs.getLong(KEY_LAST_SYNC, 0L)

    fun setCloudSession(email: String?, token: String?, uid: String?) {
        val e = prefs.edit()
        if (email == null || token == null) {
            e.remove(KEY_CLOUD_EMAIL).remove(KEY_CLOUD_TOKEN).remove(KEY_CLOUD_UID)
        } else {
            e.putString(KEY_CLOUD_EMAIL, email)
            e.putString(KEY_CLOUD_TOKEN, token)
            e.putString(KEY_CLOUD_UID, uid.orEmpty())
        }
        e.apply()
    }

    fun setLastCloudSync(ms: Long) = prefs.edit().putLong(KEY_LAST_SYNC, ms).apply()

    companion object {
        private const val KEY_STATE = "pet_state"
        private const val KEY_SFX = "sfx_on"
        private const val KEY_BGM = "bgm_on"
        private const val KEY_CLOUD_EMAIL = "cloud_email"
        private const val KEY_CLOUD_TOKEN = "cloud_token"
        private const val KEY_CLOUD_UID = "cloud_uid"
        private const val KEY_LAST_SYNC = "last_cloud_sync_ms"
    }
}
