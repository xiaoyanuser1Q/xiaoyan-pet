package com.xiaoyan.pet.game

/**
 * 全剧情文案 — 《四季的小屋》(设定圣经 v3.1 §1/§4 + 对话三补全)。
 * 台词带 mood (对齐 14 立绘表情, 换装 fallback 由 Scenes.moodFallback 处理)。
 * 定稿 6 句 (圣经 §4 D5 + v3) 原样保留: ①檐下 ②秋千 ③信 ④结局B ⑤⑥结局C 两句 ⑦离线回归。
 */
object StoryData {

    data class Line(val text: String, val mood: String)

    data class Beat(val id: String, val title: String, val lines: List<Line>)

    // ================= 序幕 · 春夜雨 (Lv1) =================

    val PROLOGUE_BEFORE = listOf(
        Line("春雨顺着屋檐落下来。你刚搬进小镇的小屋，檐下缩着一小团影子——", "neutral"),
        Line("你点亮了灯。那是一个女孩，怀里抱着一枝枯萎的桃花枝。", "neutral"),
        Line("她仰头看你。「……不记得名字，也不记得从哪里来。只记得，有个要许的约。」", "sad"),
        Line("「不管接下来我去哪……这个约，你愿意陪我许完吗？」", "shy"),
    )

    /** 起名 + 问约之后 (onboarding 结束, 进游戏) */
    fun prologueAfter(name: String, promise: String): List<Line> = listOf(
        Line("「${name}」——她把这个名字在舌尖上滚了一遍。「很好听。我会记一辈子的。」", "happy"),
        if (promise.isNotBlank())
            Line("「原来你想许的是——『$promise』。不记得了……但我知道，它一定会实现。」", "neutral")
        else
            Line("「约……不记得了。但我知道，它一定会实现。」", "neutral"),
    )

    // ================= 季节节点 / 记忆 / 过场 (B4, 全落本幕, 避开抉择点) =================

    data class ChoiceOpt(val id: String, val label: String, val result: Line, val effect: String?)
    data class Choice(val key: String, val level: Int, val prompt: Line, val options: List<ChoiceOpt>)

    val CHOICES: List<Choice> = listOf(
        Choice("20", 20,
            Line("春天快要从门前走了。「要不要，再和我一起出去一次？」她望着窗外问。", "happy"),
            listOf(
                ChoiceOpt("o20_cur", "去公园，看最后一场花",
                    Line("花瓣落了一肩。她说：「花会谢，但我知道——明年它们还会开的。」（好奇 +3）", "surprised"),
                ChoiceOpt("o20_warm", "不出门了，就在窗边取暖",
                    Line("你们挤在一只热水袋两边。她说：「外面冷，但这里比外面暖。」（温度 +3）", "shy"),
            )),
        Choice("45", 45,
            Line("夏夜的市集上，最后一串糖葫芦只剩一颗。「给你，还是给我？」她歪着头笑。", "happy"),
            listOf(
                ChoiceOpt("o45_cur", "买一张写着小镇的船票",
                    Line("她把船票收进贴身的口袋。「总有一天，我要去看看那个小镇。」（好奇 +3）", "surprised"),
                ChoiceOpt("o45_warm", "分掉最后一颗糖",
                    Line("一分为二，一人一半。「甜的。」她说。然后她看着你，突然红了脸。（温度 +3）", "shy"),
            )),
        Choice("70", 70,
            Line("信的最后，有一行被雨水洇开的字。她举着信，等你的话。", "neutral"),
            listOf(
                ChoiceOpt("o70_cur", "一起查那张旧地图",
                    Line("你在旧地图上圈出了那座山。「找到了。」她轻轻说。声音在抖。（好奇 +3）", "surprised"),
                ChoiceOpt("o70_warm", "先别走了，把约守住",
                    Line("她把信贴在心口。「那就再等一等。等我心里有底了，再说。」（温度 +3）", "shy"),
            )),
        Choice("95", 95,
            Line("冬天的最后一夜。她捧着桃花枝，很轻地问：「我现在，可以听到答案了吗？」", "neutral"),
            listOf(
                ChoiceOpt("return", "让她回小镇",
                    Line("她接住那枝桃花。眼睛亮了一下，又暗了一下。「……好。谢谢。」", "sad"),
                ChoiceOpt("keep", "一起守住约定",
                    Line("她把手放进你的手里。掌心是温的。「那这个约，就永远算数。」（承诺 +5 · 选后记账）", "happy"),
                ChoiceOpt("truth", "告诉她真相",
                    Line("她安静了很久。「……我知道了。」她抬起头，眼里有星光。", "surprised"),
            )),
    )

    fun choiceByKey(key: String): Choice = CHOICES.first { it.key == key }
    fun pendingChoiceLevel(level: Int, choices: Map<String, String>): String? {
        for (c in CHOICES) if (level >= c.level && choices[c.key].isNullOrBlank()) return c.key
        return null
    }

    // ================= 级别事件表 (过场/节点/记忆) =================

    /** 跨幕光柱过场 (600ms + BGM 交叉淡化 + 服装自动切换) */
    val ACT_CROSS: Map<Int, Beat> = mapOf(
        25 to Beat("cross25", "季节过渡 · 春 → 夏", listOf(
            Line("一束光柱穿过夜空。等它落定，窗外的花已经变成了蝉鸣。", "surprised"),
            Line("「小屋在随季节长大呢，」她说，「我也是。」", "happy"),
        )),
        50 to Beat("cross50", "季节过渡 · 夏 → 秋", listOf(
            Line("光柱扫过蝉鸣，落下时，空气里是叶子的声音。", "neutral"),
            Line("她把毯子往你那边挪了挪。「秋天了。夜里会有点凉。」", "shy"),
        )),
        75 to Beat("cross75", "季节过渡 · 秋 → 冬", listOf(
            Line("光柱把落叶变成了第一场雪。", "surprised"),
            Line("「冬天的夜会变长，」她说，「我有点期待。」", "happy"),
        )),
    )

    /** 季节节点事件 (一幕 8/13/18/23 · 二幕 28/34/40/48 · 三幕 53/58/64/73 · 四幕 78/83/88/93) */
    val SEASON_NODES: Map<Int, Beat> = mapOf(
        8 to Beat("n8", "初春", listOf(
            Line("公园的樱花刚开。她仰着头数花瓣：「一片，两片……数不清了。」", "happy"),
        )),
        13 to Beat("n13", "落花", listOf(
            Line("她把一片落花夹进书里。「我想把春天收下来，收很久。」", "shy"),
        )),
        18 to Beat("n18", "檐下", listOf(
            Line("下雨天，她把伞倾向你那边。「檐下都一样，」她说，「我们都在屋顶下面。」", "happy"),
        )),
        23 to Beat("n23", "春尽", listOf(
            Line("春天的风最后一次吹过小屋。她坐在沙发上，说：「我最近，开始做梦了。」", "neutral"),
        )),
        28 to Beat("n28", "夏夜", listOf(
            Line("「风很暖。不过最暖的，是你坐在我旁边。」", "happy"),
        )),
        34 to Beat("n34", "雷阵雨", listOf(
            Line("暴雨。她贴着窗，指着远处的闪电：「你看，闪电的时候，小镇很清楚。」", "surprised"),
        )),
        40 to Beat("n40", "树荫", listOf(
            Line("你们在树荫里玩了一下午的水。她笑到街尾还在笑。", "happy"),
        )),
        48 to Beat("n48", "长夏", listOf(
            Line("夏天的最后一个晚上，路灯有点暗。她握着你的手：「别怕。我现在记得的，比以前多了。」", "neutral"),
        )),
        53 to Beat("n53", "山丘开门", listOf(
            Line("黄昏的山丘。她怀里那枝枯桃花，今天，居然开了一点。", "surprised"),
        )),
        58 to Beat("n58", "旧箱", listOf(
            Line("她从一个旧箱子里找到一封信。字迹很淡，只有一行很深。", "neutral"),
        )),
        64 to Beat("n64", "信", listOf(
            Line("「信上写着另一个小镇……可那里没有人在等我。」", "sad"),
        )),
        73 to Beat("n73", "早星", listOf(
            Line("秋夜星出得很早。「以后这样的夜，我们都上山丘吧，」她说，「我想和你一起看。」", "shy"),
        )),
        78 to Beat("n78", "初雪", listOf(
            Line("初雪。她用指尖描着屋顶的轮廓：「小屋戴帽子了。」", "happy"),
        )),
        83 to Beat("n83", "星光裙", listOf(
            Line("她第一次把那条星光裙穿出来，站在你面前，像一小片夜空。", "shy"),
        )),
        88 to Beat("n88", "冬夜", listOf(
            Line("冬天最安静的夜里，她忽然问：「你说，那个约，真的会实现吗？」", "neutral"),
        )),
        93 to Beat("n93", "桃花枝", listOf(
            Line("她把枯桃花枝放在窗台。「它记得一切。我也记得。」", "neutral"),
        )),
    )

    /** 记忆碎片节点 (Lv10/35/60/80) */
    val MEMORY: Map<Int, Beat> = mapOf(
        10 to Beat("memory1", "记忆① · 有个小镇", listOf(
            Line("夜里她望着窗外，忽然说：「有个小镇。一定有。有街道，有挂风铃的铺子。」", "surprised"),
            Line("「可它在哪……我把它弄丢了。」", "sad"),
        )),
        35 to Beat("memory2", "记忆② · 有人在等我", listOf(
            Line("她梦话：「有人在等我。」", "neutral"),
            Line("风铃铺子……柜台后面，有人在数硬币。", "surprised"),
        )),
        60 to Beat("memory3", "记忆③ · 我的名字有人起的", listOf(
            Line("「我的名字，是有人起的。」她说这句话的时候，眼睛很亮。", "happy"),
        )),
        80 to Beat("memory4", "记忆④ · 那个约，是这个小屋", listOf(
            Line("星光山丘的夜里，她回过头：「我想起来了。那个约……是这个小屋。」", "surprised"),
        )),
    )

    /** 到达某一级时自动挂起的事件 (过场优先, 其次记忆, 再次节点) */
    fun beatsFor(level: Int): List<Beat> = buildList {
        ACT_CROSS[level]?.let { add(it) }
        MEMORY[level]?.let { add(it) }
        SEASON_NODES[level]?.let { add(it) }
    }

    // ================= 16 随机事件 (v3.1 分配表: 每幕 4 条, 隐藏线每线 2 条 × +4) =================

    data class REvent(val id: String, val act: Int, val hidden: String?, val title: String, val text: String, val mood: String)

    /** hidden: "promise" / "warmth" / "curiosity" = +4 (各限 1 次); null = 表面/中性 (冬季纯氛围) */
    val EVENTS: List<REvent> = listOf(
        // 一幕 = 承诺 + 温度 + 表面 ×2
        REvent("E01", 1, "promise", "桃花枝小盒", "她用一封信做了个小盒子，把枯桃花枝放进去。「约，是要装进盒子里的。」", "shy"),
        REvent("E02", 1, "warmth", "暖手", "她捧着两只热水袋贴在脸侧。「暖，」她说，「我现在，懂这个词了。」", "happy"),
        REvent("E03", 1, null, "窗台的小猫", "一只小猫来到檐下。她和小猫对视了十分钟，然后分享了一条鱼干。", "happy"),
        REvent("E04", 1, null, "春雨笔记本", "下雨天她写小本子。你偷看——全是屋檐和小屋的画。", "neutral"),
        // 二幕 = 承诺 + 好奇 + 表面 ×2
        REvent("E05", 2, "promise", "夏天的信", "她寄出一封信，收件地址是空白的。「有的信，」她说，「只需要被寄出去。」", "neutral"),
        REvent("E06", 2, "curiosity", "小巷的风铃", "小镇的巷子里，你们找到了一块写着风铃的旧招牌。她站在它前面，站了很久。", "surprised"),
        REvent("E07", 2, null, "夏夜的星星", "你们搬了小凳子到街上数星星。她说，星星像很多很小的灯。", "happy"),
        REvent("E08", 2, null, "冰水", "第一瓶冰水。她尝了一小口：「凉的。但是，是夏天的味道。」", "happy"),
        // 三幕 = 温度 + 好奇 + 表面 ×2
        REvent("E09", 3, "warmth", "围巾", "她织了一条围巾给你。「我看过你很多次的毛衣，」她说，「这一条，是你的。」", "shy"),
        REvent("E10", 3, "curiosity", "旧地图", "小店老板借给你们一张旧地图。某座山上有个小圈——她用手指，描了一遍又一遍。", "surprised"),
        REvent("E11", 3, null, "山丘初雪", "山丘落了初雪。她接住一片：「看，它是约的形状。」", "neutral"),
        REvent("E12", 3, null, "秋风信", "她在树下读落叶的字。「每片叶子，」她说，「都有要去的方向。」", "neutral"),
        // 四幕 = 表面/中性 ×4 (冬季留给结局, 事件纯氛围)
        REvent("E13", 4, null, "雪水茶", "你们用雪水泡茶。她说：「甜。今年第一次这么说。」", "happy"),
        REvent("E14", 4, null, "灯笼", "小镇的灯笼会。她为你提了一盏，为小屋留了一盏。", "shy"),
        REvent("E15", 4, null, "雪人", "山丘上堆了个雪人。她给它取名叫「小屋的邻居」。", "happy"),
        REvent("E16", 4, null, "岁末", "年末最后一夜，她写了一张清单。「明年，我们一起做更多的事。」", "neutral"),
    )

    fun eventsOfAct(act: Int): List<REvent> = EVENTS.filter { it.act == act }

    /** 本幕隐藏线事件 (跨幕补触发用, R4: 过场前逐条插入, 最多 2 条) */
    fun hiddenEventsOf(act: Int): List<REvent> = eventsOfAct(act).filter { it.hidden != null }

    // ================= 12 互动台词 =================

    val INTERACTIONS: Map<String, List<Line>> = mapOf(
        "feed" to listOf(Line("冰箱里是什么，你最清楚。嗯——好吃的。", "happy")),
        "feast" to listOf(Line("锅铲声、油香……这是我最喜欢的声音。", "happy")),
        "nap" to listOf(Line("沙发比梦还软。我先闭一会儿眼，帮你看着。", "neutral")),
        "tv" to listOf(Line("今天播什么？我没看过。但是你喜欢，我就一直看。", "surprised")),
        "study" to listOf(Line("我找到一段从没读过的地方。以后这是我们的「秘密角」，读完念给你听。", "happy")),
        "bookshelf" to listOf(Line("……给你讲一个。关于一个小屋，和一盏一直有人替它亮着的灯。", "neutral")),
        "sleep" to listOf(Line("被褥拍得很蓬松。晚安……我也会等你到早上。", "neutral")),
        "wake" to listOf(Line("……早上了。我刚做了个梦，梦里有这个屋。你发现了没？", "happy")),
        "play" to listOf(Line("再高一点——我看得见整个小镇！……也看得见小屋，和你。", "surprised")),
        "flowerpick" to listOf(Line("这朵花，是「咔」的一声开的。我把它夹进书里，它就能留很久。", "happy")),
        "snackbuy" to listOf(Line("叮铃，风铃响了。老板朝你挥挥手，又偷偷多给了一块。", "happy")),
        "stargaze" to listOf(Line("星星好近，好像伸手就能够到。……你说，约，是不是也长这样？", "neutral")),
    )

    // ================= 系统提示 =================

    val WARM_RETURN = Line("你回来了？我刚才在窗边等你。", "happy")           // v3 定稿 (圣经 #6)
    val WARN_SATIE = Line("有点饿了……但我不念叨。没关系。", "sad")       // S13
    val STAMINA0 = Line("她睡得很沉，精神回满了。等下再来找我。", "neutral")
    val SLEEP_CD = Line("还没睡够呢——她还要睡一会儿。", "neutral")
    val BOOKSHELF_LIMIT = Line("今天讲了三个啦。还有很多没讲完，明天好不好？", "shy")
    val TV_LIMIT = Line("让眼睛休息一下吧。世界还有很多。", "neutral")
    val STARGAZE_BLOCKED = Line("天还没黑透。星星，要夜里才出来。", "neutral")
    val LEVELUP = Line("她的声音，比刚才亮了一点。（Lv.）", "surprised")   // 升级 S12, 接级数
    val EVENT_XP_TIP = ""

    // ================= 三结局 (Lv100 判定结果, 文案 + 定稿句) =================

    fun endingTitle(ending: String): String = when (ending) {
        "A" -> "结局 · 春风归途"
        "B" -> "结局 · 小镇永远"
        else -> "结局 · 永恒的约定"
    }

    fun endingLines(ending: String, promiseAnswer: String): List<Line> = when (ending) {
        "A" -> listOf(
            Line("她背好了小书包，怀里抱着那枝桃花。", "neutral"),
            Line("「谢谢你，陪我许了一年。从今天起，我自己许。」", "sad"),
            Line("第二年春天，山丘上的那株桃树，忽然开了。", "neutral"),
            Line("「春风归途」——她带着记忆去了信里的小镇。留下一枝桃花。", "sad"),
        )
        "B" -> listOf(
            Line("她把信撕成雪花。", "neutral"),
            Line("「我决定忘掉那个小镇。因为小屋的灯，总有人为我留着。」", "happy"),
            Line("周年夜，你们重新许了一个约。这一次，你先开口：「我约着，把小屋的灯，为你亮很久，很久。」", "shy"),
            Line("「小镇永远」——她抹去小镇的记忆留了下来。", "happy"),
        )
        else -> listOf(
            Line("烟花升起来的那一瞬，她转过身，穿着星光裙，像一小片夜空。", "surprised"),
            if (promiseAnswer.isNotBlank())
                Line("「你当年许的那个约——『$promiseAnswer』。我不是从那个小镇来的。我是那个约。」", "surprised")
            else
                Line("「我不是从那个小镇来的。我是那个约。」", "surprised"),
            Line("「那约定是真的吧？以后我看着这个小屋。很久，很久。」", "happy"),
            Line("「永恒的约定」——烟花散进冬天。小屋的灯，亮着。", "happy"),
        )
    }
}
