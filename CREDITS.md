# CREDITS · xiaoyan-pet（小桃养成记 v1.3.0）

项目：小桃养成记（com.xiaoyan.pet）v1.3.0 · 设定《四季的小屋》· 开发者 宵檐（xiaoyanuser1Q）
许可：全部素材为 AI 生成 / 程序生成，随 MIT 项目使用（APP 内 + 官网），无第三方授权限制。
详细生成参数（模型 / prompt / roll 文件 / 音频合成参数 / 日期）见 v1.3 对话二归档
`/workspace/xiaoyan-pet-v1.3/credits/CREDITS.md`（随仓库发布时以本文件为准）。

## 立绘（14 张 = 3 套，res/drawable-nodpi/，WebP q90 768×1152，合计 848KB）

- 模型：Agnes 画室 `agnes-image-2.5-flash`（自建 agnes-studio v13 Edge Function，owner 豁免通道），2:3 出图 832×1248 → 直接降采样 768×1152。
- 角色一致性：12 岁软系女孩 / 浅粉齐颈短发 / 左侧小绿发卡（v1.3 去掉头顶漂浮叶子/嫩芽，保留发卡本体）/ 后颈星辉 / 五官细节加强（虹膜分层 + limbal line + 眉毛睫毛）。
- 抠图：绿幕（画室实测底色 RGB≈(70,177,79)，逐图采样 key）色度抠图 + 贴边 flood + 内部绿岛规则（仅头区异色小绿岛留发卡）+ 膨胀 3px + 羽化 1.5px + 边缘 6px despill。
- 套组：日常套 6 表情（中性/开心/难过/生气/害羞/惊讶）· 睡衣套 4（中性/开心/害羞/难过）· 星光裙 4（中性/开心/惊讶/难过泪目）。
- **铁律：q90 有损，禁止改无损**（q90→无损 ≈15 倍体积，会爆包）。

| 资源名 | 来源 |
|---|---|
| pet_daily_neutral/happy/sad/angry/shy/surprised.webp | AI 生成 + 程序抠图（2026-10-26） |
| pet_pj_neutral/happy/shy/sad.webp | 同上（睡衣套 = 粉白居家服） |
| pet_star_neutral/happy/surprised/sadtears.webp | 同上（星光裙 = 深蓝星空亮片裙，结局 C 烟花专用） |

## 背景（28 张 = 7 场景 × 4 光照，res/drawable-nodpi/，WebP q90 1920×1080，合计 5429KB）

- 模型：`agnes-image-2.5-flash`（16:9 出图 1312×736 → LANCZOS 1920×1080，CREDITS 记录尺寸偏差）。
- 场景：客厅 / 厨房 / 卧室 / 书房 / 公园 / 街道 / 星空山丘；光照：早 / 午 / 下午 / 晚（全部真图，APP 内夜间滤镜方案作废）。
- 同场景一致性：每场景 1 张锚点（画室 API 无 img2img）+ `bg/relight.py` 程序化重打光（早=调色 / 午=近原 / 下午=暖调光斑 / 晚=变暗+冷色温+降饱和+暗角+暖光池+窗户星空框(室内) / 整片夜空+月亮(山丘)）。
- 10-26 校准：bedroom 窗框 (1105,145,1580,540) α.65 f48；study 窗框 (340,85,1045,672) α.8 f120。

## 音频（20 个 opus，res/raw/，合计 758KB ≤ 0.8MB 预算）

- BGM 5 首（32kbps opus，合计 694KB）：春（八音盒+拨弦，明亮）/ 夏（lo-fi 轻吉他）/ 秋（钢琴八音盒，微怅）/ 冬（低八度八音盒+弦乐垫，暖静）/ 终曲（30s 烟花小段，结局 C 专用）。numpy 程序合成（正弦+金属分音 2.71×/5.4× + exp(-4.2t) + Karplus-Strong 拨弦垫，24s 无缝循环 10 小节 @100bpm；夏修过八分音符步进 bug）。
- SFX 15 条（32kbps opus，合计 64KB，编号 S01-S15 = sfx_01_tap … sfx_15_choice，`sfx/gen_sfx_v13.py` 逐条按设定圣经 §3 听感描述合成）。

## 图标

- 1024×1024（`agnes-image-2.5-flash`，小桃脸+肩 + 暗夜星空基调，无文字）→ LANCZOS 512 源图 → res/mipmap-* 各密度（192/144/96/72/48）。

## 代码与工程

- Kotlin + Jetpack Compose（横屏 VN 布局）；编译 = 极狐 GitLab 流水线（JDK17 + AGP 8.5.2 + Gradle 8.7）；签名证书 SHA-256 = e47e48d3…（v1.1/v1.2 同款 keystore，永不重新生成）。
- R8 双保险：静态 `when` 资源映射（主）+ `res/raw/keep.xml` 全量 tools:keep（兜底）。

## 协议与隐私

- 开源协议：MIT，见 [LICENSE](./LICENSE)
- 隐私政策：https://xiaoyan-pet.xiaoyan-pan.eu.cc/privacy.html
- v1.3.0 新增权限（已在隐私政策写明）：
  - `REQUEST_INSTALL_PACKAGES` — APP 内下载安装更新包（仅安装更新包时引导系统「安装未知应用」设置页）
  - `POST_NOTIFICATIONS`（Android 13+ 运行时权限）— 本地通知（新版本 / 升级 / 跨幕；不做推送联网）

## v1.2.0 旧素材归档（已从包内移除，仅历史参考）

- 立绘 6 张（pet_neutral 等，主人手动抠图）/ 背景 5 张（程序生成 PNG）/ bgm_main.wav + SFX 5 个（WAV）——均被 v1.3.0 上表取代。
