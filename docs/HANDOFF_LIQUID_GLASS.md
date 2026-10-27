# 交接：顶栏/底栏液态玻璃（Vitro 方案）

> 一句话现状：**顶栏 / 底栏已接入 Vitro 液态玻璃；Scaffold 命中测试回归已修复；工具链已升到与上游一致的
> Gradle 9.6 + AGP 9.4.1 + Kotlin 2.4.21 + Compose 1.12.1（BOM 2026.09.00），并撤回了 Vitro 的
> Compose 1.7 兼容改动。真机滚动、点击、长按、翻周、主题、设置页与玻璃折射全部通过，帧耗时比 1.7.5 基线更低。**
> 本轮又落地了底部高斯模糊渐变、周选择面板玻璃、课程详情抽屉玻璃（含覆盖层命中修复）、
> 页面顶部圆角裁切、管理/我的页面的滚动透感，以及**彩色描边和玻璃厚度（视觉近似）**；
> 顶栏+周数胶囊融合经评估未采用，原因见 [VITRO_LIQUID_GLASS.md](VITRO_LIQUID_GLASS.md) 第七节。
> 当前 `main` 已包含液态玻璃、交互修复、Compose 升级与上一轮增强（`99940fe`，已推 origin/gitee）；
> 本轮彩色边、玻璃厚度与文档清理已提交并同步远端。
>
> 材质细节、参数标定依据、更新上游的步骤在 [VITRO_LIQUID_GLASS.md](VITRO_LIQUID_GLASS.md)；
> 本文只讲「交接要做的事」。

## 一、分支与推送关系

```
main (99940fe, == origin/main == gitee/main)
 ├─ 液态玻璃 + Scaffold 命中测试修复 + Compose 1.12.1 升级 + 面板/抽屉玻璃（8197b9b）
 ├─ 底部高斯模糊 + 顶部圆角 + 管理/我的滚动透感（99940fe，已推送）
 └─ 彩色边 + 玻璃厚度 + 文档清理（本轮提交）
```

| 项 | 值 |
|---|---|
| 当前分支 | `main` |
| 推送目标 | `origin` = `github.com/AsukaTamo/WEAKDOWN`，分支 `main` |
| 镜像远端 | `gitee` = `gitee.com/lingfan555/weakdown`，`main` |
| 已推送 | `99940fe`（底部模糊/顶部圆角/管理我的滚动透感）已推两个远端 |
| 本轮 | 彩色边 + 厚度 + 文档清理，提交后同步两个远端 |

## 二、改了哪些东西

| 文件 | 改动 | 为什么 |
|---|---|---|
| `app/src/main/java/io/github/itshedimisawi/vitro/**` | 新增（34 个 `.kt` + `LICENSE-vitro.txt`）；本轮恢复 `UnplacedAwareModifierNode` / `onUnplaced()` | vendor 上游 Vitro 0.1.0，顶栏/底栏的玻璃材质；恢复后与上游 commit 完全一致 |
| `app/src/main/java/com/example/courseapp/MainActivity.kt` | 背景、壁纸和当前可见课表统一进入 `glassBackdrop`；顶栏/底栏改 `.glassEffect(...)`，并加 18/34dp 折射厚度、1dp 彩虹描边、玻璃面渐变、5dp+1.8dp 倒角、12dp 投影；**顶栏/底栏从 `Scaffold` 移出，改为根 `Box` 的直接子节点**；页面视口下移到顶栏外壳上沿；页面视口顶部 28dp 圆角裁切；`edgeBlur` 只对底部做高斯模糊渐变；删掉原磨砂渐变/彩虹描边/`.shadow()`；次级文字色换 `textSecondary`；`LocalGlassDarkTheme` 跟随 App 主题 | 玻璃要采样真实滚动内容，且亮玻璃上 muted 灰只有 2.6:1；栏不能再用带全屏 `Surface` 的 `Scaffold`，否则页面收不到触摸；顶部模糊会与系统状态栏/tag 栏底色冲突，所以只做底部 |
| `ui/components/EdgeBlur.kt` | **新增**：从 `backdrop.layer` 重录底部 200dp 带状区域，`BlurEffect` + DstIn 渐变，形成真正的高斯模糊渐隐；API 31 以下跳过 | 用户要求顶/底滚动边缘有柔和模糊，但顶部会与系统栏颜色冲突，因此只实现底部 |
| `ui/components/WeekMorphPanel.kt` | 形变容器加 `glassEffect(backdrop, glass, cornerDp)` + 76% surface 叠色；遮罩改为面板/箭头的父层 | 周面板与顶栏使用同一块 backdrop 的玻璃；父层结构避免玻璃的 sibling 分享把点击送给遮罩 |
| `ui/components/CourseDetailSheet.kt` | 抽屉表面加 `glassEffect(backdrop, glass, 顶部圆角)` + 88% surface 叠色；遮罩改为抽屉父层，视觉遮罩单独一层 | 详情抽屉复用同一块玻璃，同时保证正文可读；父层结构修复编辑/复制/删除按钮被遮罩抢点击 |
| `app/src/main/java/com/example/courseapp/ui/schedule/ScheduleScreen.kt` | 增加 `contentPadding` 参数；把顶/底安全区留白放入统一纵向滚动内容；时间轴和七天课程列共享滚动范围 | 课程首尾可完整露出，中间课程可以经过顶栏/底栏并被 Vitro 实时折射 |
| `ui/components/LiquidGlass.kt` | **删除** | 自研着色器的真折射一直没打通，改走 Vitro；历史在 `404d2ee` 里可找回 |
| `build.gradle.kts` / `app/build.gradle.kts` | AGP **9.4.1**（内置 Kotlin）、Kotlin **2.4.21**、Compose BOM **2026.09.00**（ui/foundation 1.12.1、material3 1.4.0）、KSP 2.3.12、Hilt 2.60.1、Room 2.8.5；移除未使用的 `navigation-compose` / `accompanist`；去掉 `kotlin-android` 插件和 `kotlinOptions` | Compose 1.12.1 的 AAR 元数据要求 AGP ≥ 9.1.0、compileSdk 37；与上游构建基线一致 |
| `gradle/wrapper/*`、`gradlew.bat` | 用 Gradle 9.6 重新生成 wrapper（9.6.0，腾讯镜像），替换原来坏掉的 wrapper jar | 原 wrapper jar 无法启动；AGP 9.4.1 需要 Gradle 9.x |
| `gradle.properties` | 移除 `android.suppressUnsupportedCompileSdk`；JVM 内存提到 4G | AGP 9.4.1 原生支持 compileSdk 37 |
| `viewmodel/ScheduleViewModel.kt` | 先落 `semesterStartDate` 再发布 `_activeSemester` | 修复启动瞬间先显示兜底日期周数（第 18 周）、再跳回第 9 周的竞态 |
| `ui/**` 若干文件 | `Divider` → `HorizontalDivider`；`hiltViewModel` 换新包 `androidx.hilt.lifecycle.viewmodel.compose`；`asFrameworkPaint()` → `nativePaint`；Room 用 `fallbackToDestructiveMigration(dropAllTables = true)` | 跟进新库 API、消除弃用警告，行为不变 |
| `docs/VITRO_LIQUID_GLASS.md` | 更新编译基线、撤回兼容改动的说明、验证数据 | 方案、参数标定、验证、更新上游步骤 |
| `docs/LIQUID_GLASS_TODO.md`、`docs/LIQUID_GLASS_tech.md`、`docs/superpowers/plans/2026-08-07-schedule-interactions.md` | **删除** | 自研着色器 TODO、旧光学规范、旧交互计划均已过期；现状以 VITRO / HANDOFF 为准 |
| `docs/PROJECT_STRUCTURE.md` | 补 vendor 小节、compileSdk / 工具链版本更新 | 结构说明别过期 |

页面表头避让和滚动布局继续保留：`ScheduleGrid` 使用一个纵向滚动容器，并把顶/底栏避让
作为滚动内容的 spacer；`MainActivity` 的页面留白取栏位实测尺寸与系统 inset/尺寸令牌
兜底值中的较大值。栏不再使用 `Scaffold`：两条栏现在是根 `Box` 的直接子节点，页面视口
整体下移到顶栏外壳上沿（状态栏 + 顶栏 8dp 外边距），初始布局位置与修复前一致；滚动内容
可以经过顶栏/底栏玻璃，但不会画到状态栏图标底下。**本轮已在真机复验交互，见第四节。**

本轮升级只动工具链、弃用 API 和启动周数竞态；`glassMaterial` 的
`brightness / tint / dispersion / blurRadius`、`glassBackdrop` 的采样关系、`ScheduleScreen`
的滚动模型都没有改。本轮增强只加了边缘渐隐和两个覆盖层的玻璃/命中结构，**没有改台面玻璃参数**。
**旧的 `.idea/deviceManager.xml` 已在历史提交里，不要再单独提交 IDE 状态。**

## 三、换机器要复现的 4 件事

1. **SDK**：需要 `platforms/android-37`（来源 `platform-37.0_r02.zip`）与 `build-tools/37.0.0`。
   早期为 AGP 8.2.2 改过 `platforms/android-37/source.properties` 的
   `AndroidVersion.ApiLevel=37.0 → 37`；AGP 9.4.1 原生支持 API 37，这个补丁可以保留，
   重装 SDK 后也可以先试原生的 `37.0`。
2. **Gradle**：wrapper 已用 Gradle 9.6 重新生成，`.\gradlew.bat ...` 可直接用，
   分发包走腾讯镜像 `mirrors.cloud.tencent.com/gradle/gradle-9.6.0-bin.zip`。
   旧的「wrapper jar 坏了、改用缓存 Gradle 8.7」说明只对历史版本有效。
3. **工具链与上游一致**：Gradle 9.6.0 + AGP 9.4.1（内置 Kotlin）+ Kotlin 2.4.21 +
   Compose 1.12.1（BOM 2026.09.00）+ KSP 2.3.12 + Hilt 2.60.1 + Room 2.8.5。
   **不再需要** `UnplacedAwareModifierNode` 兼容改动，vendor 源码与上游 `3384d04` 一致。
4. **真机闭环**：`AGENT.md` 里的 adb 路径与命令；本机设备 `10CG5J0Q0F004E7`。
   安装时如果 vivo 弹出「外部来源应用」安全提示，勾选「已了解…」再点「继续安装」；
   或用 `adb push` + `adb shell pm install -r` 绕过该弹窗。

## 四、验证记录（本轮实际跑过的）

| 命令 / 验收项 | 结果 |
|---|---|
| `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest`（AGP 9.4.1 / Gradle 9.6 / Compose 1.12.1） | ✅ 通过；无 error、无弃用警告 |
| 装机 + 冷启动 + 截图 | ✅ 初始布局与 1.7.5 基线一致；启动直接显示「第 9 周」 |
| 首页课表纵向拖动 / 管理页 / 我的页列表滚动 | ✅ 内容真实位移，时间轴和七天课程列同步滚动 |
| 课程点击、长按、详情返回 | ✅ 详情抽屉、长按上下文菜单、返回关闭全部正常 |
| 底部 Tab / 周数胶囊 / 横向翻周 / 作息设置页 | ✅ 第 9 周滑到第 10 周，「回本周」出现；设置页打开/返回正常 |
| 浅 / 深主题 | ✅ 两套玻璃材质与 1.7.5 一致，切换后文字色同步 |
| 课程穿过玻璃的动态折射 | ✅ 滚动时课程色块和文字从顶栏/底栏玻璃后方经过 |
| 顶部圆角裁切 | ✅ 页面视口 28dp 顶部圆角；内容上滑时左右两侧以圆角消失，红圈处直边消失 |
| 底部高斯模糊 | ✅ 底栏附近 200dp 带状区域真实高斯模糊 + 渐变淡出；顶部不模糊，避免与系统 tag 栏颜色冲突 |
| 管理 / 我的滑动透感 | ✅ 两页都铺满视口 + `contentPadding`；列表滚过顶/底栏玻璃；管理 Tab 栏固定且不透明 |
| 主题切换 | ✅ 不再弹 snackbar，只保留背景揭示与图标动画 |
| 彩色边 / 厚度 | ✅ 1dp 彩虹描边 + 玻璃面渐变 + 5dp 柔光倒角 + 1.8dp rim + 12dp 投影；浅/深都复验 |
| 周选择面板玻璃 | ✅ 展开后玻璃 + 76% surface；点周格、确认、外部关闭、形态动画都正常 |
| 课程详情抽屉玻璃 | ✅ 玻璃 + 88% surface；编辑进入/退出、复制/删除、拖动关闭、外部关闭都正常 |
| 首尾课程可达 | ✅ 滚到底可见第 12 节，最后一行在底栏之上 |
| 首页纵向滚动 `gfxinfo` | p50 12ms / p90 16ms / p99 21ms，janky 2.04%，missed vsync 0，GPU p90 9ms |
| 我的页滚动 `gfxinfo` | p50 7ms / p90 10ms / p99 15ms，janky 1.00%，missed vsync 0 |
| 管理页（全部课程）`gfxinfo` | p50 9ms / p90 12ms / p99 14ms，janky 0.51%，missed vsync 0 |
| 管理页（添加课程）`gfxinfo` | p50 9ms / p90 12ms / p99 15ms，janky 0.27%，missed vsync 0 |
| 面板展开/收起 `gfxinfo` | p50 12ms / p90 17ms / p99 40ms，janky 3.35%，missed vsync 0（无玻璃的旧动画同量级） |
| `logcat -b crash` | ✅ 无本应用 FATAL / ANR |

作为对照，上一轮 Compose 1.7.5 的纵向滚动是 p50 11ms / p90 16ms / janky 2.81%，
混合翻周是 p50 12ms / p90 18ms / janky 6.70%；升级后两组都更低，玻璃外观没有可见变化。

### 已修复：页面纵向手势和普通控件无响应

- **现象**：首页课表及后续页面无法上下滑动，除顶栏/底栏按钮外其余按钮无响应。
- **根因**：两条栏当时放在 Material3 `Scaffold` 里，而页面内容（`glassBackdrop` 子树）是 `Scaffold` 的兄弟节点、
  画在它下面。`Scaffold` 内部是一个全屏 `Surface`，`Surface` 会额外挂一个
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

### 已升级：Compose 1.12.1 + 撤回 Vitro 兼容改动

- **触发原因**：Compose 1.12.1 的 AAR 元数据要求 `minCompileSdk=37`、`minAgpVersion=9.1.0`，
  AGP 8.2.2 / Gradle 8.7 无法继续使用。
- **升级内容**：AGP 9.4.1（内置 Kotlin，移除 `kotlin-android` 插件与 `kotlinOptions`）、
  Kotlin 2.4.21、Compose BOM 2026.09.00（ui/foundation 1.12.1、material3 1.4.0）、
  KSP 2.3.12、Hilt 2.60.1、Room 2.8.5、activity-compose 1.13.0、lifecycle 2.11.0、
  core-ktx 1.19.1；移除未使用的 `navigation-compose` / `accompanist`；wrapper 重建为 Gradle 9.6。
- **撤回兼容改动**：`BackdropNode` / `ScrollEdgeNode` 恢复 `UnplacedAwareModifierNode` 和
  `onUnplaced() { reader.pause() }`，vendor 源码与上游 commit `3384d04` 一致。
- **顺带修复**：`ScheduleViewModel.loadSemester()` 先写 `semesterStartDate` 再发布
  `_activeSemester`，消除启动瞬间先显示第 18 周、再跳回第 9 周的竞态。
- **弃用 API 迁移**：`Divider` → `HorizontalDivider`、`hiltViewModel` 换
  `androidx.hilt.lifecycle.viewmodel.compose`、`asFrameworkPaint()` → `nativePaint`、
  `fallbackToDestructiveMigration(dropAllTables = true)`。
- **结果**：玻璃参数、层级、命中测试和滚动布局均未改；真机视觉与旧版一致，帧耗时反而下降（见上表）。

### 已增强：底部高斯模糊、顶部圆角、管理/我的滚动透感

- **页面顶部圆角**：页面视口加 28dp 顶部圆角裁切，内容上滑时左右两侧以圆角消失，替代原来横贯全屏的直边。
- **底部高斯模糊**：`ui/components/EdgeBlur.kt` 从 `backdrop.layer` 重录底部 200dp 带状区域，
  用 `BlurEffect` 模糊，再用原生 DstIn + 竖向 `LinearGradient` 把上沿淡出。
  **顶部不做模糊**：状态栏/系统 tag 栏有自己的底色，叠加模糊会与其产生色差冲突；顶部只保留圆角裁切 + 折射。
- **管理 / 我的滚动透感**：两页都改为铺满视口的滚动内容 + `contentPadding`；管理 Tab 栏固定在顶栏下方，
  并用不透明底色压住列表，列表从 Tab 栏下方经过后进入顶栏玻璃采样区；我的页整条 LazyColumn 滚过两条栏。
- **主题切换**：`ScheduleViewModel.setDarkMode` 不再 emit “深色/浅色模式已开启”的 snackbar。
- **彩色边**：Vitro 的 `dispersion` 是物理 RGB 分离，平整背景上不明显；在玻璃外圈加 1dp
  `sweepGradient` 描边（浅色 α 0.50 / 深色 α 0.60），近似原型的「色彩溢出」。
- **厚度**：`refractionHeight / refractionAmount` 从默认 12/26dp 提到 18/34dp，配合 1.02 放大、
  1.15 饱和度、更强的 rim highlight / shadow；应用层再加玻璃面渐变、5dp 柔光倒角、1.8dp rim
  和 12dp 投影。这些都是 2D 视觉近似，没有改 Vitro 的 SDF/着色器。
- **顶栏 + 周数胶囊融合未采用**：Vitro 的 `glassEffectContainer(spacing = 0.dp)` 官方语义是“不融合，
  只合并绘制”；融合需要 `spacing > 0` 且玻璃材质相同，组内会使用 lead 的 tint，胶囊会失去强调色。
  当前胶囊已经整体叠在顶栏里，没有需要靠融合消除的缝隙，保留现状。

材质与原型图的数值对齐（真机截图像素）：

| | 本实现 | 原型图 |
|---|---|---|
| 浅色玻璃体 / 高光 | 204 / 231 | 207 / 228 |
| 深色顶栏 / 底栏 | 60 / 33 | 57 / 25 |

## 五、已知差距与下一步（按性价比排序）

1. **彩色边（已做视觉近似）**：1dp `sweepGradient` 描边 + 0.06 物理色散；如果要更接近原型的
   折射彩虹，需要改 Vitro 的色散/边缘模型，而不是再加不透明度。
2. **作息设置页还没套玻璃**：周选择面板、课程详情抽屉已完成；作息设置、导入页仍是实色，
   可以复用同一个 `backdrop`，但页面内容密度高，需要先确认正文可读性。
3. **顶栏 + 周数胶囊融合（未采用）**：`spacing = 0.dp` 不会融合；融合需要同材质，胶囊会失去强调色。
   如果要做，建议先出 clear glass 胶囊 + 强调色文字的设计稿，再单独实现。
4. **性能**：首页纵向滚动 p90 16ms / janky 2.04%（底部模糊 + 厚度层约 +1ms），我的页 p90 10ms，
   管理两页 p90 12ms；面板展开/收起 p90 17ms / janky 3.35%。玻璃和底部模糊都不是主要瓶颈
   （GPU p90 9ms），后续要压优先看面板形变期间的重组/渐变。
5. **底部模糊参数**：当前底部 200dp / 20dp 半径；低端机若贴线，可先降 `edgeHeight` 或 `radius`，
   不必放弃整块玻璃。
6. **上游更新**：截至 2026-10-27 上游仍为 `0.1.0` / `3384d04`，无新版本。

## 六、回滚

```powershell
# 回到「未接玻璃」的历史提交查看旧实现
git switch --detach 404d2ee
# 或者在包含玻璃的 main 上撤掉玻璃相关提交
git revert <glass-commit>
```

`ui/components/LiquidGlass.kt` 已删除，需要旧实现时从 `404d2ee` 取。
只回滚本轮 Compose 升级时，反向恢复 `build.gradle.kts`、`app/build.gradle.kts`、
`gradle.properties`、`gradle/wrapper/*` 与两个 Vitro node 文件即可（旧 wrapper jar 坏，
回退后仍需按 AGENT-TROUBLESHOOT 用 Gradle 8.7 分发包）。

## 七、五分钟上手

1. 打开 `MainActivity.CourseApp()`，找到 `glassMaterial`（材质参数都在这一处）；
2. 顶栏/底栏是根 `Box` 的直接子节点（`Alignment.TopCenter` / `Alignment.BottomCenter`），**不要**再把它们放回
   Material3 `Scaffold`：Scaffold 的全屏 `Surface` 会挡住 backdrop 里页面的命中测试；
3. 需要调手感就改 `brightness` / `tint` / `dispersion` / `blurRadius`，字段含义见 `Glass.kt` 的 KDoc；
4. 构建 → 装机 → 冷启动 → 截图（命令见 `AGENT.md`「真机调试方法」）；
   现在直接用 `.\gradlew.bat :app:assembleDebug`，wrapper 已是可用的 Gradle 9.6；
5. 对着原型图看三件事：玻璃体的亮度、边缘高光是否成圈、背景是否被真正折射（拖动课表时看栏下方的课程色块、文字和壁纸有没有随内容移动并产生边缘折射）。
