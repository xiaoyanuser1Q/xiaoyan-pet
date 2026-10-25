# 🌸 小桃养成记 (xiaoyan-pet)

一款养成 + 视觉小说向 Android APP · Kotlin + Jetpack Compose

## 下载

- 🔗 官网（主）：https://xiaoyan-pet.xiaoyan-pan.eu.cc
- 📱 APK：https://xiaoyan-pet.xiaoyan-pan.eu.cc/download/xiaoyan-pet-v1.2.0-release.apk
- 📋 版本检查：https://xiaoyan-pet.xiaoyan-pan.eu.cc/version.json
- 🗂 源码：
  - 极狐 GitLab（主编译）：https://jihulab.com/xiaoyan_user1q/xiaoyan-pet
  - Gitee（主备份）：https://gitee.com/xiaoyanuser/xiaoyan-pet
  - GitHub（第二备份）：https://github.com/xiaoyanuser1Q/xiaoyan-pet

## 当前版本 v1.2.0

横屏视觉小说式养成 · 立绘动态呼吸 · 6 表情切换 · 轻分支剧情

- **横屏沉浸**：左立绘 + 右对话框，全屏沉浸，顶部状态栏自动隐藏
- **立绘动画**：呼吸 + 微摆 + 眨眼，情绪随台词 mood 自动切换（6 表情）
- **三幕剧情 + 轻分支**：每幕 1 个二选一抉择（注入信任 / 探索 / 理解）+ 随机小事件池
- **三种结局**：归途 · 永恒 · 消散（结局 B 玩家亲填第一句话）
- **账号与云存档**：Supabase 登录（纯 HTTP，无 SDK）+ 本地 Room 存档 + 云同步
- **存档兼容**：v1.1.0 老存档无缝升级，不丢档
- **低配模式**：设置页开关，关闭微摆与 Ken Burns，保留呼吸
- **资源安全**：静态 `when(mood)` 引用追踪 + `res/raw/keep.xml` 兜底，防 R8 误删立绘
- **网络代理**：Supabase 接口走自建 `pet-gw` Worker（路径白名单 + IP 限速 + 共享密钥头）

完全免费 · 无广告 · 无内购。

## 构建

CI 编译在极狐 GitLab 免费共享 Runner（Ubuntu + JDK 17 + Android cmdline-tools + Gradle 8.7）：
`.gitlab-ci.yml` 的 `build-release` 阶段产出 release 签名 APK，`apksigner verify --print-certs` 校验。

签名沿用固定 `release.keystore`（证书 SHA-256 前缀 `e47e48d3…`，2026-10-18 生成，证书 DN：CN=Xiaoyan Pet）。

## 素材来源

全部立绘 AI 生成，背景与音频程序化生成，详见 [CREDITS.md](./CREDITS.md)。

## 协议与隐私

- 开源协议：MIT，见 [LICENSE](./LICENSE)
- 隐私政策：https://xiaoyan-pet.xiaoyan-pan.eu.cc/privacy.html

---

Made with 🌸 by 宵檐 & 小桃 (AI 管家)
