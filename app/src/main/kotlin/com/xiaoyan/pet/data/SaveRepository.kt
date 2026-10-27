package com.xiaoyan.pet.data

import android.content.Context

/** 本地持久化: SharedPreferences + JSON (零额外依赖, 无 kapt/R8 风险) */
class SaveRepository(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("xiaoyan_pet", Context.MODE_PRIVATE)

    // ---------- 存档 (v3) ----------
    fun loadState(): PetState? = prefs.getString(KEY_STATE, null)?.let { PetState.fromJson(it) }

    fun saveState(s: PetState) {
        prefs.edit().putString(KEY_STATE, s.toJson().toString()).apply()
    }

    fun clearState() {
        prefs.edit().remove(KEY_STATE).apply()
    }

    /** 读出来给用户复制的存档文本 */
    fun exportStateText(): String? = prefs.getString(KEY_STATE, null)

    /** 旧档检测 (v2 存档文件保留但不加载): 返回旧档 saveVersion (2 = v1.2 旧档), -1 = 无 */
    fun oldSaveVersion(): Int {
        val text = prefs.getString(KEY_STATE, null) ?: return -1
        return PetState.saveVersionOf(text).let { if (it == PetState.SAVE_VERSION) -1 else it }
    }

    // ---------- 音频设置 ----------
    fun sfxOn(): Boolean = prefs.getBoolean(KEY_SFX, true)
    fun setSfxOn(v: Boolean) = prefs.edit().putBoolean(KEY_SFX, v).apply()
    fun bgmOn(): Boolean = prefs.getBoolean(KEY_BGM, true)
    fun setBgmOn(v: Boolean) = prefs.edit().putBoolean(KEY_BGM, v).apply()

    // ---------- 显示 (v1.2.0 低配模式 + v1.3.0 动画风格三档) ----------
    fun lowSpecOn(): Boolean = prefs.getBoolean(KEY_LOWSPEC, false)
    fun setLowSpecOn(v: Boolean) = prefs.edit().putBoolean(KEY_LOWSPEC, v).apply()

    /** 动画风格三档 (温柔/活泼/关闭): 参数集中常量表, 免重编译 */
    fun animStyle(): String = prefs.getString(KEY_ANIMSTYLE, "gentle") ?: "gentle"
    fun setAnimStyle(v: String) = prefs.edit().putString(KEY_ANIMSTYLE, v).apply()

    // ---------- 4 态日时手动兜底 (强制夜间 / 强制白天) ----------
    fun forceNightOn(): Boolean = prefs.getBoolean(KEY_FORCENIGHT, false)
    fun setForceNightOn(v: Boolean) = prefs.edit().putBoolean(KEY_FORCENIGHT, v).apply()
    fun forceDayOn(): Boolean = prefs.getBoolean(KEY_FORCEDAY, false)
    fun setForceDayOn(v: Boolean) = prefs.edit().putBoolean(KEY_FORCEDAY, v).apply()

    // ---------- 通知 (v1.3.0 新增, POST_NOTIFICATIONS 33+) ----------
    fun notificationsOn(): Boolean = prefs.getBoolean(KEY_NOTIF, true)
    fun setNotificationsOn(v: Boolean) = prefs.edit().putBoolean(KEY_NOTIF, v).apply()

    // ---------- 检查更新 (v1.3.0 多源自适应: 成功源下次优先) ----------
    fun lastGoodSource(): String = prefs.getString(KEY_GOODSOURCE, "") ?: ""
    fun setLastGoodSource(v: String) = prefs.edit().putString(KEY_GOODSOURCE, v).apply()

    // ---------- 离线回归温情台词周期 (>4h 每回归周期 1 次) ----------
    fun lastWarmReturnMs(): Long = prefs.getLong(KEY_LASTWARM, 0L)
    fun setLastWarmReturnMs(ms: Long) = prefs.edit().putLong(KEY_LASTWARM, ms).apply()

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
        private const val KEY_LOWSPEC = "low_spec_on"
        private const val KEY_ANIMSTYLE = "anim_style"          // gentle | lively | off (v1.3.0)
        private const val KEY_FORCENIGHT = "force_night_on"      // v1.3.0 4 态日时兜底
        private const val KEY_FORCEDAY = "force_day_on"
        private const val KEY_NOTIF = "notifications_on"         // v1.3.0 通知开关
        private const val KEY_GOODSOURCE = "update_good_source"   // v1.3.0 多源自适应
        private const val KEY_LASTWARM = "last_warm_return_ms"   // v1.3.0 离线回归周期
        private const val KEY_CLOUD_EMAIL = "cloud_email"
        private const val KEY_CLOUD_TOKEN = "cloud_token"
        private const val KEY_CLOUD_UID = "cloud_uid"
        private const val KEY_LAST_SYNC = "last_cloud_sync_ms"
    }
}
