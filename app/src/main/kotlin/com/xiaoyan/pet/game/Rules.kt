package com.xiaoyan.pet.game

import com.xiaoyan.pet.data.PetState

/**
 * 养成数值体系 — 严格按设定圣经 v3.1 §1/§2 写死 (唯一真相源)。
 *
 * 关键硬规则:
 * - 体力三档上限跟亲密: <50→5, 50-79→7, ≥80→10
 * - 睡觉: 体力回满, 代价饱食 -15 (饱食<15 睡完归 0 + S13), cd 15 分钟, 温度 +2
 * - 每日 XP 硬上限 45 (自然日, 本地 0 点); 抉择 / 事件 XP 豁免 (不计入也不受 45 限制)
 * - 隐藏线 C 路径有效上限 104(承诺) / 103(温度) / 110(好奇):
 *   三线 ≥80 判定在 Lv95 选项出现之前执行, 不叠加 Lv95 自身加分 (承诺+5/温度+3 仅选后记账)
 * - 到顶: 来源不再生效, 互动照常执行、反馈照常, 只是不再加线, 不做视觉置灰
 * - 不耗体力交互日上限 (R5): 书架 3 次/日, 电视 2 次/日
 * - 隐藏线事件跨幕强制补触发 (R4): Lv25/50/75 过场前, 本幕未触发隐藏线事件逐条补, 最多 2 条
 */
object Rules {

    const val MAX_LEVEL = 100

    // ---------- 每日 XP 硬上限 (v3.1 R2) ----------
    const val DAILY_XP_CAP = 45

    /** 互动 XP (计入 45): 喂食+2 / 大餐+5 / 小憩+1 / 电视+2 / 学习+8 / 玩耍+6 / 观星+6 */
    const val XP_FEED = 2
    const val XP_FEAST = 5
    const val XP_NAP = 1
    const val XP_TV = 2
    const val XP_STUDY = 8
    const val XP_PLAY = 6
    const val XP_STARGAZE = 6

    /** 豁免 XP (不计入 45、不受 45 限制) */
    const val XP_CHOICE = 10
    const val XP_EVENT = 3

    // ---------- 体力 ----------
    /** 体力三档上限跟亲密 */
    fun energyCapOf(intimacy: Int): Int = when {
        intimacy >= 80 -> 10
        intimacy >= 50 -> 7
        else -> 5
    }

    /** 耗体力(-1) 的 8 种交互: 喂食/大餐/小憩/学习/玩耍/观星/采花/买零食 */
    fun consumesEnergy(hotspot: String): Boolean = when (hotspot) {
        "feed", "feast", "nap", "study", "play", "stargaze", "flowerpick", "snackbuy" -> true
        else -> false  // tv / bookshelf / sleep / wake / chime 不耗体力
    }

    const val SLEEP_CD_MS = 15L * 60L * 1000L      // 睡觉 cd 15 分钟
    const val SLEEP_SATIE_PENALTY = 15              // 睡觉扣饱食 15
    const val SLEEP_WARMTH = 2                      // 睡觉 温度 +2

    // ---------- 隐藏 3 线 (v3.1 C 路径有效上限) ----------
    const val CAP_PROMISE = 104
    const val CAP_WARMTH = 103
    const val CAP_CURIOSITY = 110
    const val C_THRESHOLD = 80                       // 三线 ≥80 判定线 (Lv95 加分前)
    const val B_PROMISE_THRESHOLD = 60               // B: Lv95 前承诺 ≥60

    /** 单线增量表 (v3.1, 单次增量不变, 到顶后来源不再生效) */
    const val GAIN_NAP_WARMTH = 3                     // 沙发小憩
    const val GAIN_SLEEP_WARMTH = 2                   // 床睡觉
    const val GAIN_TV_CURIOSITY = 1                   // 电视 (2 次/日)
    const val GAIN_BOOKSHELF_PROMISE = 2              // 书架讲故事 (3 次/日)
    const val GAIN_STUDY_CURIOSITY = 3                // 书桌学习
    const val GAIN_SWING_CURIOSITY = 2                // 秋千玩耍
    const val GAIN_STARGAZE_PROMISE = 4               // 山丘观星 (三幕起 + 夜间)
    const val GAIN_CHOICE_WARMTH = 3                  // Lv20/45/70 暖系选项
    const val GAIN_CHOICE_CURIOSITY = 3               // Lv20/45/70 好奇系选项
    const val GAIN_LV95_PROMISE = 5                   // Lv95「一起守住约定」(仅选后记账)
    const val GAIN_LV95_WARMTH = 3                     // Lv95 任意选项 (仅选后记账)
    const val GAIN_EVENT_HIDDEN = 4                   // 隐藏线事件 (每线 2 条, 各 +4, 限 1 次/条)

    // ---------- 不耗体力交互日上限 (R5) ----------
    const val BOOKSHELF_PER_DAY = 3
    const val TV_PER_DAY = 2

    // ---------- 幕 / 级 ----------
    fun actOf(level: Int): Int = when {
        level >= 76 -> 4
        level >= 51 -> 3
        level >= 26 -> 2
        else -> 1
    }

    /** 每级所需 XP 分段: 一幕 8 / 二幕 12 / 三幕 16 / 四幕 20 */
    fun xpNeedFor(level: Int): Int = when {
        level >= 76 -> 20
        level >= 51 -> 16
        level >= 26 -> 12
        else -> 8
    }

    val CHOICE_LEVELS = listOf(20, 45, 70, 95)

    /** 跨幕过场 (光柱 600ms + BGM 交叉淡化 + 补触发): 25/50/75; 100 = 终幕 */
    val ACT_CROSS_LEVELS = listOf(25, 50, 75)

    /** 季节节点事件 (B4, 全落本幕, 避开抉择点): 一幕 8/13/18/23 · 二幕 28/34/40/48 · 三幕 53/58/64/73 · 四幕 78/83/88/93 */
    val SEASON_NODE_LEVELS = mapOf(
        1 to listOf(8, 13, 18, 23),
        2 to listOf(28, 34, 40, 48),
        3 to listOf(53, 58, 64, 73),
        4 to listOf(78, 83, 88, 93),
    )

    /** 记忆碎片节点 */
    val MEMORY_LEVELS = mapOf(10 to "memory1", 35 to "memory2", 60 to "memory3", 80 to "memory4")

    /** 场景解锁 (幕 → 可用场景数): 序幕/一幕 室内4, 二幕 +公园=5, 三幕 +街道=6, 四幕 +山丘=7 */
    fun unlockedScenes(act: Int): List<String> = when {
        act >= 4 -> listOf("livingroom", "kitchen", "bedroom", "study", "park", "road", "hill")
        act >= 3 -> listOf("livingroom", "kitchen", "bedroom", "study", "park", "road")
        act >= 2 -> listOf("livingroom", "kitchen", "bedroom", "study", "park")
        else -> listOf("livingroom", "kitchen", "bedroom", "study")
    }

    /** 观星条件: 山丘已解锁 (三幕起) 且处于夜间 */
    fun canStargaze(s: PetState, dayPhase: String): Boolean =
        s.act >= 3 && dayPhase == "night"

    // ---------- 隐藏线到顶判定 ----------
    fun promiseFull(v: Int) = v >= CAP_PROMISE
    fun warmthFull(v: Int) = v >= CAP_WARMTH
    fun curiosityFull(v: Int) = v >= CAP_CURIOSITY

    /** 到顶不加线、不置灰: 加值函数 (调用方照常执行互动与反馈) */
    fun addPromise(s: PetState, gain: Int) {
        if (!promiseFull(s.promiseLine)) s.promiseLine = (s.promiseLine + gain).coerceAtMost(CAP_PROMISE)
    }

    fun addWarmth(s: PetState, gain: Int) {
        if (!warmthFull(s.warmthLine)) s.warmthLine = (s.warmthLine + gain).coerceAtMost(CAP_WARMTH)
    }

    fun addCuriosity(s: PetState, gain: Int) {
        if (!curiosityFull(s.curiosityLine)) s.curiosityLine = (s.curiosityLine + gain).coerceAtMost(CAP_CURIOSITY)
    }

    /** 心意三枚星三态 (G2): 微光(0-39) / 半亮(40-79) / 全亮(≥80), 不剧透数值 */
    fun starState(v: Int): Int = when {
        v >= C_THRESHOLD -> 2
        v >= 40 -> 1
        else -> 0
    }

    /** 自然日 key (本地 0 点; XP 45 上限与日上限计数按此重置) */
    fun dayKeyOf(nowMs: Long): String = dayKeyOfMs(nowMs)

    /** 跨幕补触发 (R4): 离开第 leavingAct 幕时, 该幕未触发隐藏线事件, 逐条取, 最多 2 条 */
    fun backfillUnfired(s: PetState, leavingAct: Int): List<StoryData.REvent> =
        StoryData.hiddenEventsOf(leavingAct).filter { it.id !in s.eventsFired }.take(2)

    // ---------- 离线衰减 (圣经 §2: 饱食 4h -30, 24h 触底; 心情 12h -20; 体力/亲密无衰减) ----------
    const val OFFLINE_DECAY_CAP_MS = 24L * 3600_000L
    const val OFFLINE_RETURN_MS = 4L * 3600_000L   // 离线 >4h = 回归
    const val ENERGY_REGEN_MS = 2L * 3600_000L     // 体力自然回复每 2h +1 (仅服务离线回归体验)
}
