package com.xiaoyan.pet.game

import com.xiaoyan.pet.data.PetState
import kotlin.random.Random

/** 一次互动的结果 */
data class ActionRes(
    val line: StoryData.Line?,        // VN 台词 (带 mood)
    val sfx: String = "sfx_01_tap",
    val leveledUpTo: Int = 0,         // 0 = 未升级
    val beats: List<StoryData.Beat> = emptyList(),     // 到达新级别自动挂起 (节点/记忆/过场)
    val backfill: List<StoryData.REvent> = emptyList(),// 跨幕强制补触发 (R4, 每次最多 2 条)
    val randomEvent: StoryData.REvent? = null,
    val ended: String = "",           // 到达 100 且判定后的结局
    val notifyLevel: Int = 0,         // >0 = 发「升级」通知
    val notifyAct: Int = 0,           // >0 = 发「跨幕」通知 (25/50/75)
)

/** 剧情弹窗统一载体 (挂 PetApp.pendingEvent) */
sealed class PetEvent {
    data class Story(val beat: StoryData.Beat) : PetEvent()
    data class Random(val ev: StoryData.REvent) : PetEvent()
}

object GameLogic {

    /** 互动入口 (12 锚点 + wake)。调用方负责落盘 + 音效 + 通知。 */
    fun interact(s: PetState, hotspot: String, dayPhase: String, nowMs: Long): ActionRes {
        if (s.ended.isNotEmpty()) return ActionRes(null, "sfx_01_tap")

        // ---------- 门禁 ----------
        if (hotspot == "stargaze" && !Rules.canStargaze(s, dayPhase)) {
            return ActionRes(StoryData.STARGAZE_BLOCKED, "sfx_01_tap")
        }
        if (hotspot == "bookshelf" && s.dailyBookshelf >= Rules.BOOKSHELF_PER_DAY) {
            return ActionRes(StoryData.BOOKSHELF_LIMIT, "sfx_06_page")
        }
        if (hotspot == "tv" && s.dailyTv >= Rules.TV_PER_DAY) {
            return ActionRes(StoryData.TV_LIMIT, "sfx_05_remote")
        }
        if (hotspot == "sleep" && nowMs - s.lastSleepMs < Rules.SLEEP_CD_MS) {
            return ActionRes(StoryData.SLEEP_CD, "sfx_08_bedpat")
        }
        if (Rules.consumesEnergy(hotspot) && s.energy <= 0) {
            return ActionRes(StoryData.STAMINA0, "sfx_13_warn")
        }

        // ---------- 执行 (表面数值 + 隐藏线 [到顶不加线不置灰] + 互动 XP) ----------
        var sfx = "sfx_01_tap"
        val xpgain = Array(1, { 0 })
        when (hotspot) {
            "feed" -> {
                s.energy -= 1; s.satiety = (s.satiety + 15).coerceIn(0, 100)
                xpgain[0] = Rules.XP_FEED
                sfx = "sfx_02_fridge"
            }
            "feast" -> {
                s.energy -= 1; s.satiety = (s.satiety + 30).coerceIn(0, 100); s.mood = (s.mood + 5).coerceIn(0, 100)
                xpgain[0] = Rules.XP_FEAST
                sfx = "sfx_03_spatula"
            }
            "nap" -> {
                s.energy -= 1; s.mood = (s.mood + 8).coerceIn(0, 100)
                Rules.addWarmth(s, Rules.GAIN_NAP_WARMTH)
                s.intimacy = (s.intimacy + 1).coerceIn(0, 100)
                xpgain[0] = Rules.XP_NAP
                sfx = "sfx_04_cushion"
            }
            "tv" -> {
                s.dailyTv += 1
                Rules.addCuriosity(s, Rules.GAIN_TV_CURIOSITY)
                s.mood = (s.mood + 3).coerceIn(0, 100)
                xpgain[0] = Rules.XP_TV
                sfx = "sfx_05_remote"
            }
            "study" -> {
                s.energy -= 1
                Rules.addCuriosity(s, Rules.GAIN_STUDY_CURIOSITY)
                s.intimacy = (s.intimacy + 1).coerceIn(0, 100)
                xpgain[0] = Rules.XP_STUDY
                sfx = "sfx_06_page"
            }
            "bookshelf" -> {
                s.dailyBookshelf += 1
                Rules.addPromise(s, Rules.GAIN_BOOKSHELF_PROMISE)
                s.mood = (s.mood + 5).coerceIn(0, 100)
                s.intimacy = (s.intimacy + 1).coerceIn(0, 100)
                sfx = "sfx_06_page"   // 书架不产 XP (圣经 XP 来源表无书架)
            }
            "sleep" -> {
                s.energy = s.energyCap()
                val low = s.satiety < Rules.SLEEP_SATIE_PENALTY
                s.satiety = if (low) 0 else (s.satiety - Rules.SLEEP_SATIE_PENALTY).coerceIn(0, 100)
                Rules.addWarmth(s, Rules.GAIN_SLEEP_WARMTH)
                s.asleep = true
                s.lastSleepMs = nowMs
                s.mood = (s.mood + 4).coerceIn(0, 100)
                sfx = if (low) "sfx_13_warn" else "sfx_08_bedpat"   // 饱食<15 睡完归 0 + S13
            }
            "wake" -> {
                s.asleep = false
                s.mood = (s.mood + 3).coerceIn(0, 100)
                sfx = "sfx_08_bedpat"
            }
            "play" -> {
                s.energy -= 1
                Rules.addCuriosity(s, Rules.GAIN_SWING_CURIOSITY)
                s.mood = (s.mood + 6).coerceIn(0, 100)
                s.intimacy = (s.intimacy + 1).coerceIn(0, 100)
                xpgain[0] = Rules.XP_PLAY
                sfx = "sfx_09_creak"
            }
            "flowerpick" -> {
                s.energy -= 1
                s.mood = (s.mood + 5).coerceIn(0, 100)
                s.intimacy = (s.intimacy + 1).coerceIn(0, 100)
                sfx = "sfx_10_pluck"
            }
            "snackbuy" -> {
                s.energy -= 1
                s.satiety = (s.satiety + 10).coerceIn(0, 100)
                s.mood = (s.mood + 3).coerceIn(0, 100)
                s.intimacy = (s.intimacy + 1).coerceIn(0, 100)
                sfx = "sfx_07_chime"
            }
            "stargaze" -> {
                s.energy -= 1
                Rules.addPromise(s, Rules.GAIN_STARGAZE_PROMISE)
                s.mood = (s.mood + 4).coerceIn(0, 100)
                s.intimacy = (s.intimacy + 2).coerceIn(0, 100)
                xpgain[0] = Rules.XP_STARGAZE
                sfx = "sfx_11_starlight"
            }
            "chime" -> {
                s.mood = (s.mood + 3).coerceIn(0, 100)
                sfx = "sfx_07_chime"
            }
        }
        s.interactions += 1

        // 日常 XP (计入 45 硬上限)
        val granted = grantCappedXp(s, xpgain[0])

        // ---------- 随机事件 (15%, 当前幕池, 每条 1 次; 事件 XP +3 豁免) ----------
        var randomEvent: StoryData.REvent? = null
        if (s.level < Rules.MAX_LEVEL) {
            randomEvent = rollRandomEvent(s)
            if (randomEvent != null) {
                applyHiddenGain(s, randomEvent)
                s.xp += Rules.XP_EVENT   // 豁免: 不计入 45
            }
        }

        // ---------- 统一升级扫描 (一次互动可能跨多级: 收集路上所有节点/记忆/过场) ----------
        val crossRes = sweepLevels(s, granted + (if (randomEvent != null) Rules.XP_EVENT else 0), nowMs)

        val line = when {
            crossRes.leveledUpTo > 0 -> StoryData.LEVELUP.copy(text = StoryData.LEVELUP.text + crossRes.leveledUpTo)
            s.energy <= 0 -> StoryData.STAMINA0
            else -> StoryData.INTERACTIONS[hotspot]?.let { it.get((s.interactions / 2) % it.size) }
        }
        val resSfx = if (crossRes.leveledUpTo > 0) "sfx_12_levelup" else sfx

        return ActionRes(
            line = line,
            sfx = resSfx,
            leveledUpTo = crossRes.leveledUpTo,
            beats = crossRes.beats,
            backfill = crossRes.backfill,
            randomEvent = randomEvent,
            ended = crossRes.ended,
            notifyLevel = crossRes.leveledUpTo,
            notifyAct = crossRes.notifyAct,
        )
    }

    /** Lv95 抉择执行。canC 判定在加分前 (R1); 加分仅选后记账, 不参与结局判定。 */
    fun applyChoice(s: PetState, key: String, optId: String): ActionRes {
        val opt = StoryData.choiceByKey(key).options.firstOrNull { it.id == optId }
            ?: return ActionRes(null)
        s.choices[key] = optId
        if (key == "95") {
            s.preLinesAt95 = "${s.promiseLine}/${s.warmthLine}/${s.curiosityLine}"  // 判定前快照
            s.endingChosen = optId
            when (optId) {
                "keep" -> Rules.addPromise(s, Rules.GAIN_LV95_PROMISE)
                else -> Rules.addWarmth(s, Rules.GAIN_LV95_WARMTH)
            }
        } else {
            when (opt.effect) {
                "curiosity" -> Rules.addCuriosity(s, Rules.GAIN_CHOICE_CURIOSITY)
                "warmth" -> Rules.addWarmth(s, Rules.GAIN_CHOICE_WARMTH)
            }
        }
        s.xp += Rules.XP_CHOICE   // 豁免 45
        // 升级扫描 (抉择点一般不是节点级, 但 95→100 走真实 XP)
        val sweep = sweepLevels(s, 0, System.currentTimeMillis())
        return ActionRes(line = opt.result, sfx = "sfx_15_choice",
            leveledUpTo = sweep.leveledUpTo, beats = sweep.beats,
            backfill = sweep.backfill, ended = sweep.ended,
            notifyLevel = sweep.leveledUpTo, notifyAct = sweep.notifyAct)
    }

    /** Lv95 选项出现前: 三线 ≥80 (R1: 不叠加 Lv95 加分) */
    fun canTruth(s: PetState): Boolean =
        s.promiseLine >= Rules.C_THRESHOLD &&
            s.warmthLine >= Rules.C_THRESHOLD &&
            s.curiosityLine >= Rules.C_THRESHOLD

    /** Lv100 结局判定: A = 放她走 或 (守约但 Lv95 前承诺<60); B = 守约且承诺≥60; C = 真相 */
    fun decideEnding(s: PetState): String {
        val pre = s.preLinesAt95.split("/").map { it.toIntOrNull() ?: 0 }
        val prePromise = pre.getOrNull(0) ?: s.promiseLine
        val e = when (s.endingChosen) {
            "truth" -> "C"
            "keep" -> if (prePromise >= Rules.B_PROMISE_THRESHOLD) "B" else "A"
            else -> "A"
        }
        s.ended = e
        return e
    }

    /** 15% 随机 (当前幕池, 未触发的; 每条限 1 次) */
    fun rollRandomEvent(s: PetState): StoryData.REvent? {
        val pool = StoryData.eventsOfAct(s.act).filter { it.id !in s.eventsFired }
        if (pool.isEmpty()) return null
        if (Random.nextInt(100) >= 15) return null
        val pick = pool[Random.nextInt(pool.size)]
        s.eventsFired.add(pick.id)
        return pick
    }

    // ---------- 内部 ----------

    private data class SweepRes(
        val leveledUpTo: Int, val beats: List<StoryData.Beat>, val backfill: List<StoryData.REvent>,
        val ended: String, val notifyAct: Int,
    )

    /** 升级扫描: 从当前 level 爬到 xp 够的下一级 (可多级), 收集路上节点/记忆/过场 + 跨幕补触发 */
    private fun sweepLevels(s: PetState, extraXp: Int, nowMs: Long): SweepRes {
        // extraXp = 本次互动计入的 XP (已按 45 截断), 已写进 s.xp; 这里只爬级
        var leveled = false
        val beats = mutableListOf<StoryData.Beat>()
        val backfill = mutableListOf<StoryData.REvent>()
        var notifyAct = 0
        var ended = ""
        while (s.level < Rules.MAX_LEVEL && s.xp >= s.xpNeed()) {
            s.xp -= s.xpNeed()
            s.level += 1
            leveled = true
            // 跨幕过场 (25/50/75): 过场前补触发 + 进新幕通知
            if (s.level in Rules.ACT_CROSS_LEVELS) {
                val leavingAct = Rules.actOf(s.level - 1)
                backfill += Rules.backfillUnfired(s, leavingAct)
                notifyAct = s.level
            }
            beats += StoryData.beatsFor(s.level)
        }
        // 结局判定: 若 Lv95 抉择尚未做出则挂起 (UI 仍显示抉择块), 选完后由 applyChoice 的扫描补判
        if (s.level >= Rules.MAX_LEVEL && s.ended.isEmpty() &&
            StoryData.pendingChoiceLevel(s.level, s.choices) == null) {
            ended = decideEnding(s)
        }
        return SweepRes(
            leveledUpTo = if (leveled) s.level else 0,
            beats = beats, backfill = backfill, ended = ended, notifyAct = notifyAct,
        )
    }

    /** 日常 XP: 计入 45 硬上限 (自然日), 返回实际计入值 */
    private fun grantCappedXp(s: PetState, amount: Int): Int {
        val room = Rules.DAILY_XP_CAP - s.xpToday
        val g = if (amount > 0 && room > 0) amount.coerceAtMost(room) else 0
        s.xp += g
        s.xpToday += g
        return g
    }

    private fun applyHiddenGain(s: PetState, ev: StoryData.REvent) {
        when (ev.hidden) {
            "promise" -> Rules.addPromise(s, Rules.GAIN_EVENT_HIDDEN)
            "warmth" -> Rules.addWarmth(s, Rules.GAIN_EVENT_HIDDEN)
            "curiosity" -> Rules.addCuriosity(s, Rules.GAIN_EVENT_HIDDEN)
        }
    }

    // ---------- 离线回归 ----------

    /** 进前台: 日切重置 + 离线衰减 (饱食 4h-30 / 心情 12h-20, 24h 封顶) + 体力 2h+1。
     *  返回「是否离线 >4h 回归」(温情台词用) */
    fun onEnter(s: PetState, nowMs: Long): Boolean {
        val isReturn = s.started && s.lastOpenMs > 0 &&
            (nowMs - s.lastOpenMs) > Rules.OFFLINE_RETURN_MS && s.ended.isEmpty()
        val today = Rules.dayKeyOf(nowMs)
        if (s.dayKey != today) {
            s.dayKey = today
            s.xpToday = 0
            s.dailyBookshelf = 0
            s.dailyTv = 0
        }
        if (s.started && s.lastOpenMs > 0) {
            if (s.ended.isEmpty()) {
                val elapsed = (nowMs - s.lastOpenMs).coerceIn(0L, Rules.OFFLINE_DECAY_CAP_MS)
                val hours = elapsed / 3_600_000L
                s.satiety = (s.satiety - (hours / 4L * 30L).toInt()).coerceIn(0, 100)
                s.mood = (s.mood - (hours / 12L * 20L).toInt()).coerceIn(0, 100)
                val regen = ((elapsed / Rules.ENERGY_REGEN_MS).toInt()).coerceIn(0, s.energyCap() - s.energy).coerceAtLeast(0)
                s.energy = (s.energy + regen).coerceAtMost(s.energyCap())
            }
            s.lastOpenMs = nowMs
        } else {
            s.lastOpenMs = nowMs
        }
        return isReturn
    }
}

fun dayKeyOfMs(nowMs: Long): String {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = nowMs
    return "%04d-%02d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1, cal.get(java.util.Calendar.DAY_OF_MONTH))
}
