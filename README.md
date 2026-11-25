# 省中医汤谱 · Android APK（公网自动更新版）

装落手机之后：

- 📴 **冇网络**：19 张汤谱卡内嵌喺 APK／HTML 入面，飞机模式都查到、搜到、收藏到
- 🌐 **一有网络**（WiFi 或流动数据都得）：自动去公网攞 `soups.json` 静默更新
- 🔄 **改汤谱唔使重装 App**：改 repo 入面嘅 `soups.json`、改个 `version`，App 下次开就同步，仲会 cache 落本机，之后断网都系最新
- 🙅 **唔使填网址、唔使 NAS、唔使屋企电脑长开**：打包嗰阵 GitHub Actions 自动将你个 repo 网址写入 APK
- 🍎 **同一份 HTML 可以畀 iPhone 用**（下面第五节）

> 呢度交嘅系**完整工程＋云端自动打包**，唔系现成 `.apk`。呢边冇 Android 编译工具链，签好名嘅安装包由 GitHub Actions 喺云端生成（下面三步，唔使装 Android Studio）。

---

## 一、三步出 APK（零设定）

1. 开个 **GitHub repo**（要 **Public**，因为更新源要从公网读；Private 免费账号读唔到），将呢个资料夹全部 push 上去，主分支叫 `main`。
2. 去 **Actions** → 等「自动打包 APK」跑完（第一次约 3–6 分钟），亦可手动撳 **Run workflow**。
3. 落 **Artifacts** 下载 `省中医汤谱-APK`，入面有三样：
   - `app-debug.apk` → 送落手机安装（开「允许未知来源」）
   - `soups.json` → 留喺 repo 就得，**唔使摆去第二度**
   - `省中医汤谱_联网更新版.html` → 畀 iPhone／电脑用

**打包嗰阵 workflow 会自动做一件事**：将 `index.html` 入面嘅 `__OWNER__` / `__REPO__` 换成你个 GitHub 账号同 repo 名，变成三条更新源：

```
https://raw.githubusercontent.com/<你>/<repo>/main/soups.json   ← 先试，即时
https://cdn.jsdelivr.net/gh/<你>/<repo>@main/soups.json          ← 国内通常较稳
https://<你>.github.io/<repo>/soups.json                         ← 有开 Pages 就多个后备
```

App 开机逐个试，**第一个成功就停**；三条都唔通就静默用内置 19 张卡，唔会弹错误扰民。所以你**连「更新」页都唔使入**，装完就识自动更新。

> 出嘅系 debug APK，自动 debug keystore 签名，自己侧载完全够用，唔使上架、唔使申请开发者账号。

---

## 二、以后想加／改汤谱

**净系改 repo 入面嘅 `soups.json`，唔使重新出 APK、唔使重装**：

```json
{
  "version": "2026-12-01.1",
  "updated": "2026-12-01",
  "note": "加咗两款新汤",
  "soups": [ ... ]
}
```

- `version` **一定要改**（App 靠佢判断系咪有更新，唔改就唔会覆盖）
- `soups` 每项格式同而家一样：`id / cat / period / tags / name / ing / steps / note / caution / src / url`
- 新增用**新 `id`**（20、21…），旧 `id` 唔好改，已收藏 ☆ 先唔会乱
- commit + push，App 下次开就自动同步

> workflow 设咗 `paths-ignore: soups.json`，净系改汤谱内容**唔会**触发无谓嘅重新打包。只有改 `index.html`／界面／功能先要重新 Build。

### 📌 收录口径（v2026-11-25.2 起）

**净收广东省中医院微信公众号「每日一膳」正栏嘅煲汤／炖汤**。健康广东、南方日报／南方+、南都 N 视频、今日头条、腾讯新闻一律**唔再另开新卡** —— 「每日一膳」本身就系省中医 ×《南方日报》联合出品、几边同步推送，健康广东亦多数转载同一稿，照收会一张方变三张卡。

- **认方唔认篇**：同一「汤名＋核心材料组」只留一张；其他媒体登过净记入 `alsoIn` 做互证
- 克数有出入以**公众号原文为准**，差异写喺点评，唔另开卡
- 糖水／粥／羹／焖菜／旧季度文章一律唔收
- **现有 19 张卡一张唔删、`id` 唔变**（☆ 收藏唔会乱），只补来源标记；#11 / #12 系同一方两版，标咗 `dupGroup` 对照用

`soups.json` 顶层已经有 `scope` 字段写死呢条规则，第时自动追更照佢执就得。

**想用自己个站／国内镜像**：App 底部「↻ 更新」填一个 https 网址就覆盖自动源，撳「清走，用返自动源」可以还原。

---

## 三、国内网络两句实话

GitHub raw 喺国内有时唔稳，所以我设咗三源自动回退，jsDelivr 多数时候比 raw 顺。如果三个都慢，可以：

- 将 `soups.json` 摆去 **Gitee Pages / 腾讯云 COS / 阿里云 OSS / 自己个 https 站**，然后喺 App「更新」页填一次网址（要 https，且最好开 `Access-Control-Allow-Origin: *`，iPhone 版需要）
- 或者照用自动源，连唔到就继续用内置版，**唔会坏、唔会卡住**（总 watchdog 9 秒）

---

## 四、App 本身有啲咩

- 搜汤名／材料／功效／时节（打「莲藕」「祛湿」「菜干」即时筛）
- 分类：每日一膳正栏 / 同期专家推荐 / 我嘅汤谱 / ☆ 已收藏；另有祛湿、润燥、健脾、淮山快速标签
- 点开卡片睇材料、做法、点评、禁忌、原文链接（用系统浏览器开）
- **份量一键换算**：原方多系 3 人份，按「4 人 / 2L」「5 人 / 2.5L」自动放大克数
- ☆ 收藏、自己新增汤谱、深浅色 —— 全部存本机，唔会上传
- 更新状态、自订网址、上次同步内容都 cache 落本机

数据口径：2026-09-25 ～ 11-25，**净以省中医公众号「每日一膳」正栏为准**（其他媒体唔另开卡），19 张卡 = 18 款汤（「沙参玉竹淮山百合煲瘦肉」有公众号版同健康广东版，拆两张对照）；正栏卡 15 张、同期专家推荐卡 4 张。糖水、粥、羹、焖菜同龙舟水旧文都剔走咗。

---

## 五、🍎 iPhone 可以吗？可以，但有三个条件

**同一份 `省中医汤谱_联网更新版.html` 就系 iPhone 版**，自动更新逻辑一模一样：有网络 → `fetch('./soups.json')` → 再试注入嘅三条 GitHub 源 → 成功就 cache 落 localStorage → 断网用内置 19 张卡。

但要留意同 Android APK 唔同嘅地方：

| | Android APK | iPhone 用 HTML |
|---|---|---|
| 攞更新 | **原生 HTTP 桥** | 浏览器 `fetch()` |
| CORS | **唔使开** | **个源必须开 CORS** |
| 离线 | APK 内嵌，100% 稳 | 靠 Safari 缓存 + localStorage，唔保证 100% |
| 安装 | 侧载 APK | Safari → 分享 →「加入主屏幕」 |

**三个条件**：

1. **要 https**：`http://` 同 `file://` 嘅 `fetch()` 会被 iOS 拦。**唔好净系将个 .html 存落「文件」App 就算**。
2. **个源要开 CORS**：GitHub raw、jsDelivr、GitHub Pages 都自带 `Access-Control-Allow-Origin: *`，**直接用得**；自己部 NAS／`python -m http.server` 通常冇，iPhone 会失败（Android 反而得，呢个系两边最大分别）。
3. **最稳摆法**：将 `index.html` 同 `soups.json` **摆去同一个 https 目录**（GitHub Pages 最顺），Safari 打开个 HTML → 加入主屏幕。咁样：
   - 同源 `./soups.json` 直接成功，**连网址都唔使填、CORS 都唔使理**
   - 有正常 https origin，`localStorage`（收藏、缓存）先会稳定持久
   - 改汤谱同样净系改网上嗰份 JSON

**离线可靠度老实讲**：iOS「加入主屏幕」嘅网页，Safari 唔保证一定缓存到个页面；断网多数仲开到，但唔似 APK 咁 100% 稳。想要同 Android 一样真正离线，就要再加 `manifest.json` + Service Worker（呢份单文件版未加），或者用 Capacitor 出原生 iOS App——嗰个要 Mac + Xcode + 开发者账号，呢边做唔到。

> 简单讲：**iPhone 自动更新完全得，最顺系将两份文件一齐摆 GitHub Pages**；纯本机一个 .html 档就只保证「联网时更新」，离线就要碰运气。

---

## 六、其他提醒

- 网址净系填 `https://` 公网源；Manifest 仲保留咗局域网明文设定，但呢版已经唔靠 NAS。
- 收藏同自己加嘅汤谱存喺本机，**卸载 App／清除网站资料会冇**，重要配方另存一份。
- 汤谱系日常食养参考，唔替代睇医生；每张卡写低嘅禁忌（感冒发热、孕妇、湿热、痛风、服药相互作用等），煲之前对一对。

---

## 七、目录结构

```
省中医汤谱Android工程/
├── .github/workflows/build-apk.yml        ← 云端出 APK + 自动注入更新源
├── settings.gradle / build.gradle / gradle.properties
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml           ← INTERNET + 明文兼容
│       ├── java/com/yuanbao/souprecipe/MainActivity.java
│       │                                  ← WebView + 原生 HTTPS 桥 + hasNetwork()
│       ├── res/xml/network_security_config.xml
│       ├── res/values/strings.xml
│       └── assets/www/index.html         ← 成个 App 界面（同时系 iPhone 版）
├── soups.json                            ← ★ 改呢份就更新，唔使重新出 APK
└── README.md
```

零第三方依赖：净用 Android 内置 WebView / HttpURLConnection / org.json，Gradle 侧只有 Android Gradle Plugin。
