package com.xiaoyan.pet

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.xiaoyan.pet.audio.AudioController
import com.xiaoyan.pet.data.PetState
import com.xiaoyan.pet.data.SaveRepository
import com.xiaoyan.pet.data.SupabaseApi
import com.xiaoyan.pet.game.GameLogic
import com.xiaoyan.pet.game.StoryEvent

/** 页面 */
enum class Screen { Game, Account, Settings, About }

/**
 * 应用级状态持有者 (单例)。
 * 用 tick 触发 Compose 重组: 每次改完存档就 tick++, 所有观察方刷新。
 */
object PetApp {

    lateinit var repo: SaveRepository
    lateinit var api: SupabaseApi
    lateinit var audio: AudioController

    var pet: PetState? = null
    var screen: Screen = Screen.Game
        set(v) {
            field = v
            audio.playSfx("sfx_tap")
            bump()
        }

    /** 挂起的剧情事件 (弹窗); null = 无 */
    val pendingEvent = mutableStateOf<StoryEvent?>(null)

    /** 触发重组的计数器 */
    val tick = mutableIntStateOf(0)

    fun init(context: Context) {
        if (::repo.isInitialized) return
        repo = SaveRepository(context)
        api = SupabaseApi()
        audio = AudioController(context)
        audio.sfxEnabled = repo.sfxOn()
        audio.bgmEnabled = repo.bgmOn()
        pet = repo.loadState()
        onEnterApp()
    }

    /** 进入前台: 离线衰减 + 落盘 + 恢复 BGM */
    fun onEnterApp() {
        val p = pet
        if (p != null && p.started) {
            GameLogic.applyDecay(p, System.currentTimeMillis())
            repo.saveState(p)
        }
        audio.ensureBgm()
        bump()
    }

    fun startGame(name: String, missingThing: String) {
        val now = System.currentTimeMillis()
        pet = PetState(name = name, missingThing = missingThing, lastOpenedMs = now)
        repo.saveState(pet!!)
        audio.playSfx("sfx_levelup")
        screen = Screen.Game
    }

    /** 执行一个互动动作 (喂食/玩耍/休息/抚摸/对话), 自动落盘 + 音效 + 剧情事件 */
    fun doAction(act: (PetState) -> com.xiaoyan.pet.game.ActionResult) {
        val p = pet ?: return
        if (p.ended.isNotEmpty()) return // 结局后不再互动
        val r = act(p)
        p.lastOpenedMs = System.currentTimeMillis()
        repo.saveState(p)
        audio.playSfx(r.sfx)
        // 升级了: 把该级剧情事件按顺序挂起 (一次弹一条)
        if (r.events.isNotEmpty()) {
            queueEvents(r.events)
        }
        // 到达 100 级 (触发结局): 清掉普通弹窗, 结局由 EndingOverlay 接管
        if (p.ended.isNotEmpty()) {
            pendingEvent.value = null
        }
        bump()
    }

    private val eventQueue = ArrayDeque<StoryEvent>()

    private fun queueEvents(events: List<StoryEvent>) {
        // 多事件依次弹窗: 先进先出
        events.forEach { enqueueEvent(it) }
    }

    private fun enqueueEvent(e: StoryEvent) {
        eventQueue.addLast(e)
        if (pendingEvent.value == null) pendingEvent.value = e
    }

    fun dismissEvent() {
        pendingEvent.value = null
        if (eventQueue.isNotEmpty()) {
            pendingEvent.value = eventQueue.removeFirst()
        }
        bump()
    }

    /** 当前应显示的背景图资源 */
    fun backgroundRes(p: PetState?): Int = when {
        p == null || !p.started -> R.drawable.bg_login
        p.ended.isNotEmpty() -> R.drawable.bg_ending
        p.act == 3 -> R.drawable.bg_act3
        p.act == 2 -> R.drawable.bg_act2
        else -> R.drawable.bg_act1
    }

    fun bump() {
        tick += 1
    }

    /** 当前版本号 (与 build.gradle versionCode 保持一致, 检查更新用) */
    const val CURRENT_VERSION_CODE = 2
    const val CURRENT_VERSION_NAME = "1.1.0"
}
