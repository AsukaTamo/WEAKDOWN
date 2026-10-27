# 交接：顶栏/底栏液态玻璃（Vitro 方案）

> 一句话现状：**顶栏 / 底栏已经是 Vitro 的 AGSL 液态玻璃，app 用 `compileSdk = 37` 编译，
> 浅色/深色两套真机验证通过。** 改动全部在分支 `Lingfan666/feature-liquid-glass` 上，**没有合入 main**。
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
| `app/src/main/java/com/example/courseapp/MainActivity.kt` | 背景层标成 `glassBackdrop`；顶栏/底栏改 `.glassEffect(...)`；删掉原磨砂渐变/彩虹描边/`.shadow()`；次级文字色换 `textSecondary`；`LocalGlassDarkTheme` 跟随 App 主题 | 玻璃要能采样背景，且亮玻璃上 muted 灰只有 2.6:1 |
| `ui/components/LiquidGlass.kt` | **删除** | 自研着色器的真折射一直没打通，改走 Vitro；历史在 `404d2ee` 里可找回 |
| `app/build.gradle.kts` | `compileSdk 36 → 37` | 与 Vitro 的编译基线一致（`targetSdk` 仍 36） |
| `gradle.properties` | 新增 `android.suppressUnsupportedCompileSdk=36,37` | AGP 8.2.2 官方只测到 34，仅压提示 |
| `docs/VITRO_LIQUID_GLASS.md` | 新增 | 方案、参数标定、验证、更新上游步骤 |
| `docs/LIQUID_GLASS_TODO.md` | 顶部加状态说明，文件清单更新 | 旧的自研路线归档为历史记录 |
| `docs/PROJECT_STRUCTURE.md` | 补 vendor 小节、compileSdk 更新 | 结构说明别过期 |

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

| 命令 | 结果 |
|---|---|
| `gradle :app:assembleDebug` | ✅（compileSdk 37，无 error） |
| `gradle :app:testDebugUnitTest` | ✅ |
| 装机 + 冷启动 + 截图（浅/深） | ✅ 两条栏材质正常，无数值异常 |
| 滚动 1s 后 `dumpsys gfxinfo` | p50 8ms / p90 17ms / p99 23ms，janky 4.77%，missed vsync 0 |

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
4. **性能贴线**：p90 = 17ms。若想更稳，可以只在有壁纸时开玻璃、其它页面回到纯色/磨砂。
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
2. 需要调手感就改 `brightness` / `tint` / `dispersion` / `blurRadius`，字段含义见 `Glass.kt` 的 KDoc；
3. 构建 → 装机 → 冷启动 → 截图（命令见 `AGENT.md`「真机调试方法」）；
4. 对着原型图看三件事：玻璃体的亮度、边缘高光是否成圈、背景是否被真正折射（拖动课表时看栏下方内容有没有被「挤」）。
