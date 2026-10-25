# CREDITS · xiaoyan-pet

## 立绘（6 张表情立绘）

本 APP 全部人物立绘由 AI 生成（Agnes 画室，模型 `agnes-image-2.5-flash`，经自建 Cloudflare Worker `agnes-studio`），**未取自任何网络素材包**。

| 表情 | 资源文件 | 来源 |
|---|---|---|
| 中性 | `res/drawable-nodpi/pet_neutral.webp` | AI 生成（第 1 批 2026-10-24）· 主人手动抠图 |
| 难过 | `res/drawable-nodpi/pet_sad.webp` | AI 生成（第 1 批 2026-10-24）· 主人手动抠图 |
| 惊讶 | `res/drawable-nodpi/pet_surprised.webp` | AI 生成（第 1 批 2026-10-24）· 主人手动抠图 |
| 生气 | `res/drawable-nodpi/pet_angry.webp` | AI 生成（第 1 批 2026-10-24）· 主人手动抠图 |
| 开心 | `res/drawable-nodpi/pet_happy.webp` | AI 生成（第 2 批 2026-10-24 重 roll）· 小桃绿幕抠图 |
| 害羞 | `res/drawable-nodpi/pet_shy.webp` | AI 生成（第 2 批 2026-10-24 重 roll）· 小桃绿幕抠图 |

**角色设计模板**：12 岁少女、粉色齐下巴短发 + 嫩芽发卡、奶白色连衣裙 + 粉色针织开衫、后颈淡星辉纹路、动漫 VN 全身站姿，纯绿底 832×1248。
**转码**：WebP q90，768×1152，`res/drawable-nodpi/`。

> 注：开心 / 害羞为重 roll 批次（首批开心误出双人图、害羞缺失），与另 4 张（同模板不同 roll）面部细节有轻微差异。验收如觉跳脸，整组 6 张同 roll 重出一次即可。

## 背景图（5 张）

全部程序化生成（Python 脚本，无网络素材）：

- `res/drawable-nodpi/bg_login.png` · `bg_act1.png` · `bg_act2.png` · `bg_act3.png` · `bg_ending.png`

## 音频（6 个）

全部程序化合成（无第三方采样）：

- `res/raw/bgm_main.wav` · 24 秒八音盒 BGM
- `res/raw/sfx_feed.wav` · `sfx_play.wav` · `sfx_tap.wav` · `sfx_levelup.wav` · `sfx_warn.wav`

## 协议与隐私

- 开源协议：MIT，见 [LICENSE](./LICENSE)
- 隐私政策：https://xiaoyan-pet.xiaoyan-pan.eu.cc/privacy.html
