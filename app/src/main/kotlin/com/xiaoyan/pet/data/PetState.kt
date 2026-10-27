package com.xiaoyan.pet.data

import com.xiaoyan.pet.game.Rules
import org.json.JSONArray
import org.json.JSONObject

/**
 * 小桃养成记 v1.3.0 存档 (设定圣经 v3.1 · save_version = 3, 全新格式, 从零开始)。
 *
 * 表面 4 项: 饱食 / 心情 / 体力 / 亲密 (亲密决定体力上限档)
 * 隐藏 3 线: 承诺 / 温度 / 好奇 (C 路径有效上限 104/103/110, 见 Rules)
 * 旧档 (saveVersion=2) 文件保留但不自动加载; 云端 v2 行由新档 upsert 覆盖 (设计使然)。
 */
data class PetState(
    var name: String = "",                    // 小桃的名字 (序幕起名), 空 = 尚未开始
    var promiseAnswer: String = "",            // 序幕「你想许一个什么约」答案 (结局 C 用到)
    var level: Int = 1,
    var xp: Int = 0,
    var xpToday: Int = 0,                      // 当日(自然日)已获 XP (抉择/事件豁免不计入)
    var dayKey: String = "",                   // yyyy-MM-dd, 跨日重置 xpToday/日上限计数
    var satiety: Int = 80,                     // 饱食
    var mood: Int = 80,                        // 心情
    var energy: Int = 5,                       // 体力 (上限跟亲密档, 睡觉回满)
    var intimacy: Int = 50,                    // 亲密 (决定体力上限 5/7/10)
    var promiseLine: Int = 0,                  // 隐藏: 承诺线
    var warmthLine: Int = 0,                   // 隐藏: 温度线
    var curiosityLine: Int = 0,                // 隐藏: 好奇线
    var asleep: Boolean = false,               // 睡觉 → 夜间; 起床 → 早晨
    var lastOpenMs: Long = 0L,
    var lastSleepMs: Long = 0L,                // 睡觉 cd 15 分钟
    var interactions: Int = 0,
    var ended: String = "",                    // "" | "A" 春风归途 | "B" 小镇永远 | "C" 永恒的约定
    var endingSeen: Boolean = false,
    var endingChosen: String = "",             // Lv95 抉择结果 ("return"/"keep"/"truth")
    var preLinesAt95: String = "",            // Lv95 选择前三线快照 "p/w/c" (B 判定用, 加分前)
    var choices: MutableMap<String, String> = mutableMapOf(),      // "20"/"45"/"70"/"95" -> 选项 id
    var eventsFired: MutableList<String> = mutableListOf(),         // 已触发事件 id (16 条各 1 次)
    var dailyBookshelf: Int = 0,              // 书架讲故事故事 次/日 (上限 3, R5)
    var dailyTv: Int = 0,                     // 电视 次/日 (上限 2, R5)
    var lastAct: Int = 1,                     // 上次所在幕 (跨幕通知/补触发判定)
) {
    val started: Boolean get() = name.isNotEmpty()

    val act: Int get() = Rules.actOf(level)

    val maxLevel: Boolean get() = level >= Rules.MAX_LEVEL

    fun xpNeed(): Int = Rules.xpNeedFor(level)

    fun energyCap(): Int = Rules.energyCapOf(intimacy)

    fun isUnwell(): Boolean = satiety < 30 || mood < 20

    fun dayPhaseForced(): String =
        if (asleep) "night" else ""

    fun toJson(): JSONObject = JSONObject().apply {
        put("saveVersion", SAVE_VERSION)
        put("name", name)
        put("promiseAnswer", promiseAnswer)
        put("level", level)
        put("xp", xp)
        put("xpToday", xpToday)
        put("dayKey", dayKey)
        put("satiety", satiety)
        put("mood", mood)
        put("energy", energy)
        put("intimacy", intimacy)
        put("promiseLine", promiseLine)
        put("warmthLine", warmthLine)
        put("curiosityLine", curiosityLine)
        put("asleep", asleep)
        put("lastOpenMs", lastOpenMs)
        put("lastSleepMs", lastSleepMs)
        put("interactions", interactions)
        put("ended", ended)
        put("endingSeen", endingSeen)
        put("endingChosen", endingChosen)
        put("preLinesAt95", preLinesAt95)
        put("choices", JSONObject().apply { choices.forEach { (k, v) -> put(k, v) } })
        put("eventsFired", JSONArray().apply { eventsFired.forEach { put(it) } })
        put("dailyBookshelf", dailyBookshelf)
        put("dailyTv", dailyTv)
        put("lastAct", lastAct)
    }

    companion object {
        const val MAX_LEVEL = 100
        const val SAVE_VERSION = 3

        /** 解析失败/版本不符返回 null (全字段 opt + 默认值) */
        fun fromJson(text: String): PetState? = runCatching {
            val o = JSONObject(text)
            if (o.optInt("saveVersion", -1) != SAVE_VERSION) return@runCatching null
            val choiceObj = o.optJSONObject("choices") ?: JSONObject()
            val eventArr = o.optJSONArray("eventsFired") ?: JSONArray()
            PetState(
                name = o.optString("name", ""),
                promiseAnswer = o.optString("promiseAnswer", ""),
                level = o.optInt("level", 1).coerceIn(1, MAX_LEVEL),
                xp = o.optInt("xp", 0).coerceAtLeast(0),
                xpToday = o.optInt("xpToday", 0).coerceAtLeast(0),
                dayKey = o.optString("dayKey", ""),
                satiety = o.optInt("satiety", 80).coerceIn(0, 100),
                mood = o.optInt("mood", 80).coerceIn(0, 100),
                energy = o.optInt("energy", 5).coerceAtLeast(0),
                intimacy = o.optInt("intimacy", 50).coerceIn(0, 100),
                promiseLine = o.optInt("promiseLine", 0),
                warmthLine = o.optInt("warmthLine", 0),
                curiosityLine = o.optInt("curiosityLine", 0),
                asleep = o.optBoolean("asleep", false),
                lastOpenMs = o.optLong("lastOpenMs", 0L),
                lastSleepMs = o.optLong("lastSleepMs", 0L),
                interactions = o.optInt("interactions", 0).coerceAtLeast(0),
                ended = o.optString("ended", ""),
                endingSeen = o.optBoolean("endingSeen", false),
                endingChosen = o.optString("endingChosen", ""),
                preLinesAt95 = o.optString("preLinesAt95", ""),
                choices = choiceObj.keys().asSequence().associateWith { choiceObj.optString(it, "") }.toMutableMap(),
                eventsFired = (0 until eventArr.length()).map { eventArr.optString(it, "") }.filter { it.isNotEmpty() }.toMutableList(),
                dailyBookshelf = o.optInt("dailyBookshelf", 0).coerceIn(0, 3),
                dailyTv = o.optInt("dailyTv", 0).coerceIn(0, 2),
                lastAct = o.optInt("lastAct", 1).coerceIn(1, 4),
            )
        }.getOrNull()

        /** 存档文本的 saveVersion (旧档 2 版检测用, 解析失败 -1) */
        fun saveVersionOf(text: String?): Int =
            if (text.isNullOrBlank()) -1 else runCatching { JSONObject(text).optInt("saveVersion", -1) }.getOrDefault(-1)
    }
}
