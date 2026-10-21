package com.xiaoyan.pet.game

/** 一条剧情事件 (标题 + 正文) */
data class StoryEvent(val title: String, val text: String)

object Story {

    // ---------- 开场 ----------
    val GREET = "夜里，一点微光从天上落下来，落在你的掌心——一团粉色的小团子。它睁开眼睛，仰头看你，轻轻打了个响：「啾~」"
    val NAME_HINT = "给它起个名字"
    val THING_HINT = "你最想找回的东西（也可以以后再告诉它）"

    // ---------- 剧情节点 ----------
    private val ACT2_ANOMALY = StoryEvent(
        "第二幕 · 异变",
        "小桃的后颈悄悄浮现出淡色的星辉纹路，像被新画上的星座连线，暖暖的。你发现周围的世界也在悄悄变化：夜似乎变短了一点，星星似乎近了一点。它望着天上的光，叶子轻轻发颤。"
    )
    private val LIGHT_61 = StoryEvent(
        "第三幕 · 抉择 · 光柱苏醒",
        "远处，一道细细的光柱缓缓升起，穿过夜空。光很温和，却带着一点不安，像有什么要来寻找它。小桃仰头看着你，眼睛比之前亮了许多。从这里开始，接下来的事，由你们一起决定。"
    )
    private val LIGHT_75 = StoryEvent(
        "光柱靠近",
        "光柱离得更近了，影子落在窗边。你听见里面有很轻很轻的声响，像呼吸，又像一首很老很老的歌。小桃没有说话，它后颈的星纹亮了一些。"
    )
    private val LIGHT_90 = StoryEvent(
        "光柱笼罩",
        "整间屋子都被光柱笼罩，空气变得有些稠。小桃最后一次仰头看你：无论接下来发生什么，你们一起走过的路，已经被它记住了。"
    )

    /** 到达某一级时触发的剧情事件 */
    fun eventsFor(level: Int): List<StoryEvent> = when (level) {
        31 -> listOf(ACT2_ANOMALY)
        61 -> listOf(LIGHT_61)
        75 -> listOf(LIGHT_75)
        90 -> listOf(LIGHT_90)
        else -> emptyList()
    }

    // ---------- 结局 ----------
    fun endingTitle(ending: String): String = when (ending) {
        "A" -> "结局 · 归途"
        "B" -> "结局 · 永恒"
        else -> "结局 · 消散"
    }

    fun endingText(ending: String, name: String, firstWord: String): String = when (ending) {
        "A" -> "$name 缓缓升起，身体化作星光，流进夜空。离开之前，它最后一次打了响：「啾~」。你忽然明白，那件你一直想找回的东西，其实从未丢失——它一直都在你身上。星光会替你把回家的路照亮。"
        "B" -> if (firstWord.isBlank()) {
            "光柱散作细尘。你眼前，粉色的团子缓缓长高，站定成一个带着星辉纹路的女孩。她开口，第一次发出的不是「啾~」，而是一句你永远不会忘记的话："
        } else {
            "她看着你，说：「$firstWord」\n从此，你们都不再孤单。"
        }
        else -> "没有光柱，也没有告别。你某夜醒来，掌心空空。它安静地散进了星光里，只在掌心留下了一点温度。陪伴是要慢慢长出来的。如果下一次，你能再多陪它一会儿，它一定会记得。"
    }
}
