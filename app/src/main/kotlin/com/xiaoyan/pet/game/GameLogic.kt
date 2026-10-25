package com.xiaoyan.pet.game

import com.xiaoyan.pet.data.PetState
import com.xiaoyan.pet.ui.PortraitMood
import kotlin.random.Random

/** 一次互动动作的结果 */
data class ActionResult(
    val message: String,
    val leveledUpTo: Int = 0,        // 0 = 未升级
    val events: List<StoryEvent> = emptyList(),
    val sfx: String = "sfx_tap",     // res/raw 资源名
    val line: StoryLine? = null      // v1.2.0: VN 台词 (带 mood, 立绘切表情)
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
        return grantXp(s, 5, msg, "sfx_feed", StoryLine(msg, PortraitMood.HAPPY))
    }

    fun play(s: PetState): ActionResult {
        s.mood = (s.mood + 15).coerceIn(0, 100)
        s.health = (s.health - 3).coerceIn(0, 100)
        s.exploration = (s.exploration + 2).coerceIn(0, 100)
        val msg = "小桃和你玩了一会儿，叶子摇成了小风扇。"
        return grantXp(s, 8, msg, "sfx_play", StoryLine(msg, PortraitMood.SURPRISED))
    }

    fun rest(s: PetState): ActionResult {
        s.health = (s.health + 12).coerceIn(0, 100)
        s.hunger = (s.hunger - 6).coerceIn(0, 100)
        s.trust = (s.trust + 1).coerceIn(0, 100)
        val msg = "小桃眯起眼睛睡着了，呼吸很轻。"
        return grantXp(s, 4, msg, "sfx_tap", StoryLine(msg, PortraitMood.NEUTRAL))
    }

    fun pet(s: PetState): ActionResult {
        s.intimacy = (s.intimacy + 8).coerceIn(0, 100)
        s.mood = (s.mood + 2).coerceIn(0, 100)
        s.trust = (s.trust + 1).coerceIn(0, 100)
        val msg = "小桃蹭了蹭你的手心，发出很轻的「哼~」。"
        return grantXp(s, 6, msg, "sfx_tap", StoryLine(msg, PortraitMood.SHY))
    }

    fun talk(s: PetState, text: String): ActionResult {
        s.understanding = (s.understanding + 3).coerceIn(0, 100)
        s.intimacy = (s.intimacy + 2).coerceIn(0, 100)
        val msg = "你把心里话告诉了小桃。她听着，没有动，好像在记住。"
        return grantXp(s, 10, msg, "sfx_play", StoryLine(msg, PortraitMood.NEUTRAL))
    }

    /** v1.2.0 轻分支: 执行某幕的二选一抉择, 注入隐藏属性并记录 */
    fun applyChoice(s: PetState, act: Int, optionIndex: Int): ActionResult {
        val choice = Story.choiceFor(act)
        val opt = choice.options.getOrNull(optionIndex) ?: return ActionResult("（选择尚未做出）")
        when (opt.effect) {
            "trust" -> s.trust = (s.trust + 5).coerceIn(0, 100)
            "exploration" -> s.exploration = (s.exploration + 5).coerceIn(0, 100)
            "understanding" -> s.understanding = (s.understanding + 5).coerceIn(0, 100)
        }
        s.choices["act$act"] = opt.label
        return grantXp(s, 8, opt.result.text, "sfx_tap", opt.result)
    }

    /** 某幕是否还没做出抉择 */
    fun pendingChoice(s: PetState): Int? = when {
        s.choices["act1"].isNullOrEmpty() -> 1
        s.choices["act2"].isNullOrEmpty() -> 2
        s.choices["act3"].isNullOrEmpty() -> 3
        else -> null
    }

    /** v1.2.0 随机小事件: 15% 概率, 每条只触发一次, 未触发过才出 */
    fun rollRandomEvent(s: PetState): StoryEvent? {
        if (s.ended.isNotEmpty()) return null
        val fired = s.eventFlags.toSet()
        val pool = (0 until Story.RANDOM_EVENTS.size).filter { it !in fired }
        if (pool.isEmpty()) return null
        if (Random.nextInt(100) >= 15) return null
        val pick = pool[Random.nextInt(pool.size)]
        s.eventFlags.add(pick.toString())
        return Story.randomEventIndex(pick)
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

    private fun grantXp(
        s: PetState,
        amount: Int,
        msg: String,
        sfx: String,
        line: StoryLine? = null,
    ): ActionResult {
        s.interactions += 1
        if (s.ended.isNotEmpty()) {
            // 结局后互动不再升级
            return ActionResult(msg, sfx = sfx, line = line)
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
        val finalLine = if (leveledUpTo > 0) StoryLine("她长大了，变成 Lv.$leveledUpTo。（叮~）", PortraitMood.SURPRISED) else line
        return if (leveledUpTo > 0) {
            ActionResult(msg, leveledUpTo, events, "sfx_levelup", finalLine)
        } else {
            ActionResult(msg, 0, events, sfx, line)
        }
    }
}
