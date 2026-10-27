# 交接：顶栏/底栏液态玻璃（Vitro 方案）

> 一句话现状：**顶栏 / 底栏已接入 Vitro 液态玻璃；交互回归已定位并修复——
> 两条栏原先放在 Material3 `Scaffold` 里，Scaffold 的全屏 `Surface` 自带 pointerInput 节点，
> 在命中测试里盖住了 backdrop 内的页面内容。改为普通 `Box` 直接摆放两条栏后，滚动、课程点击、
> 普通按钮、横向翻周、周面板与主题切换均已真机验证通过；玻璃材质和滚动折射效果保持原样。**
> 改动全部在分支 `Lingfan666/feature-liquid-glass` 上，**没有合入 main**。
>
> 材质细节、参数标定依据、更新上游的步骤在 [VITRO_LIQUID_GLASS.md](VITRO_LIQUID_GLASS.md)；
> 本文只讲「交接要做的事」。

## 一、分支与推送关系

```
main (404d2ee, == origin/main)
 └─ Lingfan666/feature-liquid-glass   ← 本分支，只做液态玻璃这一件事
       ← 从 main 拉出，未合入
```

| 项 | 值 |
|---|---|
| 当前分支 | `Lingfan666/feature-liquid-glass`（基线 commit `404d2ee`） |
| 推送目标 | `origin` = `github.com/AsukaTamo/WEAKDOWN`，分支 `Lingfan666/feature-liquid-glass` |
| 镜像远端 | `gitee` = `gitee.com/lingfan555/weakdown`（同一条分支如需同步，自己再推一次） |
| **没做**的事 | 没推 `main`、没建 PR、没动远端其它分支 |
| 合入方式 | 由你在 GitHub 上开 PR / 本地 merge，AI 不代做 |

拉下来之后：

```powershell
git fetch origin
git switch Lingfan666/feature-liquid-glass
```

## 二、改了哪些东西

| 文件 | 改动 | 为什么 |
|---|---|---|
| `app/src/main/java/io/github/itshedimisawi/vitro/**` | 新增（34 个 `.kt` + `LICENSE-vitro.txt`） | vendor 上游 Vitro 0.1.0，顶栏/底栏的玻璃材质 |
| `app/src/main/java/com/example/courseapp/MainActivity.kt` | 背景、壁纸和当前可见课表统一进入 `glassBackdrop`；顶栏/底栏改 `.glassEffect(...)`；页面内容不再被栏的整体避让区域隔开；**顶栏/底栏从 `Scaffold` 移出，改为根 `Box` 的直接子节点**；页面视口下移到顶栏外壳上沿；删掉原磨砂渐变/彩虹描边/`.shadow()`；次级文字色换 `textSecondary`；`LocalGlassDarkTheme` 跟随 App 主题 | 玻璃要采样真实滚动内容，且亮玻璃上 muted 灰只有 2.6:1；栏不能再用带全屏 `Surface` 的 `Scaffold`，否则页面收不到触摸 |
| `app/src/main/java/com/example/courseapp/ui/schedule/ScheduleScreen.kt` | 增加 `contentPadding` 参数；把顶/底安全区留白放入统一纵向滚动内容；时间轴和七天课程列共享滚动范围 | 课程首尾可完整露出，中间课程可以经过顶栏/底栏并被 Vitro 实时折射 |
| `ui/components/LiquidGlass.kt` | **删除** | 自研着色器的真折射一直没打通，改走 Vitro；历史在 `404d2ee` 里可找回 |
| `app/build.gradle.kts` | `compileSdk 36 → 37` | 与 Vitro 的编译基线一致（`targetSdk` 仍 36） |
| `gradle.properties` | 新增 `android.suppressUnsupportedCompileSdk=36,37` | AGP 8.2.2 官方只测到 34，仅压提示 |
| `docs/VITRO_LIQUID_GLASS.md` | 新增 | 方案、参数标定、验证、更新上游步骤 |
| `docs/LIQUID_GLASS_TODO.md` | 顶部加状态说明，文件清单更新 | 旧的自研路线归档为历史记录 |
| `docs/PROJECT_STRUCTURE.md` | 补 vendor 小节、compileSdk 更新 | 结构说明别过期 |

页面表头避让和滚动布局继续保留：`ScheduleGrid` 使用一个纵向滚动容器，并把顶/底栏避让
作为滚动内容的 spacer；`MainActivity` 的页面留白取栏位实测尺寸与系统 inset/尺寸令牌
兜底值中的较大值。栏不再使用 `Scaffold`：两条栏现在是根 `Box` 的直接子节点，页面视口
整体下移到顶栏外壳上沿（状态栏 + 顶栏 8dp 外边距），初始布局位置与修复前一致；滚动内容
可以经过顶栏/底栏玻璃，但不会画到状态栏图标底下。**本轮已在真机复验交互，见第四节。**

刻意**没有**提交：`.idea/deviceManager.xml`（你本机的 IDE 状态）。

## 三、换机器要复现的 4 件事

1. **SDK**：需要 `platforms/android-37`（来源 `platform-37.0_r02.zip`），`android-36` 保留即可。
   装好后**必须**把 `platforms/android-37/source.properties` 里的
   `AndroidVersion.ApiLevel=37.0` 改成 `37`，否则 AGP 8.2.2 报
   `Failed to find Platform SDK with path: platforms;android-37`（本机原文件备份为 `source.properties.bak`）。
2. **Gradle**：本机 `gradlew.bat` 的 wrapper jar 是坏的，用缓存 8.7 分发包（见 `AGENT-TROUBLESHOOT.md`）。
3. **不要指望上游自带的构建**：Vitro 仓库自己用 Gradle 9.6 + AGP 9.4.1 + Kotlin 2.4.21 + Compose 1.12.1，
   本机从 `services.gradle.org` 下载 Gradle 分发包一直卡在 0 字节，所以是「vendor 源码 + 本项目 Compose 1.7.5」，
   只差一处 `UnplacedAwareModifierNode` 兼容改动（见 VITRO 文档第四节）。
4. **真机闭环**：`AGENT.md` 里的 adb 路径与命令；本机设备 `10CG5J0Q0F004E7`。

## 四、验证记录（本轮实际跑过的）

| 命令 / 验收项 | 结果 |
|---|---|
| `gradle :app:assembleDebug --offline`（本轮修复后） | ✅ 通过；使用本机缓存 Gradle 8.7 |
| `gradle :app:testDebugUnitTest --offline`（本轮修复后） | ✅ 通过 |
| 装机 + 冷启动 + 截图 | ✅ 页面可启动；初始布局与修复前一致，日期表头仍在顶栏下方 |
| 首页课表纵向拖动 | ✅ 内容真实位移，时间轴和七天课程列同步滚动 |
| 管理页 / 我的页列表滚动 | ✅ 均恢复 |
| 页面普通按钮 | ✅ 课程点击弹出详情、返回关闭、底部 Tab 切换、「作息时间设置」打开/返回、周数胶囊打开选择面板 |
| 横向翻周 | ✅ 第 9 周滑到第 10 周，出现「回本周」，课程分布随周变化 |
| 课程穿过玻璃的动态折射 | ✅ 滚动时课程色块和文字从顶栏/底栏玻璃后方经过，玻璃内采样随内容移动 |
| 首尾课程可达 | ✅ 滚到底可见第 12 节；底部留白把最后一行托在底栏之上 |
| 浅 / 深主题 | ✅ 两套材质外观正常，顶栏主题按钮切换后玻璃材质与文字色同步 |
| 本轮帧耗时（仅纵向滚动） | p50 11ms / p90 16ms / p99 19ms，janky 2.81%，missed vsync 0，GPU p90 7ms |
| 本轮帧耗时（滚动 + 横向翻周混合） | p50 12ms / p90 18ms / p99 32ms，janky 6.70%，missed vsync 1，GPU p90 7ms |
| `logcat -b crash` | ✅ 无本应用 FATAL / ANR |

旧记录中的 p50 8ms / p90 17ms / p99 23ms、janky 4.77%、missed vsync 0 是首次 Vitro 接线的历史基线；
本轮纵向滚动的 p90 已回到 16ms、janky 2.81%，翻周动画仍是混合测量里更高的那一档。

### 已修复：页面纵向手势和普通控件无响应

- **现象**：首页课表及后续页面无法上下滑动，除顶栏/底栏按钮外其余按钮无响应。
- **根因**：两条栏当时放在 Material3 `Scaffold` 里，而页面内容（`glassBackdrop` 子树）是 `Scaffold` 的兄弟节点、
  画在它下面。Material3 1.3.1 的 `Scaffold` 内部是一个全屏 `Surface`，`Surface` 会额外挂一个
  `Modifier.pointerInput(Unit) {}` 节点（用于阻止点击穿透）。Compose 命中测试里，这个全屏
  pointerInput 节点是最上层的直接命中，命中结果不再落到下面的兄弟页面，于是页面收不到任何
  down/move/up，滚动和 clickable 全部失效；栏按钮因为在这个 `Surface` 内部，反而正常。
  这不是 Vitro shader 或手势冲突问题。
- **修复**：去掉 `Scaffold`，把顶栏/底栏改成根 `Box` 的直接子节点，用 `Alignment.TopCenter` /
  `Alignment.BottomCenter` 保持原位置；玻璃仍然画在 `glassBackdrop` 之后、页面内容之外。
  页面视口整体下移 `statusBar + 顶栏 8dp 外边距`（顶部留白相应扣除），初始位置不变，滚动内容
  仍可从顶栏玻璃后方经过，但不会再画到状态栏图标底下和系统时钟重叠。
- **证据**：`app/build/outputs/apk/debug/app-debug.apk` 装机后按上表逐项验收；修复前后的滚动截图、
  课程点击详情、横向翻周、我的页列表滚动均已核对。修复仅改 `MainActivity.kt` 的栏外层结构、
  页面容器和顶部留白，没有改 Vitro 材质参数、`glassBackdrop` 采样关系或 `ScheduleScreen` 的滚动模型。

材质与原型图的数值对齐（真机截图像素）：

| | 本实现 | 原型图 |
|---|---|---|
| 浅色玻璃体 / 高光 | 204 / 231 | 207 / 228 |
| 深色顶栏 / 底栏 | 60 / 33 | 57 / 25 |

## 五、已知差距与下一步（按性价比排序）

1. **彩色边（原型的美术化「色彩溢出」）**：Vitro 的 `dispersion` 是物理 RGB 分离，背景越平越看不出彩色；
   要那种沿边一圈的彩虹，得在玻璃外再叠一层极淡的 `sweepGradient` 描边（~0.6dp）。
2. **其它面板没换材质**：周选择面板、作息设置、课程详情抽屉还是实色面板，可以复用同一个 `backdrop`。
3. **顶栏 + 周数胶囊融合**：用 `glassEffectContainer` + `spacing = 0.dp` 能做成一块玻璃。
4. **动画档性能**：纵向滚动 p90 16ms / janky 2.81% 已经达标；包含横向翻周的混合测量 p90 18ms / janky 6.70%，
   若要继续压，优先看翻周动画期间的重组/绘制，而不是玻璃本身（GPU p90 仅 7ms）。
5. **要用 Vitro 1.12+ 的新 API**：得把 Compose 升上去，并撤回第四节的兼容改动。

## 六、回滚

```powershell
git switch main            # 回到未接玻璃的状态
# 或者只回滚这一个提交
git revert <commit>
```

`ui/components/LiquidGlass.kt` 已删除，需要旧实现时从 `404d2ee` 取。

## 七、五分钟上手

1. 打开 `MainActivity.CourseApp()`，找到 `glassMaterial`（材质参数都在这一处）；
2. 顶栏/底栏是根 `Box` 的直接子节点（`Alignment.TopCenter` / `Alignment.BottomCenter`），**不要**再把它们放回
   Material3 `Scaffold`：Scaffold 的全屏 `Surface` 会挡住 backdrop 里页面的命中测试；
3. 需要调手感就改 `brightness` / `tint` / `dispersion` / `blurRadius`，字段含义见 `Glass.kt` 的 KDoc；
4. 构建 → 装机 → 冷启动 → 截图（命令见 `AGENT.md`「真机调试方法」）；
5. 对着原型图看三件事：玻璃体的亮度、边缘高光是否成圈、背景是否被真正折射（拖动课表时看栏下方的课程色块、文字和壁纸有没有随内容移动并产生边缘折射）。
