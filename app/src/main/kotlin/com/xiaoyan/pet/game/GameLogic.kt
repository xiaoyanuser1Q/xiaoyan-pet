package com.xiaoyan.pet.game

import com.xiaoyan.pet.data.PetState

/** 一次互动动作的结果 */
data class ActionResult(
    val message: String,
    val leveledUpTo: Int = 0,        // 0 = 未升级
    val events: List<StoryEvent> = emptyList(),
    val sfx: String = "sfx_tap"      // res/raw 资源名
)

object GameLogic {

    /** 离线衰减上限(小时): 离开最多扣 12 小时, 避免"惩罚回归玩家" */
    private const val DECAY_CAP_HOURS = 12

    /** 离线衰减: 按离开区间扣数值, 调用后更新 lastOpenedMs */
    fun applyDecay(s: PetState, nowMs: Long) {
        if (!s.started || s.lastOpenedMs <= 0L) {
            s.lastOpenedMs = nowMs
            return
        }
        if (s.ended.isNotEmpty()) return // 结局后不再衰减
        val elapsedMs = (nowMs - s.lastOpenedMs).coerceAtLeast(0L)
        val hours = Math.min(elapsedMs / 3_600_000L, DECAY_CAP_HOURS.toLong()).toDouble()
        if (hours > 0.0) {
            s.hunger = (s.hunger - hours * 1.2).toInt().coerceIn(0, 100)
            s.mood = (s.mood - hours * 0.8).toInt().coerceIn(0, 100)
            s.health = (s.health - hours * 0.5).toInt().coerceIn(0, 100)
            s.intimacy = (s.intimacy - hours * 0.3).toInt().coerceIn(0, 100)
        }
        s.lastOpenedMs = nowMs
    }

    fun feed(s: PetState): ActionResult {
        val rescue = s.hunger < 30
        s.hunger = (s.hunger + 18).coerceIn(0, 100)
        s.mood = (s.mood + 4).coerceIn(0, 100)
        s.trust = (s.trust + if (rescue) 3 else 1).coerceIn(0, 100)
        val msg = if (rescue) "小桃饿坏了，大口大口吃着。「啾啾~」" else "小桃开心地吃着，腮帮子鼓鼓的。"
        return grantXp(s, 5, msg, "sfx_feed")
    }

    fun play(s: PetState): ActionResult {
        s.mood = (s.mood + 15).coerceIn(0, 100)
        s.health = (s.health - 3).coerceIn(0, 100)
        s.exploration = (s.exploration + 2).coerceIn(0, 100)
        return grantXp(s, 8, "小桃和你玩了一会儿，叶子摇成了小风扇。", "sfx_play")
    }

    fun rest(s: PetState): ActionResult {
        s.health = (s.health + 12).coerceIn(0, 100)
        s.hunger = (s.hunger - 6).coerceIn(0, 100)
        s.trust = (s.trust + 1).coerceIn(0, 100)
        return grantXp(s, 4, "小桃眯起眼睛睡着了，呼吸很轻。", "sfx_tap")
    }

    fun pet(s: PetState): ActionResult {
        s.intimacy = (s.intimacy + 8).coerceIn(0, 100)
        s.mood = (s.mood + 2).coerceIn(0, 100)
        s.trust = (s.trust + 1).coerceIn(0, 100)
        return grantXp(s, 6, "小桃蹭了蹭你的手心，发出很轻的「哼~」。", "sfx_tap")
    }

    fun talk(s: PetState, text: String): ActionResult {
        s.understanding = (s.understanding + 3).coerceIn(0, 100)
        s.intimacy = (s.intimacy + 2).coerceIn(0, 100)
        return grantXp(s, 10, "你把心里话告诉了小桃。它听着，没有动，好像在记住。", "sfx_play")
    }

    /** 判断并触发结局 (level 100 时调用) */
    fun decideEnding(s: PetState): String {
        val e = when {
            s.intimacy < 30 -> "C" // 低陪伴 → 消散
            s.trust >= 70 && s.exploration >= 45 && s.understanding >= 45 -> "B" // 三线全高 → 永恒
            else -> "A" // 默认 → 归途
        }
        s.ended = e
        return e
    }

    private fun grantXp(s: PetState, amount: Int, msg: String, sfx: String): ActionResult {
        s.interactions += 1
        if (s.ended.isNotEmpty()) {
            // 结局后互动不再升级
            return ActionResult(msg)
        }
        var leveledUpTo = 0
        if (!s.maxLevel) {
            s.xp += amount
            while (!s.maxLevel && s.xp >= s.xpNeed()) {
                s.xp -= s.xpNeed()
                s.level += 1
                leveledUpTo = s.level
            }
            if (s.level >= PetState.MAX_LEVEL) {
                // 到达 100 级: 触发结局判定
                decideEnding(s)
            }
        }
        val events = if (leveledUpTo in 1..PetState.MAX_LEVEL) Story.eventsFor(leveledUpTo) else emptyList()
        return if (leveledUpTo > 0) {
            ActionResult(msg, leveledUpTo, events, "sfx_levelup")
        } else {
            ActionResult(msg, 0, events, sfx)
        }
    }
}
