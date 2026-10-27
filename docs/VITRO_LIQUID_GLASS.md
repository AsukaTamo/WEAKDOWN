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

## 三、编译基线：compileSdk 37（36 保留）

1. 本机 SDK 新增 `platforms/android-37`，来自官方 `platform-37.0_r02.zip`；
   `platforms/android-36`、`platforms/android-36.1`、`build-tools/37.0.0` 原样保留。
2. **该平台的 `source.properties` 需要一处本地改动**：`AndroidVersion.ApiLevel` 从 `37.0` 改成 `37`。
   AGP 8.2.2 只认整数 API 等级，不改会直接报
   `Failed to find Platform SDK with path: platforms;android-37`。原文件备份在同目录
   `source.properties.bak`；以后升级到原生支持 `37.0` 次版本号的 AGP 9 时应改回。
3. `app/build.gradle.kts`：`compileSdk = 37`（`targetSdk` 仍是 36，不改运行时行为）。
4. `gradle.properties`：`android.suppressUnsupportedCompileSdk=36,37`
   —— AGP 8.2.2 官方只测到 34，这行只是压掉升级提示；实际用到的最高接口是 API 33。

**为什么不用上游自己的构建**：上游用 Gradle 9.6 + AGP 9.4.1 + Kotlin 2.4.21 + Compose 1.12.1。
本机 `services.gradle.org` 的 Gradle 分发包下载卡在 0 字节（`gradle-9.6.0-bin.zip.part` 一直是 0），
所以改成把源码 vendor 进本项目、用本项目的 Compose 1.7.5 编译。这个组合只差下面一处 API。

## 四、和上游的唯一差异

`internal/node/BackdropNode.kt`、`internal/node/ScrollEdgeNode.kt`：

- 删掉 `UnplacedAwareModifierNode` 接口和 `onUnplaced()` —— 该接口是 Compose 1.9 才引入的，
  当前项目是 1.7.5；
- 影响面：`onUnplaced()` 只做 `reader.pause()`（暂停按背景亮度取材质）。本项目对两条栏
  **强制指定 `GlassAppearance`**，不会去读 tone，所以这处暂停有没有都无所谓。

升级 Compose 之后可以把这两处改动撤回。

## 五、接线方式

`MainActivity.CourseApp()`：

```kotlin
val glassBackdrop = rememberGlassBackdrop()   // 折射源
...
Box(Modifier.fillMaxSize()) {
    // 1. 背景层：底色渐变 + 壁纸 + 遮罩，整块标成 backdrop
    Box(Modifier.fillMaxSize().glassBackdrop(glassBackdrop)) { ...背景... }

    // 2. 栏是它的兄弟节点，玻璃从这里采样
    Scaffold(topBar = { ... .glassEffect(glassBackdrop, glassMaterial, RoundedCornerShape(28.dp)) ... })
}
```

要点：

- 玻璃必须画在 backdrop **之后**、且不在 backdrop 子树里，否则会自己采样自己；
- 原来的 `.shadow()` + 磨砂渐变 + 彩虹描边全部删掉，Vitro 自带阴影 / 高光 / 色散；
- 底色渐变从根 `Box` 挪进了 backdrop 层 —— 没有壁纸的页面（课程管理 / 我的）也要有东西可折射。

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
| `:app:assembleDebug`（compileSdk 37） | 通过 |
| `:app:testDebugUnitTest` | 通过 |
| 真机 `10CG5J0Q0F004E7` 冷启动截图 | 浅色 / 深色两套栏材质正常，数值与原型对齐（见上表） |
| 滚动 1s 的 `gfxinfo` | p50 8ms / p90 17ms / p99 23ms，janky 4.77%，missed vsync 0，GPU p90 6ms |

## 七、后续可做

- 课程详情抽屉、周选择面板套同一个 `backdrop`（现在仍是实色面板）；
- `glassEffectContainer` 让「顶栏 + 周数胶囊」融合成一整块玻璃；
- `scrollEdgeEffect`（内容滚到栏下渐隐）暂时没接：课表内容本来就被 Scaffold 挡在栏外；
- 想用上游 1.12 之后的新 API，需要把 Compose 升到对应版本，并撤回第四节的兼容改动。

## 八、更新上游的步骤

1. clone 上游，记录新 commit；
2. 用新的 `vitro/src/main/kotlin/io` 覆盖 `app/src/main/java/io`；
3. 重新套用第四节的 `UnplacedAwareModifierNode` 兼容改动（Compose 升级后可省）；
4. 同步 `LICENSE-vitro.txt`；
5. `:app:assembleDebug` + 真机截图复核材质有没有变。
