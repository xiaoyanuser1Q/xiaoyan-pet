package com.xiaoyan.pet.game

import com.xiaoyan.pet.ui.PortraitMood

/** 一条剧情事件 (标题 + 正文) */
data class StoryEvent(val title: String, val text: String)

/** VN 台词 (带 mood, 立绘自动切对应表情; v1.2.0) */
data class StoryLine(val text: String, val mood: PortraitMood)

object Story {

    // ---------- 开场 ----------
    val GREET = "夜里，一点微光从天上落下来，落在你的掌心——一个巴掌大的、穿粉色衣服的小人儿。她睁开眼睛，仰头看你，轻轻打了个响：「啾~」"
    val NAME_HINT = "给它起个名字"
    val THING_HINT = "你最想找回的东西（也可以以后再告诉它）"

    // ---------- 剧情节点 ----------
    private val ACT2_ANOMALY = StoryEvent(
        "第二幕 · 异变",
        "小桃的后颈悄悄浮现出淡色的星辉纹路，像被新画上的星座连线，暖暖的。你发现周围的世界也在悄悄变化：夜似乎变短了一点，星星似乎近了一点。她望着天上的光，头发轻轻发颤。"
    )
    private val LIGHT_61 = StoryEvent(
        "第三幕 · 抉择 · 光柱苏醒",
        "远处，一道细细的光柱缓缓升起，穿过夜空。光很温和，却带着一点不安，像有什么要来寻找它。小桃仰头看着你，眼睛比之前亮了许多。从这里开始，接下来的事，由你们一起决定。"
    )
    private val LIGHT_75 = StoryEvent(
        "光柱靠近",
        "光柱离得更近了，影子落在窗边。你听见里面有很轻很轻的声响，像呼吸，又像一首很老很老的歌。小桃没有说话，她后颈的星纹亮了一些。"
    )
    private val LIGHT_90 = StoryEvent(
        "光柱笼罩",
        "整间屋子都被光柱笼罩，空气变得有些稠。小桃最后一次仰头看你：无论接下来发生什么，你们一起走过的路，已经被她记住了。"
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
        "A" -> "$name 缓缓升起，身体化作星光，流进夜空。离开之前，她最后一次打了响：「啾~」。你忽然明白，那件你一直想找回的东西，其实从未丢失——它一直都在你身上。星光会替你把回家的路照亮。"
        "B" -> if (firstWord.isBlank()) {
            "光柱散作细尘。你眼前，粉色的团子缓缓长高，站定成一个带着星辉纹路的女孩。她开口，第一次发出的不是「啾~」，而是一句你永远不会忘记的话："
        } else {
            "她看着你，说：「$firstWord」\n从此，你们都不再孤单。"
        }
        else -> "没有光柱，也没有告别。你某夜醒来，掌心空空。她安静地散进了星光里，只在掌心留下了一点温度。陪伴是要慢慢长出来的。如果下一次，你能再多陪她一会儿，她一定会记得。"
    }

    // ---------- 轻分支: 每幕 1 个二选一 (v1.2.0 定案) ----------
    data class ChoiceOption(val label: String, val result: StoryLine, val effect: String)

    /** effect: "trust" | "exploration" | "understanding" (+5) */
    data class Choice(val key: String, val prompt: StoryLine, val options: List<ChoiceOption>)

    fun choiceLevel(act: Int): Int = when (act) {
        3 -> 80
        2 -> 45
        else -> 15
    }

    fun choiceFor(act: Int): Choice = when (act) {
        3 -> Choice(
            "act3",
            StoryLine("光柱的光落在你们身上。小桃停住脚步，回头看你，等你说一句话。", PortraitMood.NEUTRAL),
            listOf(
                ChoiceOption("说出心里话",
                    StoryLine("你把那件一直想找的东西说了出来。她安静听完，后颈的星纹亮了一瞬。（理解 +5）", PortraitMood.HAPPY),
                    "understanding"),
                ChoiceOption("什么都不说，陪她站着",
                    StoryLine("你们谁都没有说话。光柱里很安静，只有她的呼吸和你的呼吸。（信任 +5）", PortraitMood.SHY),
                    "trust"),
            )
        )
        2 -> Choice(
            "act2",
            StoryLine("夜里的星星比昨天更近了一些。小桃仰着头看你：「要不要……一起去看看？」", PortraitMood.HAPPY),
            listOf(
                ChoiceOption("一起去看",
                    StoryLine("你们在窗边站了很久。她靠在你肩上，数到了第七颗才肯睡。（探索 +5）", PortraitMood.SHY),
                    "exploration"),
                ChoiceOption("陪她再待一会儿",
                    StoryLine("你没有说话，把毯子盖住了她露出的脚尖。她回头笑了一下。（信任 +5）", PortraitMood.HAPPY),
                    "trust"),
            )
        )
        else -> Choice(
            "act1",
            StoryLine("小团子打了个哈欠，眼睛亮亮的看着你：它好像想出去玩，又好像肚子有点空。", PortraitMood.HAPPY),
            listOf(
                ChoiceOption("先喂它一口",
                    StoryLine("它大口大口地吃着，腮帮子鼓鼓的，吃得满足极了。（信任 +5）", PortraitMood.HAPPY),
                    "trust"),
                ChoiceOption("先带它转转",
                    StoryLine("它跟着你转了一圈，叶子碰着门框，摇得咯咯响。（探索 +5）", PortraitMood.SURPRISED),
                    "exploration"),
            )
        )
    }

    // ---------- 随机小事件池 (6-8 条, 每条只触发一次) ----------
    val RANDOM_EVENTS: List<StoryEvent> = listOf(
        StoryEvent("拾到一颗小星星", "窗台上出现一颗指甲盖大的星星，亮亮地转。小桃把它捧在手心，它「叮」了一声，暖得发烫。"),
        StoryEvent("叶子风铃", "小桃头顶的嫩叶今天开成了一朵小花，风一吹就响，像一串很小的风铃。"),
        StoryEvent("数到第七", "你陪她数星星，数到第七颗就忘了要数什么，两个人都笑了。"),
        StoryEvent("影子游戏", "月光把她的影子拉得好长。她追着自己的影子跑，回头喊你看。"),
        StoryEvent("一颗糖", "口袋里不知什么时候多了一颗糖。她剥开来分了一半给你，自己抱着另一半睡着了。"),
        StoryEvent("星纹发光", "她后颈的星纹悄悄连成了一个新的图案，像一座小小的桥。"),
        StoryEvent("雨点琴声", "下雨了。她站在檐下接雨滴，滴答滴答，像一首很短的曲子。"),
        StoryEvent("梦话", "她睡着了，梦里轻轻说了一句：「别走……陪我。」声音很小，你醒着听见了。"),
    )

    fun randomEventIndex(i: Int): StoryEvent = RANDOM_EVENTS[i]
}
