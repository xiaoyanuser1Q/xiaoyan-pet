package com.xiaoyan.pet.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.xiaoyan.pet.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 音效 (SoundPool, 15 条 opus SFX) + 背景音乐 (MediaPlayer, 5 首 opus, 季节 4 + 终曲)。
 * BGM 季节切换 = 交叉淡化 350ms (G3: 与光柱过场错开, 无爆音)。
 */
class AudioController(context: Context) {

    private val appCtx = context.applicationContext
    private val pool: SoundPool
    private val sfxIds = HashMap<String, Int>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var bgmA: MediaPlayer? = null   // 当前播放 (A)
    private var bgmB: MediaPlayer? = null   // 交叉淡化中的另一路 (B)
    private var bgmRes = -1

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
        sfxIds["sfx_01_tap"] = pool.load(appCtx, R.raw.sfx_01_tap, 1)
        sfxIds["sfx_02_fridge"] = pool.load(appCtx, R.raw.sfx_02_fridge, 1)
        sfxIds["sfx_03_spatula"] = pool.load(appCtx, R.raw.sfx_03_spatula, 1)
        sfxIds["sfx_04_cushion"] = pool.load(appCtx, R.raw.sfx_04_cushion, 1)
        sfxIds["sfx_05_remote"] = pool.load(appCtx, R.raw.sfx_05_remote, 1)
        sfxIds["sfx_06_page"] = pool.load(appCtx, R.raw.sfx_06_page, 1)
        sfxIds["sfx_07_chime"] = pool.load(appCtx, R.raw.sfx_07_chime, 1)
        sfxIds["sfx_08_bedpat"] = pool.load(appCtx, R.raw.sfx_08_bedpat, 1)
        sfxIds["sfx_09_creak"] = pool.load(appCtx, R.raw.sfx_09_creak, 1)
        sfxIds["sfx_10_pluck"] = pool.load(appCtx, R.raw.sfx_10_pluck, 1)
        sfxIds["sfx_11_starlight"] = pool.load(appCtx, R.raw.sfx_11_starlight, 1)
        sfxIds["sfx_12_levelup"] = pool.load(appCtx, R.raw.sfx_12_levelup, 1)
        sfxIds["sfx_13_warn"] = pool.load(appCtx, R.raw.sfx_13_warn, 1)
        sfxIds["sfx_14_event"] = pool.load(appCtx, R.raw.sfx_14_event, 1)
        sfxIds["sfx_15_choice"] = pool.load(appCtx, R.raw.sfx_15_choice, 1)
    }

    /** 播放音效 (raw 资源名); 未知名称静默忽略 */
    fun playSfx(name: String) {
        if (!sfxEnabled) return
        val id = sfxIds[name] ?: return
        runCatching { pool.play(id, 0.85f, 0.85f, 1, 0, 1.0f) }
    }

    /** 某幕 BGM (季节 4 首, 循环) */
    fun bgmResFor(act: Int): Int = when (act) {
        2 -> R.raw.bgm_summer
        3 -> R.raw.bgm_autumn
        4 -> R.raw.bgm_winter
        else -> R.raw.bgm_spring
    }

    fun playActBgm(act: Int) = setBgm(bgmResFor(act))

    /** 结局 C 烟花: 终曲 (冬 → 终曲交叉淡化) */
    fun playFinale() = setBgm(R.raw.bgm_finale)

    fun stopFinale() {
        val act = bgmRes.let {
            when (it) {
                R.raw.bgm_finale -> 4
                else -> 1
            }
        }
        setBgm(bgmResFor(act))
    }

    /** 切换 BGM: 交叉淡化 350ms (双 MediaPlayer); 幂等 */
    fun setBgm(res: Int) {
        if (res == bgmRes) {
            ensureBgm()
            return
        }
        bgmRes = res
        if (!bgmEnabled) {
            stopAll()
            return
        }
        val incoming = createPlayer(res) ?: return
        val outgoing = bgmA
        bgmA = incoming
        incoming.start()
        if (outgoing != null && outgoing.isPlaying) {
            bgmB = outgoing
            scope.launch {
                var v = 1.0f
                while (v > 0.02f) {
                    outgoing.setVolume(v.coerceIn(0f, 1f), v.coerceIn(0f, 1f))
                    incoming.setVolume((1f - v).coerceIn(0f, 1f), (1f - v).coerceIn(0f, 1f))
                    v -= 0.125f
                    delay(35)
                }
                runCatching { outgoing.stop() }
                runCatching { outgoing.release() }
                if (bgmB === outgoing) bgmB = null
                incoming.setVolume(1f, 1f)
            }
        } else {
            runCatching { outgoing?.release() }
        }
    }

    /** 若开关开且未播放, 启动 (幂等; 前台回归用) */
    fun ensureBgm() {
        if (!bgmEnabled) return
        val m = bgmA
        if (m != null) {
            if (m.isPlaying) return
            runCatching { m.start() }   // 存在但停着 → 重新起播
            return
        }
        // 不存在 → 新建再起播
        val p = createPlayer(bgmRes) ?: return
        bgmA = p
        runCatching { p.start() }
    }

    /** BGM 开关 (设置页): 开=立即起播当前幕, 关=停 */
    fun applyBgm(on: Boolean) {
        bgmEnabled = on
        if (on) ensureBgm() else stopAll()
    }

    private fun stopAll() {
        runCatching { bgmA?.let { if (it.isPlaying) it.stop() } }
        runCatching { bgmA?.release() }
        runCatching { bgmB?.let { if (it.isPlaying) it.stop() } }
        runCatching { bgmB?.release() }
        bgmA = null
        bgmB = null
    }

    private fun createPlayer(res: Int): MediaPlayer? = try {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        MediaPlayer.create(appCtx, res)?.apply {
            isLooping = true
            setAudioAttributes(attrs)
        }
    } catch (e: Exception) {
        null
    }

    /** 应用退出释放 */
    fun release() {
        runCatching { scope.cancel() }
        runCatching { pool.release() }
        stopAll()
    }
}
