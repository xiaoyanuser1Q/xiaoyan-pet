package com.xiaoyan.pet.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.xiaoyan.pet.R

/**
 * 音效 (SoundPool) + 背景音乐 (MediaPlayer 循环)。
 * 开关状态由 SettingsScreen 通过 setSfx/setBgm 控制, 并持久化到 SaveRepository。
 */
class AudioController(context: Context) {

    private val appCtx = context.applicationContext
    private val pool: SoundPool
    private val sfxIds = HashMap<String, Int>()
    private var bgm: MediaPlayer? = null

    @Volatile
    var sfxEnabled = true

    @Volatile
    var bgmEnabled = true

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(attrs)
            .build()
        sfxIds["sfx_tap"] = pool.load(appCtx, R.raw.sfx_tap, 1)
        sfxIds["sfx_feed"] = pool.load(appCtx, R.raw.sfx_feed, 1)
        sfxIds["sfx_play"] = pool.load(appCtx, R.raw.sfx_play, 1)
        sfxIds["sfx_levelup"] = pool.load(appCtx, R.raw.sfx_levelup, 1)
        sfxIds["sfx_warn"] = pool.load(appCtx, R.raw.sfx_warn, 1)
    }

    /** 播放音效; 未知名称静默忽略 */
    fun playSfx(name: String) {
        if (!sfxEnabled) return
        val id = sfxIds[name] ?: return
        try {
            pool.play(id, 0.85f, 0.85f, 1, 0, 1.0f)
        } catch (e: Exception) {
            // 个别设备 SoundPool 异常, 不阻断游戏
        }
    }

    /** BGM 开关状态变化 (带副作用: 开=立即起播, 关=停播) */
    fun applyBgm(on: Boolean) {
        bgmEnabled = on
        if (on) ensureBgm() else stopBgm()
    }

    /** 若开关开且未播放, 启动 BGM (幂等) */
    fun ensureBgm() {
        if (!bgmEnabled) return
        val m = bgm
        if (m != null && m.isPlaying) return
        try {
            if (m == null) {
                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                bgm = MediaPlayer.create(appCtx, R.raw.bgm_main)?.apply {
                    isLooping = true
                    setAudioAttributes(attrs)
                }
            }
            bgm?.start()
        } catch (e: Exception) {
            bgm = null
        }
    }

    fun stopBgm() {
        bgm?.let {
            try {
                if (it.isPlaying) it.stop()
            } catch (e: Exception) {
            }
            try {
                it.release()
            } catch (e: Exception) {
            }
        }
        bgm = null
    }

    /** 应用退出时释放 */
    fun release() {
        try {
            pool.release()
        } catch (e: Exception) {
        }
        stopBgm()
    }
}
