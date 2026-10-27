# 🌸 小桃养成记 (xiaoyan-pet)

一款养成 + 视觉小说向 Android APP · Kotlin + Jetpack Compose

## 下载

- 🔗 官网（主）：https://xiaoyan-pet.xiaoyan-pan.eu.cc
- 📱 APK（v1.3.0）：https://xiaoyan-pet.xiaoyan-pan.eu.cc/download/xiaoyan-pet-v1.3.0-release.apk
- 📱 APK（Gitee 国内源）：https://gitee.com/xiaoyanuser/xiaoyan-pet/releases
- 📋 版本检查：https://xiaoyan-pet.xiaoyan-pan.eu.cc/version.json
- 🗂 源码：
  - 极狐 GitLab（主编译）：https://jihulab.com/xiaoyan_user1q/xiaoyan-pet
  - Gitee（主备份）：https://gitee.com/xiaoyanuser/xiaoyan-pet
  - GitHub（第二备份）：https://github.com/xiaoyanuser1Q/xiaoyan-pet

## 当前版本 v1.3.0 · 《四季的小屋》

一年 = 四幕 = 四季。小桃是「一个没有过去的女孩」，在四季陪伴里找回记忆碎片，
最终面对「她究竟是谁 + 她想去哪里」的抉择。情感内核：陪伴、承诺、家。

- **4 幕 100 级新剧情**：序幕（春夜雨 · 起名 + 问约）+ 四幕四季（Lv25/50/75 光柱过场）
  + 三结局（A 春风归途 / B 小镇永远 / C 永恒的约定·烟花，隐藏线三线 ≥80 解锁）
- **新养成体系**：表面 4（饱食/心情/体力/亲密，体力三档上限跟亲密）+ 隐藏 3 线（承诺/温度/好奇，
  C 路径有效上限 104/103/110，三线 ≥80 判定在 Lv95 加分前且不叠加 Lv95 分）；
  每日 XP 硬上限 45（抉择/事件豁免）；书架 3 次/日、电视 2 次/日；到顶不加线不置灰；
  12 场景互动 + 16 随机事件（跨幕强制补触发 ≤2 条/幕）
- **7 场景 × 4 光照 = 28 张真图背景**（早/午/下午/晚，4 态日时系统；睡觉→夜间、起床→早晨；
  设置强制白天/夜间兜底）+ **3 套 14 张立绘**（换装优先级：四幕夜→星光裙 / 夜+卧室→睡衣 / 其余→日常；
  缺失表情 fallback 直接切帧）
- **开始菜单主页**：主图大立绘（呼吸 + 星光粒子）+ 表情条 + 入口（开始/继续/设置/检查更新）
- **T0 网络修复**：检查更新多源回退 [Gitee, eu.cc, pages.dev]（成功源写 SharedPreferences 下次优先）
  + APP 内流式下载（进度 + 断点续传 + SHA256 校验）+ FileProvider 安装 + INSTALL_PACKAGES 授权引导
- **本地通知 3 事件**：新版本 / 小桃升级 / 跨幕（双渠道；Android 13+ POST_NOTIFICATIONS 授权流）
- **音频**：四季 BGM + 终曲（结局 C 烟花）+ 15 条场景 SFX（opus，合计 758KB）
- **旧档政策**：v1.3.0 全新故事，save_version=3；本地/云端旧档保留但不加载（设置页可见提示）
- **离线回归**：>4h 先温情台词（粒子特效），饱食 warn 延后 30s 每回归周期 1 次
- **低配模式 + 动画风格三档（温柔/活泼/关闭）**；全 APP 锁横屏 sensorLandscape + 沉浸栏
- **资源安全**：静态 `when` 资源映射（主）+ `res/raw/keep.xml` 全量 tools:keep（兜底）
- **网络**：Supabase 登录/云存档走自建 `pet-gw` Worker（路径白名单 + IP 限速 + 共享密钥头），失败不阻断玩法

完全免费 · 无广告 · 无内购。

## 开发

- 编译：极狐 GitLab 流水线（JDK17 + AGP 8.5.2 + Gradle 8.7，构建前后双验证书 SHA-256 e47e48d3）
- 签名：release keystore（v1.1/v1.2/v1.3 同款，永不重新生成）
- 分发：CF Pages（主域 xiaoyan-pet.xiaoyan-pan.eu.cc + 备 xiaoyan-pet-site.pages.dev）+ Gitee Releases（国内源）
- 素材许可与生成参数：见 [CREDITS](./CREDITS.md)
- 隐私政策：https://xiaoyan-pet.xiaoyan-pan.eu.cc/privacy.html（v1.3.0 新增 REQUEST_INSTALL_PACKAGES / POST_NOTIFICATIONS 说明）
