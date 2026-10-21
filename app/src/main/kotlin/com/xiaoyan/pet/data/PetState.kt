package com.xiaoyan.pet.data

import org.json.JSONObject

/**
 * 小桃养成记 · 存档数据
 * 表面数值: 饥饿 / 心情 / 健康 / 亲密 (0-100)
 * 隐藏属性: 信任 / 探索 / 理解 (决定结局)
 */
data class PetState(
    var name: String = "",            // 小桃的名字, 空 = 尚未开始
    var missingThing: String = "",     // 玩家想找回的东西
    var level: Int = 1,
    var xp: Int = 0,
    var hunger: Int = 80,
    var mood: Int = 80,
    var health: Int = 80,
    var intimacy: Int = 50,
    var trust: Int = 10,              // 隐藏: 信任
    var exploration: Int = 10,        // 隐藏: 探索
    var understanding: Int = 10,      // 隐藏: 理解
    var lastOpenedMs: Long = 0L,
    var interactions: Int = 0,
    var ended: String = "",           // "" | "A" 归途 | "B" 永恒 | "C" 消散
    var endingFirstWord: String = "", // 结局B: 玩家填入的第一句话
    var endingSeen: Boolean = false   // 结局是否已读完
) {
    val started: Boolean get() = name.isNotEmpty()

    val act: Int get() = when {
        level >= 61 -> 3
        level >= 31 -> 2
        else -> 1
    }

    /** 1 = 粉团子, 2 = 觉醒形态(星辉纹路) */
    val stage: Int get() = if (level >= 31) 2 else 1

    val maxLevel: Boolean get() = level >= MAX_LEVEL

    fun xpNeed(): Int = 8 + level * 2

    fun isUnwell(): Boolean = hunger < 20 || mood < 20 || health < 20

    fun toJson(): JSONObject = JSONObject().apply {
        put("name", name)
        put("missingThing", missingThing)
        put("level", level)
        put("xp", xp)
        put("hunger", hunger)
        put("mood", mood)
        put("health", health)
        put("intimacy", intimacy)
        put("trust", trust)
        put("exploration", exploration)
        put("understanding", understanding)
        put("lastOpenedMs", lastOpenedMs)
        put("interactions", interactions)
        put("ended", ended)
        put("endingFirstWord", endingFirstWord)
        put("endingSeen", endingSeen)
        // 老存档兼容: 新增字段缺省时用默认值
    }

    companion object {
        const val MAX_LEVEL = 100

        /** 解析失败返回 null, 不抛异常 */
        fun fromJson(text: String): PetState? = runCatching {
            val o = JSONObject(text)
            PetState(
                name = o.optString("name", ""),
                missingThing = o.optString("missingThing", ""),
                level = o.optInt("level", 1).coerceIn(1, MAX_LEVEL),
                xp = o.optInt("xp", 0).coerceAtLeast(0),
                hunger = o.optInt("hunger", 80).coerceIn(0, 100),
                mood = o.optInt("mood", 80).coerceIn(0, 100),
                health = o.optInt("health", 80).coerceIn(0, 100),
                intimacy = o.optInt("intimacy", 50).coerceIn(0, 100),
                trust = o.optInt("trust", 10).coerceIn(0, 100),
                exploration = o.optInt("exploration", 10).coerceIn(0, 100),
                understanding = o.optInt("understanding", 10).coerceIn(0, 100),
                lastOpenedMs = o.optLong("lastOpenedMs", 0L),
                interactions = o.optInt("interactions", 0).coerceAtLeast(0),
                ended = o.optString("ended", ""),
                endingFirstWord = o.optString("endingFirstWord", ""),
                endingSeen = o.optBoolean("endingSeen", false)
            ).also {
                if (it.ended.isNotEmpty()) it.level = it.level.coerceAtMost(MAX_LEVEL)
            }
        }.getOrNull()
    }
}
