package com.xiaoyan.pet.game

import com.xiaoyan.pet.R

/**
 * 7 场景 × 4 光照 = 28 张背景 (全部真图, 夜间滤镜作废) + 12 互动锚点 + 换装优先级。
 * 主保险 (R8): 全静态 when 映射; 兜底 res/raw/keep.xml。
 */
object Scenes {

    const val LIVINGROOM = "livingroom"
    const val KITCHEN = "kitchen"
    const val BEDROOM = "bedroom"
    const val STUDY = "study"
    const val PARK = "park"
    const val ROAD = "road"
    const val HILL = "hill"

    val ALL = listOf(LIVINGROOM, KITCHEN, BEDROOM, STUDY, PARK, ROAD, HILL)

    fun label(scene: String): String = when (scene) {
        LIVINGROOM -> "客厅"
        KITCHEN -> "厨房"
        BEDROOM -> "卧室"
        STUDY -> "书房"
        PARK -> "公园"
        ROAD -> "街道"
        HILL -> "星空山丘"
        else -> ""
    }

    fun icon(scene: String): String = when (scene) {
        LIVINGROOM -> "🛋️"
        KITCHEN -> "🍳"
        BEDROOM -> "🛏️"
        STUDY -> "📖"
        PARK -> "🌳"
        ROAD -> "🏮"
        HILL -> "✨"
        else -> ""
    }

    /** 场景 × 4 光照 → 背景资源 (全静态 when, AAPT 可追踪) */
    fun bgResFor(scene: String, phase: String): Int = when (scene) {
        LIVINGROOM -> when (phase) {
            DayPhase.MORNING -> R.drawable.bg_livingroom_morning
            DayPhase.NOON -> R.drawable.bg_livingroom_noon
            DayPhase.AFTERNOON -> R.drawable.bg_livingroom_afternoon
            else -> R.drawable.bg_livingroom_night
        }
        KITCHEN -> when (phase) {
            DayPhase.MORNING -> R.drawable.bg_kitchen_morning
            DayPhase.NOON -> R.drawable.bg_kitchen_noon
            DayPhase.AFTERNOON -> R.drawable.bg_kitchen_afternoon
            else -> R.drawable.bg_kitchen_night
        }
        BEDROOM -> when (phase) {
            DayPhase.MORNING -> R.drawable.bg_bedroom_morning
            DayPhase.NOON -> R.drawable.bg_bedroom_noon
            DayPhase.AFTERNOON -> R.drawable.bg_bedroom_afternoon
            else -> R.drawable.bg_bedroom_night
        }
        STUDY -> when (phase) {
            DayPhase.MORNING -> R.drawable.bg_study_morning
            DayPhase.NOON -> R.drawable.bg_study_noon
            DayPhase.AFTERNOON -> R.drawable.bg_study_afternoon
            else -> R.drawable.bg_study_night
        }
        PARK -> when (phase) {
            DayPhase.MORNING -> R.drawable.bg_park_morning
            DayPhase.NOON -> R.drawable.bg_park_noon
            DayPhase.AFTERNOON -> R.drawable.bg_park_afternoon
            else -> R.drawable.bg_park_night
        }
        ROAD -> when (phase) {
            DayPhase.MORNING -> R.drawable.bg_road_morning
            DayPhase.NOON -> R.drawable.bg_road_noon
            DayPhase.AFTERNOON -> R.drawable.bg_road_afternoon
            else -> R.drawable.bg_road_night
        }
        HILL -> when (phase) {
            DayPhase.MORNING -> R.drawable.bg_hill_morning
            DayPhase.NOON -> R.drawable.bg_hill_noon
            DayPhase.AFTERNOON -> R.drawable.bg_hill_afternoon
            else -> R.drawable.bg_hill_night
        }
        else -> R.drawable.bg_livingroom_morning
    }

    /** 12 互动锚点: 场景 → 可点交互 (id, 显示名, 音效 raw 名) */
    data class Hotspot(val id: String, val label: String, val sfx: String)

    fun hotspots(scene: String): List<Hotspot> = when (scene) {
        LIVINGROOM -> listOf(
            Hotspot("nap", "沙发小憩", "sfx_04_cushion"),
            Hotspot("tv", "看电视", "sfx_05_remote"),
        )
        KITCHEN -> listOf(
            Hotspot("feed", "冰箱喂食", "sfx_02_fridge"),
            Hotspot("feast", "灶台大餐", "sfx_03_spatula"),
        )
        BEDROOM -> listOf(
            Hotspot("sleep", "上床睡觉", "sfx_08_bedpat"),
            Hotspot("bookshelf", "书架讲故事", "sfx_06_page"),
        )
        STUDY -> listOf(
            Hotspot("study", "书桌学习", "sfx_06_page"),
            Hotspot("chime", "窗前风铃", "sfx_07_chime"),
        )
        PARK -> listOf(
            Hotspot("play", "秋千玩耍", "sfx_09_creak"),
            Hotspot("flowerpick", "花坛采花", "sfx_10_pluck"),
        )
        ROAD -> listOf(
            Hotspot("snackbuy", "小店买零食", "sfx_07_chime"),
        )
        HILL -> listOf(
            Hotspot("stargaze", "山丘观星", "sfx_11_starlight"),
        )
        else -> emptyList()
    }

    // ---------- 换装优先级 (B3/D2, 自上而下命中即停) ----------
    const val COSTUME_DAILY = "daily"   // 日常套 6 表情
    const val COSTUME_PJ = "pj"         // 睡衣套 4 表情
    const val COSTUME_STAR = "star"     // 星光裙 4 表情

    /** 1. 四幕(Lv76+)且夜间 → 星光裙; 2. 夜间且卧室 → 睡衣套; 3. 其余 → 日常套 */
    fun costumeFor(act: Int, phase: String, scene: String): String = when {
        act >= 4 && phase == DayPhase.NIGHT -> COSTUME_STAR
        phase == DayPhase.NIGHT && scene == BEDROOM -> COSTUME_PJ
        else -> COSTUME_DAILY
    }

    /** 表情 fallback 表 (B2 + v2 补行): fallback 直接切帧, 不交叉淡化 */
    fun moodFallback(costume: String, mood: String): String = when (costume) {
        COSTUME_PJ -> when (mood) {
            "angry" -> "sad"
            "surprised" -> "neutral"
            else -> mood
        }
        COSTUME_STAR -> when (mood) {
            "angry" -> "sadtears"
            "shy" -> "neutral"
            else -> mood
        }
        else -> mood
    }

    /** 立绘资源 (静态 when, 主保险; 14 张 = 3 套 × 表情) */
    fun portraitResFor(costume: String, mood: String): Int = when (costume) {
        COSTUME_DAILY -> when (mood) {
            "happy" -> R.drawable.pet_daily_happy
            "sad" -> R.drawable.pet_daily_sad
            "angry" -> R.drawable.pet_daily_angry
            "shy" -> R.drawable.pet_daily_shy
            "surprised" -> R.drawable.pet_daily_surprised
            else -> R.drawable.pet_daily_neutral
        }
        COSTUME_PJ -> when (mood) {
            "happy" -> R.drawable.pet_pj_happy
            "shy" -> R.drawable.pet_pj_shy
            "sad" -> R.drawable.pet_pj_sad
            else -> R.drawable.pet_pj_neutral
        }
        COSTUME_STAR -> when (mood) {
            "happy" -> R.drawable.pet_star_happy
            "surprised" -> R.drawable.pet_star_surprised
            "sadtears" -> R.drawable.pet_star_sadtears
            "sad" -> R.drawable.pet_star_sadtears
            else -> R.drawable.pet_star_neutral
        }
        else -> R.drawable.pet_daily_neutral
    }

    /** 表情条 (开始菜单主页): 当前套装 6 格 (缺失表情 = fallback 目标), 缩略图资源 */
    fun expressionStrip(costume: String): List<Pair<String, Int>> {
        val moods = when (costume) {
            COSTUME_PJ -> listOf("neutral", "happy", "shy", "sad")
            COSTUME_STAR -> listOf("neutral", "happy", "surprised", "sadtears")
            else -> listOf("neutral", "happy", "sad", "angry", "shy", "surprised")
        }
        return moods.map { it to portraitResFor(costume, it) }
    }
}
