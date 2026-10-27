# 液态玻璃：Vitro 接入说明

> 结论：顶栏 / 底栏的材质改为 [Vitro](https://github.com/itshedimisawi/vitro) 的 AGSL 液态玻璃，
> 源码已 vendor 进本仓库（`app/src/main/java/io/github/itshedimisawi/vitro/`）。
> 上一版自研着色器 `ui/components/LiquidGlass.kt` 已删除，取舍过程见 [LIQUID_GLASS_TODO.md](LIQUID_GLASS_TODO.md)。

## 一、为什么换成 Vitro

自研版能画出磨砂 + 彩虹边，但真折射一直没打通，核心死路是「采集到的屏幕纹理」这一层：

- `GraphicsLayer` 采集出来的是**硬件位图**，直接当 `BitmapShader` 的输入取到的是黑块；
- 自己按屏幕坐标切片又会踩到「只画出左上角一小块」，换过三种坐标约定都一样。

Vitro 用的是「`glassBackdrop` 把背景内容录进 `GraphicsLayer` + 着色器里按场景采样」的成熟管线，
并且把离屏采样、两趟模糊、阴影、按压反馈、邻近玻璃融合都做好了，API 也和 SwiftUI 的
`glassEffect` 对齐。与其继续修自研管线，不如直接用它。

## 二、来源与版本

| 项 | 值 |
|---|---|
| 上游 | https://github.com/itshedimisawi/vitro |
| 版本 | `Release 0.1.0`（commit `3384d0432dc2a7fe69804873f607af941c2d1088`，2026-10-24） |
| 许可证 | Apache-2.0，副本见 `app/src/main/java/io/LICENSE-vitro.txt` |
| 落点 | `app/src/main/java/io/github/itshedimisawi/vitro/`（34 个 .kt，包名保持上游不变） |

## 三、编译基线：与上游一致（compileSdk 37）

| 项 | 版本 |
|---|---|
| Gradle | 9.6.0（wrapper 已重建，分发包走腾讯镜像） |
| AGP | 9.4.1（内置 Kotlin，不再单独应用 `org.jetbrains.kotlin.android`） |
| Kotlin | 2.4.21（`kotlin.plugin.compose`；AGP 内置 Kotlin 由它带到同一版本） |
| Compose | BOM `2026.09.00` → ui / foundation / runtime `1.12.1`，material3 `1.4.0` |
| KSP / Hilt / Room | `2.3.12` / `2.60.1` / `2.8.5` |
| compileSdk / targetSdk / minSdk | 37 / 36 / 26 |

1. 本机 SDK 需 `platforms/android-37`（官方 `platform-37.0_r02.zip`）与 `build-tools/37.0.0`。
   早期为了 AGP 8.2.2 把 `AndroidVersion.ApiLevel=37.0` 改成 `37` 的本地补丁仍保留；
   AGP 9.4.1 原生支持 API 37，重装 SDK 后若 `37.0` 也能识别，就可以去掉这个补丁。
2. Compose 1.12.1 的 AAR 元数据写死 `minCompileSdk=37`、`minAndroidGradlePluginVersion=9.1.0`，
   所以 **AGP 不能停留在 8.x**；本仓库直接对齐上游的 AGP 9.4.1 + Gradle 9.6。
3. AGP 9 默认启用内置 Kotlin：`app/build.gradle.kts` 去掉了 `org.jetbrains.kotlin.android`
   和 `android.kotlinOptions {}`，`jvmTarget` 跟随 `compileOptions`（Java 21）。
   KSP 用 `2.3.12`，Hilt 用 `2.60.1`（AGP 9 的最低兼容要求分别是 2.3.6 / 2.59.2）。
4. `gradle.properties` 不再需要 `android.suppressUnsupportedCompileSdk`；
   `gradlew.bat` 的坏 wrapper jar 已用 Gradle 9.6 重新生成，现在可直接运行 `.\gradlew.bat ...`。
5. 未使用的 `navigation-compose` 与 `accompanist-systemuicontroller` 依赖已移除（源码中没有引用）。
   `hiltViewModel` 改用新包 `androidx.hilt.lifecycle.viewmodel.compose`。

## 四、与上游一致（兼容改动已撤回）

之前为了在 Compose 1.7.5 上编译，删掉了 `internal/node/BackdropNode.kt`、
`internal/node/ScrollEdgeNode.kt` 里的 `UnplacedAwareModifierNode` 与 `onUnplaced()`。
升级到 Compose 1.12.1 后已按上游原样恢复：

```kotlin
// Unplaced content is not drawn, so there is nothing to read until it is placed again.
override fun onUnplaced() {
    reader.pause()
}
```

vendor 源码现在与上游 commit `3384d04` 完全一致，不再有本地兼容差异；
末尾的 `onUnplaced()` 会暂停按背景亮度取材质的 `ToneReader`，本项目强制指定
`GlassAppearance` 时它不影响视觉。

## 五、接线方式

`MainActivity.CourseApp()`：

```kotlin
val glassBackdrop = rememberGlassBackdrop()   // 折射源
...
Box(Modifier.fillMaxSize()) {
    // 1. 场景层：底色、壁纸、遮罩和当前课表内容统一录入 backdrop
    Box(Modifier.fillMaxSize().glassBackdrop(glassBackdrop)) { ...背景 + 页面... }

    // 2. 两条栏是 backdrop 的兄弟节点、直接放在根 Box 里；玻璃从这里采样
    Box(Modifier.align(Alignment.TopCenter)...) { ... .glassEffect(glassBackdrop, glassMaterial, RoundedCornerShape(28.dp)) ... }
    Box(Modifier.align(Alignment.BottomCenter)...) { ... .glassEffect(glassBackdrop, glassMaterial, RoundedCornerShape(28.dp)) ... }
}
```

要点：

- 玻璃必须画在 backdrop **之后**、且不在 backdrop 子树里，否则会自己采样自己；
- **不要用 Material3 `Scaffold` 摆栏**。Scaffold 内部的全屏 `Surface` 会附加一个
  `Modifier.pointerInput(Unit) {}` 节点；Compose 命中测试里它是最上层的直接命中，下面的兄弟页面
  （backdrop 子树）就再也收不到 down/move/up，表现为整页不能滚动、除栏按钮外全部失效。
  用普通 `Box` + `Alignment` 摆放即可，既保留玻璃层级，也不挡页面；
- 壁纸、遮罩、课程网格必须属于同一个 backdrop；课表的滚动内容放进自己的纵向滚动容器，
  首尾留白也作为滚动内容，这样首尾课程可完整露出，中间课程可以真实地从顶栏和底栏后方经过；
- 页面视口在顶部裁到顶栏外壳上沿（状态栏 + 顶栏 8dp 外边距），顶部留白相应扣除：
  初始布局位置不变，滚动内容仍从玻璃后方经过，但不会画到状态栏图标底下；
- 外壳在首次布局前使用现有尺寸令牌与系统 inset，布局完成后用 `onGloballyPositioned`
  记录包含安全区的顶栏/底栏实际高度，并回填给课表滚动留白，适配不同设备的系统栏尺寸；
- 时间轴和七天课程列共享滚动状态与留白，横向周分页仍保持原有行为；
- 原来的 `.shadow()` + 磨砂渐变 + 彩虹描边全部删掉，Vitro 自带阴影 / 高光 / 色散；
- 底色渐变从根 `Box` 挪进了 backdrop 层 —— 没有壁纸的页面（课程管理 / 我的）也要有东西可折射；
- `scrollEdgeEffect(style = ScrollEdgeEffectStyle.Shaded, isTopHidden = true)` 挂在**页面视口**
  （backdrop 内的内容层，不是壁纸层）：只让内容在**底栏**下面按 iOS 方式压暗渐隐。
  顶边关闭的原因：Vitro 的 veil 是**全宽矩形**，会在圆角顶栏两侧和状态栏下方留下方角暗带，
  与圆角玻璃/系统栏不和谐；要开顶边需要按顶栏圆角裁剪 veil（改 ScrollEdgeNode 或加遮罩），
  当前先用“顶部保持真实折射、底部渐隐”的组合；
- 周选择面板 / 课程详情抽屉复用同一个 backdrop 和玻璃材质，玻璃外再叠一层半透明 `surface`
  （面板 0.76、抽屉 0.88）保证正文可读；
- **覆盖层的 scrim 必须是面板/抽屉的父层，不能是兄弟层**：非交互玻璃的
  `sharePointerInputWithSiblings()` 返回 true，兄弟遮罩会一起进入命中路径，点周格/编辑按钮会
  变成“点到遮罩”，直接把面板关掉。把遮罩改成父层后，子内容先消费点击，遮罩只接住空白区域；
- `glassEffectContainer(spacing = 0.dp)` **不会融合**（官方 KDoc：零间距只是合并绘制、省一点开销）。
  要融合得 `spacing > 0` 且玻璃材质相同，组内会使用 lead 的 `tint`——彩色周数胶囊会因此失去强调色。

### 材质参数（对着原型图调出来的）

| | 浅色 | 深色 |
|---|---|---|
| appearance | `Light` | `Dark` |
| brightness | **0.30**（默认 0.5） | 0.55 |
| tint | 无 | `#0B0B10` α 0.72 |
| dispersion | 0.05（默认 0） | 0.05 |
| blurRadius | 7.dp（默认 2.5dp） | 7.dp |

- `brightness` 调低是因为默认 0.5 会把亮背景糊成一片死白：实测栏内 227，原型是 207；
  降到 0.30 后实测玻璃体 204 / 高光 231，和原型的 207 / 228 基本重合。
- 深色上游默认材质对**亮背景**会留亮（实测 150+），加 72% 的深色染色后实测 60（顶栏）/ 33（底栏），
  与原型 57 / 25 一致，白字对比度也就够了。
- `dispersion` 默认 0（完全没有色散），而原型标注了「边缘色散 / 色彩溢出」，所以开 0.05。
- `blurRadius` 从 2.5dp 提到 7dp，贴近原型的磨砂程度。

### 文字与主题

- 亮玻璃上的次级文字：`textMuted` 只有约 2.6:1，所以学期名、未选中 Tab 改用 `textSecondary`
  （`barMutedInk` / `navInactiveInk`）；暗玻璃继续用原 `muted`。
- `CompositionLocalProvider(LocalGlassDarkTheme provides isDarkMode)`：让 Vitro 跟随 App 自己的
  深浅设置，而不是系统设置。

## 六、验证

| 项 | 结果 |
|---|---|
| `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest`（AGP 9.4.1 / Gradle 9.6 / Compose 1.12.1） | 通过 |
| 真机 `10CG5J0Q0F004E7` 冷启动截图 | 浅 / 深两套栏材质与 1.7.5 基线一致，数值仍与原型对齐（见上表） |
| 真机交互回归（2026-10-27，Compose 1.12.1） | 首页/管理/我的页纵向滚动、课程点击与详情返回、长按菜单、底部 Tab、周面板、横向翻周、主题切换、作息设置页全部通过 |
| 启动周数 | 冷启动直接显示「第 9 周」，不再先闪兜底日期的「第 18 周」 |
| 滚动边缘渐隐 | 只开**底边**（`Shaded`）；顶边关闭，避免全宽 veil 在圆角顶栏两侧留下方角暗带 |
| 周选择面板玻璃 | 展开后玻璃 + 76% surface；周格选择、确认、外部点击关闭均正常（修复了玻璃抢命中后子控件点不到的问题） |
| 课程详情抽屉玻璃 | 玻璃 + 88% surface；编辑进入/退出、复制/删除按钮、拖动关闭、外部点击关闭均正常 |
| 滚动 + 玻璃动态折射 | 课程色块与文字从顶栏/底栏后方经过，玻璃内采样随内容移动，栏内无黑块或固定纹理 |
| 纵向滚动的 `gfxinfo`（最终） | p50 11ms / p90 15ms / p99 17ms，janky 1.54%，missed vsync 0，GPU p90 8ms |
| 面板展开/收起（玻璃） | p50 12ms / p90 17ms / p99 40ms，janky 3.35%，missed vsync 0；无玻璃的旧动画也在同一量级，玻璃不是瓶颈 |
| `logcat -b crash` | 无本应用 FATAL / ANR |

上一轮 Compose 1.7.5 的纵向滚动是 p90 16ms / janky 2.81%，混合翻周是 p90 18ms / janky 6.70%；
升级后两组都更低。横向翻周仍包含页面切换动画，不代表纯滚动性能。

交互回归必须连同命中测试一起验：栏虽然能点，不代表页面能收到触摸。验收时至少覆盖首页滚动、
课程点击/返回、管理/我的页列表滚动、普通设置按钮、横向翻周和玻璃内动态采样；没有连接设备时
不得把静态截图当作动态折射或交互通过的证据。

Android 13（API 33）及以上应使用 Vitro 的 AGSL 动态折射；Android 12 及以下使用库内的
兼容渲染路径，只验证内容可见性、交互和无崩溃，不宣称与 AGSL 具有相同的边缘光学效果。

## 七、后续可做（2026-10-27 已落地）

- [x] **`scrollEdgeEffect`（底边）**：页面视口用 `ScrollEdgeEffectStyle.Shaded` + `isTopHidden = true`。
  底边内容滚到玻璃栏下面时压暗渐隐；顶边关闭，因为 veil 是全宽矩形，会越过圆角顶栏在状态栏
  下方形成方角暗带，和圆角玻璃不和谐。若以后要顶边，需要先按顶栏轮廓裁剪 veil。
- [x] **周选择面板玻璃**：同一个 `backdrop` + 玻璃，surface 染色 76%；形变容器用
  `glassEffect(... RoundedCornerShape(cornerDp))`，外层的 `clip` 跟着 spring 形变。
- [x] **课程详情抽屉玻璃**：同一个 `backdrop` + 玻璃，surface 染色 88%；按钮、拖动、外部点击都正常。
- [x] **覆盖层命中修复**：遮罩从「面板/抽屉的兄弟」改成「父层」，否则非交互玻璃的
  `sharePointerInputWithSiblings()` 会把遮罩拉进命中路径，点周格/编辑按钮会误关面板。
- [ ] **`glassEffectContainer` 顶栏 + 周数胶囊融合**：**未采用**。Vitro 的 KDoc 明确
  `spacing = 0.dp` 时不会融合（只合并绘制）；要融合需要 `spacing > 0` 且材质相同，而组内会使用
  lead 的 tint——橙色/蓝色周数胶囊会失去强调色，主控件的对比度会下降。当前胶囊本来就整体叠在
  顶栏区域内，没有需要靠融合消除的“两个孤岛”缝隙。若确实要做，建议改成 clear glass 胶囊 +
  强调色文字，作为单独设计评审。
- [x] **上游更新**：截至 2026-10-27，上游最新仍是 `Release 0.1.0` / commit `3384d04`，没有新版本可升。

## 八、更新上游的步骤

1. clone 上游，记录新 commit；
2. 用新的 `vitro/src/main/kotlin/io` 覆盖 `app/src/main/java/io`；
3. Compose 1.12.1 已支持 `UnplacedAwareModifierNode`，**不需要**再删接口/`onUnplaced()`；
4. 同步 `LICENSE-vitro.txt`；
5. `.\gradlew.bat :app:assembleDebug` + 真机截图复核材质有没有变。
