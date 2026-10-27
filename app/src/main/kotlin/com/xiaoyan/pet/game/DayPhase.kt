package com.xiaoyan.pet.game

import com.xiaoyan.pet.data.SaveRepository
import java.util.Calendar
import java.util.TimeZone

/**
 * 4 态日时系统 (v4 裁定: 日时 2 态扩 4 态; 圣经未定触发/推进规则, 对话三定案如下):
 *
 * 状态: morning(早 05-11) / noon(午 11-14) / afternoon(下午 14-18) / night(晚 18-05)
 * 推进规则:
 *  1. 默认 = 游戏内时钟 1:1 跟现实时间, 每次进前台/互动时校正到当前时段;
 *  2. 「卧室·床 睡觉」→ 立即切 night (bedtime); 「起床」→ 切 morning;
 *  3. 设置页「强制夜间 / 强制白天」手动开关兜底 (强制白天 = afternoon, 优先级最高);
 *  4. 离线回归: 不推进、不衰减, 仅回归时按 1/2/3 重新校正。
 * 观星条件: 山丘已解锁 (三幕起) 且当前处于 night (一幕起日时系统即可用, 观星要等山丘)。
 * 背景 = 场景 × 4 光照真图 (夜间滤镜 ColorMatrix 方案 v4 作废, 直接切图)。
 */
object DayPhase {

    const val MORNING = "morning"
    const val NOON = "noon"
    const val AFTERNOON = "afternoon"
    const val NIGHT = "night"

    /** 现实小时 → 时段 */
    fun fromClock(nowMs: Long): String {
        val h = Calendar.getInstance(TimeZone.getDefault()).apply { timeInMillis = nowMs }.get(Calendar.HOUR_OF_DAY)
        return when {
            h in 5..10 -> MORNING
            h in 11..13 -> NOON
            h in 14..17 -> AFTERNOON
            else -> NIGHT
        }
    }

    /** 当前生效时段 (优先级: 强制开关 > 睡觉 > 时钟) */
    fun current(repo: SaveRepository, asleep: Boolean, nowMs: Long): String = when {
        repo.forceNightOn() -> NIGHT
        repo.forceDayOn() -> AFTERNOON
        asleep -> NIGHT
        else -> fromClock(nowMs)
    }

    /** 时段显示名 (UI 用) */
    fun label(phase: String): String = when (phase) {
        MORNING -> "早"
        NOON -> "午"
        AFTERNOON -> "下午"
        NIGHT -> "晚"
        else -> "晚"
    }

    fun icon(phase: String): String = when (phase) {
        MORNING -> "🌅"
        NOON -> "☀️"
        AFTERNOON -> "🌇"
        else -> "🌙"
    }
}
