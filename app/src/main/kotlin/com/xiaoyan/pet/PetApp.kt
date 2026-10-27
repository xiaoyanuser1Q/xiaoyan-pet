package com.xiaoyan.pet

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.xiaoyan.pet.audio.AudioController
import com.xiaoyan.pet.data.PetState
import com.xiaoyan.pet.data.SaveRepository
import com.xiaoyan.pet.data.SupabaseApi
import com.xiaoyan.pet.data.Updater
import kotlinx.coroutines.launch
import com.xiaoyan.pet.game.ActionRes
import com.xiaoyan.pet.game.DayPhase
import com.xiaoyan.pet.game.GameLogic
import com.xiaoyan.pet.game.PetEvent
import com.xiaoyan.pet.game.StoryData

/** 页面 (v1.3.0: 开始菜单主页为常驻入口) */
enum class Screen { Start, Game, Account, Settings, About }

/**
 * 应用级状态持有者 (单例)。tick 触发 Compose 重组。
 */
object PetApp {

    lateinit var repo: SaveRepository
    lateinit var api: SupabaseApi
    lateinit var audio: AudioController

    var pet: PetState? = null
    var screen: Screen = Screen.Start
        set(v) {
            field = v
            audio.playSfx("sfx_01_tap")
            bump()
        }

    /** 挂起的剧情事件 (弹窗队列); null = 无 */
    val pendingEvent = mutableStateOf<PetEvent?>(null)

    /** 离线回归温情台词待播 (>4h, 每回归周期 1 次) */
    val warmReturnPending = mutableStateOf(false)
    val warmShown = mutableStateOf(false)

    /** 升级光效 (S12 时的短闪) */
    val lastLevelUp = mutableIntStateOf(0)

    // ---------- 更新流程 (T0) ----------
    val updateInfo = mutableStateOf<Updater.UpdateInfo?>(null)
    val updateChecking = mutableStateOf(false)
    val downloading = mutableStateOf(false)
    val downloadProgress = mutableFloatStateOf(0f)
    val downloadedApk = mutableStateOf<String?>(null)     // 本地文件路径
    val updateMsg = mutableStateOf<String?>(null)

    /** 触发重组的计数器 */
    val tick = mutableIntStateOf(0)

    // ---------- debug 面板钩子 (仅 debug 构建有内容; release 下 debugPanel 恒 null, 面板类未编译进包) ----------
    val debugOpen = mutableStateOf(false)
    var debugPanel: (@androidx.compose.runtime.Composable () -> Unit)? = null

    fun init(context: Context) {
        if (::repo.isInitialized) return
        repo = SaveRepository(context)
        api = SupabaseApi()
        audio = AudioController(context)
        audio.sfxEnabled = repo.sfxOn()
        audio.bgmEnabled = repo.bgmOn()
        com.xiaoyan.pet.data.Notifier.init(context)
        pet = repo.loadState()
        _appCtx.value = context.applicationContext
        // 启动即静默检查更新 (多源; 全失败不弹错)
        scope.launch { checkForUpdate() }
    }

    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main
    )

    /** 进入前台: 日切/离线衰减/回归标记 + 恢复 BGM */
    fun onEnterApp() {
        val p = pet
        if (p != null && p.started) {
            val isReturn = GameLogic.onEnter(p, System.currentTimeMillis())
            if (isReturn && p.ended.isEmpty()) {
                val last = repo.lastWarmReturnMs()
                // 每回归周期仅 1 次 (回归周期 = 两次回归之间)
                if (System.currentTimeMillis() - last > com.xiaoyan.pet.game.Rules.OFFLINE_RETURN_MS) {
                    warmReturnPending.value = true
                    warmShown.value = false
                    repo.setLastWarmReturnMs(System.currentTimeMillis())
                }
            }
            repo.saveState(p)
            audio.playActBgm(p.act)
        }
        bump()
    }

    /** 序幕结束 (起名 + 问约) → 新档 */
    fun startGame(name: String, promise: String) {
        val now = System.currentTimeMillis()
        val p = PetState(name = name, promiseAnswer = promise, lastOpenMs = now)
        p.dayKey = com.xiaoyan.pet.game.Rules.dayKeyOf(now)
        pet = p
        repo.saveState(p)
        audio.playSfx("sfx_12_levelup")
        audio.playActBgm(1)
        screen = Screen.Game
        // 序幕开场白
        pushBeats(StoryData.prologueAfter(name, promise))
    }

    /** 继续存档 → 序幕后开场 (仅首次进入时补播) */
    fun prologueLinesIfFresh(p: PetState): List<StoryData.Line>? =
        if (p.interactions == 0) StoryData.prologueAfter(p.name, p.promiseAnswer) else null

    // ---------- 互动 ----------

    fun doInteraction(hotspot: String) {
        val p = pet ?: return
        val phase = DayPhase.current(repo, p.asleep, System.currentTimeMillis())
        var r: ActionRes? = null
        run {
            val tmp = p
            r = GameLogic.interact(tmp, hotspot, phase, System.currentTimeMillis())
        }
        p.lastOpenMs = System.currentTimeMillis()
        repo.saveState(p)
        val res = r ?: return
        audio.playSfx(res.sfx)
        if (res.randomEvent != null) audio.playSfx("sfx_14_event")
        // 事件队列: 跨幕补触发 (先, 过场前) → 节点/记忆/过场 beat → 随机事件
        val events = mutableListOf<PetEvent>()
        res.backfill.forEach { ev -> p.eventsFired.add(ev.id); events += PetEvent.Random(ev) }
        res.beats.forEach { b -> events += PetEvent.Story(b) }
        if (res.randomEvent != null) events += PetEvent.Random(res.randomEvent!!)
        events.forEach { enqueue(it) }
        if (res.leveledUpTo > 0) lastLevelUp.intValue = res.leveledUpTo
        // 通知 (本地)
        if (res.notifyLevel > 0) com.xiaoyan.pet.data.Notifier.leveledUp(appContext(), res.notifyLevel)
        if (res.notifyAct > 0) com.xiaoyan.pet.data.Notifier.actCrossed(appContext(), res.notifyAct, com.xiaoyan.pet.game.Rules.actOf(res.notifyAct + 1))
        // 季节 BGM 交叉淡化 (随幕切换)
        audio.playActBgm(p.act)
        if (p.ended.isNotEmpty()) {
            pendingEvent.value = null
            if (p.ended == "C") audio.playFinale()
        }
        bump()
    }

    /** 抉择 (Lv20/45/70/95) */
    fun doChoice(key: String, optId: String) {
        val p = pet ?: return
        val res = GameLogic.applyChoice(p, key, optId)
        p.lastOpenMs = System.currentTimeMillis()
        repo.saveState(p)
        audio.playSfx(res.sfx)
        res.beats.forEach { enqueue(PetEvent.Story(it)) }
        res.backfill.forEach { enqueue(PetEvent.Random(it)) }
        if (p.ended.isNotEmpty()) {
            pendingEvent.value = null
            if (p.ended == "C") audio.playFinale()
        }
        audio.playActBgm(p.act)
        bump()
    }

    /** 温情台词消费 (进主页 30s warn 延后由 GameScreen 处理) */
    fun consumeWarmReturn() {
        warmReturnPending.value = false
        warmShown.value = true
        bump()
    }

    fun pushLines(lines: List<StoryData.Line>) {
        // 序幕/结局后补播: 打包成一条 beat 事件
        if (lines.isEmpty()) return
        enqueue(PetEvent.Story(StoryData.Beat("open", "", lines)))
    }

    private fun pushBeats(lines: List<StoryData.Line>) = pushLines(lines)

    // ---------- 事件队列 (挂根上, 切页不丢) ----------
    private val eventQueue = ArrayDeque<PetEvent>()

    private fun enqueue(e: PetEvent) {
        eventQueue.addLast(e)
        if (pendingEvent.value == null) pendingEvent.value = e
    }

    fun dismissEvent() {
        pendingEvent.value = null
        if (eventQueue.isNotEmpty()) pendingEvent.value = eventQueue.removeFirst()
        bump()
    }

    // ---------- 检查更新 / 下载 / 安装 (T0) ----------

    suspend fun checkForUpdate() {
        updateChecking.value = true
        updateMsg.value = null
        bump()
        val (info, hit) = Updater.checkUpdate(repo, CURRENT_VERSION_CODE)
        updateInfo.value = info
        updateMsg.value = when {
            info != null -> null
            !hit -> "网络不可用（可开启 VPN 后重试）"
            else -> null   // 命中且已最新
        }
        if (info != null) {
            com.xiaoyan.pet.data.Notifier.versionFound(appContext(), info.version, info.note)
            downloadedApk.value = null
        }
        updateChecking.value = false
        bump()
    }

    /** APP 内下载 (流式 + 进度 + 断点续传 + SHA256); 完成后走安装 */
    fun downloadUpdate() {
        val info = updateInfo.value ?: return
        if (downloading.value) return
        downloading.value = true
        downloadProgress.floatValue = 0f
        scope.launch {
            val file = Updater.download(appContext(), info) { p -> downloadProgress.floatValue = p.toFloat() }
            downloading.value = false
            if (file != null) {
                downloadedApk.value = file.absolutePath
                updateMsg.value = "下载完成，点「安装」开始（离线也可装）"
                audio.playSfx("sfx_12_levelup")
            } else {
                updateMsg.value = "下载失败或校验不一致，包已删除，请重试"
                audio.playSfx("sfx_13_warn")
            }
            bump()
        }
    }

    /** FileProvider 安装 + INSTALL_PACKAGES 授权引导 */
    fun installUpdate(activity: Activity) {
        val path = downloadedApk.value ?: return
        val file = java.io.File(path)
        if (!file.exists()) { updateMsg.value = "安装包不存在，请重新下载"; bump(); return }
        if (!Updater.canInstall(activity)) {
            Updater.openInstallPermission(activity)
            updateMsg.value = "请先允许「安装未知应用」，然后回来点安装"
            bump()
            return
        }
        if (Updater.requestInstall(activity, file)) {
            updateMsg.value = "安装中…装完点「重启生效」"
        } else {
            updateMsg.value = "启动安装失败，请到官网下载"
        }
        bump()
    }

    fun dismissUpdate() {
        updateInfo.value = null
        downloadedApk.value = null
        updateMsg.value = null
        downloadProgress.floatValue = 0f
        bump()
    }

    // ---------- 工具 ----------
    private val _appCtx = mutableStateOf<Context?>(null)
    fun appContext(): Context = _appCtx.value ?: throw IllegalStateException("not inited")
    fun bindContext(ctx: Context) { _appCtx.value = ctx.applicationContext }

    fun bump() {
        tick.intValue = tick.intValue + 1
    }

    /** 与 build.gradle versionCode 保持一致 (检查更新用) */
    const val CURRENT_VERSION_CODE = 4
    const val CURRENT_VERSION_NAME = "1.3.0"
}
